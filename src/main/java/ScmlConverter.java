import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ScmlConverter {

	private static final int VERSION = 10;

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
		BILDData.frames = 0;
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
				hashTable.put(entry.name, entry.name.hashCode());
			}
		}
		return null;
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
		BILDData.version = VERSION;
		setSymbolsAndFrames(BILDData, baseTexturePath, imgPath);
		BILDData.name = name;

		List<AtlasEntry> orderedAtlasEntries = getOrderedAtlasEntries(reader);

		// collections sort is stable so sort by index then sort by name
		Collections.sort(orderedAtlasEntries, Comparator.comparingInt(e -> e.index));
		Collections.sort(orderedAtlasEntries, Comparator.comparing(e -> e.name));

		Map<String, Integer> hashTable = getHashTable(orderedAtlasEntries);
		Map<String, Integer> histogram = getHistogram(orderedAtlasEntries);

		BILDData.symbolsList = new ArrayList<>();
		int symbolIndex = -1;
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
				BILDData.symbolsList.add(symbol);
				symbolIndex++;
			}
			BILDFrame frame = new BILDFrame();
			frame.sourceFrameNum = entry.index;
			// duration is always 1 because the frames for a symbol always are numbered incrementing by 1
			// (or at least that's why I think it's always 1 in the examples I looked at)
			frame.duration = 1;
			// this value as read from the file is unused by Klei code and all example files have it set to 0 for all symbols
			frame.buildImageIdx = 0;

		}


		DataOutputStream out = new DataOutputStream(new FileOutputStream(path + name + "_BILD.txt"));


	}

	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		String path = "C:\\Users\\Davis\\Documents\\airconditioner\\";
		ScmlConverter converter = new ScmlConverter();
		converter.init(path, ScmlConverter.loadSCML(path + "airconditioner.scml"));
		converter.packBILD(path);
	}

}
