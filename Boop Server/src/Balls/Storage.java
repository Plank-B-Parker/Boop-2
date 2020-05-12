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
			
			//Scaling for screen.
			int X = (int)((x + 1)*0.5*main.windowWidth);
			int Y = (int)((y + 1)*0.5*main.windowHeight);
			int RadX = (int)(0.5*rad*main.windowWidth);
			int RadY = (int)(0.5*rad*main.windowHeight);
			
			//Second set of coordinates for edge.
			int X2 = X;
			int Y2 = Y;
			
			if(x + rad > 1 && x - rad < 1) {
				X2 = X - main.windowWidth;
			}
			else if(x + rad > -1 && x - rad < -1) {
				X2 = X + main.windowWidth;
			}
			
			if(y + rad > 1 && y - rad < 1) {
				Y2 = Y - main.windowHeight;
			}
			else if(y + rad > -1 && y - rad < -1) {
				Y2 = Y + main.windowHeight;
			}
			
			g.setColor(ball.getColour());
			g.fillOval(X - RadX, Y - RadY, 2*RadX, 2*RadY);
			if(X2 != X && Y2 != Y) {
				g.fillOval(X2 - RadX, Y2 - RadY, 2*RadX, 2*RadY);
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
