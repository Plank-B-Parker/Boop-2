package Math;

import java.util.List;

import Balls.Ball;

public class physics {
	
	//For pos, x and y go between -1 and 1.
	//If they go above 1, then loop back to -1.
	//If they go below -1, they loop to 1. 
	public Vec2f pos = new Vec2f();
	public Vec2f vel = new Vec2f();
	public Vec2f acc = new Vec2f();
	public float mass;
	public float mag = 0.02f;
	public float bounciness;
	private static float dragCoefficient = 10f;
	
	public Ball owner;
	
	//Temp variables to avoid object creation;
	final private static Vec2f temp1 = new Vec2f();
	final private static Vec2f temp2 = new Vec2f();
	final private static Vec2f temp3 = new Vec2f();
	
	public physics(Ball owner) {
		this.owner = owner;
	}
	
	/**Calculates and returns the energy of the current ball.
	 * 
	 * @param balls: the list of balls that this ball is attracted/repulsed by.
	 * @param dt
	 * @return
	 */
	public float calcEnergy(List<Ball> balls) {
		float KE = 0.5f*mass*vel.lengthSq();
		float PE = 0;
		
		Vec2f disp = temp1;
		for(Ball ball: balls) {
			if(ball == owner)
				continue;
			disp(disp, ball.phys.pos, pos);
			float dist = (float)Math.sqrt(disp.lengthSq());
			PE -= mag*ball.phys.mag/dist;
		}
		
		return PE + KE;
	}
	
	/**
	 * Increments the velocity and position. And normalises the position to be between -1 and 1.
	 * @param dt: Time step.
	 */
	public void update(float dt) {
		Vec2f.increment(vel, vel, acc, dt);
		Vec2f.increment(pos, pos, vel, dt);
		normalisePos();
	}
	
	//Might try a more accurate integration method if I figure it out.
	public void updateRungaCuttaYoMama(float dt) {
		
	}
	
	/**
	 * Calculates the acceleration of the ball. 
	 * @param balls: The balls this ball is attracted to.
	 */
	public void calcAcc(List<Ball> balls) {
		acc.set(0, 0);
		//Strong mid range attractive force.
		addAttraction(acc, balls, 2f, owner.getRad()*5, 0.5f);
		//weaker small range repulsive force
		addAttraction(acc, balls, -5f, owner.getRad(), owner.getRad()*5);
		//Drag force to stop spinning.
		addDrag(acc);
	}
	
	//Drag force.
	private void addDrag(Vec2f acc) {
		Vec2f.increment(acc, acc, vel, -dragCoefficient*(float)Math.sqrt(vel.lengthSq()));
	}
	
	//TODO: Implement some sort of broad phase collision detection.
	/**
	 * Checks and resolves collisions with all balls in the list.
	 * @param balls: A list of balls that could be colliding.
	 */
	public static void checkCollision(List<Ball> balls) {
		Vec2f disp = temp1;
		
		for(int i = 0; i < balls.size() - 1; i++) {
			Ball ball = balls.get(i);
			//Go through every ball after current ball in list.
			for(int j = i + 1; j < balls.size(); j++) {
				Ball otherBall = balls.get(j);
				
				disp(disp, otherBall.phys.pos, ball.phys.pos);
				float minimumDistance = ball.getRad() + otherBall.getRad();
				
				//If distance between centres is bigger that the sum of the radi than skip.
				if(disp.lengthSq() > minimumDistance*minimumDistance)
					continue;
				
				float distance = (float)Math.sqrt(disp.lengthSq());
				float overlap = minimumDistance - distance;
				
				//Normalise disp.
				Vec2f.scale(disp, disp, 1f/distance);
				
				//Move balls apart to stop overlap.
				Vec2f.increment(ball.phys.pos, ball.phys.pos, disp, -overlap*0.5f);
				Vec2f.increment(otherBall.phys.pos, otherBall.phys.pos, disp, overlap*0.5f);
				
				float impulse = calcImpulse(ball, otherBall, disp);
				
				//Add impulse to the velocities.
				Vec2f.increment(ball.phys.vel, ball.phys.vel, disp, -impulse);
				Vec2f.increment(otherBall.phys.vel, otherBall.phys.vel, disp, impulse);
				
			}
		}
	}
	
	//Calculates impulse of a collision.
	private static float calcImpulse(Ball ball1, Ball ball2, Vec2f norm) {
		float e1 = ball1.phys.bounciness, e2 = ball2.phys.bounciness;
		float m1 = ball1.phys.mass, m2 = ball2.phys.mass;
		
		float bounce = (e1 + e2)*0.5f;
		Vec2f v1 = ball1.phys.vel, v2 = ball2.phys.vel;
		
		float impulse = (1+bounce)*(Vec2f.dot(v1, norm) - Vec2f.dot(v2, norm))/(1/m1 + 1/m2);
		
		return impulse;
	}
	
	/**
	 * Calculates minimum distance between two points.
	 * @param result
	 * @param pos2
	 * @param pos1
	 */
	public static void disp(Vec2f result, Vec2f pos2, Vec2f pos1) {
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
		
		//Length of this vector is often divided by. Avoid 0 displacement. Might remove later.
		if(x == 0 && y == 0) {
			result.x = (float) Math.random();
			result.y = (float) Math.random();
		}
	}
	
	//adds gravitational attraction to acc.
	/**
	 * Avoid making min Dist 0 as you can get infinite force if you're not careful.
	 * @param acc: Current acceleration.
	 * @param balls: List of balls that this ball could be attracted to.
	 * @param attractionStrength: Repulsive if negative.
	 * @param minDist: Balls closer than this wont be affected.
	 * @param maxDist: Balls farther than this wont be affected.
	 */
	private void addAttraction(Vec2f acc, List<Ball> balls, float attractionStrength, float minDist, float maxDist) {
		for(Ball ball: balls) {
			//Skip if the other ball is this ball.
			if(ball == owner) 
				continue;
			Vec2f disp = temp1;
			disp(disp, ball.phys.pos, pos);
			
			//If distance is less than minDist then skip.
			if(disp.lengthSq()<minDist*minDist)
				continue;
			//If distance is bigger than maxDist then skip.
			if(disp.lengthSq()>maxDist*maxDist)
				continue;
			
			float distCubed = disp.lengthSq();
			distCubed *= Math.sqrt(distCubed);
			Vec2f.increment(acc, acc, disp, attractionStrength*mag*ball.phys.mag/(mass*distCubed));
		}
	}
	
	//Sets pos components to go between -1 and 1.
	private void normalisePos() {
		if(pos.x > 1) 
			pos.x = -1 + pos.x%1f;
		if(pos.x < -1)
			pos.x = 1 + pos.x%1f;
		
		if(pos.y > 1) 
			pos.y = -1 + pos.y%1f;
		if(pos.y < -1)
			pos.y = 1 + pos.y%1f;
	}
	
}
