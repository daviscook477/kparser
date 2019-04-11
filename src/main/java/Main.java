import java.io.FileInputStream;

public class Main {

	public static void main(String[] args) throws Exception {
		String pathStart = "E:\\Steam Games\\SteamApps\\common\\OxygenNotIncluded\\OxygenNotIncluded_Data\\";
		String pathBILD = "airConditioner_BILD.txt";
		String pathANIM = "airConditioner_ANIM.txt";
		String pathIMG = "airConditioner.png";
		Reader reader = new Reader(new FileInputStream(pathStart + pathBILD),
				new FileInputStream(pathStart + pathANIM),
				new FileInputStream(pathStart + pathIMG));
		reader.parseBILDData();
		reader.exportTextures(pathStart);
		reader.parseANIMData();
		Writer writer = new Writer();
		writer.init(reader.BILDTable, reader.BILDData, reader.ANIMData, reader.ANIMHash);
		writer.save(pathStart + "airconditioner.scml");
	}

}
