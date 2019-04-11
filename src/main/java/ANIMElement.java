public class ANIMElement {

	public int image, index, layer, flags;
	// flags has just one value
	// 1-> fg
	// but we don't represent/deal with that in  spriter so we always leave
	// it as 0
	public float a, b, g, r;
	/*
	 * m1 m2 m3 m4 make up rotation + scaling matrix for element on this frame
	 * m5 m6 are the position offset of the element on this frame
	 * specifically [a b]     [m1 m2]
	 * 				[c d]  =  [m3 m4]
	 * 	and (x, y) = (m5, m6)
	 *
	 * 	okay after some further investigation they don't *exactly* just mean
	 * 	rotation and translation - it's a 2x3 tranformation matrix so it
	 * 	can have some more complex stuff going on in the transformation but
	 * 	it can be broken down into mostly accurate rotation + scaling + translation
	 *
	 * 	luckily going from rotation + scaling + translation to a 2x3 transformation matrix
	 * 	is well defined even if the reverse isn't
	 */
	public float m1, m2, m3, m4, m5, m6;

	public float order;

	public int zIndex; // only used in scml -> kanim conversion

}
