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
	//List of slots that are empty in the balls list.
	private Stack<Integer> emptySlots = new Stack<>();
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
	
	public void setBallData(float data[]) {
		int ID = (int)data[0];
		
		if(ID == 0) {
			return;
		}
		
		boolean ballFound = false;
		for (Ball ball: balls) {
			if (ball.getID() == ID) {
				ball.updateBall(data);
				ballFound = true;
			}
		}
		if (! ballFound) {
			add(data);
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
	
	public void add(float data[]) {
		//Check if there is an empty slot for this ball.
		if(!emptySlots.isEmpty()) {
			int index = emptySlots.pop();
			Ball ball = balls.get(index);
			ball.setBall(data);
			return;
		}
		Ball ball = new Ball(data);
		balls.add(ball);
	}

	public void remove(Ball b) {
		int index = balls.indexOf(b);
		//Add an empty slot
		emptySlots.push(index);
		b.setID(-1);
	}
	
	public Ball getBall(int ID) {
		return balls.get(ID);
	}

}
