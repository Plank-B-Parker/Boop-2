package math;

import java.util.List;
import balls.Ball;
import balls.Storage;

public class Physics {
	
	//For pos, x and y go between -1 and 1.
	//If they go above 1, then loop back to -1.
	//If they go below -1, they loop to 1. 
	public Vec2f pos = new Vec2f();
	public Vec2f vel = new Vec2f();
	public Vec2f acc = new Vec2f();
	
	public float mass;
	public float mag = 0.02f;
	public boolean magnetic = false;
	public float bounciness = 1f;
	private static float dragCoefficient = 10f;
	
	private Ball owner;
	
	//Temp variables to avoid object creation;
	static VecPool tempVecs = new VecPool();
	
	public Physics(Ball owner) {
		this.owner = owner;
	}
	
	/**Calculates and returns the energy of the current ball.
	 * 
	 * @param balls: the list of balls that this ball is attracted/repulsed by.
	 * @param dt
	 * @return
	 */
	public float calcEnergy(List<Ball> balls) {
		tempVecs.startOfMethod();
		/////////////////////////
		
		float KE = 0.5f*mass*vel.lengthSq();
		float PE = 0;
		
		Vec2f disp = tempVecs.getVec();

		for (Ball ball: balls) {
			if(ball == owner)
				continue;
			disp(disp, ball.phys.pos, pos);
			float dist = (float)Math.sqrt(disp.lengthSq());
			PE -= mag*ball.phys.mag/dist;
		}
		
		/////////////////////////
		tempVecs.endOfMethod();
		
		return PE + KE;
	}
	
	/**
	 * Increments the velocity and position. And normalises the position to be between -1 and 1.
	 * @param dt: Time step.
	 */
	public void update(float dt) {
		Vec2f.increment(vel, vel, acc, dt);
		Vec2f.increment(pos, pos, vel, dt);
		
		//Set pos to be between -1 and 1;
		normalisePos();
	}
	
	//Might try a more accurate integration method if I figure it out.
	public void updateRungaCutta(float dt) {
		
	}
	
	/**
	 * Calculates the attractions of the ball to other balls in the list. 
	 * @param balls: The balls this ball is attracted to.
	 */
	public void calcAttraction(List<Ball> balls) {
		//Strong mid range attractive force.
		addAttraction(balls, 1f, owner.getRad()*5, 0.5f);
		//weaker small range repulsive force
		addAttraction(balls, -10f, owner.getRad(), owner.getRad()*5f);
	}
	
	/**
	 * Sets acceleration to 0, do this before calculating forces.
	 */
	public void nullifyForces() {
		acc.set(0, 0);
	}
	
	/**
	 * Adds drag to acceleration.
	 */
	public void calcDrag() {
		addDrag();
	}
	
	public void calcClientAttraction(Vec2f clientPos, float clientStrength) {
		this.addAttraction(clientPos, clientStrength);
	}
	
	//Drag force.
	private void addDrag() {
		Vec2f.increment(acc, acc, vel, -dragCoefficient*(float)Math.sqrt(vel.lengthSq())/mass);
	}
	
	//TODO: Implement some sort of broad phase collision detection.
	/**
	 * Checks and resolves collisions with all balls in the list.
	 * @param balls: A list of balls that could be colliding.
	 */
	public static void checkCollision(Storage balls) {
		tempVecs.startOfMethod();
		/////////////////////////
		
		Vec2f disp = tempVecs.getVec();
		
		for(var i = 0; i < balls.numBalls - 1; i++) {
			var ball = balls.getBall(i);
			if (ball.getID() == -1) continue;
			
			//Go through every ball after current ball in list.
			for(var j = i + 1; j < balls.numBalls; j++) {
				var otherBall = balls.getBall(j);
				
				if (otherBall.getID() == -1) continue;
				
				disp(disp, otherBall.phys.pos, ball.phys.pos);
				float minimumDistance = ball.getRad() + otherBall.getRad();
				
				//If distance between centres is bigger that the sum of the radi than skip.
				if (disp.lengthSq() > minimumDistance*minimumDistance) {
					continue;
				}
				
				float distance = (float)Math.sqrt(disp.lengthSq());
				float overlap = minimumDistance - distance;
				
				//Normalise disp.
				Vec2f.scale(disp, disp, 1f/distance);
				
				//Move balls apart to stop overlap.
				ball.moveBall(disp, -overlap*0.5f);
				otherBall.moveBall(disp, overlap*0.5f);
				
				float impulse = calcImpulse(ball, otherBall, disp);
				
				//Add impulse to the velocities.
				Vec2f.increment(ball.phys.vel, ball.phys.vel, disp, -impulse/ball.phys.mass);
				Vec2f.increment(otherBall.phys.vel, otherBall.phys.vel, disp, impulse/otherBall.phys.mass);
				
				
				// If the types of each ball are exploding types, explode them into 16 smaller balls with 2J of explosion power
				if (ball.getType() == 2 && otherBall.getType() == 2) {
					Vec2f pos1 = tempVecs.getVec();
					Vec2f pos2 = tempVecs.getVec();
					
					pos1 = ball.phys.pos.copy();
					pos2 = otherBall.phys.pos.copy();
					
					ball.phys.explode(balls, 16, 0f);
					otherBall.phys.explode(balls, 16, 0f);

					shockwave(balls, pos1, 0.02f);
					shockwave(balls, pos2, 0.02f);
				}
			}
		}
		
		///////////////////////
		tempVecs.endOfMethod();
	}
	
	//Calculates impulse of a collision.
	private static float calcImpulse(Ball ball1, Ball ball2, Vec2f norm) {
		float e1 = ball1.phys.bounciness, e2 = ball2.phys.bounciness;
		float m1 = ball1.phys.mass, m2 = ball2.phys.mass;
		
		float bounce = (e1 + e2)*0.5f;
		Vec2f v1 = ball1.phys.vel, v2 = ball2.phys.vel;
		
		return (1+bounce)*(Vec2f.dot(v1, norm) - Vec2f.dot(v2, norm))/(1/m1 + 1/m2);
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
	private void addAttraction(List<Ball> balls, float attractionStrength, float minDist, float maxDist) {
		tempVecs.startOfMethod();
		/////////////////////////
		
		for(Ball ball: balls) {
			//Skip if the other ball is this ball.
			if (ball == owner) continue;
			
			Vec2f disp = tempVecs.getVec();
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
		
		///////////////////////
		tempVecs.endOfMethod();
	}
	
	private void addAttraction(Vec2f pos, float strength) {
		tempVecs.startOfMethod();
		/////////////////////////
		
		Vec2f disp = tempVecs.getVec();
		disp(disp, pos, this.pos);
		
		float distSq = disp.lengthSq();
		if(distSq < 0.005) return;
		
		float distCubed = (float)(distSq*Math.sqrt(distSq));
		Vec2f.increment(acc, acc, disp, strength/(mass*distCubed));
		
		///////////////////////
		tempVecs.endOfMethod();
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
	
	
	private static int removeCount = 0;
	// Makes a ball explode into a given number of parts with a given amount of extra energy
	private void explode(Storage balls, int parts, float energy) {
		tempVecs.startOfMethod();
		/////////////////////////
		
		// Remove the ball that is exploding
		owner.remove();
		removeCount++;
		System.out.println(removeCount);
		
		// Calculate the new radius and mass of each smaller ball part
		float newRad = (float) (owner.getRad() / Math.sqrt(parts));
		float newMass = mass / parts;
		
		// The small balls will spawn on the vertices of a regular polygon, with as many sides as there parts,
		// with sides the length of a small ball's diameter and centred on the original ball's position.
		
		// Calculate the distance from the centre of the polygon to a vertex
		float polygonRad = (float) (newRad / Math.sin(Math.PI/parts));
		
		// Calculate the angle made by connecting two adjacent vertices to the centre
		float angleDif = (float) (Math.PI * 2 / parts);
		
		// Calculate the magnitude of the extra velocity given to each small ball
		float velAdd = (float) Math.sqrt(2 * energy / (mass * parts));
		
		// Spawn in the small balls
		for (var i = 0; i < parts; i++) {
			
			var ball = new Ball(3);
			ball.setRad(newRad);
			ball.phys.mass = newMass;
			
			Vec2f direction = tempVecs.getVec();
			direction.x = (float)Math.cos(angleDif * i);
			direction.y = (float)Math.sin(angleDif * i);
			
			//Increment: ballPos = pos + direction*polygonRad
			var ballPos = Vec2f.increment(tempVecs.getVec(), pos, direction, polygonRad);
			ball.setPos(ballPos);
			Vec2f.increment(ball.phys.vel, vel, direction, velAdd);
			balls.add(ball);
		}
		
		/////////////////////////
		tempVecs.endOfMethod();
	}
	
	//Produces a shock wave that makes the balls move.
	private static void shockwave(Storage balls, Vec2f centre, float impulse) {
		tempVecs.startOfMethod();
		/////////////////////////
		
		for(var i = 0; i < balls.numBalls; i++) {
			var ball = balls.getBall(i);
			if(ball.getID() == -1) continue;
			
			var disp = tempVecs.getVec();
			Vec2f.sub(disp, ball.phys.pos, centre);
			float distCubed = disp.lengthSq();
			distCubed *= Math.sqrt(distCubed);
			
			Vec2f.increment(ball.phys.vel, ball.phys.vel, disp, impulse/(ball.phys.mass*distCubed + 0.01f));
		}
		
		///////////////////////
		tempVecs.endOfMethod();
	}

	public final Ball getOwner() {
		return owner;
	}

	public final void setOwner(Ball owner) {
		this.owner = owner;
	}
	
}
