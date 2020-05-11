package Math;

public class Vec2f {
	
	float x = 0;
	float y = 0;
	
	public Vec2f() {
	}
	public Vec2f(float X, float Y) {
		x = X;
		y = Y;
	}
	/** Returns a new Vec2f object identical to the instance*/
	public Vec2f copy() {
		return new Vec2f(x,y);
	}
	
	/**
	 * Sets vector components as (x,y). Used to avoid object creation.
	 * @param x
	 * @param y
	 */
	public void set(float x, float y) {
		this.x = x;
		this.y = y;
	}
	
	/**Adds A to B and stores the value in result.
	 * No new Object created.
	 * @param result
	 * @param A
	 * @param B
	 */
	public static void add(Vec2f result, Vec2f A, Vec2f B) {
		result.x = A.x + B.x;
		result.y = A.y + B.y;
		
	}
	
	/**Subtracts A by B and stores value in result.
	 * No new Object created.
	 * @param result
	 * @param A
	 * @param B
	 */
	public static void sub(Vec2f result, Vec2f A, Vec2f B) {
		result.x = A.x - B.x;
		result.y = A.y - B.y;
	}
	
	/**Scales A by k and stores value in result.
	 * No new Object created.
	 * @param result
	 * @param A
	 * @param B
	 */
	public static void scale(Vec2f result, Vec2f A, float k) {
		result.x = A.x*k;
		result.y = A.y*k;
	}
	
	/**
	 * Sets result as A + k*B.
	 * @param result
	 * @param A
	 * @param B
	 * @param k
	 */
	public static void increment(Vec2f result, Vec2f A, Vec2f B, float k) {
		result.x = A.x + k*B.x;
		result.y = A.y + k*B.y;
	}
	
	/**
	 * Returns dot product between A and B.|A|*|B|*cos(<AB)
	 * @param A
	 * @param B
	 * @return
	 */
	public static float dot(Vec2f A, Vec2f B) {
		return A.x*B.x + A.y*B.y;
	}
	public float lengthSq() {
		return x*x + y*y;
	}
}
