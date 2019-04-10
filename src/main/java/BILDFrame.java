public class BILDFrame {

	public int sourceFrameNum;
	public int duration;
	public int buildImageIdx;
	// these are the pivot invormation for the sprite image
	public float pivotX, pivotY, pivotWidth, pivotHeight;
	// so these x y coordinates are actually uv texture coordinates - floats in the range 0 to 1
	public float x1, y1, x2 , y2;
	public int time;

}
