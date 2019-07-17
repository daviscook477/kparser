import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ScmlConverter {

	private static final int KleiHash(String str) {
		if (str == null) {
			return 0;
		}
		int num = 0;
		for (int i = 0; i < str.length(); i++) {
			num = ((int) str.toLowerCase().charAt(i)) + (num << 6) + (num << 16) - num;
		}
		return num;
	}
	
	private static final int BILD_VERSION = 10;
	private static final int ANIM_VERSION = 5;
	private static final int MS_PER_S = 1000;

	private String path;
	private Document scml;
	private boolean initialized = false;

	public static Document loadSCML(String path) throws IOException, SAXException, ParserConfigurationException {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
		Document scml = documentBuilder.parse(new File(path));
		return scml;
	}

	public void init(String path, Document scml) {
		if (initialized) return;
		this.path = path;
		this.scml = scml;
		this.initialized = true;
	}

	private Element firstMatching(String name) {
		NodeList list = scml.getElementsByTagName(name);
		if (list == null || list.getLength() < 1) throw new RuntimeException("Could not find any tags with name");
		return (Element) list.item(0);
	}

	private Element firstMatching(Element parent, String name) {
		NodeList list = parent.getElementsByTagName(name);
		if (list.getLength() < 1) throw new RuntimeException("Could not find any tags with name");
		return (Element) list.item(0);
	}

	private String nameOfEntity() {
		Element entity = firstMatching("entity");
		return entity.getAttribute("name");
	}

	private void writeInt(DataOutputStream out, int val) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		byte[] asBytes = buffer.putInt(val).array();
		out.write(asBytes);
	}

	private void writeFloat(DataOutputStream out, float val) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		byte[] asBytes = buffer.putFloat(val).array();
		out.write(asBytes);
	}

	private void writeString(DataOutputStream out, String val) throws IOException {
		writeString(out, val, true);
	}

	private void writeString(DataOutputStream out, String val, boolean writeLength) throws IOException {
		if (writeLength) {
			writeInt(out, val.length());
		}
		byte[] asBytes = val.getBytes(StandardCharsets.US_ASCII);
		out.write(asBytes);
	}

	private String getFileExtension(File file) {
		String filePath = file.getAbsolutePath();
		int i = filePath.lastIndexOf('.');
		if (i > 0) {
			return filePath.substring(i + 1);
		} else {
			return "";
		}
	}

	/*
	 * may throw an exception if file name is improper formatted
	 */
	private String getFileFrameCount(File file) {
		String filePath = file.getAbsolutePath();
		int i = filePath.lastIndexOf('.');
		String subFilePath = filePath.substring(0, i);
		int j = subFilePath.lastIndexOf('_');
		String frameCount = subFilePath.substring(j + 1);
		return frameCount;
	}

	private void setSymbolsAndFrames(BILD BILDData, String baseTexturePath, String ignoredFile) {
		File textureFolder = new File(baseTexturePath);
		File[] children = textureFolder.listFiles();
		BILDData.symbols = 0;
		BILDData.frames = -1; // frames count starts from 0
		if (children == null) return;

		for (File child : children) {
			if (getFileExtension(child).equals("png")) {
				BILDData.frames++;
				try {
					String frameCount = getFileFrameCount(child);
					if (Integer.parseInt(frameCount) == 0) {
						BILDData.symbols++;
					}
				} catch (IndexOutOfBoundsException | NumberFormatException e) {
					if (child.getPath().equals(ignoredFile)) continue;
					throw new RuntimeException("Improperly formatted texture name " + child.getName());
				}
			}
		}
	}

	private static class AtlasEntry {
		public String name;
		public boolean rotate;
		int x, y;
		int w, h;
		int originX, originY;
		int offsetX, offsetY;
		int index;
	}

	private String getOne(String line) {
		int i = line.lastIndexOf(':');
		String after = line.substring(i + 1);
		return after.trim();
	}

	private String getFirst(String line) {
		int i = line.lastIndexOf(':');
		String after = line.substring(i + 1);
		String[] tokens = after.split(",");
		return tokens[0].trim();
	}

	private String getSecond(String line) {
		int i = line.lastIndexOf(':');
		String after = line.substring(i + 1);
		String[] tokens = after.split(",");
		return tokens[1].trim();
	}

	private AtlasEntry attemptParseEntry(BufferedReader reader) throws IOException {
		String name = reader.readLine();
		boolean rotate = Boolean.parseBoolean(getOne(reader.readLine()));
		String xy = reader.readLine();
		int x = Integer.parseInt(getFirst(xy));
		int y = Integer.parseInt(getSecond(xy));
		String size = reader.readLine();
		int w = Integer.parseInt(getFirst(size));
		int h = Integer.parseInt(getSecond(size));
		String origin = reader.readLine();
		int originX = Integer.parseInt(getFirst(origin));
		int originY = Integer.parseInt(getSecond(origin));
		String offset = reader.readLine();
		int offsetX = Integer.parseInt(getFirst(offset));
		int offsetY = Integer.parseInt(getSecond(offset));
		int index = Integer.parseInt(getOne(reader.readLine()));
		AtlasEntry entry = new AtlasEntry();
		entry.name = name;
		entry.rotate = rotate;
		entry.x = x;
		entry.y = y;
		entry.w = w;
		entry.h = h;
		entry.originX = originX;
		entry.originY = originY;
		entry.offsetX = offsetX;
		entry.offsetY = offsetY;
		entry.index = index;
		return entry;
	}

	private List<AtlasEntry> getOrderedAtlasEntries(BufferedReader reader) throws IOException {
		// first 6 lines are unnecessary defs
		reader.readLine();
		reader.readLine();
		reader.readLine();
		reader.readLine();
		reader.readLine();
		reader.readLine();
		List<AtlasEntry> entries = new ArrayList<>();
		boolean done = false;
		while (!done) {
			try {
				AtlasEntry entry = attemptParseEntry(reader);
				entries.add(entry);
			} catch (Exception e) {
				done = true;
			}
		}
		return entries;
	}

	private Map<String, Integer> getHashTable(List<AtlasEntry> entries) {
		Map<String, Integer> hashTable = new HashMap<>();
		for (AtlasEntry entry : entries) {
			if (entry.index == 0) {
				hashTable.put(entry.name, KleiHash(entry.name));
			}
		}
		return hashTable;
	}

	private Map<String, Integer> getHistogram(List<AtlasEntry> entries) {
		Map<String, Integer> histogram = new HashMap<>();
		for (AtlasEntry entry : entries) {
			if (!histogram.containsKey(entry.name)) {
				histogram.put(entry.name, 1);
			} else {
				histogram.put(entry.name, histogram.get(entry.name) + 1);
			}
		}
		return histogram;
	}

	private Map<AtlasEntry, Element> getAtlasMap(List<AtlasEntry> orderedAtlasEntries) {
		Element folder = firstMatching("folder");
		NodeList children = folder.getChildNodes();
		List<Element> elementList = new ArrayList<>();
		for (int i = 0; i < children.getLength(); i++) {
			Node file = children.item(i);
			if (file instanceof Element) {
				elementList.add((Element) file);
			}
		}
		Map<AtlasEntry, Element> map = new HashMap<>();
		// this requires that no other image files be in the directory that are put into the atlas that aren't referenced in the file
		for (int i = 0; i < elementList.size(); i++) {
			for (AtlasEntry entry : orderedAtlasEntries) {
				if (elementList.get(i).getAttribute("name").equals(entry.name + '_' + entry.index)) {
					map.put(entry, elementList.get(i));
				}
			}
		}
		return map;
	}

	/**
	 * Packs the BILD file given the scml file
	 * Note that this will not work with *any* scml file
	 * There is the additional requirement that each individual animated component in the
	 * textures have it's own name with a postfix of the animation frame for it.
	 *
	 * For example if you were animating a bouncing ball, all the frames for the ball would need
	 * to have the same name "ball" at the start and then frame 0 would be "ball_0",
	 * frame 1 would be "ball_1" ... frame n would be "ball_n"
	 *
	 * If this invariant is not maintained, I have no idea if packBILD will work
	 */
	public void packBILD(String baseTexturePath) throws IOException {
		if (!initialized) throw new RuntimeException("Must initialize ScmlConverter before packing");
		TexturePacker.Settings settings = new TexturePacker.Settings();
		settings.square = true;
		String name = nameOfEntity();
		TexturePacker.process(settings, baseTexturePath, baseTexturePath, name);
		String imgPath = baseTexturePath + name + ".png";
		String atlasPath = baseTexturePath + name + ".atlas";
		// read the produced atlas file to know what data must be included in the BILD file
		BufferedReader reader = new BufferedReader(new FileReader(atlasPath));

		BILD BILDData = new BILD();
		BILDData.version = BILD_VERSION;
		setSymbolsAndFrames(BILDData, baseTexturePath, imgPath);
		BILDData.name = name;

		List<AtlasEntry> orderedAtlasEntries = getOrderedAtlasEntries(reader);
		Map<String, Integer> hashTable = getHashTable(orderedAtlasEntries);
		Map<String, Integer> histogram = getHistogram(orderedAtlasEntries);
		Map<AtlasEntry, Element> atlasMap = getAtlasMap(orderedAtlasEntries);

		BILDData.symbolsList = new ArrayList<>();
		int symbolIndex = -1;
		BufferedImage packedImg = ImageIO.read(new File(imgPath));
		int imgWidth = packedImg.getWidth();
		int imgHeight = packedImg.getHeight();
		String lastName = null;
		for (AtlasEntry entry : orderedAtlasEntries) {
			if (lastName == null || !entry.name.equals(lastName)) {
				BILDSymbol symbol = new BILDSymbol();
				symbol.hash = hashTable.get(entry.name);
				symbol.path = hashTable.get(entry.name);
				symbol.color = 0; // no Klei files use color other than 0 so fair assumption is it can be 0
				// only check in decompile for flag checks flag = 8 for a layered anim (which we won't do)
				// so should be safe to leave flags = 0
				// have seen some Klei files in which flags = 1 for some symbols but can't determine what that does
				symbol.flags = 0;
				symbol.numFrames = histogram.get(entry.name);
				symbol.framesList = new ArrayList<>();
				BILDData.symbolsList.add(symbol);
				symbolIndex++;
				lastName = entry.name;
			}
			BILDFrame frame = new BILDFrame();
			frame.sourceFrameNum = entry.index;
			// duration is always 1 because the frames for a symbol always are numbered incrementing by 1
			// (or at least that's why I think it's always 1 in the examples I looked at)
			frame.duration = 1;
			// this value as read from the file is unused by Klei code and all example files have it set to 0 for all symbols
			frame.buildImageIdx = 0;
			float x1 = (float) entry.x / imgWidth;
			float x2 = (float) (entry.x + entry.w) / imgWidth;
			float y1 = (float) entry.y / imgHeight;
			float y2 = (float) (entry.y + entry.h) / imgHeight;
			frame.x1 = x1;
			frame.y1 = y1;
			frame.x2 = x2;
			frame.y2 = y2;
			// do not set frame.time since it was a calculated property and not actually used in kbild
			frame.pivotWidth = entry.w * 2;
			frame.pivotHeight = entry.h * 2;
			frame.pivotX = -(Float.parseFloat(atlasMap.get(entry).getAttribute("pivot_x")) - 0.5f) * frame.pivotWidth;
			frame.pivotY = (Float.parseFloat(atlasMap.get(entry).getAttribute("pivot_y")) - 0.5f) * frame.pivotHeight;
			BILDData.symbolsList.get(symbolIndex).framesList.add(frame);
		}

		DataOutputStream out = new DataOutputStream(new FileOutputStream(path + name + "_build.bytes"));
		writeString(out, "BILD", false);
		// have to use custom write for noncharacter strings because need to write in little endian
		writeInt(out, BILD_VERSION);
		System.out.println("version="+ BILD_VERSION);
		writeInt(out, BILDData.symbols);
		System.out.println("symbols="+BILDData.symbols);
		writeInt(out, BILDData.frames);
		System.out.println("frames="+BILDData.frames);
		writeString(out, BILDData.name);
		System.out.println("name="+BILDData.name);
		int i = 0;
		for (BILDSymbol symbol : BILDData.symbolsList) {
			System.out.println("symbol " + i + "=("+symbol.hash+","+symbol.path+","+symbol.color+","+symbol.flags+","+symbol.numFrames+")");
			writeInt(out, symbol.hash);
			writeInt(out, symbol.path);
			writeInt(out, symbol.color);
			writeInt(out, symbol.flags);
			writeInt(out, symbol.numFrames);
			int j = 0;
			for (BILDFrame frame : symbol.framesList) {
				writeInt(out, frame.sourceFrameNum);
				writeInt(out, frame.duration);
				writeInt(out, frame.buildImageIdx);
				writeFloat(out, frame.pivotX);
				writeFloat(out, frame.pivotY);
				writeFloat(out, frame.pivotWidth);
				writeFloat(out, frame.pivotHeight);
				writeFloat(out, frame.x1);
				writeFloat(out, frame.y1);
				writeFloat(out, frame.x2);
				writeFloat(out, frame.y2);
				j++;
			}
			i++;
		}

		writeInt(out, hashTable.entrySet().size());
		for (Map.Entry<String, Integer> hashPair : hashTable.entrySet()) {
			System.out.println(hashPair.getValue()+"="+hashPair.getKey());
			writeInt(out, hashPair.getValue());
			writeString(out, hashPair.getKey());
		}
		out.close();
	}

	private Element getMainline(NodeList timelines) {
		for (int i = 0; i < timelines.getLength(); i++) {
			if (!(timelines.item(i) instanceof Element)) {
				System.out.println("skipping non-element tag");
				continue;
			}
			Element ele = (Element) timelines.item(i);
			if (ele.getTagName().equals("mainline")) {
				return ele;
			}
		}
		throw new RuntimeException("SCML format exception - no mainline tag child of animation");
	}

	private Map<Integer, Element> getTimelineMap(NodeList timelines) {
		Map<Integer, Element> map = new HashMap<>();
		for (int i = 0; i < timelines.getLength(); i++) {
			if (!(timelines.item(i) instanceof Element)) {
				continue;
			}
			Element ele = (Element) timelines.item(i);
			if (ele.getTagName().equals("timeline")) {
				map.put(Integer.parseInt(ele.getAttribute("id")), ele);
			}
		}
		return map;
	}

	private void setAggregateData(ANIM ANIMData) {
		Element entity = firstMatching("entity");
		NodeList animations = entity.getChildNodes();
		int maxVisibleSymbolFrames = 0;
		for (int anim = 0; anim < animations.getLength(); anim++) {
			if (!(animations.item(anim) instanceof Element)) {
				System.out.println("skipping non-element child");
				continue;
			}
			Element animation = (Element) animations.item(anim);
			if (!animation.getTagName().equals("animation")) {
				throw new RuntimeException("SCML format exception - all children of entity must be animation tags");
			}
			NodeList timelines = animation.getChildNodes();
			Element mainline = getMainline(timelines);
			NodeList keyFrames = mainline.getChildNodes();
			for (int frame = 0; frame < keyFrames.getLength(); frame++) {
				if (!(keyFrames.item(frame) instanceof Element)) {
					System.out.println("skipping non-element child");
					continue;
				}
				Element key = (Element) keyFrames.item(frame);
				if (!key.getTagName().equals("key")) {
					throw new RuntimeException("SCML format exception - all children of animation must be key tags");
				}
				NodeList objects = key.getChildNodes();
				for (int object = 0; object < objects.getLength(); object++) {
					if (!(objects.item(object) instanceof Element)) {
						continue;
					}
					Element objectRef = (Element) objects.item(object);
					if (!objectRef.getTagName().equals("object_ref")) {
						throw new RuntimeException("SCML format exception - all chilredn of key must be object_ref tags");
					}
					if (objects.getLength() > maxVisibleSymbolFrames) {
						maxVisibleSymbolFrames = objects.getLength();
					}
				}
			}
		}
		ANIMData.anims = animations.getLength();
		// these two bits of data are ignored by Klei/the kanim format so don't bother calculating them
		ANIMData.frames = 0;
		ANIMData.elements = 0;
		ANIMData.maxVisSymbolFrames = maxVisibleSymbolFrames;
	}

	private Map<Integer, Element> getFileMap() {
		Element folder = firstMatching("folder");
		Map<Integer, Element> fileMap = new HashMap<>();
		NodeList files = folder.getChildNodes();
		for (int i = 0; i < files.getLength(); i++) {
			if (!(files.item(i) instanceof Element)) {
				continue;
			}
			Element file = (Element) files.item(i);
			if (!file.getTagName().equals("file")) {
				throw new RuntimeException("SCML format exception - all children of folder must be file tags");
			}
			int id = Integer.parseInt(file.getAttribute("id"));
			fileMap.put(id, file);
		}
		return fileMap;
	}

	private Element getFrameFromTimeline(Element timeline, int frame) {
		NodeList keyList = timeline.getChildNodes();
		for (int i = 0; i < keyList.getLength(); i++) {
			if (!(keyList.item(i) instanceof Element)) {
				continue;
			}
			Element key = (Element) keyList.item(i);
			if (!key.getTagName().equals("key")) {
				throw new RuntimeException("SCML format exception - all children of timeline must be key tags");
			}
			if (Integer.parseInt(key.getAttribute("id")) == frame) {
				return key;
			}
		}
		throw new RuntimeException("SCML parse exception - expected frame " + frame + " to exist in timeline " + timeline.getAttribute("id") + " but it did not");
	}

	private String getImageName(String image) {
		int i = image.lastIndexOf('_');
		return image.substring(0, i);
	}

	private int getImageIndex(String image) {
		int i = image.lastIndexOf('_');
		return Integer.parseInt(image.substring(i + 1));
	}

	private Point2D.Float rotateAbout(float pivotX, float pivotY, float angle, Point2D.Float point, float scaleX, float scaleY) {
		// order of transformations applied is:
		// 1. -pivot
		// 2. rotate angle
		// 3. scale
		// 4. +pivot
		float sin = (float) Math.sin(angle);
		float cos = (float) Math.cos(angle);
		Point2D.Float point1 = new Point2D.Float(point.x - pivotX, point.y - pivotY);
		Point2D.Float point2 = new Point2D.Float(point1.x * cos - point1.y * sin, point1.x * sin + point1.y * cos);
		Point2D.Float point3 = new Point2D.Float(point2.x * scaleX, point2.y * scaleY);
		Point2D.Float point4 = new Point2D.Float(point3.x + pivotX, point3.y + pivotY);
		return point4;
	}

	private void populateHashTableWithAnimations(Map<String, Integer> hashTable) {
		Element entity = firstMatching("entity");
		NodeList animations = entity.getChildNodes();
		for (int anim = 0; anim < animations.getLength(); anim++) {
			if (!(animations.item(anim) instanceof Element)) {
				continue;
			}
			Element animation = (Element) animations.item(anim);
			if (!animation.getTagName().equals("animation")) {
				throw new RuntimeException("SCML format exception - all children of entity must be animation tags");
			}
			String name = animation.getAttribute("name");
			if (!hashTable.containsKey(name)) {
				hashTable.put(name, KleiHash(name));
			}
		}
	}

	private static class AnimationData {
		public float x, y, angle, scaleX, scaleY;
	}

	public void packANIM(String baseTexturePath) throws IOException {
		if (!initialized) throw new RuntimeException("Must initialize ScmlConverter before packing");

		String name = nameOfEntity();

		ANIM ANIMData = new ANIM();
		ANIMData.version = ANIM_VERSION;
		setAggregateData(ANIMData);
		ANIMData.animList = new ArrayList<>();

		// could build hash table different way but this code already works for BILD making
		// hash table so just reuse it here
		String atlasPath = baseTexturePath + name + ".atlas";
		BufferedReader reader = new BufferedReader(new FileReader(atlasPath));
		List<AtlasEntry> orderedAtlasEntries = getOrderedAtlasEntries(reader);
		Map<String, Integer> hashTable = getHashTable(orderedAtlasEntries);

		populateHashTableWithAnimations(hashTable);

		// file map is a mapping from the ids assigned to each image file and the xml element that represents it
		Map<Integer, Element> fileMap = getFileMap();

		Element entity = firstMatching("entity");
		NodeList animations = entity.getChildNodes();
		int animCount = 0;
		for (int anim = 0; anim < animations.getLength(); anim++) {
			if (!(animations.item(anim) instanceof Element)) {
				continue;
			}
			animCount++;
			Element animation = (Element) animations.item(anim);
			if (!animation.getTagName().equals("animation")) {
				throw new RuntimeException("SCML format exception - all children of entity must be animation tags");
			}

			ANIMBank bank = new ANIMBank();
			bank.name = animation.getAttribute("name");
			System.out.println("bank.name="+bank.name);
			System.out.println("hashTable="+hashTable);
			bank.hash = hashTable.get(bank.name);
			int interval = 33;
			try {
				interval = Integer.parseInt(animation.getAttribute("interval"));
			} catch (NumberFormatException e) {}
			bank.rate = (float) MS_PER_S / interval; // interval is ms per frame so this gets fps
			bank.framesList = new ArrayList<>();

			NodeList timelines = animation.getChildNodes();
			Element mainline = getMainline(timelines);
			Map<Integer, Element> timelineMap = getTimelineMap(timelines);
			NodeList keyFrames = mainline.getChildNodes();
			int frameCount = 0;
			Map<Integer, AnimationData> lastDataMap = new HashMap<>();
			for (int frame = 0; frame < keyFrames.getLength(); frame++) { // mainline key frames are the frames
				if (!(keyFrames.item(frame) instanceof Element)) {
					continue;
				}
				frameCount++;

				// that will be sent to klei kanim format so we have to match the timeline data to key frames
				// - this matching will be the part for
				Element key = (Element) keyFrames.item(frame);
				if (!key.getTagName().equals("key")) {
					throw new RuntimeException("SCML format exception - all children of animation must be key tags");
				}

				ANIMFrame ANIMFrame = new ANIMFrame();
				ANIMFrame.elementsList = new ArrayList<>();
				// the elements for this frame will be all the elements
				// referenced in the object_ref(s) -> their data will be found
				// in their timeline
				// note that we need to calculate the animation's overall bounding
				// box for this frame which will be done by computing locations
				// of 4 rectangular bounds of each element under transformation
				// and tracking the max and min of x and y
				float minX = Float.MAX_VALUE;
				float minY = Float.MAX_VALUE;
				float maxX = Float.MIN_VALUE;
				float maxY = Float.MIN_VALUE;

				// look through object refs - will need to maintain list of object refs
				// because in the end it must be sorted in accordance with the z-index
				// before appended in correct order to elementsList
				NodeList objects = key.getChildNodes();
				int elementCount = 0;
				for (int object = 0; object < objects.getLength(); object++) {
					if (!(objects.item(object) instanceof Element)) {
						continue;
					}
					Element objectRef = (Element) objects.item(object);
					if (!objectRef.getTagName().equals("object_ref")) {
						throw new RuntimeException("SCML format exception - all chilredn of key must be object_ref tags");
					}
					ANIMElement element = new ANIMElement();
					// we dont' use any flags so set to 0
					element.flags = 0;
					// spriter does not support changing colors of components
					// through animation so this can be safely set to 0
					element.a = 1.0f; // everything should be fully opaque
					element.b = 1.0f;
					element.g = 1.0f;
					element.r = 1.0f;
					// this field is actually unused entirely (it is parsed but ignored)
					element.order = 0.0f;
					// store z Index so later can be reordered
					element.zIndex = Integer.parseInt(objectRef.getAttribute("z_index"));
					int timelineId = Integer.parseInt(objectRef.getAttribute("timeline"));

					// now need to get corresponding timeline object ref
					Element timeline = timelineMap.get(timelineId);
					int frameId = Integer.parseInt(objectRef.getAttribute("key"));
					Element timelineFrame = getFrameFromTimeline(timeline, frameId);
					Element dataObject = firstMatching(timelineFrame, "object");
					try {
						Element image = fileMap.get(Integer.parseInt(dataObject.getAttribute("file")));
						String imageName = image.getAttribute("name");
						element.image = hashTable.get(getImageName(imageName));
						element.index = getImageIndex(imageName);
						// layer doesn't seem to actually be used for anything after it is parsed as a "folder"
						// but it does need to have an associated string in the hash table so we will just
						// write layer as the same as the image being used
						element.layer = hashTable.get(getImageName(imageName));
						// spriter animation files don't repeat data if it is unchanged between frames
						// for an object so we have to track the last know value of the data and use
						// that if we don't see it
						float scaleX = 1.0f;
						if (dataObject.hasAttribute("scale_x")) {
							scaleX = Float.parseFloat(dataObject.getAttribute("scale_x"));
						} else if (lastDataMap.containsKey(timelineId)) {
							scaleX = lastDataMap.get(timelineId).scaleX;
						}
						float scaleY = 1.0f;
						if (dataObject.hasAttribute("scale_y")) {
							scaleY = Float.parseFloat(dataObject.getAttribute("scale_y"));
						} else if (lastDataMap.containsKey(timelineId)) {
							scaleY = lastDataMap.get(timelineId).scaleY;
						}
						float angle = 0.0f;
						if (dataObject.hasAttribute("angle")) {
							angle = Float.parseFloat(dataObject.getAttribute("angle"));
						} else if (lastDataMap.containsKey(timelineId)) {
							angle = lastDataMap.get(timelineId).angle;
						}
						float xOffset = 0.0f;
						if (dataObject.hasAttribute("x")) {
							xOffset = Float.parseFloat(dataObject.getAttribute("x"));
						} else if (lastDataMap.containsKey(timelineId)) {
							xOffset = lastDataMap.get(timelineId).x;
						}
						float yOffset = 0.0f;
						if (dataObject.hasAttribute("y")) {
							yOffset = Float.parseFloat(dataObject.getAttribute("y"));
						} else if (lastDataMap.containsKey(timelineId)) {
							yOffset = lastDataMap.get(timelineId).y;
						}
						AnimationData data = new AnimationData();
						data.scaleX = scaleX;
						data.scaleY = scaleY;
						data.angle = angle;
						data.x = xOffset;
						data.y = yOffset;
						lastDataMap.put(timelineId, data);
						element.m5 = xOffset * 2;
						element.m6 = -yOffset * 2;
						double angleRadians = Math.toRadians(angle);
						double sin = Math.sin(angleRadians);
						double cos = Math.cos(angleRadians);
						element.m1 = (float) (scaleX * cos);
						element.m2 = (float) (scaleX * -sin);
						element.m3 = (float) (scaleY * sin);
						element.m4 = (float) (scaleY * cos);

						// calculate transformed bounds of this element
						// note that we actually need the pivot of the element in order to determine where the
						// element is located b/c the pivot acts as 0,0 for the x and y offsets
						// additionally it is necessary b/c rotation is done aroudn the pivot
						// (mathematically compute this as rotation around the origin just composed with
						// translating the pivot to and from the origin)
						float pivotX = Float.parseFloat(image.getAttribute("pivot_x"));
						float pivotY = Float.parseFloat(image.getAttribute("pivot_y"));
						int width = Integer.parseInt(image.getAttribute("width"));
						int height = Integer.parseInt(image.getAttribute("height"));
						pivotX *= width;
						pivotY *= height;
						float centerX = pivotX + xOffset;
						float centerY = pivotY + yOffset;
						float x1= xOffset;
						float y1 = yOffset;
						float x2 = x1 + width;
						float y2 = y1 + width;
						Point2D.Float p1 = new Point2D.Float(x1, y1);
						Point2D.Float p2 = new Point2D.Float(x2, y1);
						Point2D.Float p3 = new Point2D.Float(x2, y2);
						Point2D.Float p4 = new Point2D.Float(x1, y2);
						p1 = rotateAbout(centerX, centerY, (float) angleRadians, p1, scaleX, scaleY);
						p2 = rotateAbout(centerX, centerY, (float) angleRadians, p2, scaleX, scaleY);
						p3 = rotateAbout(centerX, centerY, (float) angleRadians, p3, scaleX, scaleY);
						p4 = rotateAbout(centerX, centerY, (float) angleRadians, p4, scaleX, scaleY);
						minX = Math.min(minX, p1.x);
						minX = Math.min(minX, p2.x);
						minX = Math.min(minX, p3.x);
						minX = Math.min(minX, p4.x);
						minY = Math.min(minY, p1.y);
						minY = Math.min(minY, p2.y);
						minY = Math.min(minY, p3.y);
						minY = Math.min(minY, p4.y);
						ANIMFrame.elementsList.add(element);
						elementCount++;
					} catch (NumberFormatException e) {
						System.out.println("found invalid file reference - skipping");
					}
				}

				Collections.sort(ANIMFrame.elementsList, Comparator.comparing(e -> -e.zIndex));

				ANIMFrame.x = 0.5f * (minX + maxX);
				ANIMFrame.y = 0.5f * (minY + maxY);
				ANIMFrame.w = maxX - minX;
				ANIMFrame.h = maxY - minY;
				ANIMFrame.elements = elementCount;
				bank.framesList.add(ANIMFrame);
			}

			bank.frames = frameCount;
			ANIMData.animList.add(bank);
		}
		ANIMData.anims = animCount;

		DataOutputStream out = new DataOutputStream(new FileOutputStream(path + name + "_anim.bytes"));
		writeString(out, "ANIM", false);
		// simply read through built ANIM data structure and write out the properties
		writeInt(out, ANIMData.version);
		writeInt(out, ANIMData.elements);
		writeInt(out, ANIMData.frames);
		writeInt(out, ANIMData.anims);
		for (ANIMBank bank : ANIMData.animList) {
			writeString(out, bank.name);
			writeInt(out, bank.hash);
			writeFloat(out, bank.rate);
			writeInt(out, bank.frames);
			for (ANIMFrame frame : bank.framesList) {
				writeFloat(out, frame.x);
				writeFloat(out, frame.y);
				writeFloat(out, frame.w);
				writeFloat(out, frame.h);
				writeInt(out, frame.elements);
				for (ANIMElement element : frame.elementsList) {
					writeInt(out, element.image);
					writeInt(out, element.index);
					writeInt(out, element.layer);
					writeInt(out, element.flags);
					writeFloat(out, element.a);
					writeFloat(out, element.b);
					writeFloat(out, element.g);
					writeFloat(out, element.r);
					writeFloat(out, element.m1);
					writeFloat(out, element.m2);
					writeFloat(out, element.m3);
					writeFloat(out, element.m4);
					writeFloat(out, element.m5);
					writeFloat(out, element.m6);
					writeFloat(out, element.order);
				}
			}
		}
		writeInt(out, ANIMData.maxVisSymbolFrames);

		writeInt(out, hashTable.entrySet().size());
		for (Map.Entry<String, Integer> hashPair : hashTable.entrySet()) {
			System.out.println(hashPair.getValue()+"="+hashPair.getKey());
			writeInt(out, hashPair.getValue());
			writeString(out, hashPair.getKey());
		}
		out.close();
	}

	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		String path = "C:\\Users\\Davis\\Documents\\ONI-export\\sharedassets1.assets\\Assets\\Test\\coldwheatfiles\\outscml\\";
		ScmlConverter converter = new ScmlConverter();
		converter.init(path, ScmlConverter.loadSCML(path + "coldwheat_0.scml"));
		converter.packBILD(path); // reuse output path as texture path
		converter.packANIM(path);
	}

}
