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
	public float bounciness;
	
	public Ball owner;
	
	//Temp variables to avoid object creation;
	final private static Vec2f temp1 = new Vec2f();
	final private static Vec2f temp2 = new Vec2f();
	final private static Vec2f temp3 = new Vec2f();
	
	public physics(Ball owner) {
		this.owner = owner;
	}
	
	public void update(float dt) {
		calcAcc();
		Vec2f.increment(vel, vel, acc, dt);
		Vec2f.increment(pos, pos, vel, dt);
		normalisePos();
	}
	
	public static void checkCollision(List<Ball> balls) {
		Vec2f disp = temp1;
		
		for(int i = 0; i < balls.size() - 1; i++) {
			Ball ball = balls.get(i);
			for(int j = i + 1; j < balls.size(); j++) {
				Ball otherBall = balls.get(j);
				
				Vec2f.sub(disp, otherBall.getPos(), ball.getPos());
				float minimumDistance = ball.getRad() + otherBall.getRad();
				if(disp.lengthSq() > minimumDistance*minimumDistance)
					continue;
				if(disp.lengthSq() == 0) {
					disp.x = (float)Math.random();
					disp.x = (float)Math.random();
					
				}
				float distance = (float)Math.sqrt(disp.lengthSq());
				float overlap = minimumDistance - distance;
				
				Vec2f.scale(disp, disp, 1f/distance);
				
				Vec2f.increment(ball.getPos(), ball.getPos(), disp, -overlap*0.5f);
				Vec2f.increment(otherBall.getPos(), otherBall.getPos(), disp, overlap*0.5f);
				
				float impulse = calcImpulse(ball, otherBall, disp);
				
				Vec2f.increment(ball.getVel(), ball.getVel(), disp, -impulse);
				Vec2f.increment(otherBall.getVel(), otherBall.getVel(), disp, impulse);
				
			}
		}
	}
	
	private static float calcImpulse(Ball ball1, Ball ball2, Vec2f norm) {
		float e1 = ball1.getBounciness(), e2 = ball2.getBounciness();
		float m1 = ball1.getMass(), m2 = ball2.getMass();
		
		float bounce = (e1 + e2)*0.5f;
		Vec2f v1 = ball1.getVel(), v2 = ball2.getVel();
		
		float impulse = (1+bounce)*(Vec2f.dot(v1, norm) - Vec2f.dot(v2, norm))/(1/m1 + 1/m2);
		
		return impulse;
	}
	
	//adds gravitational attraction to acc.
	private void addAttraction(Vec2f acc, List<Ball> balls) {
		for(Ball ball: balls) {
			if(ball == owner) 
				continue;
			Vec2f disp1 = temp1;
			Vec2f disp2 = temp2;
			
			Vec2f.sub(disp1, ball.getPos(), pos);
			
			
		}
	}
	
	//Sets pos components to go between -1 and 1.
	private void normalisePos() {
		while(pos.x > 1) 
			pos.x = pos.x - 2;
		while(pos.x < -1)
			pos.x = pos.x + 2;
		
		while(pos.y > 1) 
			pos.y = pos.y - 2;
		while(pos.y < -1)
			pos.y = pos.y + 2;
	}
	
	private void calcAcc() {
		acc.set(0, 0);
	}
	
}
