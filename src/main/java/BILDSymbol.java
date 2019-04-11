import java.util.List;

public class BILDSymbol {

	public int hash, path, color, flags, numFrames;
	// flags has 4 flags
	// 1 -> bloom
	// 2  -> onlight
	// 4 -> snapto
	// 8 -> fg
	// none of these matter for spriter and spriter can't write to any of these either
	// so flag always = 0 for translation purposes
	public List<BILDFrame> framesList;

}
