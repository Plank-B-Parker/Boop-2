package balls;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import math.Physics;
import networking.Client;
import networking.ClientHandler;

public class Storage {

	public int numBalls = 0;
	
	private List<Ball> balls = new ArrayList<>();
	private List<Ball> removedBalls = new ArrayList<>();
	
	
	public Storage() {
		balls = Collections.synchronizedList(balls);
	}
	
	
	public void updateBalls(ClientHandler clientHandler, float dt) {
		Physics.checkCollision(this);
		
		List<Client> clients = clientHandler.getClients();
		
		//Maybe put the following bit of code into the client accept class, or whatever should handle client updates.
		synchronized (balls) {
			if(clients != null) {
				
				//For each client check if a ball has escaped from their grasp.
				for(Client client: clients) {
					//List of balls out of reach.
					ArrayList<Ball> ballsToRemove = new ArrayList<Ball>();
					
					for(Ball ball: client.localBalls) {
						
						if(!client.isInReach(ball)) {
							ballsToRemove.add(ball);
							ball.phys.magnetic = false;
							//Set ID as not owned. Should be set to contested in later code if it is contested.
							ball.setOwnerID(-1);
						}
			
					}
					//Remove all balls out of reach.
					for(Ball ball: ballsToRemove) {
						client.localBalls.remove(ball);
					}
				}
				
				//For each ball check if any client can claim a ball. If they can, then let them.
				for(Ball ball: balls) {
					
					for(Client client: clients) {
						
						if(client.isInReach(ball)) {
							
							long ownerID = ball.getOwnerID();
							
							//Make sure balls in territories are magnetic.
							ball.phys.magnetic = true;
							
							//If the ball already belongs to the current client... well, it belongs to the current client.
							if(ownerID == client.getIdentity()) {
								continue;
							}
							
							//Pop the ball in the local ball list of the client.
							if(!client.localBalls.contains(ball)) {
								client.localBalls.add(ball);
							}
							
							//If the ball is in a dispute between two attractive clients.
							if(ownerID == -2) {
								continue;
							}
							//If the ball is within but a single clients territory.
							if(ownerID == -1) {
								client.ownedBalls.add(ball);
								ball.setOwnerID(client.getIdentity());
								continue;
							}
							//If the ball is within another clients territory and the current client has stumbled across it.
							if(ownerID >= 0) {
								(clientHandler.getClientByID(ball.getOwnerID())).ownedBalls.remove(ball);
								ball.setOwnerID(-2);
							}
							
							
						}
						
					}
				}
				
				//Make sure all owned balls are actually owned.
				for(Client client: clients) {
					
					ArrayList<Ball> ballsToRemove = new ArrayList<Ball>();
					System.out.println("local balls size: " + client.localBalls.size());
					
					for(Ball ball: client.ownedBalls) {
						if(ball.ownerID < 0)
							ballsToRemove.add(ball);		
					}
					
					for(Ball ball: ballsToRemove) {
						client.ownedBalls.remove(ball);
					}
				}
				
			}
		}
		
		synchronized (balls) {
		
			//Set acceleration to 0 to begin with calculate drag force.
			for(Ball ball: balls) {
				ball.phys.nullifyForces();
				ball.phys.calcDrag();
			}
			
			if(clients != null) { 
				//Calculate attraction of all magnetic balls.
				for(Client client: clients) {
					for(Ball ball: client.localBalls) {
						ball.phys.calcAttraction(client.localBalls);
						ball.phys.calcClientAttraction(client.centrePos, 0.01f);
					}
				}
			}
			
		}
		
		synchronized (balls) {
			for(Ball ball: balls) {
				ball.phys.update(dt);
			}
		}

		List<Ball>ballsToRemove = new ArrayList<Ball>();
		synchronized (balls) {
			for(Ball ball: balls) {
				if(ball.getID() != -1) {
					if(ball.toBeRemoved()) ballsToRemove.add(ball);
				}
			}
			for(Ball ball: ballsToRemove) remove(ball);
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
		b.setID(createNewBallID());
		balls.add(b);
		numBalls++;
	}
	
	int maxID = 0;
	private int createNewBallID() {
		return ++maxID;
	}
	
	private int removedCount = 0;
	public void remove(Ball b) {
		balls.remove(b);
		numBalls--;
		//Top secret: Classified code right here.
		removedBalls.add(b);
		System.out.println(removedCount++);
	}
	
	public Ball getBall(int index) {
		return balls.get(index);
	}
	
	public Collection<Ball> getBalls() {
		return Collections.unmodifiableCollection(balls);
	}

}
