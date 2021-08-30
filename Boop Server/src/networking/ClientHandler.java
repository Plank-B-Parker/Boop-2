package networking;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import balls.Ball;

public class ClientHandler {

	List<Client> clientsToAdd = new ArrayList<>(4);
	List<Client> clientsToRemove = new ArrayList<>(4);
	List<Client> clients = new ArrayList<>();

	private boolean everyOneKnowsEachOther = true;

	public ClientHandler() {
	}

	public Client createNewClient() {
		var client = new Client();

		client.setIdentity(generateID());

		return client;
	}

	public void setUpClient(Client client, SocketChannel channel, BlockingQueue<Packet> outboundPacketQueue)
			throws IOException {
		client.setupConnection(channel);
		// Anything to send or recieve here:
		client.finishSetUp(outboundPacketQueue);

		addClient(client);
	}

	public void updateClients(List<Ball> balls, float dt) {
		if (clients == null)
			return;

		// Update Velocities of all the clients.
		for (Client client : clients) {
			client.updateVelocity(dt);
		}

		// Update positions of all the clients.
		for (Client client : clients) {
			client.updatePos(dt);
		}

		// Following handles who owns who's balls.

		// For each client check if a ball has escaped from their grasp.
		for (Client client : clients) {
			// List of balls out of reach.
			ArrayList<Ball> ballsToRemove = new ArrayList<>();

			for (Ball ball : client.localBalls) {

				if (!client.isInReach(ball)) {
					ballsToRemove.add(ball);
					ball.phys.magnetic = false;
					// Set ID as not owned. Should be set to contested in later code if it is
					// contested.
					ball.setOwnerID(-1);
				}

			}
			// Remove all balls out of reach.
			for (Ball ball : ballsToRemove) {
				client.localBalls.remove(ball);
			}
		}

		// For each ball check if any client can claim a ball. If they can, then let
		// them.
		for (Ball ball : balls) {

			for (Client client : clients) {

				if (client.isInReach(ball)) {

					long ownerID = ball.getOwnerID();

					// Make sure balls in territories are magnetic.
					ball.phys.magnetic = true;

					// If the ball already belongs to the current client... well, it belongs to the
					// current client.
					if (ownerID == client.getIdentity()) {
						continue;
					}

					// Pop the ball in the local ball list of the client.
					if (!client.localBalls.contains(ball)) {
						client.localBalls.add(ball);
					}

					// If the ball is in a dispute between two attractive clients.
					if (ownerID == -2) {
						continue;
					}
					// If the ball is within but a single clients territory.
					if (ownerID == -1) {
						client.ownedBalls.add(ball);
						ball.setOwnerID(client.getIdentity());
						continue;
					}
					// If the ball is within another clients territory and the current client has
					// stumbled across it.
					if (ownerID >= 0) {
						(getClientByID(ball.getOwnerID())).ownedBalls.remove(ball);
						ball.setOwnerID(-2);
					}

				}

			}
		}

		// Make sure all owned balls are actually owned.
		for (Client client : clients) {

			List<Ball> ballsToRemove = new ArrayList<>();

			for (Ball ball : client.ownedBalls) {
				if (ball.ownerID < 0)
					ballsToRemove.add(ball);
			}

			for (Ball ball : ballsToRemove) {
				client.ownedBalls.remove(ball);
			}

			client.updateRadii();
		}

	}

	public void moveWaitingClients() {
		if (!everyOneKnowsEachOther)
			return;

		for (Client client : clientsToAdd) {
			clients.add(client);
		}

		for (Client client : clientsToRemove) {
			for (Ball ball : client.localBalls) {
				ball.ownerID = -1;
			}
			clients.remove(client);
		}

		clientsToAdd.clear();
		clientsToRemove.clear();
	}

	public void checkClientsConnection() {
		for (int i = 0; i < clients.size(); i++) {
			if (!clients.get(i).isConnected())
				clientsToRemove.add(clients.get(i));
		}
	}

	public void disconnectClient(Client client) {
		client.disconnect();
	}

	public void disconnectAllClients() {
		for (Client client : clients) {
			client.disconnect();
		}
	}

	/**
	 * Adds a client into the buffer.
	 * 
	 * @param newClient The client to be added.
	 * @see {@link #moveWaitingClients} Use this method to move the clients from the
	 *      buffer to the clients list.
	 */
	public void addClient(Client newClient) {
		everyOneKnowsEachOther = false;
		clientsToAdd.add(newClient);
	}

	/**
	 * Removes a client into the buffer.
	 * 
	 * @param client The client to be removed.
	 * @see {@link #moveWaitingClients} Use this method to move the clients out of
	 *      the client list.
	 */
	public void removeClient(Client leavingClient) {
		everyOneKnowsEachOther = false;
		clientsToRemove.add(leavingClient);
	}

	public long generateID() {
		var validID = true;
		long id = 0;

		do {
			id = ((long) (Math.random() * 1000 + 1000));
			for (var i = 0; i < clients.size(); i++) {
				if (clients.get(i).getIdentity() == id) {
					validID = false;
				}
			}
		} while (!validID); // While ID is not valid

		return id;
	}

	public Client getClientByAddressAndPort(InetAddress address, int port) {

		for (Client client : clients) {
			if (client.getIpv4Address().equals(address) && client.getClientPort() == port)
				return client;
		}

		return null;
	}

	public Client getClientByID(long ID) {
		for (Client client : clients) {
			if (client.getIdentity() == ID)
				return client;
		}
		return null;
	}

	Client findClientByID(long ID) {
		for (var client : clients) {
			if (client.getIdentity() == ID)
				return client;
		}

		for (var client : clientsToAdd) {
			if (client.getIdentity() == ID)
				return client;
		}

		return null;
	}

	/**
	 * @return A reference to an unmodifiable list of all clients. (Elements are
	 *         still mutable).
	 */
	public List<Client> getClients() {
		return Collections.unmodifiableList(clients);
	}

	public boolean doesEveryOneKnowEachOther() {
		return everyOneKnowsEachOther;
	}

	public void setEveryOneKnowsEachOther(boolean everyOneKnowsEachOther) {
		this.everyOneKnowsEachOther = everyOneKnowsEachOther;
	}
}
