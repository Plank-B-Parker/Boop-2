package math;

import java.util.List;

import balls.Ball;
import balls.BallType;
import balls.Storage;

public class Physics {
	
	//Motion and position attributes. 
	public Vec2f pos = new Vec2f();
	public Vec2f vel = new Vec2f();
	public Vec2f acc = new Vec2f();
	//For pos, x and y go between -1 and 1.
	//If they go above 1, then loop back to -1.
	//If they go below -1, they loop to 1.
	
	//Material attributes.
	private float mass;
	private float bounciness = 1f;
	
	//Game attributes.
	private Ball owner;
	
	//Global world attributes.
	private static float dragCoefficient = 5f;
	private static float magStrength = 0.01f;
	
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

		//Calculate potential energy.
		
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
		pos.nomaliseCoordinates();
	}
	
	//Might try a more accurate integration method if I figure it out.
	private void updateRungaCutta(float dt) {
		
	}
	
	/**
	 * Computes and adds the force of attraction to other balls in the list. 
	 * @param balls: The balls this ball is attracted to.
	 */
	public void calcAttraction(List<Ball> balls) {
		addAttraction(balls, 0f, 3f);
	}
	
	/**
	 * Computes and adds force attracting ball to client's centre.
	 * @param clientPos
	 * @param clientStrength
	 */
	public void calcClientAttraction(Vec2f clientPos, float clientStrength, float radOfInf) {
		this.addAttraction(clientPos, clientStrength, radOfInf/3f);
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
		//Df = - Cd * |v| * v
		Vec2f.increment(acc, acc, vel, -dragCoefficient*(float)Math.sqrt(vel.lengthSq())/mass);
	}
	
	//TODO: Implement some sort of broad phase collision detection.
	/**
	 * Checks and resolves collisions with all balls in the list.
	 * @param balls: A list of balls that could be colliding.
	 */
	public static void checkCollision(Storage balls) {
		//Using vec pool as this generates a lot of vectors every update.
		tempVecs.startOfMethod();
		/////////////////////////
		
		//Vector for displacement between each pair of balls.
		Vec2f disp = tempVecs.getVec();
		
		for(var i = 0; i < balls.numBalls - 1; i++) {
			var ball = balls.getBall(i);
			if (ball.getID() == -1) continue;
			
			//Go through every ball after current ball in list.
			for(var j = i + 1; j < balls.numBalls; j++) {
				var otherBall = balls.getBall(j);
				if (otherBall.getID() == -1) continue; //The ball's not real :(
				
				Vec2f.minDisp(disp, otherBall.phys.pos, ball.phys.pos);	//disp = p2 - p1
				float minimumDistance = ball.getRad() + otherBall.getRad();	// Minimum distance = distance between balls when they touch.
				
				//If distance between centres is bigger that the sum of the radi than skip.
				if (disp.lengthSq() > minimumDistance*minimumDistance)
					continue;
				
				float distance = (float)Math.sqrt(disp.lengthSq());
				float overlap = minimumDistance - distance;	//Overlap of two balls radii.
				
				//Normalise disp to length 1.
				Vec2f.scale(disp, disp, 1f/distance);
				
				//Move balls apart to stop overlap.
				ball.moveBall(disp, -overlap*0.5f);
				otherBall.moveBall(disp, overlap*0.5f);
				
				//Impulse is momentum given to each ball due to collision.
				float impulse = calcImpulse(ball, otherBall, disp);
				
				//Add impulse to the velocities.
				Vec2f.increment(ball.phys.vel, ball.phys.vel, disp, -impulse/ball.phys.mass);
				Vec2f.increment(otherBall.phys.vel, otherBall.phys.vel, disp, impulse/otherBall.phys.mass);
				
				
				// If the types of each ball are exploding types, explode them into 16 smaller balls with 4J of explosion power
				if (ball.getType() == BallType.EXPLOSIVE && otherBall.getType() == BallType.EXPLOSIVE) {
					
					Vec2f pos1 = ball.phys.pos.copy();
					Vec2f pos2 = otherBall.phys.pos.copy();
					
					int numberOfParts = (int) (BallType.EXPLOSIVE.getMass()/BallType.SHRAPNEL.getMass());
					
					ball.phys.explode(balls, numberOfParts, 5f);
					otherBall.phys.explode(balls, numberOfParts, 5f);

					shockwave(balls, pos1, 0.16f);
					shockwave(balls, pos2, 0.16f);
				}
			}
		}
		
		///////////////////////
		tempVecs.endOfMethod();
	}
	
	/**
	 * Calculates impulse of a collision.
	 * @param ball1
	 * @param ball2
	 * @param norm
	 * @return
	 */
	private static float calcImpulse(Ball ball1, Ball ball2, Vec2f norm) {
		//Formula: 0.5*(e1 + e2)*dot(v1 - v2, normal)/(1/m1 + 1/m2)
		
		float e1 = ball1.phys.bounciness, e2 = ball2.phys.bounciness;
		float m1 = ball1.phys.mass, m2 = ball2.phys.mass;
		
		float bounce = (e1 + e2)*0.5f;
		Vec2f v1 = ball1.phys.vel, v2 = ball2.phys.vel;
		
		return (1+bounce)*(Vec2f.dot(v1, norm) - Vec2f.dot(v2, norm))/(1/m1 + 1/m2);
	}
	
	
	/**
	 * Adds magnetic attraction to acc due to other balls.
	 * Avoid making minDist 0 as you can get infinite force if you're not careful.
	 * @param balls: List of balls that this ball could be attracted to.
	 * @param minDist: Balls closer than this wont be affected.
	 * @param maxDist: Balls farther than this wont be affected.
	 */
	private void addAttraction(List<Ball> balls, float minDist, float maxDist) {
		tempVecs.startOfMethod();
		/////////////////////////
		
		Vec2f disp = tempVecs.getVec();	//Vector for displacement.
		
		for(Ball ball: balls) {
			//Skip if the other ball is this ball.
			if (ball == owner) continue;
			
			Vec2f.minDisp(disp, ball.phys.pos, pos);	//p2 - p1
			var distSq = disp.lengthSq();	//Distance squared.
			
			//If distance is less than minDist or bigger than maxDist, then skip.
			if(distSq < minDist*minDist || distSq > maxDist*maxDist)
				continue;
			
			//Get force constants for this type pairing.
			float[] forceConstants = BallType.getTypeForces(owner.getType(), ball.getType());

			//Near forces are repulsive if both magNears are the same sign.
			Vec2f nearForce = tempVecs.getVec();
			float nearForceConstant = forceConstants[0];
			Vec2f.scale(nearForce, disp, magStrength*nearForceConstant/(mass*distSq*distSq));
			
			//Formula: F = (S * K1 ) * (r/|r|^4)
			
			
			//Far forces are attractive if both magNears are the same sign.
			Vec2f farForce = tempVecs.getVec();
			float farForceConstant = forceConstants[1];
			Vec2f.scale(farForce, disp, magStrength*farForceConstant/(mass*distSq));
			
			//Formula: F = (S * K2 ) * (r/|r|^2)
			
			Vec2f.add(acc, acc, nearForce);
			Vec2f.add(acc, acc, farForce);
		}
		
		///////////////////////
		tempVecs.endOfMethod();
	}
	
	/**
	 * Adds magnetic attraction to acc due to a magnetic zone or area in the map
	 * @param pos - Position of zone.
	 * @param attractionStrength - Strength of attraction.
	 * @param magDensity - Magnetic density.
	 * @param rad - Radius of zone.
	 */
	private void addAttraction(Vec2f pos, float magDensity, float rad) {
		tempVecs.startOfMethod();
		/////////////////////////
		
		//Get force constant from type.
		float forceConstant = BallType.getClientForce(owner.getType());
		
		Vec2f disp = tempVecs.getVec();	//Vector for displacement
		Vec2f.minDisp(disp, pos, this.pos);	//p2 - p1
		
		float distSq = disp.lengthSq();
		
		//Area of magnetic zone.
		float area = (float) Math.PI*Math.min(distSq, rad*rad);	//If outside of magnetic zone, then use area of entire zone.
		
		Vec2f.increment(acc, acc, disp, magStrength*forceConstant*magDensity*area/(mass*distSq));	//Using magFar for this.
		//Formula: F = (K * m1 * area * magDensity) * (r/|r|^2)
		
		///////////////////////
		tempVecs.endOfMethod();
	}
	
	
	/**
	 *  Makes a ball explode into a given number of parts with a given amount of extra energy
	 * @param balls - Balls list.
	 * @param parts - Number of parts.
	 * @param energy - Shock wave energy.
	 */
	private void explode(Storage balls, int parts, float energy) {
		tempVecs.startOfMethod();
		/////////////////////////
		
		// Remove the ball that is exploding
		owner.remove();
		
		// Calculate the new radius and mass of each smaller ball part
		float newRad = (float) (owner.getRad() / Math.sqrt(parts));
		
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
			
			var ball = new Ball(BallType.SHRAPNEL);
			
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
			
			Vec2f.increment(ball.phys.vel, ball.phys.vel, disp, impulse/(ball.phys.mass*disp.lengthSq() + 0.00001f));
		}
		
		///////////////////////
		tempVecs.endOfMethod();
	}

	/**
	 * Returns the owner of the Physics object.
	 * @return
	 */
	public final Ball getOwner() {
		return owner;
	}
	/**
	 * Sets the owner of the Physics object.
	 * @param owner
	 */
	public final void setOwner(Ball owner) {
		this.owner = owner;
	}
	
	/**
	 * Sets the material attributes of ball using type.
	 */
	public void setType(BallType type) {
		mass = type.getMass();
		bounciness = type.getBounciness();
	}
	
}
