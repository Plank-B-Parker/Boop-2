package math;

public class Vec2f {
	
	public float x = 0;
	public float y = 0;
	
	public static final Vec2f Origin = new Vec2f(); 
	
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
	 * Sets vector components as (x,y).
	 * Used to avoid object creation.
	 * @param x
	 * @param y
	 */
	public void set(final float x, final float y) {
		this.x = x;
		this.y = y;
	}
	
	/**
	 * Sets result as A + B.
	 * No new Object created.
	 * @param result
	 * @param A
	 * @param B
	 */
	public static Vec2f add(Vec2f result, final Vec2f A, final Vec2f B) {
		result.x = A.x + B.x;
		result.y = A.y + B.y;
		
		return result;
	}
	
	/**
	 * Returns A + B.
	 * New Object returned.
	 * @param result
	 * @param A
	 * @param B
	 */
	public static Vec2f add(final Vec2f A, final Vec2f B) {
		Vec2f result = new Vec2f();
		return add(result, A, B);
	}
	
	/**
	 * Sets result as A - B.
	 * No new Object created.
	 * @param result
	 * @param A
	 * @param B
	 */
	public static Vec2f sub(Vec2f result, final Vec2f A, final Vec2f B) {
		result.x = A.x - B.x;
		result.y = A.y - B.y;
		
		return result;
	}
	
	/**
	 * Returns A - B.
	 * New Object created.
	 * @param result
	 * @param A
	 * @param B
	 */
	public static Vec2f sub(final Vec2f A, final Vec2f B) {
		Vec2f result = new Vec2f();
		return sub(result, A, B);
	}
	
	/**
	 * Sets result as min(|pos2 - pos1|) on a torus.
	 * Use for calculating displacement.
	 * No new object created.
	 * @param result
	 * @param pos2
	 * @param pos1
	 */
	public static Vec2f minDisp(Vec2f result, final Vec2f pos2, final Vec2f pos1) {
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
	
	/**
	 * Returns min(|pos2 - pos1|) on a torus.
	 * Use for calculating displacement.
	 * New object created.
	 * @param pos2
	 * @param pos1
	 */
	public static Vec2f minDisp(final Vec2f pos2, final Vec2f pos1) {
		return minDisp(new Vec2f(), pos2, pos1);
	}
	
	/**
	 * Sets result as A*k.
	 * No new Object created.
	 * @param result
	 * @param A
	 * @param B
	 */
	public static Vec2f scale(Vec2f result, final Vec2f A, final float k) {
		result.x = A.x*k;
		result.y = A.y*k;
		
		return result;
	}
	
	/**
	 * Returns A*k.
	 * New object created.
	 * @param A
	 * @param B
	 */
	public static Vec2f scale(final Vec2f A, final float k) {
		return scale(new Vec2f(), A, k);
	}
	
	/**
	 * Sets result as A + k*B.
	 * No new object created.
	 * @param result
	 * @param A
	 * @param B
	 * @param k
	 */
	public static Vec2f increment(Vec2f result, final Vec2f A, final Vec2f B, final float k) {
		result.x = A.x + k*B.x;
		result.y = A.y + k*B.y;
		
		return result;
	}
	
	/**
	 * Returns A + k*B.
	 * New object created.
	 * @param A
	 * @param B
	 * @param k
	 */
	public static Vec2f increment(final Vec2f A, final Vec2f B, final float k) {
		return increment(new Vec2f(), A, B, k);
	}
	
	/**
	 * Returns dot product between A and B (= |A|*|B|*cos(<AB)).
	 * @param A
	 * @param B
	 * @return
	 */
	public static float dot(final Vec2f A, final Vec2f B) {
		return A.x*B.x + A.y*B.y;
	}
	
	/**
	 * Returns the magnitude/length of instance vector.
	 * @return
	 */
	public float lengthSq() {
		return x*x + y*y;
	}
	
	/**
	 * Sets vector to length 1 in same direction.
	 */
	public void normalise() {
		scale(this,this, (float) (1/Math.sqrt(lengthSq())));
	}
	
	/**
	 * Sets coordinate to the minimum displacement between the coordinate and origin.
	 */
	public void nomaliseCoordinates() {
		minDisp(this, this, Origin);
	}
	
	//Might use this instead of method above.
	//Sets pos components to go between -1 and 1.
		private void normalisePos() {
			if(x > 1) 
				x = -1 + x%1f;
			if(x < -1)
				x = 1 + x%1f;
			
			if(y > 1) 
				y = -1 + y%1f;
			if(y < -1)
				y = 1 + y%1f;
		}
}
