package main;

import java.util.ArrayList;
import java.util.List;

import balls.Ball;
import math.Vec2f;

public class PlayerHandler extends ArrayList<Player> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8656568538126391881L;
	
	public volatile static Player Me = new Player(true, -1, new Vec2f(0,0));
	
	public PlayerHandler() {
		add(Me);
	}

	public Player getPlayerByID(long ID) {
		for(Player P: this) {
			if(P.ID == ID) return P;
		}
		return null;
	}
	
	public void updatePlayers(List<Ball> balls, float dt) {
		if(size() == 0) return;
		
		//Update positions first.
		for(Player player: this) {
			player.updatePos(dt);
		}
				
		//For each player check if a ball has escaped from their grasp.
		for(Player player: this) {
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
			
			for(Player player: this) {
				
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
						(getPlayerByID(ball.getOwnerID())).ownedBalls.remove(ball);
						ball.setOwnerID(-2);
					}
					
					
				}
				
			}
		}
		
		//Make sure all owned balls are actually owned.
		for(Player player: this) {
			
			ArrayList<Ball> ballsToRemove = new ArrayList<Ball>();
			
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
