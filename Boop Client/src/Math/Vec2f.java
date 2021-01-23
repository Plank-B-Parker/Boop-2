package math;

public class Vec2f {
	// COPIED from server file
	
	public float x = 0;
	public float y = 0;
	
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
	public static Vec2f add(Vec2f result, Vec2f A, Vec2f B) {
		result.x = A.x + B.x;
		result.y = A.y + B.y;
		
		return result;
	}
	
	/**Subtracts A by B and stores value in result.
	 * No new Object created.
	 * @param result
	 * @param A
	 * @param B
	 */
	public static Vec2f sub(Vec2f result, Vec2f A, Vec2f B) {
		result.x = A.x - B.x;
		result.y = A.y - B.y;
		
		return result;
	}
	
	/**
	 * Calculates minimum distance between two points on a torus/doughnut.
	 * @param result
	 * @param pos2
	 * @param pos1
	 */
	public static Vec2f disp(Vec2f result, Vec2f pos2, Vec2f pos1) {
		float x = pos2.x - pos1.x;
		float y = pos2.y - pos1.y;
		
		if(x > 1) {
			x = x-2;
		}
		if(x < -1) {
			x = x+2;
		}
		
		if(y > 1) {
			y = y-2;
		}
		if(y < -1) {
			y = y+2;
		}
		
		result.x = x;
		result.y = y;
		
		return result;
	}
	
	/**Scales A by k and stores value in result.
	 * No new Object created.
	 * @param result
	 * @param A
	 * @param B
	 */
	public static Vec2f scale(Vec2f result, Vec2f A, float k) {
		result.x = A.x*k;
		result.y = A.y*k;
		
		return result;
	}
	
	/**
	 * Sets result as A + k*B.
	 * @param result
	 * @param A
	 * @param B
	 * @param k
	 */
	public static Vec2f increment(Vec2f result, Vec2f A, Vec2f B, float k) {
		result.x = A.x + k*B.x;
		result.y = A.y + k*B.y;
		
		return result;
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
