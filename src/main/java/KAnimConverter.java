import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

public class KAnimConverter {
	
	public static void convert(String imgPathStr, String buildPathStr, String animPathStr) throws FileNotFoundException, IOException, ParserConfigurationException, TransformerException {
		var imgPath = Path.of(imgPathStr);
		var build = Path.of(buildPathStr).toFile();
		var anim = Path.of(animPathStr).toFile();

		Reader reader = new Reader(new FileInputStream(build),
				new FileInputStream(anim),
				new FileInputStream(imgPath.toFile()));
		reader.parseBILDData();
		Path outputPath = Path.of("").resolve("scml");
		outputPath.toFile().mkdirs();
		reader.exportTextures(outputPath);
		reader.parseANIMData();
		Writer writer = new Writer();
		writer.init(reader.BILDTable, reader.BILDData, reader.ANIMData, reader.ANIMHash);

		var filename = imgPath.getFileName().toString();
		String scmlFileName = filename
				.substring(0, filename.lastIndexOf('.')) + ".scml";
		var outputFilePath = outputPath.resolve(scmlFileName);
		writer.save(outputFilePath);
	}
	
}
