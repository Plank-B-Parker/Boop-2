package Balls;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import Math.Vec2f;
import Math.physics;
import Mian.main;

public class Storage {

	private List<Ball> balls = new ArrayList<>();
	
	public Storage() {
		
	}
	
	public void updateBalls(float dt) {
		physics.checkCollision(balls);
		for(Ball ball: balls) {
			ball.update(dt);
		}
	}
	
	public void renderBalls(Graphics2D g, float dt) {
		for(Ball ball: balls) {
			Vec2f pos = ball.getPos();
			Vec2f vel = ball.getVel();
			float rad = ball.getRad();
			
			float x = pos.x + vel.x*dt;
			float y = pos.y + vel.y*dt;
			
			int X = (int)((x + 1)*0.5*main.windowWidth);
			int Y = (int)((y + 1)*0.5*main.windowHeight);
			int RadX = (int)(0.5*rad*main.windowWidth);
			int RadY = (int)(0.5*rad*main.windowHeight);
			
			if(x + rad > 1 && x - rad < 1) {
				
			}
			else if(x + rad > -1 && x - rad < -1) {
				
			}
			
		}
	}
	
	public void add(Ball b) {
		balls.add(b);
	}
	
	public void remove(Ball b) {
		balls.remove(b);
	}
	
	public Ball getBall(int ID) {
		return balls.get(ID);
	}

}
