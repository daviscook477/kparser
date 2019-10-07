public class Main {

//	public static void main(String[] args) throws Exception {
//		String pathStart = "C:\\Users\\Davis\\Documents\\ONI-export\\sharedassets1.assets\\Assets\\Test\\coldwheatfiles\\";
//		String pathBILD = "coldwheat_build.bytes";
//		String pathANIM = "coldwheat_anim.bytes";
//		String pathIMG = "coldwheat_0.png";
//		Reader reader = new Reader(new FileInputStream(pathStart + pathBILD),
//				new FileInputStream(pathStart + pathANIM),
//				new FileInputStream(pathStart + pathIMG));
//		reader.parseBILDData();
//		reader.exportTextures(pathStart);
//		reader.parseANIMData();
//		Writer writer = new Writer();
//		writer.init(reader.BILDTable, reader.BILDData, reader.ANIMData, reader.ANIMHash);
//		writer.save(pathStart + "coldwheat_0.scml");
//	}

	public static boolean BE_VERBOSE = true;
	public static boolean BE_VERY_VERBOSE = false;
	public static final String SCML = "scml";
	public static final String KANIM = "kanim";
	public static final String HELP = "help";
	
	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.out.println("K-Parser must be run with arguments: [direction] [file paths]");
			System.exit(0);
		}
		if (args[0].equals(HELP)) {
			System.out.println("K-Parser must be run with arguments: [direction] [file paths]");
			System.out.println("[direction] must be either \"scml\" to create scml project or \"kanim\" to compile Klei animation");
			System.out.println("K-Parser must be provided 3 filenames when run in \"scml\" mode. These are the img file, the build file, and the anim file in that order");
			System.out.println("K-Parser must be provided 1 filename when run in \"kanim\" mode. This is scml file");
			System.exit(0);
		}
		if (!args[0].equals(SCML) && !args[0].contentEquals(KANIM)) {
			System.out.println("[direction] must be either \"scml\" to create scml project or \"kanim\" to compile Klei animation");
			System.exit(-1);
		}
		if (args[0].equals(SCML)) {
			if (args.length < 4) {
				System.out.println("K-Parser must be provided 3 filenames when run in \"scml\" mode. These are the img file, the build file, and the anim file in that order");
				System.exit(-3);
			}
			KAnimConverter.convert(args[1], args[2], args[3]);
			System.exit(0);
		} else if (args[0].equals(KANIM)) {
			if (args.length < 2) {
				System.out.println("K-Parser must be provided the path to the scml file when run in 'kanim' mode.");
				System.exit(-4);
			}
			// Try to get the parent directory.
			ScmlConverter.convert(Utilities.getAbsolutePath(args[1]));
		} else {
			System.out.println("Should not be able to get here.");
		}
	}
}
