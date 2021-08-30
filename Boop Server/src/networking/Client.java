package networking;

import java.awt.Color;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import balls.Ball;
import math.Bitmaths;
import math.Physics;
import math.Vec2f;
import networking.PacketData.Protocol;

public class Client {

	private long ID = 0;
	private SocketChannel clientChannel;
	private InetAddress ipv4Address;
	private int clientPort;
	private SocketAddress socketAddress;
	private boolean endOfStream = false;

	long msPing = 0;

	PacketReader tcpPacketReader;
	PacketWriter tcpPacketWriter;

	private volatile boolean connected = false;
	private volatile boolean readyToRecieveUDP = false;

	// Set these values up at client and have them sent.
	String name = "Pakistan > India";
	Color colour = Color.GREEN;

	public Vec2f centrePos = new Vec2f(); // centre of screen of client.
	public Vec2f velocity = new Vec2f();
	public Vec2f direction = new Vec2f(); // Direction player wants to go.
	public float radOfVision = 0.5f; // radius of region balls are sent to client.
	public float radOfInf = 0.5f; // radius of region balls are attracted to the client.

	public static final float attractionCoefficient = 0.001f; // multiplied by number of owned balls to give attraction
																// strength.
	public static final float influenceCoefficient = 0.01f; // multiplied by number of balls to give area of influence.
	// NOTE: May make radius of influence proportional to number of local balls so,
	// rate of area increase slows
	// as it gets bigger.

	public List<Ball> ownedBalls = new ArrayList<>(); // list of balls that the player possesses.
	public List<Ball> localBalls = new ArrayList<>(); // All balls in the territory.

	private long[] timeSinceLastPacket = new long[PacketData.getEnums().length]; // in milliseconds

	private float maxSpeed = 0.3f; // Speed that the client's centre moves
	private boolean[] pressedKeys = new boolean[4]; // Array to track which keys are being pressed

	private AtomicInteger udpPacketsSent = new AtomicInteger(0);
	private AtomicInteger udpPacketsRecieved = new AtomicInteger(0);

	Client() {
	}

	public void read(ByteBuffer readBuffer) throws IOException {
		tcpPacketReader.read(this, readBuffer);
	}

	public int write(ByteBuffer byteBuffer) throws IOException {
		int bytesWritten = clientChannel.write(byteBuffer);
		int totalBytesWritten = bytesWritten;

		while (bytesWritten > 0 && byteBuffer.hasRemaining()) {
			bytesWritten = clientChannel.write(byteBuffer);
			totalBytesWritten += bytesWritten;
		}

		return totalBytesWritten;
	}

	/**
	 * Assigns the socketChannel and ipAddresses to the client class. As well as
	 * sends the ID to the client.
	 * 
	 * @param socketChannel belongs to this client
	 * @throws IOException
	 */
	public void setupConnection(SocketChannel socketChannel) throws IOException {
		if (ID == 0) {
			System.out.println("Client ID has not been set");
			return;
		}

		this.clientChannel = socketChannel;

		ByteBuffer buffer = ByteBuffer.allocate(8).putLong(ID);
		buffer.flip();

		clientChannel.write(buffer);

		socketAddress = socketChannel.getRemoteAddress();
		ipv4Address = ((InetSocketAddress) socketAddress).getAddress();
		clientPort = ((InetSocketAddress) socketAddress).getPort();

		System.out.println("Client ID: " + ID);
		System.out.println("Client port: " + clientPort);
		System.out.println("Client ip: " + ipv4Address);

		connected = true;
	}

	public void finishSetUp(BlockingQueue<Packet> outboundPacketQueue) {
		byte[] data = { 5 };
		data = Bitmaths.pushByteArrayToData(Bitmaths.intToBytes(1), data);
		data = Bitmaths.pushByteToData(PacketData.CLIENT_SETUP.getID(), data);

		Packet joinPacket = new Packet(data, ID);

		System.out.println("client class- last set up data sent");

		// Effectively block until there's space
		// TODO Create a list of not setup clients and try offering every tick
		boolean offered = false;
		while (!offered) {
			System.out.println("Offering finish Setup packet");
			offered = outboundPacketQueue.offer(joinPacket);
		}
	}

	public void disconnect() {
		if (connected) {
			connected = false;
			try {
				if (!clientChannel.isConnected()) {
					clientChannel.close();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			System.out.println("Client " + ID + " has Disconnected");

		}
	}

	/**
	 * Check if it's time to send this client a particular packet given a delay
	 * between the last one.
	 * 
	 * @param msDelayBetweenPackets Delay in milliseconds between each packet of
	 *                              this type.
	 * @param packet                The packet type being sent.
	 */
	public boolean isReadyForPacket(float msDelayBetweenPackets, PacketData packet) {
		long currentTime = System.currentTimeMillis();
		long lastPacketTime = timeSinceLastPacket[packet.ordinal()];
		long dt = currentTime - lastPacketTime;

		boolean notUdpReady = packet.getProtocol() == Protocol.UDP && !readyToRecieveUDP;

		if (dt < msDelayBetweenPackets || notUdpReady) {
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

	public SocketAddress getSocketAddress() {
		return socketAddress;
	}

	public InetAddress getIpv4Address() {
		return ipv4Address;
	}

	public int getClientPort() {
		return clientPort;
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
		if (key < 0 || key > 3)
			return;

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

		if (centrePos.y < -1)
			centrePos.y += 2;
		if (centrePos.x < -1)
			centrePos.x += 2;
		if (centrePos.y > 1)
			centrePos.y -= 2;
		if (centrePos.x > 1)
			centrePos.x -= 2;

	}

	public void updateRadii() {
		radOfInf = (float) Math.sqrt(influenceCoefficient * ownedBalls.size() / Math.PI) + 0.1f;
		radOfVision = 3 * radOfInf;
	}

	/**
	 * Checks if the ball is within the attraction range of the client.
	 * 
	 * @param b The ball to check with.
	 * @return If the ball is in range.
	 */
	public boolean isInReach(Ball b) {
		var disp = new Vec2f();
		Physics.disp(disp, centrePos, b.phys.pos);

		return (disp.lengthSq() <= (radOfInf + b.getRad()) * (radOfInf + b.getRad()));
	}

	/**
	 * Checks if the ball is within range for the ball data to be sent to the
	 * client.
	 * 
	 * @param b The ball to check with.
	 * @return If it's appropriate to send the ball to the client.
	 */
	public boolean isInSendingRange(Ball b) {
		var disp = new Vec2f();
		Physics.disp(disp, centrePos, b.phys.pos);

		return (disp.lengthSq() <= (radOfVision + b.getRad()) * (radOfVision + b.getRad()));
	}

	public SocketChannel getClientChannel() {
		return clientChannel;
	}

	public void setClientChannel(SocketChannel clientChannel) {
		this.clientChannel = clientChannel;
	}

	public boolean isEndOfStream() {
		return endOfStream;
	}

	public void setEndOfStream(boolean endOfStream) {
		this.endOfStream = endOfStream;
	}

	public long getMsPing() {
		return msPing;
	}

	public int incrementUdpPacketsReceived() {
		return udpPacketsRecieved.incrementAndGet();
	}

	public int getUdpPacketsReceived() {
		return udpPacketsRecieved.get();
	}

	public int incrementUdpPacketsSent() {
		return udpPacketsSent.incrementAndGet();
	}

	public int getUdpPacketsSent() {
		return udpPacketsSent.get();
	}

	public boolean isReadyToRecieveUDP() {
		return readyToRecieveUDP;
	}

	public void setReadyToRecieveUDP(boolean readyToRecieveUDP) {
		this.readyToRecieveUDP = readyToRecieveUDP;
	}

}
