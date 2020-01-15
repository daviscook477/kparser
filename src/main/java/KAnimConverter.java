import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

public class KAnimConverter {
	
	public static void convert(String imgPathStr, String buildPathStr, String animPathStr, String outputDir) throws FileNotFoundException, IOException, ParserConfigurationException, TransformerException {
		var outputPath = Path.of(outputDir);
		// Ensure output dirs exist
		outputPath.toFile().mkdirs();

		Utilities.PrintInfo(String.format("Outputting to %s", outputPath.toAbsolutePath().toString()));
		Utilities.PrintInfo("Unpack started.");
		var imgPath = Path.of(imgPathStr);
		var build = Path.of(buildPathStr).toFile();
		var anim = Path.of(animPathStr).toFile();

		Reader reader = new Reader(new FileInputStream(build),
				new FileInputStream(anim),
				new FileInputStream(imgPath.toFile()));
		Utilities.PrintInfo("Parsing build data.");
		reader.parseBILDData();
		Utilities.PrintInfo("Exporting textures.");

		reader.exportTextures(outputPath);
		Utilities.PrintInfo("Parsing animation data.");
		reader.parseANIMData();
		Writer writer = new Writer();
		writer.init(reader.BILDTable, reader.BILDData, reader.ANIMData, reader.ANIMHash);

		var filename = imgPath.getFileName().toString();
		String scmlFileName = filename
				.substring(0, filename.lastIndexOf('.')) + ".scml";
		var outputFilePath = outputPath.resolve(scmlFileName);

		Utilities.PrintInfo("Writing...");
		writer.save(outputFilePath);

		Utilities.PrintInfo("Done.");
	}
	
}
