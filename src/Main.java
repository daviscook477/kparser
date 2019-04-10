import java.io.FileInputStream;

public class Main {

	public static void main(String[] args) throws Exception {
		String pathStart = "E:\\Steam Games\\SteamApps\\common\\OxygenNotIncluded\\OxygenNotIncluded_Data\\";
		String pathBILD = "anim_machinemg_build-sharedassets0.assets-1432.txt";
		String pathANIM = "anim_machinemg_anim-sharedassets0.assets-2002.txt";
		String pathIMG = "anim_machinemg_0-sharedassets0.assets-884.png";
		Reader reader = new Reader(new FileInputStream(pathStart + pathBILD),
				new FileInputStream(pathStart + pathANIM),
				new FileInputStream(pathStart + pathIMG));
		reader.parseBILDData();
		reader.exportTextures(pathStart);
		reader.parseANIMData();
		Writer writer = new Writer();
		writer.init(reader.BILDTable, reader.BILDData, reader.ANIMData, reader.ANIMHash);
		writer.save(pathStart + "machinemg.scml");

	}

}