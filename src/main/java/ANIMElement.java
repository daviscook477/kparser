public class ANIMElement {

	public int image, index, layer, flags;
	public float a, b, g, r;
	/*
	 * m1 m2 m3 m4 make up rotation + scaling matrix for element on this frame
	 * m5 m6 are the position offset of the element on this frame
	 * specifically [a b]     [m1 m2]
	 * 				[c d]  =  [m3 m4]
	 * 	and (x, y) = (m5, m6)
	 */
	public float m1, m2, m3, m4, m5, m6;

	public float order, repeat;

}
