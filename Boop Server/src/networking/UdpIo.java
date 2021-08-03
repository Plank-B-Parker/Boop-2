package networking;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import balls.Ball;
import math.Bitmaths;

/**
 * Deals with the receiving and sending of packets using the UDP protocol.
 * <p>
 * Packets received are put into a linked blocking queue for the handler class to deal
 * with.
 */
public class UdpIo implements Runnable {

	private ClientHandler clientHandler;
	private DatagramSocket socket;
	private Thread threadUDP;
	private BlockingQueue<DatagramPacket> packets;

	public static final AtomicInteger recievedPacketsUDP = new AtomicInteger(0);
	public static final AtomicInteger sentPacketsUDP = new AtomicInteger(0);

	public UdpIo(ClientHandler clientHandle) {
		clientHandler = clientHandle;
		packets = new LinkedBlockingQueue<>(180);

		try {
			this.socket = new DatagramSocket(ClientAccept.PORT);
		} catch (SocketException e) {
			e.printStackTrace();
			System.out.println("Udp IO socket could not be created");
		}

		threadUDP = new Thread(this, "UDP-IO");
		threadUDP.setDaemon(true);
	}

	@Override
	public void run() {
		while (ClientAccept.serverON) {
			byte[] data = new byte[PacketData.MAX_PAYLOAD_SIZE];

			var packet = new DatagramPacket(data, data.length);

			try {
				socket.receive(packet);
				recievedPacketsUDP.incrementAndGet();

				boolean enqueued = packets.offer(packet, 2, TimeUnit.MILLISECONDS);
				if (!enqueued) {
					System.out.println("No space to enqueue udp packet: UdpIO class run()");
				}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
				System.out.println("UDP recieve problem");
				Thread.currentThread().interrupt();
				break;
			}
		}

	}
	
	public void sendDataQueue(byte[] data, InetAddress ipAddress, int port) {
		// TODO queue a packet for sending (use ArrayBlocking queue)
		// Ideally use 1 thread for receiving and 1 thread for sending
	}
	
	public void sendData(byte[] data, InetAddress ipAddress, int port) {
		var packet = new DatagramPacket(data, data.length, ipAddress, port);

		try {
			socket.send(packet);
			sentPacketsUDP.incrementAndGet();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public void sendPacket(DatagramPacket packet) {
		try {
			socket.send(packet);
			sentPacketsUDP.incrementAndGet();
		} catch (IOException e) {
			e.printStackTrace();
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
			if (!client.isReadyForPacket(1000, PacketData.NEW_BALLS) || !client.isReadyToRecieveUDP())
				continue;

			readyClients.add(client);
		}
		
		// Store every client with a number of packets filled with data
		var data = new byte[readyClients.size()][maxPackets][PacketData.FREE_PAYLOAD_SIZE];
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

				sendData(completePacket, client.getIpv4Address(), client.getClientPort());

			}
		}

	}

	
	/**
	 * Send every client information about every other client's position and movement.
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
		
		// Shorten list of clients only to those who are ready and prepare the data for them
		for (var client : clients) {
			if (!client.isReadyForPacket(1000, PacketData.CLIENT_DATA) || !client.isReadyToRecieveUDP())
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
				
				sendData(completePacket, client.getIpv4Address(), client.getClientPort());
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

	public void startUdpIo() {
		threadUDP.start();
	}

	public void disconnect() {
		try {
			socket.close();
			threadUDP.join();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			e.printStackTrace();
		}
	}

	public class UdpHandler implements Runnable {

		// TODO Possibly expand to a thread pool to handle hundreds of clients.
		Thread thread;

		public UdpHandler() {
			thread = new Thread(this, "Udp-Handler");
			thread.setDaemon(true);
		}

		@Override
		public void run() {

			while (ClientAccept.serverON) {
				try {
					DatagramPacket packet = packets.poll(2, TimeUnit.MILLISECONDS);
					
					if (packet == null) continue; // TODO adjust polling time to help branch predictor
					
					handleData(packet);
				} catch (InterruptedException e) {
					e.printStackTrace();
					System.out.println("Udp handler thread has been interrupted");
					Thread.currentThread().interrupt();
				}
			}
		}

		private void handleData(DatagramPacket packet) {

			var packetType = PacketData.getEnumByID(packet.getData()[0]);

			var client = clientHandler.getClientByAddressAndPort(packet.getAddress(), packet.getPort());

			if (client == null) {
				System.out.println("UDP packet sent from unknown client: " + packet.getAddress().toString());
				return;
			}

			switch (packetType) {
			case CLIENT_DIR:
				var directionY = Bitmaths.bytesToFloat(packet.getData(), 1);
				var directionX = Bitmaths.bytesToFloat(packet.getData(), 5);
				
				client.setDirection(directionX, directionY);
				break;
			default:
				System.out.println("Packet type not supported to handle: " + packet.getData()[0] + " UdpHandler class");
				return;
			}
		}

		public void startUdpHandler() {
			thread.start();
		}
		
		public void stopUdpHandler() {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
				Thread.currentThread().interrupt();
			}
		}

	} // End of UdpHandler inner class

} // End of UDP outer class
