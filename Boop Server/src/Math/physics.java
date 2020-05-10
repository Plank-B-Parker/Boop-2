package Math;

import java.util.ArrayList;

import Balls.Ball;

public class physics {
	
	public Vec2f pos;
	public Vec2f vel = new Vec2f();
	public Vec2f acc = new Vec2f();
	public float mass;
	public float bounciness;
	
	public physics() {
		
	}
	
	public void update(float dt) {
		calcAcc();
		Vec2f.increment(vel, vel, acc, dt);
		Vec2f.increment(pos, pos, vel, dt);
		normalisePos();
	}
	
	public static void checkCollision(ArrayList<Ball> balls) {
		Vec2f disp = new Vec2f();
		
		for(int i = 0; i < balls.size() - 1; i++) {
			Ball ball = balls.get(i);
			for(int j = i + 1; j < balls.size(); j++) {
				Ball otherBall = balls.get(j);
				
				Vec2f.sub(disp, otherBall.getPos(), ball.getPos());
				float minimumDistance = ball.getRad() + otherBall.getRad();
				if(disp.lengthSq() > minimumDistance*minimumDistance)
					continue;
				
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
	
	public static float calcImpulse(Ball ball1, Ball ball2, Vec2f norm) {
		float e1 = ball1.getBounciness(), e2 = ball2.getBounciness();
		float m1 = ball1.getMass(), m2 = ball2.getMass();
		
		float bounce = (e1 + e2)*0.5f;
		Vec2f v1 = ball1.getVel(), v2 = ball2.getVel();
		
		float impulse = (1+bounce)*(Vec2f.dot(v1, norm) - Vec2f.dot(v2, norm))/(1/m1 + 1/m2);
		
		return impulse;
	}
	
	//Sets pos components to go between -1 and -1.
	private void normalisePos() {
		pos.x = pos.x % 1f;
		pos.y = pos.y % 1f;
	}
	
	private void calcAcc() {
		acc.set(0, 0);
	}
	
}
