package networking;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import balls.Ball;
import math.Bitmaths;
import math.Physics;
import math.Vec2f;

public class UDP implements Runnable{
	
	DatagramSocket socket;

	Thread threadUDP;
	
	ArrayList<InetAddress> confirmedIPs;
	ArrayList<InetAddress> problemIPs;
	ArrayList<InetAddress> maliciousIPs;
	
	public AtomicInteger RecievedPacketsUDP = new AtomicInteger(0);
	public AtomicInteger SentPacketsUDP = new AtomicInteger(0);
	
	
	private ClientHandler clientHanlder;

	public UDP(ClientAccept clientAcceptor) {
		clientHanlder = clientAcceptor.clientHandler;
		
		try {
			this.socket = new DatagramSocket(ClientAccept.PORT);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		threadUDP = new Thread(this, "UDP-thread");
		
	}
	
	@Override
	public void run() {
		while(ClientAccept.serverON) {
			byte[] data = new byte[Packet.MAX_PAYLOAD_SIZE];
			
			var packet = new DatagramPacket(data, data.length);
			
			try {
				socket.receive(packet);
				RecievedPacketsUDP.incrementAndGet();
				handleData(packet);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("UDP problem");
				break;
			}
		}
		
	}
	
	private void handleData(DatagramPacket packet) {

		var client = clientHanlder.getClientByAddressAndPort(packet.getAddress(), packet.getPort());

		if (client == null) {
			System.out.println("UDP packet sent from unknown client: "+packet.getAddress().toString());
			return;
		}

		switch (packet.getData()[0]) {
		case 1:
			byte data = packet.getData()[1];
			boolean pressed = 1 == ((data >> 6) & 1); // Determines whether a key has been pressed or released
			data = (byte) (data & ~(1 << 6)); // Resets the pressed bit to revert to the key number
			client.handleKey(pressed, data);
			break;
		case 5:
			var directionY = Bitmaths.bytesToFloat(packet.getData(),1);
			var directionX = Bitmaths.bytesToFloat(packet.getData(),5);
			
//			System.out.println("(" + client.centrePos.x + ", " + client.centrePos.y + ")");
			//System.out.println("(" +  directionX + ", " +  directionY + ")");
			
			client.setDirection(directionX, directionY);
			break;
		case 10:
			
			
			byte[] test = Bitmaths.intToBytes(packet.getPort());
			test = Bitmaths.pushByteToData((byte) 10, test);
			test = Bitmaths.pushByteArrayToData(Bitmaths.intToBytes(client.udpPacketsSent.incrementAndGet()), test);
			
			System.out.println(packet.getPort());
			sendData(test, packet.getAddress(), packet.getPort());
			break;
		default:
			return;
		}
	}
	
	public void sendData(byte[] data, InetAddress ipAddress, int port) {
		var packet = new DatagramPacket(data, data.length, ipAddress, port);
		
		try {
			socket.send(packet);
			SentPacketsUDP.incrementAndGet();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//var sequence = Bitmaths.bytesToInt(data);
		//System.out.println("Packet sequence: " + Bitmaths.bytesToInt(data));
		//System.out.println("Total packets sent: " + SentPacketsUDP.get());
	}
	
	/**
	 * Sends every client all of the ball that are close to them in sequential order.
	 * @param allBalls All of the balls on the server.
	 * @param clients All clients on the server
	 */
	public void sendBalls(final List<Ball> allBalls, final List<Client> clients) {
		// packet id, ballID, ball type, x, y, velx, vely, ownerID 
		
		final int maxBallsPerPacket = Packet.NEW_BALLS.getNumObj();
		
		int packetsNo = (int) Math.ceil((float) allBalls.size() / (float) maxBallsPerPacket);
		
		// Prepare data only for clients that are ready to accept packets.
		List<Client> readyClients = new ArrayList<>();
		
		// Shorten list of clients to only clients that are ready to accept.
		for (var i = 0; i < clients.size(); i++) {
			
			var client = clients.get(i);
			//Check if client is ready;
			if(!client.isReadyForPacket(100, Packet.NEW_BALLS)) continue;
			
			readyClients.add(client);
		}
		
		byte[][][] data = new byte[readyClients.size()][packetsNo][Packet.FREE_PAYLOAD_SIZE];
		int[] clientMaxPackets = new int[readyClients.size()];
		
		// Fill the packets with data for each client with the correct amount of balls.
		for (var i = 0; i < readyClients.size(); i++) {
			var client = readyClients.get(i);
			
			// check if ball is in client's vision area (simple circle).
			List<Ball> inRange = new ArrayList<>();
			for (Ball ball: allBalls) {
				if (client.isInSendingRange(ball)) inRange.add(ball);
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
				headerInfo = Bitmaths.pushByteArrayToData(Bitmaths.intToBytes(client.udpPacketsSent.incrementAndGet()), headerInfo);
				headerInfo = Bitmaths.pushByteToData(Packet.NEW_BALLS.getID(), headerInfo);
				
				byte[] completePacket = Bitmaths.pushByteArrayToData(headerInfo, data[i][j]);
				
				sendData(completePacket, client.getIpv4Address(), client.getClientPort());
				
			}
		}
		
	}
	
	public void sendAllClientInfo(final List<Client> clients) {
		final int numItemsPerObj = Packet.CLIENTDATA.getNumberOfItems();
		final int maxClientsPerPacket = Packet.CLIENTDATA.getNumObj();
		
		// Prepare data only for clients that are ready to accept packets.
		List<Client> readyClients = new ArrayList<>();
		
		var clientsData = new Number[clients.size() * numItemsPerObj];
		var numClients = 0;
		var offset = 0;
		
		for (var client : clients) {
			if (!client.isReadyForPacket(200, Packet.CLIENTDATA)) continue;
			
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
		
		int packetNo = (int)Math.ceil((float)numClients / (float)maxClientsPerPacket);
		boolean isSinglePacket = packetNo == 1;
		
		int payloadSize = isSinglePacket 
				? numClients * Packet.CLIENTDATA.getObjectSize() : Packet.CLIENTDATA.getMaxPayload();
		
		int numbersPerPacket = isSinglePacket ? 
				numClients * numItemsPerObj : numItemsPerObj * maxClientsPerPacket;
		
		byte[][] data = new byte[packetNo][payloadSize];
		Number[][] splitInput = new Number[packetNo][numbersPerPacket];
		
		var inputFilled = 0;
		offset = 0;
		
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
		
		// Fill packets with data
		// array to replace with header information
		byte[] headerInfo = new byte[0];
		for(int i = 0; i < packetNo; i++) {
			data[i] = Bitmaths.numberArrayToBytes(splitInput[i]);
		}
		
		// Send data to all clients sequentially.
		for (var client : readyClients) {
			for (var i = 0; i < packetNo; i++) {
				headerInfo = new byte[0];
				headerInfo = Bitmaths.pushByteArrayToData(Bitmaths.intToBytes(client.udpPacketsSent.incrementAndGet()), headerInfo);
				headerInfo = Bitmaths.pushByteToData(Packet.CLIENTDATA.getID(), headerInfo);
				byte[] completePacket = Bitmaths.pushByteArrayToData(headerInfo, data[i]);
				
				sendData(completePacket, client.getIpv4Address(), client.getClientPort());
			}
		}
		
	}
	
	/**
	 * Converts floatData from all balls in range of the client into a byte array of and stores
	 * it into the give 'data' byte array.
	 * @param numberOfPackets number of packets to fill.
	 * @param ballsInRange all balls that are in range of the client.
	 * @param data byte array to fill with ball data.
	 */
	private void fillBallData(int numberOfPackets, final List<Ball> ballsInRange, byte[][] data) {
		final int numberOfItems = Packet.NEW_BALLS.getNumberOfItems();
		final int maxBallsPerPacket = Packet.NEW_BALLS.getNumObj();
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
				offset = numberOfItems *  b1;
				
				floatData[packetsFilled][offset] = (float) ball.getID();
				floatData[packetsFilled][offset + 1] = (float) ball.getType();
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
	
	public void startUDP() {
		threadUDP.start();
	}
	
	public void disconnect(){
		try {
			socket.close();
			threadUDP.join();
		} catch (InterruptedException e) {
			threadUDP.interrupt();
			e.printStackTrace();
		}
	}
}
