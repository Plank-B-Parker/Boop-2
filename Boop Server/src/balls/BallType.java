package balls;

import java.awt.Color;

public enum BallType {
	
	PLAIN (1, 0.01f, 1, Color.BLUE),
	EXPLOSIVE (32, 0.04f, 1f, Color.RED),
	SHRAPNEL (2, 0.01f, 0f, Color.YELLOW),
	GHOST (1, 0.01f, 1, Color.BLUE);
	
	private static int numberOfTypes = 4;
	
	private float mass;
	private float radius;
	private float bounciness;
	private Color colour;
	
	//Table that contains constants that can be used to get forces between pairs of types.
	//Should this be in physics class? 
	//Ask Ibraheem about table if it isn't very clear.
	private static final float [][][] FORCE_PAIRS = 
			{
			//		  PLAIN			   	EXPLOSIVE 	 	  SHRAPNEL		 	GHOST			  CLIENT
					{ {2.00f, -10.0f}, {-28.00f, +1.0f}, {-125.0f, +1.5f}, {-25.00f, +2.0f}, {-0f, +350.00f} },	//PLAIN
					{ {-28.00f, +1.0f}, {+1.563f, -4.0f}, {-32.00f, -9.0f}, {-4.000f, +2.0f}, {-0f, +200.0f} },	//EXPLOSIVE
					{ {-125.0f, +1.5f}, {-32.00f, -9.0f}, {-50.00f, +1.0f}, {-25.00f, +2.0f}, {-0f, +20.00f} },	//SHRAPNEL
					{ {-25.00f, +2.0f}, {-4.000f, +2.0f}, {-25.00f, +2.0f}, {-25.00f, +2.0f}, {-0f, +50.00f} }	//GHOST
			};
	//					{(near force)/ ballRadius^2, far force}
	//					Negative is repulsive, positive is attractive.
	
	//		Plain on Plain = {-50.00f, 2.00f}
	
	private BallType(float mass, float radius, float bounciness, Color colour) {
		this.mass = mass;
		this.radius = radius;
		this.bounciness = bounciness;
		this.colour = colour;
	}
	
	public static BallType getType(int n) {
		return values()[n];
	}
	
	/**
	 * Returns the constants that determine forces between two ball types 
	 * @param a - Type of first ball.
	 * @param b - Type of second ball.
	 * @return - An array of size two in the form [near force, far force]
	 */
	public static float[] getTypeForces(BallType a, BallType b) {
		float near = FORCE_PAIRS[a.ordinal()][b.ordinal()][0];
		float far = FORCE_PAIRS[a.ordinal()][b.ordinal()][1];
		
		float[] forcePair =  { near*(a.radius + b.radius)*(a.radius + b.radius), far };
		return forcePair;
	}
	
	/**
	 * Returns the attraction to client for particular type t.
	 * @param t
	 * @return - Float
	 */
	public static float getClientForce(BallType t) {
		return FORCE_PAIRS[t.ordinal()][numberOfTypes][1];
	}
	
	public float getMass() {
		return mass;
	}

	public float getRadius() {
		return radius;
	}

	public float getBounciness() {
		return bounciness;
	}

	public Color getColour() {
		return colour;
	}
}
