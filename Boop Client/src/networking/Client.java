package networking;

import java.awt.Color;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import balls.Storage;
import display.Display;
import main.Player;
import main.PlayerHandler;
import math.Bitmaths;
import math.Vec2f;
import networking.PacketData.Protocol;

public class Client {

	private ServerLink serverLink;
	private UdpLink udpLink;
	private PacketHandler packetHandler;

	private Thread serverWorker;
	private Thread udpWorker;
	private Thread packetWorker;

	private PlayerHandler players;
	private Storage storage;

	private long[] timeSinceLastPacket = new long[PacketData.getEnums().length];
	volatile boolean serverConnected = false;

	public static final int SERVER_PORT = 2300;

	public Client(PlayerHandler players, Storage storage) {
		this.players = players;
		this.storage = storage;

		serverLink = new ServerLink(this, players);
		udpLink = new UdpLink(this, players);
		packetHandler = new PacketHandler();

		serverWorker = new Thread(serverLink, "Server-Link");
		udpWorker = new Thread(udpLink, "UDP-Link");
		packetWorker = new Thread(packetHandler, "Packet-Handler");
	}

	public void sendData(Packet packet) {
		boolean dataQueued = packet.identity.getProtocol() == Protocol.TCP ? serverLink.offerOutboundPacket(packet)
				: udpLink.offerOutboundPacket(packet);

		if (!dataQueued) {
			System.out.println(packet.identity + ": Outbound packet queue is full, abandoning packet");
		}
	}
	
	public void submitPacketToHandler(Packet packet) {
		boolean offered = packetHandler.packetsToHandle.offer(packet);
		
		if (!offered) {
			System.out.println("PacketHandler is too full, abandoning packet. Please slow down!");
		}
	}

	public void sendInputs() {
		// sends any input changes to the server

		if (!isServerReadyForPacket(50, PacketData.CLIENT_DIR))
			return;

		byte[] data = new byte[0];
		data = Bitmaths.pushByteArrayToData(Bitmaths.floatToBytes(Player.direction.x), data);
		data = Bitmaths.pushByteArrayToData(Bitmaths.floatToBytes(Player.direction.y), data);
		// Add header information
		data = Bitmaths.pushByteArrayToData(Bitmaths.floatToBytes(UdpLink.sentPacketsUDP.get()), data);
		data = Bitmaths.pushByteToData(PacketData.CLIENT_DIR.getID(), data);
		sendData(new Packet(data, PlayerHandler.Me.ID));
	}

	public void connectToServer(InetAddress serverIP, int serverPort) throws IOException {
		var serverAddress = new InetSocketAddress(serverIP, serverPort);
		serverLink.connectToServer(serverAddress);
		udpLink.connectToServerUDP(serverAddress, serverLink.getMyPort());

		serverConnected = true;

		serverWorker.start();
		udpWorker.start();
		packetWorker.start();
	}

	public void disconnectFromServer() {
		serverConnected = false;
		try {
			serverWorker.join();
			udpWorker.join();
			packetWorker.join();

			// TODO Clear resources in serverLink and UDPLink classes
			serverLink.closeConnection();
			udpLink.closeConnection();
		} catch (InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean isServerConnected() {
		return serverConnected;
	}

	/**
	 * Check if it's time to send the server a particular packet given a delay
	 * between the last one.
	 * 
	 * @param msDelayBetweenPackets Delay in milliseconds between each packet of
	 *                              this type.
	 * @param packet                The packet type being sent.
	 */
	private boolean isServerReadyForPacket(float msDelayBetweenPackets, PacketData packet) {
		long currentTime = System.currentTimeMillis();
		long lastPacketTime = timeSinceLastPacket[packet.ordinal()];
		long dt = currentTime - lastPacketTime;

		if (dt < msDelayBetweenPackets) {
			return false;
		}

		timeSinceLastPacket[packet.ordinal()] = currentTime;
		return true;
	}

	private class PacketHandler implements Runnable {
		
		BlockingQueue<Packet> packetsToHandle = new LinkedBlockingQueue<>(180);
		
		@Override
		public void run() {
			while (serverConnected) {
				var packet = packetsToHandle.poll();
				
				if (packet == null) continue;
				
				handlePacketData(packet);
			}
			
			packetsToHandle.clear();
		}

		private void handlePacketData(Packet packet) {
			if (packet.identity.getProtocol() == Protocol.TCP) {
				handleTcpData(packet.payload);
			} else {
				handleUdpData(packet.payload);
			}
		}

		private void handleTcpData(byte[] data) {
			var packetType = PacketData.getEnumByID(data[0]);
			
			// Index at the start of the payload
			final int headIndex = Protocol.TCP.getHeaderLength();
			final int payloadLength = Bitmaths.bytesToInt(data, 1);

			switch (packetType) {
			case CLIENT_SETUP:
				serverLink.setUpDataRecieved = true;
				System.out.println("last setup data recieved from server");
				break;
			case CLIENT_JOIN:
				// Last thing sent.
				// Other Players info.
				var playerID = Long.parseLong(Bitmaths.bytesToString(data, headIndex, 4));

				var nameLength = Integer.valueOf(Bitmaths.bytesToString(data, headIndex + 4, 2)) - 10;
				String name = Bitmaths.bytesToString(data, headIndex + 4 + 2, nameLength);

				var joining = (Bitmaths.bytesToString(data, headIndex + 4 + 2 + nameLength, 1).equals("1"));

				var colourLength = payloadLength - (headIndex + 4 + 2 + nameLength + 1);
				var colour = Integer.valueOf(Bitmaths.bytesToString(data, headIndex + 4 + 2 + nameLength + 1, colourLength));

				if (joining)
					players.add(new Player(false, playerID, name, new Color(colour), new Vec2f()));
				else
					players.remove(playerID);

				break;

			case PING:
				var receiveTime = System.nanoTime();

				// Client is the sender when the packet contains both client and server time.
				var isClientSender = data.length == PacketData.PING.getObjectSize() + 1;

				// Calculate ping and store data
				if (isClientSender) {
					var receivedServer = Bitmaths.bytesToLong(data, 1);
					long clientToServer = receivedServer - Bitmaths.bytesToLong(data, 9);
					long serverToClient = receiveTime - receivedServer;
					long rtt = clientToServer + serverToClient;
					PlayerHandler.Me.setMsPing(rtt / 1000000);
				}
				// Add time received to packet and echo back to server.
				else {
					byte[] echoData = new byte[8];
					System.arraycopy(data, 1, echoData, 0, 8);
					echoData = Bitmaths.pushByteArrayToData(Bitmaths.longToBytes(receiveTime), echoData);
					echoData = Bitmaths.pushByteArrayToData(Bitmaths.intToBytes(16), echoData);
					echoData = Bitmaths.pushByteToData(PacketData.PING.getID(), echoData);
					sendData(new Packet(echoData, PlayerHandler.Me.ID));
				}
				break;

			default:
				return;
			}

		}

		private boolean firstBall = false;

		private void handleUdpData(byte[] data) {
			// Data[0] is packetID and the next 4 bytes (int)
			// is the number of udp packets sent by the server

			byte[] payload = new byte[data.length - Protocol.UDP.getHeaderLength()];
			System.arraycopy(data, 5, payload, 0, data.length - 5);
			PacketData dataPacket = PacketData.getEnumByID(data[0]);

			switch (dataPacket) {
			case NEW_BALLS:
				UdpLink.recievedPacketsUDP.incrementAndGet();

				if (!firstBall) {
					System.out.println("UDP LINK: GOT BALL DATA");
					firstBall = true;
				}

				float[] ballData = new float[payload.length / 4];
				ByteBuffer.wrap(payload).asFloatBuffer().get(ballData);

				int numberOfItems = PacketData.NEW_BALLS.getNumberOfItems();
				int bytesPerBall = PacketData.NEW_BALLS.getMaxPayload() / PacketData.NEW_BALLS.getNumObj();
				int numberOfEntities = payload.length / bytesPerBall;

				// update balls
				var currentBall = new float[numberOfItems];
				for (int i = 0; i < numberOfEntities; i++) {
					int offset = i * numberOfItems;

					// Put all data into currentBall.
					for (var j = 0; j < numberOfItems; j++) {
						currentBall[j] = ballData[offset + j];
					}

					storage.setBallData(currentBall);
				}

				break;
			case CLOCK_SYN:
				UdpLink.recievedPacketsUDP.incrementAndGet();
				break;

			case CLIENT_DATA:
				UdpLink.recievedPacketsUDP.incrementAndGet();

				for (var i = 0; i < payload.length; i += PacketData.CLIENT_DATA.getObjectSize()) {
					var posX = Bitmaths.bytesToFloat(payload, i);
					var posY = Bitmaths.bytesToFloat(payload, i + 4);
					var velX = Bitmaths.bytesToFloat(payload, i + 8);
					var velY = Bitmaths.bytesToFloat(payload, i + 12);
					var radOfInf = Bitmaths.bytesToFloat(payload, i + 16);
					var ID = Bitmaths.bytesToLong(payload, i + 20);

					if (ID == PlayerHandler.Me.ID)
						Display.setDiameterInServerFromRadOfInf(radOfInf);
					players.serverUpdatePlayer(ID, posX, posY, velX, velY, radOfInf);
				}
				break;
			default:
				System.out.println("Packet type not supported to handle: " + data[0] + " UdpLinkHandler class");
				return;
			}
		}

	} // End of Packet Handler class

} // End of Client connection class
