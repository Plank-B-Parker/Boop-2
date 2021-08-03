package networking;

import java.awt.Color;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import balls.Ball;
import math.Bitmaths;

public class ClientHandler implements Runnable{
	
	List<Client> clientsToAdd = new ArrayList<>(4);
	List<Client> clientsToRemove = new ArrayList<>(4);
	List<Client> clients = new ArrayList<>();
	
	private boolean everyOneKnowsAboutEveryOne = true;
	
	private Thread thread;
	BlockingQueue<byte[]> dataBuffer;
	
	// Support for idea 1. A shared buffer between all clients and this class.
	public ClientHandler() {
		dataBuffer = Client.dataBufferAll;
		thread = new Thread(this, "Client-Handler");
		thread.setDaemon(true);
	}
	
	@Override
	public void run() {
		while (ClientAccept.serverON) {
			try {
				byte[] data = dataBuffer.poll(2, TimeUnit.MILLISECONDS);
				
				if (data == null) continue; // TODO adjust polling time to help branch predictor
				
				handleData(data);
			} catch (InterruptedException e) {
				e.printStackTrace();
				Thread.currentThread().interrupt();
			}
		}
	}
	
	private void handleData(byte[] data) {
		// NOTE: Index 0 is packetID, Index 1-8 is the Client's ID (long)
		// Payload starts at Index 9 !!!
		var type = PacketData.getEnumByID(data[0]);
		var client = findClientByID(Bitmaths.bytesToLong(data, 1));
		
		// Index at the start of the payload
		final int headIndex = 9;
		
		if (client == null) return;
		
		switch (type) {
		case CLIENT_JOIN:
			client.setReadyToRecieveUDP(true);
			System.out.println("Client class, ready for UDP");
			break;
		case CLIENT_DATA:
			var nameLength = Integer.valueOf(Bitmaths.bytesToString(data, headIndex, 2)) - 10;
			var name = Bitmaths.bytesToString(data, headIndex + 2, nameLength);
			var colour = Integer.valueOf(Bitmaths.bytesToString(data, headIndex + 2 + nameLength, data.length - (headIndex + 2 + nameLength)));
			
			client.name = name;
			client.colour = new Color(colour);
			
			//Colour last thing to be sent, relies on knowing the length of everything else.
			break;
		case PING:
			var receiveTime = System.nanoTime();
			
			// Server is the originator when the packet contains both server and client time.
			var isOriginator = data.length == PacketData.PING.getObjectSize() + headIndex;
			
			// Calculate ping and store data
			if (isOriginator) {
				var receivedClient = Bitmaths.bytesToLong(data, headIndex);
				long serverToClient = receivedClient - Bitmaths.bytesToLong(data, headIndex + 8);
				long clientToServer = receiveTime - receivedClient;
				long rtt = serverToClient + clientToServer;
				client.msPing = rtt / 1000000;
			}
			// Add time received to packet and echo back to client.
			else {
				byte[] echoData = new byte[8];
				System.arraycopy(data, headIndex, echoData, 0, 8);
				echoData = Bitmaths.pushByteArrayToData(Bitmaths.longToBytes(receiveTime), echoData);
				echoData = Bitmaths.pushByteArrayToData(Bitmaths.intToBytes(16), echoData);
				echoData = Bitmaths.pushByteToData(PacketData.PING.getID(), echoData);
				client.sendData(echoData);
			}
			break;
		default:
			System.out.println("Packet type not supported to handle: " + data[0] + " ClientHandler");
			break;
		}
	}
	
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
			
			if (!client.isReadyForPacket(1000, PacketData.PING)) continue;
			
			long sendTime = System.nanoTime();
			byte[] payload = Bitmaths.longToBytes(sendTime);
			payload = Bitmaths.pushByteArrayToData(Bitmaths.intToBytes(8), payload); // payload length
			payload = Bitmaths.pushByteToData(PacketData.PING.getID(), payload);
			client.sendData(payload);
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
	 * @param newClient The client to be added.
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
	
	private Client findClientByID(long ID) {
		for (var client : clients) {
			if (client.getIdentity() == ID) return client;
		}
		
		for (var client : clientsToAdd) {
			if (client.getIdentity() == ID) return client;
		}
		
		return null;
	}
	
	/**
	 * @return A reference to an unmodifiable list of all clients. (Elements are still mutable).
	 */
	public List<Client> getClients(){
		return Collections.unmodifiableList(clients);
	}
	
	public void startHandlingTcp() {
		thread.start();
	}
	
	public void stopHandlingTcp() {
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
	}
}
