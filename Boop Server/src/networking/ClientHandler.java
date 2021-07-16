package networking;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import balls.Ball;
import math.Bitmaths;

public class ClientHandler{
	
	List<Client> clientsToAdd = new ArrayList<>(4);
	List<Client> clientsToRemove = new ArrayList<>(4);
	List<Client> clients = new ArrayList<>();
	
	private boolean everyOneKnowsAboutEveryOne = true;
	
	public void updateClients(List<Ball> balls, float dt) {
		if(clients == null) return;
		
		//Update Velocities of all the clients.
		for(Client client: clients) {
			client.updateVelocity(dt);
		}
		
		//Update positions of all the clients.
		for(Client client: clients) {
			client.updatePos(dt);
		}
		
		//Following handles who owns who's balls.
			
		//For each client check if a ball has escaped from their grasp.
		for(Client client: clients) {
			//List of balls out of reach.
			ArrayList<Ball> ballsToRemove = new ArrayList<>();
			
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
						(getClientByID(ball.getOwnerID())).ownedBalls.remove(ball);
						ball.setOwnerID(-2);
					}
					
					
				}
				
			}
		}
		
		//Make sure all owned balls are actually owned.
		for(Client client: clients) {
			
			List<Ball> ballsToRemove = new ArrayList<>();
			//System.out.println("local balls size: " + client.localBalls.size());
			
			for(Ball ball: client.ownedBalls) {
				if(ball.ownerID < 0)
					ballsToRemove.add(ball);		
			}
			
			for(Ball ball: ballsToRemove) {
				client.ownedBalls.remove(ball);
			}
			
			client.updateRadii();
		}
		
		
	}
	
	/**
	 * Sends a 'ping' packet to each client and expects a response with
	 * an attached receive time on the packet to ignore processing delay.
	 */
	public void pingClients() {
		for (var client : clients) {
			
			if (!client.isReadyForPacket(1000, Packet.PING)) continue;
			
			long sendTime = System.nanoTime();
			byte[] payload = Bitmaths.longToBytes(sendTime);
			payload = Bitmaths.pushByteArrayToData(Bitmaths.intToBytes(8), payload); // payload length
			payload = Bitmaths.pushByteToData(Packet.PING.getID(), payload);
			try {
				client.out.write(payload);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Something went wrong with this client: " + client.getIdentity());
			}
		}
	}
	
	public void moveWaitingClients() {
		if(!everyOneKnowsAboutEveryOne) return;
		
		for (Client client : clientsToAdd) {
			clients.add(client);
		}
		
		for (Client client : clientsToRemove) {
			for(Ball ball: client.localBalls) {
				ball.ownerID = -1;
			}
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
	}
	
	/**
	 * Adds a client into the buffer.
	 * @param client The client to be added.
	 * @see {@link #moveWaitingClients}
	 * Use this method to move the clients from the buffer to the clients list.
	 */
	public void addClient(Client newClient) {
		everyOneKnowsAboutEveryOne = false;
		clientsToAdd.add(newClient);
		
		for(Client client: clients) {
			newClient.alertClient(client, true);
			client.alertClient(newClient, true);
		}
		everyOneKnowsAboutEveryOne = true;
	}
	
	/**
	 * Removes a client into the buffer.
	 * @param client The client to be removed.
	 * @see {@link #moveWaitingClients}
	 * Use this method to move the clients out of the client list.
	 */
	public void removeClient(Client leavingClient) {
		everyOneKnowsAboutEveryOne = false;
		clientsToRemove.add(leavingClient);
		
		for(Client client: clients) {
			client.alertClient(leavingClient, false);
		}
		everyOneKnowsAboutEveryOne = true;
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
	
	/**
	 * @return A reference to an unmodifiable list of all clients. (Elements are still mutable).
	 */
	public List<Client> getClients(){
		return Collections.unmodifiableList(clients);
	}
}
