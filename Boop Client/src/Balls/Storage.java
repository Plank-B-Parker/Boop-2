package Balls;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import Math.physics;

public class Storage {
	// COPIED from server file

	private List<Ball> balls = new ArrayList<>();
	private List<Ball> removedBalls = new ArrayList<>();
	
	public Storage() {
		balls = Collections.synchronizedList(balls);
	}
	
	public void updateBalls(float dt) {
		physics.checkCollision(balls);
		
		synchronized (balls) {
			for(Ball ball: balls) {
				ball.phys.calcAcc(balls);
			}
		}
		
		synchronized (balls) {
			for(Ball ball: balls) {
				ball.phys.update(dt);
			}
		}
		
	}
	
	public void setBallData(int ID, int type, float x, float y, float velx, float vely, int ownerID) {
		boolean ballFound = false;
		for (Ball ball: balls) {
			if (ball.getID() == ID) {
				ball.setPos(x, y);
				ball.setVel(velx, vely);
				// set OwnerID
				ball.setType(type);
				ballFound = true;
			}
		}
		if (! ballFound) {
			Ball ball = new Ball(type);
			ball.setPos(x, y);
			ball.setVel(velx, vely);
			// set OwnerID
			ball.setType(type);
			balls.add(ball);
		}
	}
	
	public void renderBalls(Graphics2D g, float dt) {
		float energy = 0;
		
		synchronized (balls) {
			for (Ball ball: balls) {
				ball.render(g, dt);
				energy += ball.phys.calcEnergy(balls);
			}
		}

		g.setColor(Color.PINK);
		g.drawString("energy: " + energy, 20, 200);
	}
	
	public void add(Ball b) {
		balls.add(b);
		b.setID(balls.size()-1);
	}
	
	private int removedCount = 0;
	public void remove(Ball b) {
		balls.remove(b);
		//Top secret: Classified code right here.
		removedBalls.add(b);
		System.out.println(removedCount++);
	}
	
	public Ball getBall(int ID) {
		return balls.get(ID);
	}

}
