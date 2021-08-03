package networking;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import display.Display;
import main.Main;
import main.PlayerHandler;
import math.Bitmaths;

/**
 * Deals with the receiving and sending of packets using the UDP protocol.
 * <p>
 * Packets received are put into a linked blocking queue for the handler class to deal
 * with.
 */
public class UdpLink implements Runnable{
	
	private DatagramSocket socket;
	private InetAddress serverIP;
	volatile boolean connected = false;
	private BlockingQueue<DatagramPacket> packets;
	
	private Thread threadUDP;
	Main main;
	
	public UdpLink(Main main) {
		this.main = main;
		packets = new LinkedBlockingQueue<>(60);
		
		threadUDP = new Thread(this, "Udp-Link");
		threadUDP.setDaemon(true);
	}
	
	public static final AtomicInteger recievedPacketsUDP = new AtomicInteger(0);
	public static final AtomicInteger sentPacketsUDP = new AtomicInteger(0);
	
	@Override
	public void run() {
		while (connected) {
			byte[] data = new byte[PacketData.MAX_PAYLOAD_SIZE];
			
			DatagramPacket packet = new DatagramPacket(data, data.length);
			try {
				socket.receive(packet);
				boolean enqueued = packets.offer(packet, 2, TimeUnit.MILLISECONDS);
				
				if (!enqueued) {
					System.out.println("No space to enqueue udp packet: UdpIO class run()");
				}
				
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
				Thread.currentThread().interrupt();
				if (e.getClass() == SocketTimeoutException.class) {
					System.out.println("UDP Socket Timeout");
				}
				else {
					closeConnection();
				}
				break;
			}
			
			
		}
		
	}
	
	public void sendData(byte[] data) {
		DatagramPacket packet = new DatagramPacket(data, data.length, serverIP, ServerLink.SERVER_PORT);
		
		try {
			socket.send(packet);
			sentPacketsUDP.incrementAndGet();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void connectToServerUDP(InetAddress serverIPaddress, int localPort) {
		this.serverIP = serverIPaddress;
		
		try {
			socket = new DatagramSocket(localPort);
			socket.setSoTimeout(5000);
			
			System.out.println("My UDP port: " + socket.getLocalPort());
			System.out.println("My Local IP: " + InetAddress.getLocalHost());
			
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.connected = true;
		threadUDP.start();
	}
	
	private void closeConnection() {
		this.connected = false;
		
		socket.close();
		System.out.println("closed UDP Socket");
	}
	
	public void stopRunningUDP() {
		if (! connected && ! threadUDP.isAlive()) return;
		
		// Kills thread if not already dead
		try {
			closeConnection();
			System.out.println("Thread UDP joining");
			// Wait for thread to die
			threadUDP.join();
			// Death confirmed
			System.out.println("ThreadUDP has been killed");
		} catch (InterruptedException e) {
			e.printStackTrace();
			threadUDP.interrupt();
		}
	}
	
	public boolean isSocketClosed() {
		return ((socket != null) && socket.isClosed());
	}
	
	public boolean isConnected() {
		return connected;
	}
	
	public class UdpLinkHandler implements Runnable{
		
		private Thread thread;
		
		public UdpLinkHandler() {
			thread = new Thread(this, "UdpLink-Handler");
			thread.setDaemon(true);
		}
		
		@Override
		public void run() {
			while (connected) {
				try {
					var packet = packets.poll(2, TimeUnit.MILLISECONDS);
					
					if (packet == null) continue; // TODO adjust polling time to help branch predictor
					
					handleData(packet);
				} catch (InterruptedException e) {
					e.printStackTrace();
					Thread.currentThread().interrupt();
				}
			}
		}
		
		private boolean firstBall = false;
		private void handleData(DatagramPacket packet) {
			// Data[0] is packetID and the next 4 bytes (int)
			// is the number of udp packets sent by the server
			
			byte[] payload = new byte[packet.getData().length - 5];
			System.arraycopy(packet.getData(), 5, payload, 0, packet.getLength() - 5);
			PacketData dataPacket = PacketData.getEnumByID(packet.getData()[0]);
			
			switch (dataPacket) {
			case NEW_BALLS:
				recievedPacketsUDP.incrementAndGet();
				
				if(!firstBall) {
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
					
					//Put all data into currentBall.
					for(var j = 0; j < numberOfItems; j++) {
							currentBall[j] = ballData[offset + j];
						}
					
					main.storage.setBallData(currentBall);
				}
				
				break;
			case CLOCK_SYN:
				recievedPacketsUDP.incrementAndGet();
				break;
				
			case CLIENT_DATA:
				recievedPacketsUDP.incrementAndGet();
				
				for(var i = 0; i < payload.length; i += PacketData.CLIENT_DATA.getObjectSize()) {
					var posX = Bitmaths.bytesToFloat(payload, i);
					var posY = Bitmaths.bytesToFloat(payload, i + 4);
					var velX = Bitmaths.bytesToFloat(payload, i + 8);
					var velY = Bitmaths.bytesToFloat(payload, i + 12);
					var radOfInf = Bitmaths.bytesToFloat(payload, i + 16);
					var ID = Bitmaths.bytesToLong(payload, i + 20);
			
					if(ID == PlayerHandler.Me.ID)
						Display.setDiameterInServerFromRadOfInf(radOfInf);
					main.players.serverUpdatePlayer(ID, posX, posY, velX, velY, radOfInf);
				}
				break;
			default:
				System.out.println("Packet type not supported to handle: " + packet.getData()[0] + " UdpLinkHandler class");
				return;
			}
		}
		
		public void startUdpLinkHandler() {
			thread.start();
		}
		
		public void stopUdpLinkHandler() {
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
				Thread.currentThread().interrupt();
			}
		}
		
	} // End of UdpLinkHandler
	
} // End of UdpLink class
