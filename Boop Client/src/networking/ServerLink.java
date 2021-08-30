package networking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import main.PlayerHandler;
import math.Bitmaths;

public class ServerLink implements Runnable {

	private SocketChannel channel;
	private int myPort = 0;

	private boolean establishedUDP = false;
	private volatile boolean endOfStream = false;
	volatile boolean setUpDataRecieved = false;

	private Packet nextInboundPacket;
	private Packet nextOutboundPacket = null;
	private BlockingQueue<Packet> inboundTcpQueue;
	private BlockingQueue<Packet> outboundTcpQueue;

	// Buffers can hold a minimum of 4 max sized packets at once
	private ByteBuffer readBuffer = ByteBuffer.allocate(PacketData.MAX_PAYLOAD_SIZE * 4);
	private ByteBuffer writeBuffer = ByteBuffer.allocate(PacketData.MAX_PAYLOAD_SIZE * 4);

	Client client;
	PlayerHandler players;
	InetSocketAddress serverAddress = null;

	ServerLink(Client client, PlayerHandler players) {
		this.client = client;
		this.players = players;

		inboundTcpQueue = new LinkedBlockingQueue<>(40);
		outboundTcpQueue = new LinkedBlockingQueue<>(40);
		
		nextInboundPacket = new Packet();
	}

	@Override
	public void run() {
		readIDfromServer();
		sendMyData();

		while (client.serverConnected) {
			try {
				readDataFromServer();
				writeDataToServer();

				// When client is fully initialised and set up, tell server to start sending
				// UDP.
				if (setUpDataRecieved && !establishedUDP) {
					readyForUDP();
				}

			} catch (IOException e) {
				e.printStackTrace();
				break;
			}

		}
	}

	private void readDataFromServer() throws IOException {
		// Read all data in the stream and store in buffer
		read(readBuffer);
		readBuffer.flip();

		// No data was read into the buffer and so there's no data to process
		// Early exit and clear buffer for reuse
		if (readBuffer.remaining() == 0) {
			readBuffer.clear();
			return;
		}

		// Write data to the next packets
		nextInboundPacket.writeToPacket(readBuffer);
		
		if (nextInboundPacket.isFilled()) {
			var packet = new Packet();
			packet.writeExtraDataToPacket(nextInboundPacket);
			
			client.submitPacketToHandler(nextInboundPacket);
			
			/*boolean offered = inboundTcpQueue.offer(nextInboundPacket);
			if (!offered) {
				System.out.println("Failed to offer packet to inboundTcpQueue");
				System.out.println("Abandoning packet");
			}*/
			
			nextInboundPacket = packet;
		}
		

		// Clear buffer for reuse
		readBuffer.clear();

		if (endOfStream) {
			closeConnection();
		}
	}

	private void writeDataToServer() throws IOException {
		// Once packet has been written to server, get the next packet.
		if (nextOutboundPacket == null || nextOutboundPacket.isWritten()) {
			Packet packet = outboundTcpQueue.poll();
			if (packet == null) return;
			
			nextOutboundPacket = packet;
		}
		
		int bytesToWrite = nextOutboundPacket.headerLength + nextOutboundPacket.expectedLength - nextOutboundPacket.bytesWritten;
		writeBuffer.put(nextOutboundPacket.payload, nextOutboundPacket.bytesWritten, bytesToWrite);
		writeBuffer.flip();

		nextOutboundPacket.bytesWritten += write(writeBuffer);
		
		writeBuffer.clear();
	}

	private int read(ByteBuffer byteBuffer) throws IOException {
		int bytesRead = channel.read(byteBuffer);
		int totalBytesRead = bytesRead;

		// Read as much data as it can until there is no more or the end of stream is
		// reached
		while (bytesRead > 0) {
			bytesRead = channel.read(byteBuffer);
			totalBytesRead += bytesRead;
		}

		if (bytesRead == -1) {
			endOfStream = true;
			return totalBytesRead + 1; // Account for bytesRead being -1
		}

		return totalBytesRead;
	}

	private int write(ByteBuffer byteBuffer) throws IOException {
		int bytesWritten = channel.write(byteBuffer);
		int totalBytesWritten = bytesWritten;

		while (bytesWritten > 0 && byteBuffer.hasRemaining()) {
			bytesWritten = channel.write(byteBuffer);
			totalBytesWritten += bytesWritten;
		}

		return totalBytesWritten;
	}

	private void readIDfromServer() {
		try {
			int bytesRead = 0;

			// Read until there is enough for the ID
			while (bytesRead < 8) {
				bytesRead += read(readBuffer);
				// Did server shutdown during this time
				if (isEndOfStream()) {
					closeConnection();
					return;
				}
			}

			readBuffer.flip();
			PlayerHandler.Me.ID = readBuffer.getLong();
			readBuffer.clear();

			// Used to keep extra data read into the buffer
			byte[] array = readBuffer.array();
			if (bytesRead > 8) {
				// Copy data read after ID value into this array
				byte[] copy = new byte[bytesRead - 8];
				System.arraycopy(array, 8, copy, 0, bytesRead - 8);
				readBuffer.put(copy);
			}

			System.out.println("My session ID: " + PlayerHandler.Me.ID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendMyData() {
		var nameLength = Integer.toString(PlayerHandler.Me.name.length() + 10);
		var name = PlayerHandler.Me.name;
		var colour = Integer.toString(PlayerHandler.Me.colour.getRGB());

		int payload = 2 + name.length() + colour.length();

		String[] myData = { nameLength, name, colour };

		byte[] myDataBytes = Bitmaths.stringArrayToBytes(myData);
		myDataBytes = Bitmaths.pushByteArrayToData(Bitmaths.intToBytes(payload), myDataBytes);
		myDataBytes = Bitmaths.pushByteToData(PacketData.CLIENT_DATA.getID(), myDataBytes);

		Packet packet = new Packet(myDataBytes, PlayerHandler.Me.ID);

		client.sendData(packet);
	}

	private void readyForUDP() {
		System.out.println("Informing server that I'm ready for UDP");
		byte[] data = { 5 };
		data = Bitmaths.pushByteArrayToData(Bitmaths.intToBytes(1), data);
		data = Bitmaths.pushByteToData(PacketData.CLIENT_SETUP.getID(), data);

		Packet packet = new Packet(data, PlayerHandler.Me.ID);
		
		System.out.println("I am ready for UDP packets");
		
		client.sendData(packet);
		establishedUDP = true;
	}

	public void connectToServer(InetSocketAddress serverAddress) throws IOException {
		channel = SocketChannel.open();
		channel.configureBlocking(false);
		boolean connected = channel.connect(serverAddress);

		// Will exit loop when connected or throws the IOException
		while (!connected) {
			connected = channel.finishConnect();
		}

		this.serverAddress = serverAddress;
		myPort = ((InetSocketAddress) channel.getLocalAddress()).getPort();
		endOfStream = false;
		System.out.println("my TCP port: " + myPort);

	}

	public boolean offerOutboundPacket(Packet packet) {
		return outboundTcpQueue.offer(packet);
	}

	public void closeConnection() throws IOException {
		channel.close();
		this.serverAddress = null;
		readBuffer.clear();
		writeBuffer.clear();
		System.out.println("Closed TCP channel");
	}
	
	public BlockingQueue<Packet> getCompletePackets(){
		return inboundTcpQueue;
	}

	public boolean isEndOfStream() {
		return endOfStream;
	}

	public int getMyPort() {
		return myPort;
	}

}
