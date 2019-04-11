import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class Writer {

	private static final int MS_PER_S = 1000;

	private Document scml;
	private List<BILDRow> BILDTable;
	private BILD BILDData;
	private ANIM ANIMData;
	private Map<Integer, String> ANIMHash;
	private Map<String, String> fileNameIndex;

	public void init(List<BILDRow> BILDTable, BILD BILDData, ANIM ANIMData,
					 Map<Integer, String> ANIMHash) throws ParserConfigurationException {
		this.BILDTable = BILDTable;
		this.BILDData = BILDData;
		this.ANIMData = ANIMData;
		this.ANIMHash = ANIMHash;

		initFile();
		initFolderInfo();
		initEntityInfo();
		initAnimationInfo();
	}

	public void save(String path) throws TransformerException, IOException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(scml);
		FileWriter writer = new FileWriter(new File(path));
		StreamResult result = new StreamResult(writer);
		transformer.transform(source, result);
	}

	private void initFile() throws ParserConfigurationException {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
		scml = documentBuilder.newDocument();
		Element spriterData = scml.createElement("spriter_data");
		scml.appendChild(spriterData);
		spriterData.setAttribute("scml_version", "1.0");
		spriterData.setAttribute("generator", "BrashMonkey Spriter");
		spriterData.setAttribute("generator_version", "r11");
	}

	private Element getElement(Document scml, String tagName) {
		NodeList list = scml.getElementsByTagName(tagName);
		return (Element) list.item(0);
	}

	private void initFolderInfo() {
		Element root = getElement(scml, "spriter_data");

		int folders = 1;
		for (int i = 0; i < folders; i++) {
			Element folder = scml.createElement("folder");
			folder.setAttribute("id", Integer.toString(i));
			root.appendChild(folder);

			fileNameIndex = new HashMap<>();
			for (int fileIndex = 0; fileIndex < BILDData.frames; fileIndex++) {
				BILDRow row = BILDTable.get(fileIndex);
				String key = row.name + "_" + row.index;
				if (fileNameIndex.containsKey(key)) {
					key += "_" + fileIndex;
				}
				fileNameIndex.put(key, Integer.toString(fileIndex));

				float x = row.pivotX - row.pivotWidth / 2f;
				float y = row.pivotY - row.pivotHeight / 2f;
				// this computation changes pivot from being in whatever
				// coordinate system it was originally being specified in to being specified
				// as just a scalar multiple of the width/height (starting at the midpoint of 0.5)
				float pivot_x = 0 - x / row.pivotWidth;
				float pivot_y = 1 + y / row.pivotHeight;

				Element file = scml.createElement("file");
				file.setAttribute("id", Integer.toString(fileIndex));
				file.setAttribute("name", row.name + "_" + row.index);
				file.setAttribute("width", Integer.toString((int) row.w));
				file.setAttribute("height", Integer.toString((int) row.h));
				file.setAttribute("pivot_x", Float.toString(pivot_x));
				file.setAttribute("pivot_y", Float.toString(pivot_y));

				folder.appendChild(file);
			}
		}
	}

	private void initEntityInfo() {
		Element root = getElement(scml, "spriter_data");
		Element entity = scml.createElement("entity");
		entity.setAttribute("id", "0");
		entity.setAttribute("name", BILDTable.get(0).bild.name);
		root.appendChild(entity);
	}

	private void initAnimationInfo() {
		Element root = getElement(scml, "entity");

		for (int animIndex = 0; animIndex < ANIMData.anims; animIndex++) {
			ANIMBank bank = ANIMData.animList.get(animIndex);
			int rate = (int) (MS_PER_S / bank.rate);

			Element animation = scml.createElement("animation");
			animation.setAttribute("id", Integer.toString(animIndex));
			animation.setAttribute("name", bank.name);
			animation.setAttribute("length", Integer.toString(rate * bank.frames));
			animation.setAttribute("interval", Integer.toString(rate));
			root.appendChild(animation);

			initMainlineInfo(animation, animIndex);
			initTimelineInfo(animation, animIndex);
		}
	}

	private String nameOf(ANIMElement ele) {
		return ANIMHash.get(ele.image)  + '_' + ele.index;
	}

	private String nameOf(ANIMElement ele, Map<String, Integer> occurrenceMap) {
		return nameOf(ele, occurrenceMap.get(nameOf(ele)));
	}

	private String nameOf(ANIMElement ele, int occurrenceNumber) {
		return nameOf(ele) + '_' + occurrenceNumber;
	}

	private String nameOf(String baseName, int occurrenceNumber) {
		return baseName + '_' + occurrenceNumber;
	}

	private SortedMap<String, Integer> buildAnimHistogram(ANIMBank bank) {
		SortedMap<String, Integer> perFrameHistogram = new TreeMap<>();
		SortedMap<String, Integer> overallHistogram = new TreeMap<>();
		for (int frame = 0; frame < bank.frames; frame++) {
			// build per frame histogram
			perFrameHistogram.clear();
			for (int element = 0; element < bank.framesList.get(frame).elements; element++) {
				ANIMElement ele = bank.framesList.get(frame).elementsList.get(element);
				String name = nameOf(ele);
				if (perFrameHistogram.containsKey(name)) {
					perFrameHistogram.put(name, perFrameHistogram.get(name) + 1);
				} else {
					perFrameHistogram.put(name, 1);
				}
			}
			// update overall histogram if maximums are found
			for (String name : perFrameHistogram.keySet()) {
				if (!overallHistogram.containsKey(name) || overallHistogram.get(name) < perFrameHistogram.get(name)) {
					overallHistogram.put(name, perFrameHistogram.get(name));
				}
			}
		}
		return overallHistogram;
	}

	private Map<String, Integer> buildIdMap(ANIMBank bank) {
		SortedMap<String, Integer> histogram = buildAnimHistogram(bank);
		Map<String, Integer> idMap = new HashMap<>();
		int index  = 0;
		for (String name : histogram.keySet()) {
			for (int i = 0; i < histogram.get(name); i++) {
					idMap.put(nameOf(name, i), index++);
			}
		}
		return idMap;
	}

	private Element buildKeyFrame(int frame, int rate) {
		Element key = scml.createElement("key");
		key.setAttribute("id", Integer.toString(frame));
		key.setAttribute("time", Integer.toString(frame * rate));
		return key;
	}

	private void updateOccurrenceMap(ANIMElement ele, Map<String, Integer> occurrenceMap) {
		String name = nameOf(ele);
		if (!occurrenceMap.containsKey(name)) {
			occurrenceMap.put(name, 0);
		} else {
			occurrenceMap.put(name, occurrenceMap.get(name) + 1);
		}
	}

	private void initMainlineInfo(Element parent, int animIndex) {
		Element mainline = scml.createElement("mainline");
		parent.appendChild(mainline);

		ANIMBank bank = ANIMData.animList.get(animIndex);
		int rate = (int) (MS_PER_S / bank.rate); // convert provided fps rate to number of ms per frame
		Map<String, Integer> idMap = buildIdMap(bank);
		Map<String, Integer> occurrenceMap = new HashMap<>();

		for (int frame = 0; frame < bank.frames; frame++) {
			Element key = buildKeyFrame(frame, rate);
			occurrenceMap.clear();
			for (int element = 0; element < bank.framesList.get(frame).elements; element++) {
				Element object_ref = scml.createElement("object_ref");
				ANIMElement ele = bank.framesList.get(frame).elementsList.get(element);
				updateOccurrenceMap(ele, occurrenceMap);
				String name = nameOf(ele, occurrenceMap);
				System.out.println(name);
				object_ref.setAttribute("id", Integer.toString(idMap.get(name)));
				object_ref.setAttribute("timeline", Integer.toString(idMap.get(name)));
				// b/c ONI has animation properties for each element specified at every frame the timeline key frame that
				// matches a mainline key frame is always the same
				object_ref.setAttribute("key", Integer.toString(frame));
				object_ref.setAttribute("z_index", Integer.toString(bank.framesList.get(frame).elements - element));

				key.appendChild(object_ref);
			}
			mainline.appendChild(key);
		}
	}

	private void initTimelineInfo(Element parent, int animIndex) {
		ANIMBank bank = ANIMData.animList.get(animIndex);
		int rate = (int) (MS_PER_S / bank.rate); // convert provided fps rate to number of ms per frame
		Map<Integer, Element> timelineMap = new HashMap<>();
		Map<String, Integer> idMap = buildIdMap(bank);
		for (String name : idMap.keySet()) {
			Element timeline = scml.createElement("timeline");
			timeline.setAttribute("id", Integer.toString(idMap.get(name)));
			timeline.setAttribute("name", name);
			timelineMap.put(idMap.get(name), timeline);
		}
		Map<String, Integer> occurrenceMap = new HashMap<>();
		for (int frame = 0; frame < bank.frames; frame++) {
			occurrenceMap.clear();
			for (int element = 0; element < bank.framesList.get(frame).elements; element++) {
				Element key = buildKeyFrame(frame, rate);
				ANIMElement ele = bank.framesList.get(frame).elementsList.get(element);
				updateOccurrenceMap(ele, occurrenceMap);
				String name = nameOf(ele, occurrenceMap);
				// is part of the formula for decomposing transformation matrix into components
				// see https://math.stackexchange.com/questions/237369/given-this-transformation-matrix-how-do-i-decompose-it-into-translation-rotati
				double scale_x = Math.sqrt(ele.m1 * ele.m1 + ele.m2 * ele.m2);
				double scale_y = Math.sqrt(ele.m3 * ele.m3 + ele.m4 * ele.m4);

				double det = ele.m1 * ele.m4 - ele.m3 * ele.m2;
				if (det < 0) {
					scale_y = -scale_y;
				}

				// still part of the formula for obtaining rotation component from combined rotation + scaling
				// undue scaling by dividing by scaling and then taking average value of sin/cos to make it more
				// accurate (b/c sin and cos appear twice each in 2d rotation matrix)
				double sin_approx = 0.5 * (ele.m3 / scale_y - ele.m2 / scale_x);
				double cos_approx = 0.5 * (ele.m1 / scale_x + ele.m4 / scale_y);
				double m1 = Math.max(Math.min(ele.m1 / scale_x, 1f), -1f);
				double m2 = Math.max(Math.min(ele.m2 / scale_x, 1f), -1f);
				double m3 = Math.max(Math.min(ele.m3 / scale_y, 1f), -1f);
				double m4 = Math.max(Math.min(ele.m4 / scale_y, 1f), -1f);

				double angle = Math.atan2(sin_approx, cos_approx);
				// it seems as if the notion of simply haveing x,y, angle and scale are not really sufficient to describe the
				// transformation applied to each point since the 2x3 matrix m1...m6 doesn't nicely decompose into a valid rotation matrix
				// basically the two components that are sin are not equal and the two components that are cos are not equal. This would imply
				// that there is some additional transformation being applied to each point in addition to just the scale and rotation information
				// that makes it such that when we just look at that rotation information it does not produce the correct result

				if (angle < 0) {
					angle += 2 * Math.PI;
				}
				angle *= 180 / Math.PI;
				Element objectDef = scml.createElement("object");
				objectDef.setAttribute("folder", "0");
				String fileName = nameOf(ele);
				objectDef.setAttribute("file", fileNameIndex.get(fileName));
				objectDef.setAttribute("x", Float.toString((float) (+ele.m5*0.5f)));
				objectDef.setAttribute("y", Float.toString((float) (-ele.m6*0.5f)));
				objectDef.setAttribute("angle", Double.toString(angle));
				objectDef.setAttribute("scale_x", Double.toString(scale_x));
				objectDef.setAttribute("scale_y", Double.toString(scale_y));

				key.appendChild(objectDef);
				timelineMap.get(idMap.get(name)).appendChild(key);
			}
		}

		for (Element timeline : timelineMap.values()) {
			parent.appendChild(timeline);
		}
	}

}
