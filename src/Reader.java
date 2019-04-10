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
import java.util.Set;
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
	public List<ANIMRow> ANIMTable;

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
			table.append("additional data: (" + row.animX + ", " + row.animY + ", " + row.animWidth + ", " + row.animHeight + ")\n");
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

	private void printANIMTable() {
		StringBuilder table = new StringBuilder();
		for (ANIMRow row : ANIMTable) {
			table.append("for animation \"" + row.name + "\" id=(" + row.idanim + ") on frame=" + row.idframe + " for symbol " + row.hash + " runs at rate " + row.rate + "\n");
			table.append("it occupies the rectangle (" + row.x + ", " + row.y + ", " + (row.x + row.w) + ", " + (row.y + row.h) + ")\n");
			table.append("it uses element image " + row.elementImage + " on layer " + row.elementLayer + " with index " + row.index + " flags " + row.flags + "\n");
			table.append("	for element " + row.idelement + " on timeline " + row.timeline + " with a line key = " + row.line_key + "\n");
			table.append("	animation matrix is " + row.m1 + " " + row.m2 + " " + row.m3 + " " + row.m4 + " " + row.m5 + " " + row.m6 + "\n");
			table.append("	order is " + row.order + " and repeat is " + row.repeat + "\n");
		}
		System.out.println(table);
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
		this.ANIMTable = null;
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
		int symbols = BILD.getInt();
		int frames = BILD.getInt();
		String name = readString(BILD);
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
				frame.animX = num6;
				frame.animY = num7;
				frame.animWidth = num8;
				frame.animHeight = num9;
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
				row.animX = frame.animX;
				row.animY = frame.animY;
				row.animWidth = frame.animWidth;
				row.animHeight = frame.animHeight;
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
				bank.framesList.add(frame);
			}
			ANIMData.animList.add(bank);
		}
		int maxVisSymbolFrames = ANIM.getInt();
		ANIMData.maxVisSymbolFrames = maxVisSymbolFrames;

		Map<Integer, String> ANIMHash = new HashMap<>();
		int num = ANIM.getInt();
		for (int i = 0; i < num; i++) {
			int hash = ANIM.getInt();
			String text = readString(ANIM);
			ANIMHash.put(hash, text);
		}

		List<ANIMRow> ANIMTable = new ArrayList<>();
		int idanim = 0;
		for (ANIMBank bank : ANIMData.animList) {
			int idframe = 0;
			Map<String, Integer> timelines = new HashMap<>();
			for (ANIMFrame frame : bank.framesList) {
				for (int idelement = 0; idelement < frame.elements; idelement++) {
					ANIMElement element = ANIMData.animList.get(idanim).framesList.get(idframe).elementsList.get(idelement);

					int timelineid = 0;

					String timeline = element.image + "_" + element.index + '_' + element.layer;
					if (!timelines.containsKey(timeline)) {
						timelines.put(timeline, 0);
					} else {
						if (timelines.get(timeline) >= idframe) {
							for (int special = 0; special < frame.elements * idframe + 1; special++) {
								String timelineSpecial = timeline + '_' + special;

								if (timelines.containsKey(timelineSpecial) && timelines.get(timelineSpecial) >= idframe) {
									continue;
								} else {
									timeline = timelineSpecial;
									if (timelines.containsKey(timelineSpecial)) {
										timelines.put(timeline, timelines.get(timeline) + 1);
									} else {
										timelines.put(timeline, 0);
									}

									element.repeat = special + 1;
									ANIMElement element1 = ANIMData.animList.get(idanim).framesList.get(idframe).elementsList.get(idelement);
									element1.repeat = special + 1;
									ANIMData.animList.get(idanim).framesList.get(idframe).elementsList.set(idelement, element1);
									break;
								}
							}
						} else {
							timelines.put(timeline, timelines.get(timeline) + 1);
						}
					}

					Set<String> timelineKeyset = timelines.keySet();
					List<String> timelineKeys = new ArrayList(timelineKeyset);
					for (int lineid = 0; lineid < timelineKeys.size(); lineid++) {
						if (timelineKeys.get(lineid) == timeline) {
							timelineid = lineid;
							break;
						}
					}

					ANIMRow row = new ANIMRow();
					row.name = bank.name;
					row.hash = ANIMHash.get(bank.hash);
					row.rate = bank.rate;
					row.x = frame.x;
					row.y = frame.y;
					row.w = frame.w;
					row.h = frame.h;
					row.elementImage = ANIMHash.get(element.image);
					row.elementLayer = ANIMHash.get(element.layer);
					row.image = element.image;
					row.index = element.index;
					row.layer = element.layer;
					row.flags = element.flags;
					row.idanim = idanim;
					row.idframe = idframe;
					row.idelement = idelement;
					row.timeline = timelineid;
					row.line_key = timelines.get(timelineKeys.get(timelineid));
					row.m1 = element.m1;
					row.m2 = element.m2;
					row.m3 = element.m3;
					row.m4 = element.m4;
					row.m5 = element.m5;
					row.m6 = element.m6;
					row.order = element.order;
					row.repeat = element.repeat;
					ANIMTable.add(row);
				}
				idframe++;
			}
			idanim++;
		}

		this.ANIMData = ANIMData;
		this.ANIMHash = ANIMHash;
		this.ANIMTable = ANIMTable;
		this.ANIMparsed = true;
		printANIMData();
		printANIMHash();
		printANIMTable();
	}

}
