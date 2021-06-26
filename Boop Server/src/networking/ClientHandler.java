package networking;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import balls.Ball;

public class ClientHandler{
	
	List<Client> clientsToAdd = new ArrayList<>(4);
	List<Client> clientsToRemove = new ArrayList<>(4);
	List<Client> clients = new ArrayList<>();
	
	public void updateClients(Collection<Ball> collection, float dt) {
		if(clients == null) return;
		
		//Update positions of all the clients.
		for(Client client: clients) {
			client.updatePos(dt);
		}
		
		//Following handles who owns who's balls.
			
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
		for(Ball ball: collection) {
			
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
						(getClientByID(ball.getOwnerID())).ownedBalls.remove(ball);
						ball.setOwnerID(-2);
					}
					
					
				}
				
			}
		}
		
		//Make sure all owned balls are actually owned.
		for(Client client: clients) {
			
			ArrayList<Ball> ballsToRemove = new ArrayList<Ball>();
			//System.out.println("local balls size: " + client.localBalls.size());
			
			for(Ball ball: client.ownedBalls) {
				if(ball.ownerID < 0)
					ballsToRemove.add(ball);		
			}
			
			for(Ball ball: ballsToRemove) {
				client.ownedBalls.remove(ball);
			}
		}
		
		
	}
	
	public void moveWaitingClients() {
		for (Client client : clientsToAdd) {
			clients.add(client);
		}
		
		for (Client client : clientsToRemove) {
			clients.remove(client);
		}
		
		clientsToAdd.clear();
		clientsToRemove.clear();
	}
	
	public void checkClientsConnection() {
		for (int i = 0; i < clients.size(); i++) {
			if (! clients.get(i).isConnected()) clientsToRemove.add(clients.get(i));
		}
	}
	
	public void disconnectClient(Client client){
		client.disconnect();
	}
	
	public void disconnectAllClients(){
		for (Client client: clients) {
			client.disconnect();
		}
		clients.clear();
	}

	public Client getClientByAddressAndPort(InetAddress address, int port) {

		for (Client client: clients) {
			if (client.getIpv4Address().equals(address) && client.getClientPort() == port) return client;
		}

		return null;
	}
	
	public Client getClientByID(long ID) {
		for(Client client: clients) {
			if(client.getIdentity() == ID) 
				return client;
		}
		return null;
	}
	
	public List<Client> getClients(){
		return clients;
	}
}
