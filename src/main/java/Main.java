import java.io.FileInputStream;

public class Main {

	public static void main(String[] args) throws Exception {
		String pathStart = "E:\\Steam Games\\SteamApps\\common\\OxygenNotIncluded\\OxygenNotIncluded_Data\\";
		String pathBILD = "jukebot_build-sharedassets0.assets-1680.txt";
		String pathANIM = "jukebot_anim-sharedassets0.assets-2233.txt";
		String pathIMG = "jukebot_0-sharedassets0.assets-326.png";
		Reader reader = new Reader(new FileInputStream(pathStart + pathBILD),
				new FileInputStream(pathStart + pathANIM),
				new FileInputStream(pathStart + pathIMG));
		reader.parseBILDData();
		reader.exportTextures(pathStart);
		reader.parseANIMData();
		Writer writer = new Writer();
		writer.init(reader.BILDTable, reader.BILDData, reader.ANIMData, reader.ANIMHash);
		writer.save(pathStart + "jukebot.scml");
	}

}
