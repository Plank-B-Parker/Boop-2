package balls;

import java.awt.Color;

public enum BallType {
	
	PLAIN (1, 0.01f, 1, Color.BLUE),
	EXPLOSIVE (32, 0.04f, 1f, Color.RED),
	SHRAPNEL (2, 0.01f, 0f, Color.YELLOW),
	GHOST (1, 0.01f, 1, Color.BLUE);
	
	private static int numberOfTypes = BallType.values().length;
	
	private float mass;
	private float radius;
	private float bounciness;
	private Color colour;
	
	//Table that describes the forces between different pairs of types of balls.
	//Should this be in physics class?
	//Should restructure this so that force constants are obtained using the radius of types as equilibrium is at sqrt(Near/Far). 
	//Ask Ibraheem about ^^ if it isn't very clear.
	private static final float [][][] FORCE_PAIRS = 
			{
			//		  PLAIN			   EXPLOSIVE 	 	SHRAPNEL		 GHOST			  CLIENT
					{ {-0.02f, +2.0f}, {-0.07f, +1.0f}, {-0.05f, +1.5f}, {-0.01f, +2.0f}, {-0f, +50.0f} },	//PLAIN
					{ {-0.07f, +1.0f}, {+0.01f, -4.0f}, {-0.80f, -9.0f}, {-0.01f, +2.0f}, {-0f, +200.0f} },	//EXPLOSIVE
					{ {-0.05f, +1.5f}, {-0.80f, -9.0f}, {-0.02f, +1.0f}, {-0.01f, +2.0f}, {-0f, +20.0f} },	//SHRAPNEL
					{ {-0.01f, +2.0f}, {-0.01f, +2.0f}, {-0.01f, +2.0f}, {-0.01f, +2.0f}, {-0f, +50.0f} }	//GHOST
			};
	//					{near force, far force}
	//					Negative is repulsive, positive is attractive.
	
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
		return FORCE_PAIRS[a.ordinal()][b.ordinal()];
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
