package networking;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import balls.Ball;
import math.Bitmaths;
import networking.PacketData.Protocol;

/**
 * Manages all server-related references such as threads, packet and client handling
 * <p>
 * The server creates 4 threads to handle client connections, UDP packets and packet (data) handling.
 * All objects related to core networking functionality are created and shared from this class.
 */
public class Server {

	private ClientHandler clientHandler;
	private ClientAccept clientAcceptor;
	private ClientProcessor clientProcessor;
	private UdpIo udpIo;

	private PacketHandler packetHandler;

	private Thread acceptorWorker;
	private Thread processorWorker;
	private Thread udpWorker;
	private Thread packetWorker;

	private final BlockingQueue<Client> inboundClientBuffer;

	public static final int PORT = 2300;
	volatile boolean serverON = false;

	public Server(ClientHandler clientHandler) {
		this.clientHandler = clientHandler;

		// Client acceptor adds to this queue while the processor polls from queue
		inboundClientBuffer = new LinkedBlockingQueue<>(64);

		clientAcceptor = new ClientAccept(this, clientHandler, inboundClientBuffer);
		clientProcessor = new ClientProcessor(this, clientHandler, inboundClientBuffer);
		udpIo = new UdpIo(this, clientHandler);
		packetHandler = new PacketHandler();

		acceptorWorker = new Thread(clientAcceptor, "Client-Acceptor");
		processorWorker = new Thread(clientProcessor, "Client-Processor");
		udpWorker = new Thread(udpIo, "Udp-Input-Output");
		packetWorker = new Thread(packetHandler, "Packet-Handler");
	}

	public void sendData(Packet packet, Client client) {
		if (client != null) {
			packet.sourceAddress = client.getSocketAddress();
		}

		boolean dataQueued = packet.identity.getProtocol() == Protocol.TCP ? clientProcessor.addOutboundPacket(packet)
				: udpIo.addOutboundPacket(packet);

		if (!dataQueued) {
			System.out.println(packet.identity.getProtocol() + ": Outbound packet queue is full, abandoning packet");
		}

	}

	public void submitEveryClientsData(List<Client> clients) {
		for (var client : clients) {
			submitAllClientData(client);
		}
	}

	private void submitAllClientData(Client client) {
		BlockingQueue<Packet> receivedData = client.tcpPacketReader.getCompletePackets();
		Packet packet = receivedData.poll();

		while (packet != null) {
			packetHandler.submitPacket(packet, client);
			packet = receivedData.poll();
		}
	}

	public void submitAllUdpData() {
		BlockingQueue<Packet> receivedData = udpIo.getCompletePackets();
		Packet udpPacket = receivedData.poll();

		while (udpPacket != null) {
			packetHandler.submitUdpPacket(udpPacket);
			udpPacket = receivedData.poll();
		}

	}

	// Alerts client about another client joining or leaving.
	public void alertClient(Client existingClient, Client newClient, boolean joining) {
		// Made all strings because name is a string.
		var clientStringData = new String[5];

		// 4 + 2 + 89 + 8 + 1
		clientStringData[0] = Long.toString(newClient.getIdentity()); // ID always has four digits and so has four bytes
																		// as a string.
		clientStringData[1] = Integer.toString(newClient.name.length() + 10); // Add 10 to ensure string always has two
																				// digits. No name > 89 allowed
		clientStringData[2] = newClient.name;
		clientStringData[3] = (joining) ? "1" : "0";
		clientStringData[4] = Integer.toString(newClient.colour.getRGB());

		int payload = 4 + 2 + newClient.name.length() + 1 + clientStringData[4].length();

		byte[] clientData = Bitmaths.stringArrayToBytes(clientStringData);
		clientData = Bitmaths.pushByteArrayToData(Bitmaths.intToBytes(payload), clientData);
		clientData = Bitmaths.pushByteToData(PacketData.CLIENT_JOIN.getID(), clientData);

		Packet packet = new Packet(clientData, existingClient.getIdentity());

		sendData(packet, null);
	}

	/**
	 * Sends a 'ping' packet to each client and expects a response with an attached
	 * receive time on the packet to ignore processing delay.
	 */
	public void pingClients() {
		for (var client : clientHandler.clients) {

			if (!client.isReadyForPacket(1000, PacketData.PING))
				continue;

			long sendTime = System.nanoTime();
			byte[] payload = Bitmaths.longToBytes(sendTime);
			payload = Bitmaths.pushByteArrayToData(Bitmaths.intToBytes(8), payload); // payload length
			payload = Bitmaths.pushByteToData(PacketData.PING.getID(), payload);

			Packet packet = new Packet(payload, client.getIdentity());

			sendData(packet, null);
		}
	}

	/**
	 * Synchronises the client's clock to the server. This stops the client
	 * manipulating time and correctly works out time sensitive information.
	 * 
	 * @param clients
	 */
	public void synchroniseClientClocks(List<Client> clients) {
		List<Client> readyClients = new ArrayList<>();

		for (Client client : clients) {
			if (client.isReadyForPacket(1000, PacketData.CLOCK_SYN)) {
				readyClients.add(client);
			}
		}

		for (Client client : readyClients) {
			byte[] payload = Bitmaths.longToBytes(System.nanoTime());
			payload = Bitmaths.pushByteArrayToData(Bitmaths.intToBytes(8), payload);
			payload = Bitmaths.pushByteToData(PacketData.CLOCK_SYN.getID(), payload);

			Packet packet = new Packet(payload, client.getIdentity());

			sendData(packet, null);
		}

	}

	/**
	 * Sends every client all of the balls that are close to them in sequential
	 * order.
	 * 
	 * @param allBalls list of all the balls on the server.
	 * @param clients  list of all clients on the server
	 */
	public void sendBalls(final List<Ball> allBalls, final List<Client> clients) {
		// packet id, ballID, ball type, x, y, velx, vely, ownerID

		final int maxBallsPerPacket = PacketData.NEW_BALLS.getNumObj();

		final int maxPackets = (int) Math.ceil((float) allBalls.size() / (float) maxBallsPerPacket);

		// Prepare data only for clients that are ready to accept packets.
		List<Client> readyClients = new ArrayList<>();

		// Shorten list of clients to only clients that are ready to accept.
		for (var i = 0; i < clients.size(); i++) {

			var client = clients.get(i);
			// Check if client is ready
			if (!client.isReadyForPacket(1000, PacketData.NEW_BALLS))
				continue;

			readyClients.add(client);
		}

		// Store every client with a number of packets filled with data
		var data = new byte[readyClients.size()][maxPackets][PacketData.NEW_BALLS.getMaxPayload()];
		var clientMaxPackets = new int[readyClients.size()];

		// Fill the packets with data for each client with the correct amount of balls.
		for (var i = 0; i < readyClients.size(); i++) {
			var client = readyClients.get(i);

			// check if ball is in client's vision area (simple circle).
			List<Ball> inRange = new ArrayList<>();
			for (Ball ball : allBalls) {
				if (client.isInSendingRange(ball))
					inRange.add(ball);
			}

			// Calculate number of packets for this client.
			int numberOfPackets = (int) Math.ceil((double) inRange.size() / maxBallsPerPacket);
			clientMaxPackets[i] = numberOfPackets;

			// Fills all of the packet data of this client.
			fillBallData(numberOfPackets, inRange, data[i]);

		}

		// Send packets to client
		for (var i = 0; i < readyClients.size(); i++) {
			var client = readyClients.get(i);
			for (var j = 0; j < clientMaxPackets[i]; j++) {
				// array to replace with header information
				byte[] headerInfo = new byte[0];

				// Add packet id and packet sequence
				headerInfo = Bitmaths.pushByteArrayToData(Bitmaths.intToBytes(client.incrementUdpPacketsSent()),
						headerInfo);
				headerInfo = Bitmaths.pushByteToData(PacketData.NEW_BALLS.getID(), headerInfo);

				byte[] completePacket = Bitmaths.pushByteArrayToData(headerInfo, data[i][j]);

				Packet packet = new Packet(completePacket, client.getIdentity());

				sendData(packet, client);

			}
		}

	}

	/**
	 * Send every client information about every other client's position and
	 * movement.
	 * 
	 * @param clients list of all clients on the server
	 */
	public void sendAllClientInfo(final List<Client> clients) {
		final int numItemsPerObj = PacketData.CLIENT_DATA.getNumberOfItems();
		final int maxClientsPerPacket = PacketData.CLIENT_DATA.getNumObj();

		// Prepare data only for clients that are ready to accept packets.
		List<Client> readyClients = new ArrayList<>();

		var clientsData = new Number[clients.size() * numItemsPerObj];
		var numClients = 0;
		var offset = 0;

		// Shorten list of clients only to those who are ready and prepare the data for
		// them
		for (var client : clients) {
			if (!client.isReadyForPacket(1000, PacketData.CLIENT_DATA))
				continue;

			offset = numClients * numItemsPerObj;

			// Fill data to prepare for sending. (Front of the array will be filled first).
			readyClients.add(client);
			clientsData[offset + 0] = client.centrePos.x;
			clientsData[offset + 1] = client.centrePos.y;
			clientsData[offset + 2] = client.velocity.x;
			clientsData[offset + 3] = client.velocity.y;
			clientsData[offset + 4] = client.radOfInf;
			clientsData[offset + 5] = client.getIdentity();

			numClients++;
		}

		final int packetNo = (int) Math.ceil((float) numClients / (float) maxClientsPerPacket);
		boolean isSinglePacket = packetNo == 1;

		// Shrink payload size to fit the only packet
		final int maxPayloadSize = isSinglePacket ? numClients * PacketData.CLIENT_DATA.getObjectSize()
				: PacketData.CLIENT_DATA.getMaxPayload();

		int numbersPerPacket = isSinglePacket ? numClients * numItemsPerObj : numItemsPerObj * maxClientsPerPacket;

		var data = new byte[packetNo][maxPayloadSize];
		var splitInput = new Number[packetNo][numbersPerPacket];

		var inputFilled = 0;
		offset = 0;

		// Fill array of numbers with data from clients
		while (inputFilled < packetNo) {

			int numToFill = numbersPerPacket;

			// Trim the size of the array at the last packet
			if (!isSinglePacket && inputFilled >= packetNo - 1) {
				numToFill = (numClients * numItemsPerObj) % (maxClientsPerPacket * numItemsPerObj);

				splitInput[inputFilled] = new Number[numToFill];
			}

			for (var i = 0; i < numToFill; i++) {
				splitInput[inputFilled][i] = clientsData[i + offset];
			}

			inputFilled++;
			offset += maxClientsPerPacket * numItemsPerObj;
		}

		// Convert numbers into bytes
		for (int i = 0; i < packetNo; i++) {
			data[i] = Bitmaths.numberArrayToBytes(splitInput[i]);
		}

		// array to replace with header information
		byte[] headerInfo;

		// Send data to all clients sequentially.
		for (var client : readyClients) {
			for (var i = 0; i < packetNo; i++) {
				headerInfo = new byte[0];
				headerInfo = Bitmaths.pushByteArrayToData(Bitmaths.intToBytes(client.incrementUdpPacketsSent()),
						headerInfo);
				headerInfo = Bitmaths.pushByteToData(PacketData.CLIENT_DATA.getID(), headerInfo);
				byte[] completePacket = Bitmaths.pushByteArrayToData(headerInfo, data[i]);

				Packet packet = new Packet(completePacket, client.getIdentity());

				sendData(packet, client);
			}
		}

	}

	/**
	 * Converts floatData from all balls in range of the client into a byte array of
	 * and stores it into the give 'data' byte array.
	 * 
	 * @param numberOfPackets number of packets to fill.
	 * @param ballsInRange    all balls that are in range of the client.
	 * @param data            byte array to fill with ball data.
	 */
	private void fillBallData(int numberOfPackets, final List<Ball> ballsInRange, byte[][] data) {
		final int numberOfItems = PacketData.NEW_BALLS.getNumberOfItems();
		final int maxBallsPerPacket = PacketData.NEW_BALLS.getNumObj();
		final int maxFloatsPerPacket = maxBallsPerPacket * numberOfItems;

		float[][] floatData = new float[numberOfPackets][maxFloatsPerPacket];

		var packetsFilled = 0;
		var lastBall = 0;
		Ball ball;
		var offset = 0;

		while (packetsFilled < numberOfPackets) {
			// Fills data until the all packets are filled or all balls are considered,
			// whichever is first.
			for (var b1 = 0; b1 < maxBallsPerPacket && lastBall < ballsInRange.size(); b1++, lastBall++) {
				ball = ballsInRange.get(lastBall);
				offset = numberOfItems * b1;

				floatData[packetsFilled][offset] = ball.getID();
				floatData[packetsFilled][offset + 1] = ball.getType();
				floatData[packetsFilled][offset + 2] = ball.phys.pos.x;
				floatData[packetsFilled][offset + 3] = ball.phys.pos.y;
				floatData[packetsFilled][offset + 4] = ball.phys.vel.x;
				floatData[packetsFilled][offset + 5] = ball.phys.vel.y;
				floatData[packetsFilled][offset + 6] = ball.getOwnerID();

			}
			data[packetsFilled] = Bitmaths.floatArrayToBytes(floatData[packetsFilled]);
			packetsFilled++;
		}
	}

	public void startServer() {
		serverON = true;
		// Start threads for objects here
		acceptorWorker.start();
		processorWorker.start();
		udpWorker.start();
		packetWorker.start();
	}

	public void stopServer() {
		serverON = false;
		try {
			acceptorWorker.join();
			processorWorker.join();
			udpWorker.join();
			packetWorker.join();

			clientAcceptor.closeServerChannel();
			clientProcessor.stopProcessing();
			udpIo.stopProcessingUdp();
			clientHandler.disconnectAllClients();
		} catch (InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private class PacketHandler implements Runnable {

		BlockingQueue<Packet> packetsToHandle = new LinkedBlockingQueue<>(180);
		List<Client> activeClients = Collections.synchronizedList(new ArrayList<>()); 

		@Override
		public void run() {
			while (serverON) {
				checkClientConnection();

				Packet packet = packetsToHandle.poll();
				Client owner;

				// Checks if both packet is null and if the client is null (client does not
				// exist here)
				if ((owner = searchActiveClients(packet)) == null) {
					continue;
				}

				handlePacketData(packet, owner);
			}

			// Server is no longer running
			packetsToHandle.clear();
			activeClients.clear();
		}

		private void handlePacketData(Packet packet, Client client) {
			if (packet.identity.getProtocol() == Protocol.TCP) {
				handleTcpPacket(packet.payload, client);
			} else {
				handleUdpPacket(packet.payload, client);
			}
		}

		private void handleTcpPacket(byte[] data, Client client) {
			// NOTE: Index 0 is packetID, and the next 4 bytes is the length of the actual
			// data
			var type = PacketData.getEnumByID(data[0]);

			// Index at the start of the payload
			final int headIndex = Protocol.TCP.getHeaderLength();

			switch (type) {
			case CLIENT_SETUP:
				client.setReadyToRecieveUDP(true);
				System.out.println("Client is ready for UDP: Server class");
				break;
			case CLIENT_DATA:
				var nameLength = Integer.valueOf(Bitmaths.bytesToString(data, headIndex, 2)) - 10;
				var name = Bitmaths.bytesToString(data, headIndex + 2, nameLength);
				var colour = Integer.valueOf(Bitmaths.bytesToString(data, headIndex + 2 + nameLength,
						data.length - (headIndex + 2 + nameLength)));

				client.name = name;
				client.colour = new Color(colour);

				// Colour last thing to be sent, relies on knowing the length of everything
				// else.
				break;
			case PING:
				var receiveTime = System.nanoTime();

				// Server is the originator when the packet contains both server and client
				// time.
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
					sendData(new Packet(echoData, client.getIdentity()), null);
				}
				break;
			default:
				System.out.println("Packet type not supported to handle: " + data[0] + " ClientHandler");
				break;
			}
		}

		private void handleUdpPacket(byte[] data, Client client) {
			var packetType = PacketData.getEnumByID(data[0]);

			// Index at the start of the payload
			final int headIndex = Protocol.UDP.getHeaderLength();

			switch (packetType) {
			case CLIENT_DIR:
				var directionY = Bitmaths.bytesToFloat(data, headIndex);
				var directionX = Bitmaths.bytesToFloat(data, headIndex + 4);

				client.setDirection(directionX, directionY);
				break;
			default:
				System.out.println("Packet type not supported to handle: " + data[0] + " UdpHandler class");
				return;
			}
		}
		
		private void checkClientConnection() {
			List<Client> toRemove = new ArrayList<>();
			
			synchronized (activeClients) {
				for (var client : activeClients) {
					if (!client.isConnected()) {
						toRemove.add(client);
					}
				}
				
			}
			
			activeClients.removeAll(toRemove);
		}

		private Client searchActiveClients(Packet packet) {
			if (packet == null)
				return null;
			
			synchronized (activeClients) {
				for (var client : activeClients) {
					if (packet.clientID == client.getIdentity()
							|| Objects.equals(packet.sourceAddress, client.getSocketAddress())) {
						return client;
					}
				}
			}
			
			return null;
		}
		
		/**
		 * Submits a TCP packet to be handled as well as marking the client as active
		 */
		public void submitPacket(Packet packet, Client client) {
			if (!activeClients.contains(client)) activeClients.add(client);
			
			boolean offered = packetsToHandle.offer(packet);

			if (!offered) {
				System.out.println("Packet handling queue is full. Abandoning packet");
			}
		}
		
		/**
		 * Submits a UDP packet to be handled (Does not care if the client exists or not)
		 */
		public void submitUdpPacket(Packet packet) {

			boolean offered = packetsToHandle.offer(packet);

			if (!offered) {
				System.out.println("Packet handling queue is full. Abandoning packet");
			}
		}

	} // End of PacketHandler class

} // End of Server class
