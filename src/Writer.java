import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class Writer {

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

				float x = row.animX - row.animWidth / 2f;
				float y = row.animY - row.animHeight / 2f;
				float pivot_x = 0 - x / row.animWidth;
				float pivot_y = 1 + y / row.animHeight;

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

			Element animation = scml.createElement("animation");
			animation.setAttribute("id", Integer.toString(animIndex));
			animation.setAttribute("name", bank.name);
			animation.setAttribute("length", Integer.toString((int) (bank.rate * bank.frames)));
			animation.setAttribute("interval", "100");
			root.appendChild(animation);

			initMainlineInfo(animation, animIndex);
			initTimelineInfo(animation, animIndex);
		}
	}

	private void initMainlineInfo(Element parent, int animIndex) {
		Element mainline = scml.createElement("mainline");
		parent.appendChild(mainline);

		ANIMBank bank = ANIMData.animList.get(animIndex);
		float rate = bank.rate;
		for (int frame = 0; frame < bank.frames; frame++) {
			Element key = scml.createElement("key");
			key.setAttribute("id", Integer.toString(frame));
			key.setAttribute("time", Integer.toString((int) (frame * rate)));
			for (int element = 0; element < bank.framesList.get(frame).elements; element++) {
				Element object_ref = scml.createElement("object_ref");
				object_ref.setAttribute("id", Integer.toString(element));
				object_ref.setAttribute("timeline", Integer.toString(element));
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
		float rate = bank.rate;
		// invariant bank.framesList.get(any frame).elements is the same
		int elements = bank.framesList.get(0).elements;
		for (int element = 0; element < elements; element++) {
			Element timeline = scml.createElement("timeline");
			ANIMElement baseElement = bank.framesList.get(0).elementsList.get(element);
			timeline.setAttribute("id", Integer.toString(element));
			String name = ANIMHash.get(baseElement.image) + '_' + baseElement.index;
			timeline.setAttribute("name", name);

			boolean isFirst = false;
			for (int frame = 0; frame < bank.frames; frame ++) {
				Element key = scml.createElement("key");
				key.setAttribute("id", Integer.toString(frame));
				key.setAttribute("time", Integer.toString((int) (frame * rate)));

				ANIMElement ele = bank.framesList.get(frame).elementsList.get(element);
				double scale_x = Math.sqrt(ele.m1 * ele.m1 + ele.m2 * ele.m2);
				double scale_y = Math.sqrt(ele.m3 * ele.m3 + ele.m4 * ele.m4);

				double det = ele.m1 * ele.m4 - ele.m3 * ele.m2;
				if (det < 0)
				{
					if (isFirst) {
						scale_x = -scale_x; isFirst = false;
					} else {
						scale_y = -scale_y;
					}
				}

				double sin_approx = 0.5 * (ele.m3 / scale_y - ele.m2 / scale_x);
				double cos_approx = 0.5 * (ele.m1 / scale_x + ele.m4 / scale_y);
				double angle = Math.atan2(sin_approx, cos_approx);

				if (angle < 0) {
					angle += 2 * Math.PI;
				}
				angle *= 180 / Math.PI;
				Element objectDef = scml.createElement("object");
				objectDef.setAttribute("folder", "0");
				objectDef.setAttribute("file", fileNameIndex.get(name));
				objectDef.setAttribute("x", Float.toString(+ele.m5 * 0.5f));
				objectDef.setAttribute("y", Float.toString(-ele.m6 * 0.5f));
				objectDef.setAttribute("angle", Double.toString(angle));
				objectDef.setAttribute("scale_x", Double.toString(scale_x));
				objectDef.setAttribute("scale_y", Double.toString(scale_y));

				key.appendChild(objectDef);
				timeline.appendChild(key);
			}
			parent.appendChild(timeline);
		}
	}

}