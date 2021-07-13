package balls;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import main.Player;
import main.PlayerHandler;
import math.Physics;

public class Storage {

	public int numBalls = 0;
	
	private List<Ball> balls = new ArrayList<>();
	private List<Ball> ballsToAdd = new ArrayList<>();
	//List of slots that are empty in the balls list.
	private Stack<Integer> emptySlots = new Stack<>();
	private List<Ball> removedBalls = new ArrayList<>();
	
	private float timeBeforeRemovingAbandonedBall = 5;
	
	public Storage() {
	}
	
	public void updateBalls(PlayerHandler players, float dt) {
		Physics.checkCollision(this);

		for (Ball ball: balls) {
			if(ball.getID() != -1) {
				ball.updateClientPrediction(dt);
			}
		}
		

		for(Ball ball: balls) {
			ball.phys.nullifyForces();
			ball.phys.calcDrag();
		}
		
		for(Player player: players) {
			for(Ball ball: player.localBalls) {
				if(ball.getID() != -1) {
					ball.phys.calcAttraction(player.localBalls);
					ball.phys.calcClientAttraction(player.centrePos, 0.01f);
				}
			}
		}
		
		for(Ball ball: balls) {
			if(ball.getID() != -1) {
				ball.phys.update(dt);
			}
		}
		
		
		
		List<Ball>ballsToRemove = new ArrayList<>();
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
	
	public void renderBalls(Graphics2D g, float dt, PlayerHandler players, boolean debugging) {
		float energy = 0;
		synchronized (balls) {
			for (Ball ball: balls) {
				if(ball.getID() == -1) continue;
				ball.renderScaled(g, dt, players,debugging);
				energy += ball.phys.calcEnergy(balls);
			}
		}

		g.setColor(Color.PINK);
		g.drawString("energy: " + energy, 20, 200);
	}
	
	/**
	 * Transfers balls from the toAdd list to the main balls list.
	 */
	public void moveBallsList() {
		for (var ball : ballsToAdd) {
			balls.add(ball);
		}
		ballsToAdd.clear();
	}
	
	//Handles server balls.
	public void add(float[] data) {
		//Check if there is an empty slot for this ball.
		if(!emptySlots.isEmpty()) {
			int index = emptySlots.pop();
			var ball = balls.get(index);
			ball.setBall(data);
			return;
		}
		var ball = new Ball(data);
		ballsToAdd.add(ball);
		numBalls++;
	}
	
	//Handles client created balls.
	public void add(Ball ball) {
		//Add index of new ball to client created balls index list.
		emptySlots.add(balls.size());
		
		ballsToAdd.add(ball);
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
	
	public int getBallListSize() {
		return balls.size();
	}
	
	public List<Ball> getBalls(){
		return balls;
	}

}