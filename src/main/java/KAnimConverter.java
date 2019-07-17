import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

public class KAnimConverter {
	
	public static void convert(String imgPath, String buildPath, String animPath) throws FileNotFoundException, IOException, ParserConfigurationException, TransformerException {
		Reader reader = new Reader(new FileInputStream(buildPath),
				new FileInputStream(animPath),
				new FileInputStream(imgPath));
		reader.parseBILDData();
		File imgFile = new File(imgPath);
		String scmlFolderPath = imgFile.getParent() + "\\scml\\";
		new File(scmlFolderPath).mkdir();
		reader.exportTextures(scmlFolderPath);
		reader.parseANIMData();
		Writer writer = new Writer();
		writer.init(reader.BILDTable, reader.BILDData, reader.ANIMData, reader.ANIMHash);
		String scmlPath = imgFile.getParent() + "\\scml\\" + imgFile.getName().substring(0, imgFile.getName().lastIndexOf('.')) + ".scml";
		writer.save(scmlPath);
	}
	
}
