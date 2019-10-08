import java.io.IOException;
import java.nio.file.Path;

public class Utilities {
    // Attempt to get the directory of the input file.
    public static Path getDirectory(String patharg) {
        var path = Path.of(patharg);
        try {
            path = path.toRealPath().getParent().toAbsolutePath();
            if (path == null) {
                System.out.println(String.format("The path \"%s\" is null for some reason.", patharg));
                System.exit(1);
            }
        } catch (IOException e) {
            System.out.println(String.format("The path \"%s\" is not a valid path.",
                    patharg));
            System.exit(1);
        } catch (NullPointerException e) {
            System.out.println(e);
            System.exit(1);
        }
        return path;
    }

    public static Path getAbsolutePath(String patharg) {
        var path = Path.of(patharg);
        try {
            path = path.toRealPath().toAbsolutePath();
        } catch (IOException e) {
            System.out.println(String.format("The path \"%s\" is not a valid path.",
                    patharg));
            System.exit(1);
        }
        return path;
    }

    public static void PrintInfo(String str) {
        if (!Main.settings.BE_SILENT) {
            System.out.println(str);
        }
    }

    public static void PrintDebug(String str) {
        if (Main.settings.BE_VERBOSE) {
            System.out.println(str);
        }
    }
}