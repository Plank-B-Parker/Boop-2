package main;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import balls.Ball;
import math.Vec2f;

//All players.
public class PlayerHandler extends ArrayList<Player> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8656568538126391881L;
	
	public static Player Me = new Player(true, -1, "Pakistan > India",
										Color.GREEN,
										new Vec2f(0,0));
	
	private ArrayList<Player> playersToAdd = new ArrayList<>(); 		//Joined players to be added.
	private ArrayList<Player> playersToRemove = new ArrayList<>(); 		//Left players to be removed.
	private ArrayList<Long> playersToBeUpdatedID = new ArrayList<>();	//Players' IDs from server update.
	private ArrayList<float[]> playersToBeUpdatedData = new ArrayList<>();	//Players' data from server update. 
	private ArrayList<Player> trackedPlayers = new ArrayList<>();		//Players being tracked by client. 
	public static final float timeBeforeWeDontCareAboutSomeone = 5;
	
	public PlayerHandler() {
		add(Me);
	}
	
	public void updatePlayers(final List<Ball> balls, float dt) {
		if(size() == 0) return;
		
		//ServerUpdate
		processServerUpdate();
		
		//Update positions first.
		for(Player player: trackedPlayers) {
			player.updateAcc();
			player.updateVel(dt);
			player.updatePos(dt);
		}
				
		//For each player check if a ball has escaped from their grasp.
		for(Player player: trackedPlayers) {
			checkIfBallEscaped(player);
		}
		
		//For each ball check if any client can claim a ball. If they can, then let them.
		for(Ball ball: balls) {
			for(Player player: trackedPlayers) {
				if(player.isInReach(ball)) 
					determineOwnerShip(ball, player);
			}
		}
		
		//Make sure all owned balls are actually owned.
		for(Player player: trackedPlayers) {
			
			ArrayList<Ball> ballsToRemove = new ArrayList<>();
			
			for(Ball ball: player.ownedBalls) {
				if(ball.getOwnerID() < 0)
					ballsToRemove.add(ball);		
			}
			
			for(Ball ball: ballsToRemove) {
				player.ownedBalls.remove(ball);
			}
		}
		
		//Get rid of players too far away.
		for(Player player: trackedPlayers) {
			if(player.timeSinceLastUpdate > timeBeforeWeDontCareAboutSomeone) 
				trackedPlayers.remove(player);
		}
				
	}


	private void processServerUpdate() {
		
		for(Player player: playersToAdd) {
			add(player);
		}
		for(Player player: playersToRemove) {
			remove(player);
			trackedPlayers.remove(player);
		}
		for(var i = 0; i < playersToBeUpdatedID.size(); i++) {
			var id = playersToBeUpdatedID.get(i);
			var playerData = playersToBeUpdatedData.get(i);
			
			float posX = playerData[0];
			float posY = playerData[1];
			float velX = playerData[2];
			float velY = playerData[3];
			float radOfInf = playerData[4];
			
			for(Player player: this) {
				if(player.ID == id) 
					player.serverUpdate(posX, posY, velX, velY, radOfInf);
				
				if(!trackedPlayers.contains(player))
					trackedPlayers.add(player);
			}
		}
		
		playersToAdd.clear();
		playersToRemove.clear();
		playersToBeUpdatedData.clear();
		playersToBeUpdatedID.clear();
	}
	
	public void serverUpdatePlayer(long ID, float posX, float posY, float velX, float velY, float radOfInf) {
		if(!playerIsKnown(ID)) return;
		if(ID == -1) System.out.println("LADIES AND GENTLEMEN... WE GOT EM: -1 CLIENT - class: PlayerHandler");
		
		playersToBeUpdatedID.add(ID);
		float[] data = {posX, posY, velX, velY, radOfInf};
		playersToBeUpdatedData.add(data);
	}
	
	/**
	 * Only use when someone joins server and server notifies client.
	 */
	public void addPlayer(Player p) {
		playersToAdd.add(p);
	}
	
	/**
	 * Only use when someone leaves server and server notifies client.
	 */
	public void removePlayer(long ID) {
		playersToRemove.add(getPlayerByID(ID));
	}
	
	public boolean playerIsKnown(long ID) {
		return getPlayerByID(ID) != null;
	}
	
	private void checkIfBallEscaped(Player player) {
		//List of balls out of reach.
		ArrayList<Ball> ballsToRemove = new ArrayList<>();
		
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
	
	private void determineOwnerShip(Ball ball, Player player) {
		long ownerID = ball.getOwnerID();
		
		//Make sure balls in territories are magnetic.
		ball.phys.magnetic = true;
		
		//Pop the ball in the local ball list of the client.
		if(!player.localBalls.contains(ball)) {
			player.localBalls.add(ball);
		}
		
		//If the ball already belongs to the current client... well, it belongs to the current client.
		if(ownerID == player.ID) {
			return;
		}
		
		//If the ball is in a dispute between two attractive clients.
		if(ownerID == -2) {
			return;
		}
		//If the ball is within but a single clients territory.
		if(ownerID == -1) {
			player.ownedBalls.add(ball);
			ball.setOwnerID(player.ID);
			return;
		}
		//If the ball is within another clients territory and the current client has stumbled across it.
		if(ownerID >= 0 && getPlayerByID(ball.getOwnerID()) != null) {
			(getPlayerByID(ball.getOwnerID())).ownedBalls.remove(ball);
			ball.setOwnerID(-2);
		}
	}
	
	public Player getPlayerByID(long ID) {
		for(Player P: this) {
			if(P.ID == ID) return P;
		}
		return null;
	}
		
	
}
