import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

public class Reader {

	private ByteBuffer BILD, ANIM;
	private BufferedImage IMG;

	private boolean BILDparsed;
	private boolean ANIMparsed;

	public BILD BILDData;
	public Map<Integer, String> BILDHash;
	public List<BILDRow> BILDTable;

	public ANIM ANIMData;
	public Map<Integer, String> ANIMHash;
	private Map<String, Integer> ANIMIdMap;

	private void printBILDData() {
		StringBuilder data = new StringBuilder();
		data.append(BILDData.name + " v" + BILDData.version + '\n');
		data.append("there are " + BILDData.symbols + " symbols and " + BILDData.frames + " frames");
		System.out.println(data);
	}

	private void printBILDHash() {
		StringBuilder hash = new StringBuilder();
		for (Map.Entry<Integer, String> entry : BILDHash.entrySet()) {
			hash.append("value " + entry.getKey() + " maps onto symbol " + entry.getValue() + '\n');
		}
		System.out.println(hash);
	}

	private void printBILDTable() {
		StringBuilder table = new StringBuilder();
		for (BILDRow row : BILDTable) {
			table.append("for symbol " + row.name + ", frame index " + row.index + " has duration " +
					row.duration + " and occupies rectangle (" + row.x1 + ", " +
					row.y1 + ", " + row.x2 + ", " + row.y2 + ") and has size " + row.w + " x " + row.h + '\n');

			table.append("pivot information: offset=(" + row.pivotX + ", " + row.pivotY + " comparedToSize=(" + row.pivotWidth + ", " + row.pivotHeight +")\n");
		}
		System.out.println(table);
	}

	private void printANIMData() {
		StringBuilder data = new StringBuilder();
		data.append("v" + ANIMData.anims + " has " + ANIMData.anims + " different animations with " +
				ANIMData.frames + " frames and " + ANIMData.elements + " elements with " + ANIMData.maxVisSymbolFrames +
				" maximum visible symbol frames");
		System.out.println(data);
	}

	private void printANIMHash() {
		StringBuilder hash = new StringBuilder();
		for (Map.Entry<Integer, String> entry : ANIMHash.entrySet()) {
			hash.append("value " + entry.getKey() + " maps onto symbol " + entry.getValue() + '\n');
		}
		System.out.println(hash);
	}

	private void printANIMIdMap() {
		StringBuilder ids = new StringBuilder();
		for (Map.Entry<String, Integer> entry : ANIMIdMap.entrySet()) {
			ids.append("element " + entry.getKey() + " maps onto index " + entry.getValue() + '\n');
		}
		System.out.println(ids);
	}

	public Reader(FileInputStream BILD, FileInputStream ANIM, FileInputStream IMG) throws IOException {
		this.BILD = ByteBuffer.wrap(BILD.readAllBytes());
		this.ANIM = ByteBuffer.wrap(ANIM.readAllBytes());
		this.IMG = ImageIO.read(IMG);
		this.BILDparsed = false;
		this.ANIMparsed = false;
		this.BILDData = null;
		this.BILDHash = null;
		this.BILDTable = null;
		this.ANIMData = null;
		this.ANIMHash = null;
		this.ANIMIdMap = null;
	}

	public void exportTextures(String basePath) throws IOException {
		for (BILDRow row : BILDTable) {
			System.out.println(row.x1 + " " + (row.h - row.y1) + " " + row.w + " " + row.h + "    " + IMG.getWidth() + " " + IMG.getHeight());
			BufferedImage texture = IMG.getSubimage((int) row.x1, (int) (IMG.getHeight() - row.y1), (int) row.w, (int) row.h);
			File outFile = new File(basePath + row.name + '_' + row.index + ".png");
			ImageIO.write(texture, "png", outFile);
		}
	}

	private String readString(int length, ByteBuffer buff) {
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < length; i++) {
			str.append((char) buff.get());
		}
		return str.toString();
	}

	private void checkHeader(String expectedHeader, ByteBuffer buff) throws IOException {
		String header = readString(expectedHeader.length(), buff);
		if (!header.equals(expectedHeader)) {
			throw new RuntimeException("Expected header was " + expectedHeader + " but actual header was " + header);
		}
	}

	private String readString(ByteBuffer buff) {
		int length = buff.getInt();
		if (length < 0) return null;
		byte[] strBuffer = new byte[length];
		buff.get(strBuffer);
		return new String(strBuffer, StandardCharsets.UTF_8);
	}

	public void parseBILDData() throws IOException {
		if (BILDparsed) return;

		checkHeader("BILD", BILD);
		BILD.order(ByteOrder.LITTLE_ENDIAN); // seems the data is stored in little endian order

		int version = BILD.getInt();
		System.out.println("version="+version);
		int symbols = BILD.getInt();
		System.out.println("symbols="+symbols);
		int frames = BILD.getInt();
		System.out.println("frames="+frames);
		String name = readString(BILD);
		System.out.println("name="+name);
		List<BILDSymbol> symbolsList = new ArrayList<>();
		BILD BILDData = new BILD();
		BILDData.version = version;
		BILDData.symbols = symbols;
		BILDData.frames = frames;
		BILDData.name = name;
		BILDData.symbolsList = symbolsList;

		for (int i = 0; i < BILDData.symbols; i++) {

			int hash = BILD.getInt();
			int path = BILDData.version > 9 ? BILD.getInt() : 0;
			int color = BILD.getInt();
			int flags = BILD.getInt();
			int numFrames = BILD.getInt();
			System.out.println("symbol " + i + "=(" + hash + ","+path+","+color+","+flags+","+numFrames);
			List<BILDFrame> framesList = new ArrayList<>();
			BILDSymbol symbol = new BILDSymbol();
			symbol.hash = hash;
			symbol.path = path;
			symbol.color = color;
			symbol.flags = flags;
			symbol.numFrames = numFrames;
			symbol.framesList = framesList;

			int time = 0;
			for (int j = 0; j < symbol.numFrames; j++) {
				System.out.println(i + " " + j);
				int sourceFrameNum = BILD.getInt();
				int duration = BILD.getInt();
				int buildImageIdx = BILD.getInt();
				float num6 = BILD.getFloat();
				float num7 = BILD.getFloat();
				float num8 = BILD.getFloat();
				float num9 = BILD.getFloat();
				float x1 = BILD.getFloat();
				float y1 = BILD.getFloat();
				float x2 = BILD.getFloat();
				float y2 = BILD.getFloat();
				BILDFrame frame = new BILDFrame();
				frame.sourceFrameNum = sourceFrameNum;
				frame.duration = duration;
				frame.buildImageIdx = buildImageIdx;
				frame.pivotX = num6;
				frame.pivotY = num7;
				frame.pivotWidth = num8;
				frame.pivotHeight = num9;
				frame.x1 = x1;
				frame.y1 = y1;
				frame.x2 = x2;
				frame.y2 = y2;
				frame.time = time;
				time += frame.duration;
				symbol.framesList.add(frame);
			}
			BILDData.symbolsList.add(symbol);
		}

		Map<Integer, String> BILDHash = new HashMap<>();
		int num = BILD.getInt();
		for (int i = 0; i < num; i++) {
			int hash = BILD.getInt();
			String text = readString(BILD);
			System.out.println(hash+"="+text);
			BILDHash.put(hash, text);
		}

		int imgWidth = IMG.getWidth();
		int imgHeight = IMG.getHeight();
		List<BILDRow> BILDTable = new ArrayList<>();
		for (BILDSymbol symbol : BILDData.symbolsList) {
			for (BILDFrame frame : symbol.framesList) {
				BILDRow row = new BILDRow();
				row.bild = BILDData;
				row.name = BILDHash.get(symbol.hash);
				row.index = frame.sourceFrameNum;
				row.hash = symbol.hash;
				row.time = frame.time;
				row.duration = frame.duration;
				row.x1 = frame.x1 * imgWidth;
				row.y1 = (1 - frame.y1) * imgHeight;
				row.x2 = frame.x2 * imgWidth;
				row.y2 = (1 - frame.y2) * imgHeight;
				row.w = (frame.x2 - frame.x1) * imgWidth;
				row.h = (frame.y2 - frame.y1) * imgHeight;
				row.pivotX = frame.pivotX;
				row.pivotY = frame.pivotY;
				row.pivotWidth = frame.pivotWidth;
				row.pivotHeight = frame.pivotHeight;
				BILDTable.add(row);
			}
		}

		this.BILDData = BILDData;
		this.BILDHash = BILDHash;
		this.BILDTable = BILDTable;
		this.BILDparsed = true;
		printBILDData();
		printBILDHash();
		printBILDTable();
	}

	public void parseANIMData() throws IOException {
		if (ANIMparsed) return;

		checkHeader("ANIM", ANIM);
		ANIM.order(ByteOrder.LITTLE_ENDIAN);

		int version = ANIM.getInt();
		int elements = ANIM.getInt();
		int frames = ANIM.getInt();
		int anims = ANIM.getInt();
		List<ANIMBank> animList = new ArrayList<>();
		ANIM ANIMData = new ANIM();
		ANIMData.version = version;
		ANIMData.elements = elements;
		ANIMData.frames = frames;
		ANIMData.anims = anims;
		ANIMData.animList = animList;

		for (int i = 0; i < ANIMData.anims; i++) {
			String name = readString(ANIM);
			int hash = ANIM.getInt();
			float rate = ANIM.getFloat();
			int frames1 = ANIM.getInt();
			List<ANIMFrame> framesList = new ArrayList<>();
			ANIMBank bank = new ANIMBank();
			bank.name = name;
			bank.hash = hash;
			bank.rate = rate;
			bank.frames = frames1;
			bank.framesList = framesList;

			for (int j = 0; j < bank.frames; j++) {
				float x = ANIM.getFloat();
				float y = ANIM.getFloat();
				float w = ANIM.getFloat();
				float h = ANIM.getFloat();
				int elements1 = ANIM.getInt();
				List<ANIMElement> elementsList = new ArrayList<>();
				ANIMFrame frame = new ANIMFrame();
				frame.x = x;
				frame.y = y;
				frame.w = w;
				frame.h = h;
				frame.elements = elements1;
				frame.elementsList = elementsList;

				for (int k = 0; k < frame.elements; k++) {
					int image = ANIM.getInt();
					int index = ANIM.getInt();
					int layer = ANIM.getInt();
					int flags = ANIM.getInt();
					float a = ANIM.getFloat();
					float b = ANIM.getFloat();
					float g = ANIM.getFloat();
					float r = ANIM.getFloat();
					float m1 = ANIM.getFloat();
					float m2 = ANIM.getFloat();
					float m3 = ANIM.getFloat();
					float m4 = ANIM.getFloat();
					float m5 = ANIM.getFloat();
					float m6 = ANIM.getFloat();
					float order = ANIM.getFloat();
					ANIMElement element = new ANIMElement();
					element.image = image;
					element.index = index;
					element.layer = layer;
					element.flags = flags;
					element.a = a;
					element.b = b;
					element.g = g;
					element.r = r;
					element.m1 = m1;
					element.m2 = m2;
					element.m3 = m3;
					element.m4 = m4;
					element.m5 = m5;
					element.m6 = m6;
					element.order = order;
					element.repeat = 0;
					frame.elementsList.add(element);
				}
				System.out.println();
				bank.framesList.add(frame);
			}
			ANIMData.animList.add(bank);
		}
		int maxVisSymbolFrames = ANIM.getInt();
		ANIMData.maxVisSymbolFrames = maxVisSymbolFrames;
		System.out.println("maxVisSymbolFrames=" + maxVisSymbolFrames);

		Map<Integer, String> ANIMHash = new HashMap<>();
		int num = ANIM.getInt();
		for (int i = 0; i < num; i++) {
			int hash = ANIM.getInt();
			String text = readString(ANIM);
			ANIMHash.put(hash, text);
		}

		Map<String, Integer> ANIMIdMap = new HashMap<>();
		int key = 0;
		for (ANIMBank bank : ANIMData.animList) {
			for (ANIMFrame frame : bank.framesList) {
				for (ANIMElement element : frame.elementsList) {
					String name = ANIMHash.get(element.image) + '_' + element.index + '_' + ANIMHash.get(element.layer);
					if (!ANIMIdMap.containsKey(name)) {
						ANIMIdMap.put(name, key);
						key += 1;
					}
				}
			}
		}

		this.ANIMData = ANIMData;
		this.ANIMHash = ANIMHash;
		this.ANIMIdMap = ANIMIdMap;
		this.ANIMparsed = true;
		printANIMData();
		printANIMHash();
		printANIMIdMap();
	}

}
