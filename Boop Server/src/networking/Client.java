package networking;

import java.awt.Color;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import balls.Ball;
import math.Bitmaths;
import math.Physics;
import math.Vec2f;

public class Client implements Runnable{
	
	private Socket myClientSocket;
	
	DataInputStream in;
	DataOutputStream out;
	
	private long ID = 0;
	private InetAddress ipv4Address;
	private long msPing = 0;
	
	private int clientPort;
	
	public LinkedBlockingQueue<byte[]> dataBuffer;
	
	private volatile boolean connected = false;
	public volatile boolean readyToRecieveUDP = false;
	
	Thread clientThread;
	
	//Set these values up at client and have them sent.
	String name = "Pakistan > India";
	Color colour = Color.GREEN;
	
	public Vec2f centrePos = new Vec2f(); 	//centre of screen of client.
	public Vec2f velocity = new Vec2f();
	public Vec2f direction = new Vec2f();   //Direction player wants to go.
	public float radOfVision =  0.5f;       //radius of region balls are sent to client.
	public float radOfInf = 0.5f;			//radius of region balls are attracted to the client.
	
	public static final float attractionCoefficient = 0.001f; //multiplied by number of owned balls to give attraction strength.
	public static final float influenceCoefficient = 0.01f; //multiplied by number of balls to give area of influence. 
	//NOTE: May make radius of influence proportional to number of local balls so, rate of area increase slows
	//		as it gets bigger.
	
	public List<Ball> ownedBalls = new ArrayList<>();	//list of balls that the player possesses.
	public List<Ball> localBalls = new ArrayList<>(); // All balls in the  territory.
	
	private long[] timeSinceLastPacket = new long[Packet.values().length];

	private float maxSpeed = 0.3f; //Speed that the client's centre moves
	private boolean[] pressedKeys = new boolean[4]; // Array to track which keys are being pressed
	
	public AtomicInteger udpPacketsSent = new AtomicInteger(0);
	public AtomicInteger udpPacketsRecieved = new AtomicInteger(0);
	
	
	public Client() {
		clientThread = new Thread(this, "Client-Thread");
		
		// Accessing thread will be blocked until the queue is not empty.
		dataBuffer = new LinkedBlockingQueue<>(30);
		
		centrePos.set(0f, 0f);
	}
	
	@Override
	public void run() {
		while(connected && ClientAccept.serverON) {
			// Read data in stream and store in buffer
			try {
				dataBuffer.add(recieveData());
				handleAllData();
			} catch (IOException e) {
				e.printStackTrace();
				disconnect();
				break;
			}
		}
	}
	
	private byte[] recieveData() throws IOException{
		var packetID = in.readByte();
		if (packetID == Packet.DISCONNECT.getID()) throw new IOException();
		
		// Reads the correct length of data to store into the dataBuffer.
		var len = in.readInt();
		byte[] data = new byte[len + 1];
		data[0] = packetID;
		
		byte[] payload = in.readNBytes(len);
		
		System.arraycopy(payload, 0, data, 1, len);
		
		return data;
	}
	
	private void handleAllData() throws IOException{
		while (!dataBuffer.isEmpty()) {
			byte[] data = dataBuffer.poll();
			
			switch (data[0]) {
			
			case 71:
				readyToRecieveUDP = true;
				System.out.println("Client class, ready for UDP");
				break;
			case 70:
				var nameLength = Integer.valueOf(Bitmaths.bytesToString(data, 1, 2)) - 10;
				var name = Bitmaths.bytesToString(data, 3, nameLength);
				var colour = Integer.valueOf(Bitmaths.bytesToString(data, 3 + nameLength, data.length - (3 + nameLength)));
				
				this.name = name;
				this.colour = new Color(colour);
				
				//Colour last thing to be sent, relies on knowing the length of everything else.
				break;
			case 5:
				var receiveTime = System.nanoTime();
				
				// Server is the sender when the packet contains both server and client time.
				var isServerSender = data.length == Packet.PING.getObjectSize() + 1;
				
				// Calculate ping and store data
				if (isServerSender) {
					var receivedClient = Bitmaths.bytesToLong(data, 1);
					long serverToClient = receivedClient - Bitmaths.bytesToLong(data, 9);
					long clientToServer = receiveTime - receivedClient;
					long rtt = serverToClient + clientToServer;
					msPing = rtt / 1000000;
				}
				// Add time received to packet and echo back to client.
				else {
					byte[] echoData = new byte[8];
					System.arraycopy(data, 1, echoData, 0, 8);
					echoData = Bitmaths.pushByteArrayToData(Bitmaths.longToBytes(receiveTime), echoData);
					echoData = Bitmaths.pushByteArrayToData(Bitmaths.intToBytes(16), echoData);
					echoData = Bitmaths.pushByteToData(Packet.PING.getID(), echoData);
					out.write(echoData);
				}
				
				break;
			default:
				System.out.println("Tcp packet id not supported: " + data[0]);
				break;
			}
		}
	}
	
	public void setupConnection(Socket socket) throws IOException{
		
		if (ID == 0) {
			System.out.println("Client ID has not been set");
			return;
		}
		
		myClientSocket = socket;
		
		in = new DataInputStream(myClientSocket.getInputStream());
		out = new DataOutputStream(myClientSocket.getOutputStream());
		out.writeLong(ID);
		
		ipv4Address = socket.getInetAddress();
		clientPort = socket.getPort();
		
		System.out.println("Client ID: " + ID);
		System.out.println("Client port: " + clientPort);
		System.out.println("Client ip: " + ipv4Address);
		
		connected = true;
		clientThread.start();
	}
	
	//Alerts client about another client joining or leaving.
	public void alertClient(Client client, boolean joining){
		//Made all strings because name is a string.
		var clientStringData = new String[5];
		
		//4 + 2 + 89 + 8 + 1
		clientStringData[0] = Long.toString(client.ID);  //ID always has four digits and so has four bytes as a string.
		clientStringData[1] = Integer.toString(client.name.length() + 10); //Add 10 to ensure string always has two digits. No name > 89 allowed
		clientStringData[2] = client.name;
		clientStringData[3] = (joining)? "1":"0";
		clientStringData[4] = Integer.toString(client.colour.getRGB());
		
		int payload = 4 + 2 + client.name.length() + 1 + clientStringData[4].length();
		
		byte[] clientData = Bitmaths.stringArrayToBytes(clientStringData);
		clientData = Bitmaths.pushByteArrayToData(Bitmaths.intToBytes(payload), clientData);
		clientData = Bitmaths.pushByteToData((byte) 70, clientData);
		
		try {
			out.write(clientData);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void finishSetUp(){
		byte[] data = {5};
		data = Bitmaths.pushByteArrayToData(Bitmaths.intToBytes(1), data);
		data = Bitmaths.pushByteToData((byte) 71, data);
		try {
			out.write(data);
			System.out.println("client class- last set up data sent");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void disconnect(){
		if (connected) {
			connected = false;
			try {
				if (!myClientSocket.isClosed()) {
					byte[] disconnect = {Packet.DISCONNECT.getID()};
					out.write(disconnect);
					myClientSocket.close();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			System.out.println("Client " + ID + " has Disconnected");
			try {
				clientThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
				clientThread.interrupt();
			}

		}
	}
	
	/**
	 * Check if it's time to send this client a particular packet given a delay between the last one.
	 * @param msDelayBetweenPackets Delay in milliseconds between each packet of this type.
	 * @param packet The packet type being sent.
	 */
	public boolean isReadyForPacket(float msDelayBetweenPackets, Packet packet) {
		long currentTime = System.currentTimeMillis();
		long lastPacketTime = timeSinceLastPacket[packet.ordinal()];
		long dt = currentTime - lastPacketTime;
		
		
		if (dt < msDelayBetweenPackets || !readyToRecieveUDP) {
			return false;
		}
		
		timeSinceLastPacket[packet.ordinal()] = currentTime;
		return true;
	}
	
	public void setIdentity(long id) {
		ID = id;
	}
	
	public long getIdentity() {
		return ID;
	}

	public int getClientPort() {
		return clientPort;
	}

	public InetAddress getIpv4Address() {
		return ipv4Address;
	}
	
	/**
	 * @return ping of the client in milliseconds.
	 */
	public long getPing() {
		return msPing;
	}
	
	public boolean isConnected() {
		return connected;
	}

	public void handleKey(boolean pressed, int key) {
		if (key < 0 || key > 3) return;

		pressedKeys[key] = pressed;
	}
	
	public void setDirection(float dx, float dy) {
		direction.set(dx, dy);
	}

	public void updateVelocity(float dt) {
		Vec2f.scale(velocity, direction, maxSpeed);
	}
	
	public void updatePos(float dt) {
		
		Vec2f.increment(centrePos, centrePos, velocity, dt);
		
		if (centrePos.y < -1) centrePos.y += 2;
		if (centrePos.x < -1) centrePos.x += 2;
		if (centrePos.y > 1) centrePos.y -= 2;
		if (centrePos.x > 1) centrePos.x -= 2;

	}

	public void sendCentrePos() {
		float[] clientPosData = new float[4];
		clientPosData[1] = centrePos.x;
		clientPosData[2] = centrePos.y;
		clientPosData[3] = radOfVision;
		clientPosData[0] = (float) 3 * 4; // 3 floats * 4 bytes = 12 byte payload (length)
		
		byte[] clientPos = Bitmaths.floatArrayToBytes(clientPosData);
		clientPos = Bitmaths.pushByteToData((byte) 70, clientPos);
		
		try {
			out.write(clientPos);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void updateRadii() {
		radOfInf = (float) Math.sqrt(influenceCoefficient*ownedBalls.size()/Math.PI) + 0.1f;
		radOfVision = 3*radOfInf;
	}
	
	/**
	 * Checks if the ball is within the attraction range of the client.
	 * @param b The ball to check with.
	 * @return If the ball is in range.
	 */
	public boolean isInReach(Ball b) {
		var disp = new Vec2f();
		Vec2f.minDisp(disp, centrePos, b.phys.pos);
		
		return (disp.lengthSq() <= (radOfInf + b.getRad())*(radOfInf+b.getRad()));
	}
	
	/**
	 * Checks if the ball is within range for the ball data to be sent to the client.
	 * @param b The ball to check with.
	 * @return If it's appropriate to send the ball to the client.
	 */
	public boolean isInSendingRange(Ball b) {
		var disp = new Vec2f();
		Vec2f.minDisp(disp, centrePos, b.phys.pos);
		
		return (disp.lengthSq() <= (radOfVision + b.getRad())*(radOfVision + b.getRad()));
	}

	public final long getMsPing() {
		return msPing;
	}
	
//	public void updateLocalBalls() {
//		
//	}
}
