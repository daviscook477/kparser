import java.io.FileInputStream;

public class Main {

	public static void main(String[] args) throws Exception {
		String pathStart = "C:\\Users\\Davis\\Documents\\ONI-export\\sharedassets1.assets\\Assets\\Test\\coldwheatfiles\\";
		String pathBILD = "coldwheat_build.bytes";
		String pathANIM = "coldwheat_anim.bytes";
		String pathIMG = "coldwheat_0.png";
		Reader reader = new Reader(new FileInputStream(pathStart + pathBILD),
				new FileInputStream(pathStart + pathANIM),
				new FileInputStream(pathStart + pathIMG));
		reader.parseBILDData();
		reader.exportTextures(pathStart);
		reader.parseANIMData();
		Writer writer = new Writer();
		writer.init(reader.BILDTable, reader.BILDData, reader.ANIMData, reader.ANIMHash);
		writer.save(pathStart + "coldwheat_0.scml");
	}

}
