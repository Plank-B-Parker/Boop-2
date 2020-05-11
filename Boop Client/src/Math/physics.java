package Math;

import java.util.ArrayList;

import Balls.Ball;

public class physics {
	
	//For pos, x and y go between -1 and 1.
	//If they go above 1, then loop back to -1.
	//If they go below -1, they loop to 1. 
	public Vec2f pos;
	public Vec2f targetPos;
	private Vec2f posError;
	
	public Vec2f vel;
	//Vec2f targetVel;
	//Vec2f velError;
	
	public Vec2f acc;
	
	//Time between getting updates from server.
	private static float server_dt = 1f/30f;
	
	
	public float mass;
	public float bounciness;
	
	public physics() {
		// TODO Auto-generated constructor stub
	}
	
	//Try changing pos proportional to their error.
	public void update(float dt) {
		calcAcc();
		//Predict target pos.
		Vec2f.increment(vel, vel, acc, dt);
		Vec2f.increment(targetPos, targetPos, vel, dt);
		Vec2f.increment(pos, pos, vel, dt);
		
		//Calc the errors.
		Vec2f.sub(posError, targetPos, pos);
		
		//Correct position.
		Vec2f.increment(pos, pos, posError, dt/server_dt);
		
		normalisePos();
	}
	
	public static void checkCollision(ArrayList<Ball> balls) {
		Vec2f disp = new Vec2f();
		
		for(int i = 0; i < balls.size() - 1; i++) {
			Ball ball = balls.get(i);
			//Check if ball is not empty.
			if(ball.getID() == -1)
				continue;
			for(int j = i + 1; j < balls.size(); j++) {
				Ball otherBall = balls.get(j);
				//Check if ball is not empty.
				if(otherBall.getID() == -1)
					continue;
				
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
		
		targetPos.x = targetPos.x % 1f;
		targetPos.y = targetPos.y % 1f;
	}

	private void calcAcc() {
		acc.set(0, 0);
	}
	
	public void setPos(float x, float y) {
		targetPos.x = x;
		targetPos.y = y;
	}
	
	public void setVel(float x, float y) {
		vel.x = x;
		vel.y = y;
	}

}
