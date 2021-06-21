package balls;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import main.Player;
import main.PlayerHandler;
import math.Physics;

public class Storage {

	public int numBalls = 0;
	
	private List<Ball> balls = new ArrayList<>();
	//List of slots that are empty in the balls list.
	private Stack<Integer> emptySlots = new Stack<>();
	private List<Ball> removedBalls = new ArrayList<>();
	
	private float timeBeforeRemovingAbandonedBall = 5;
	
	public Storage() {
		balls = Collections.synchronizedList(balls);
	}
	
	public void updateBalls(PlayerHandler players, float dt) {
		Physics.checkCollision(this);
		
		//Maybe put the following bit of code into the player handler and player classes, or whatever should handle player updates.
		synchronized (balls) {
			if(players.size() != 0) {
				
				//For each player check if a ball has escaped from their grasp.
				for(Player player: players) {
					//List of balls out of reach.
					ArrayList<Ball> ballsToRemove = new ArrayList<Ball>();
					
					for(Ball ball: player.localBalls) {
						
						if(!player.isInReach(ball)) {
							ballsToRemove.add(ball);
							ball.phys.magnetic = false;
							//Set ID as not owned. Should be set to contested in later code if it is contested.
							ball.setOwnerID(-1);
						}
			
					}
					//Remove all balls out of reach.
					for(Ball ball: ballsToRemove) {
						player.localBalls.remove(ball);
					}
				}
				
				//For each ball check if any client can claim a ball. If they can, then let them.
				for(Ball ball: balls) {
					
					for(Player player: players) {
						
						if(player.isInReach(ball)) {
							
							long ownerID = ball.getOwnerID();
							
							//Make sure balls in territories are magnetic.
							ball.phys.magnetic = true;
							
							//Pop the ball in the local ball list of the client.
							if(!player.localBalls.contains(ball)) {
								player.localBalls.add(ball);
							}
							
							//If the ball already belongs to the current client... well, it belongs to the current client.
							if(ownerID == player.ID) {
								continue;
							}
							
							//If the ball is in a dispute between two attractive clients.
							if(ownerID == -2) {
								continue;
							}
							//If the ball is within but a single clients territory.
							if(ownerID == -1) {
								player.ownedBalls.add(ball);
								ball.setOwnerID(player.ID);
								continue;
							}
							//If the ball is within another clients territory and the current client has stumbled across it.
							if(ownerID >= 0) {
								(players.getPlayerByID(ball.getOwnerID())).ownedBalls.remove(ball);
								ball.setOwnerID(-2);
							}
							
							
						}
						
					}
				}
				
				//Make sure all owned balls are actually owned.
				for(Player player: players) {
					
					ArrayList<Ball> ballsToRemove = new ArrayList<Ball>();
					System.out.println("local balls size: " + player.localBalls.size());
					
					for(Ball ball: player.ownedBalls) {
						if(ball.getOwnerID() < 0)
							ballsToRemove.add(ball);		
					}
					
					for(Ball ball: ballsToRemove) {
						player.ownedBalls.remove(ball);
					}
				}
				
			}
		}

		synchronized (balls) {
			for (Ball ball: balls) {
				if(ball.getID() != -1) {
					ball.updateClientPrediction(dt);
				}
			}
		}
		
		synchronized (balls) {

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
		}
		
		synchronized (balls) {
			for(Ball ball: balls) {
				if(ball.getID() != -1) {
					ball.phys.update(dt);
				}
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
	
	public int getBallListSize() {
		return balls.size();
	}

}
