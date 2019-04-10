public class ANIMRow {

	public String name, hash;
	public float rate;
	public float x, y, w, h;
	public String elementImage, elementLayer;
	public int image, index, layer, flags;
	public int idanim, idframe, idelement, timeline, line_key;
	public float m1, m2, m3, m4, m5, m6; // TODO: how do these map to position rotation matrix - is it (x, y) = (m1, m2) and then
	// rotation matrix
	// [ a b ]   [ m3 m4 ]
	// [ c d ] = [ m5 m6 ] ????
	public float order, repeat; // TODO: these can be ints?

}
