import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.util.ArrayList;
import java.util.List;

class Settings {
	@Option(name="--verbose", aliases={"-v"}, usage="Debug-level verbosity.")
	public boolean BE_VERBOSE = false;

	@Option(name="--silent", aliases={"-s"}, usage="KParser will be silent on success.")
	public boolean BE_SILENT = false;

	@Option(name="--to-kanim", aliases={"-k"}, usage="Convert from SCML to KAnim.")
	public boolean MAKE_KANIM = false;

	@Option(name="--to-scml", aliases={"-S"}, usage="Convert from KAnim to SCML.")
	public boolean MAKE_SCML = false;

	// receives other command line parameters than options
	@Argument
	public List<String> arguments;

	Settings() {
		arguments = new ArrayList<String>();
	}
}

public class Main {
	public static Settings settings;
	
	public static void main(String[] args) throws Exception {
		settings = new Settings();
		var parser = new CmdLineParser(settings);
		// parse the arguments.
		parser.parseArgument(args);

		if( settings.arguments.isEmpty() ) {
			System.err.println("java -jar kparser [options...] arguments...");
			// print the list of available options
			parser.printUsage(System.out);
		} else {
			var files = settings.arguments;
			if (settings.MAKE_KANIM) {
				ScmlConverter.convert(Utilities.getAbsolutePath(files.get(0)));
			} else if (settings.MAKE_SCML) {
				KAnimConverter.convert(files.get(0), files.get(1), files.get(2));
			} else {
				System.out.println("You must specify the conversion direction (--kanim or --scml).");
			}
		}
	}
}
