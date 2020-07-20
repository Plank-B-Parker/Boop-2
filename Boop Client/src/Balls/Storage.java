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

	public int numBalls = 0;
	
	private List<Ball> balls = new ArrayList<>();
	//List of slots that are empty in the balls list.
	private Stack<Integer> emptySlots = new Stack<>();
	private List<Ball> removedBalls = new ArrayList<>();
	
	private float timeBeforeRemovingAbandonedBall = 5;
	
	public Storage() {
		balls = Collections.synchronizedList(balls);
	}
	
	public void updateBalls(float dt) {
		physics.checkCollision(balls);
		
		synchronized (balls) {
			for(Ball ball: balls) {
				if(ball.getID() != -1)
					ball.phys.calcAcc(balls);
			}
		}
		
		synchronized (balls) {
			for(Ball ball: balls) {
				if(ball.getID() != -1)
					ball.phys.update(dt);
			}
		}
		
		List<Ball>ballsToRemove = new ArrayList<Ball>();
		synchronized (balls) {
			for(Ball ball: balls) {
				if(ball.getID() != -1) {
					ball.updateTimer(dt);
					if(ball.getTimeAlive() > timeBeforeRemovingAbandonedBall || ball.toBeRemoved()) ballsToRemove.add(ball);
				}
			}
			for(Ball ball: ballsToRemove) remove(ball);
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
				if(ball.getID() == -1) continue;
				ball.render2(g, dt);
				energy += ball.phys.calcEnergy(balls);
			}
		}

		g.setColor(Color.PINK);
		g.drawString("energy: " + energy, 20, 200);
	}
	
	//Handles server balls.
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
		numBalls++;
	}
	
	//Handles client created balls.
	public void add(Ball ball) {
		//Add index of new ball to client created balls index list.
		emptySlots.add(balls.size());
		
		balls.add(ball);
		//Client created balls have an index of -2.
		ball.setID(-2);
		numBalls++;
	}

	public void remove(Ball b) {
		int index = balls.indexOf(b);
		//Add an empty slot
		emptySlots.push(index);
		b.setID(-1);
		numBalls--;
	}
	
	public Ball getBall(int index) {
		return balls.get(index);
	}

}
