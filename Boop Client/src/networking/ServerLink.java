package networking;

import java.awt.Color;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

import main.Main;
import main.Player;
import main.PlayerHandler;
import math.Bitmaths;
import math.Vec2f;

public class ServerLink implements Runnable{
	
	Socket socketTCP;
	private int myPort = 0;
	
	public static final int SERVER_PORT = 2300;
	
	public long ID = -1;
	
	volatile boolean serverConnection = false;
	
	DataInputStream in;
	DataOutputStream out;
	
	Thread threadTCP;
	
	public LinkedBlockingQueue<byte[]> dataBuffer;
	
	Main main;
	
	public ServerLink(Main main){
		this.main = main;
		
		dataBuffer = new LinkedBlockingQueue<>(30);
		
		threadTCP = new Thread(this, "TCP-Thread");
		threadTCP.setDaemon(true);
	}

	@Override
	public void run() {
		readIDfromServer();
		sendMyData();
		
		while(serverConnection) {
			try {
				// Read data in stream and store in buffer	
				byte[] data = recieveData();
				if (data.length != 0) dataBuffer.add(data);
				handleAllTCPData();
				
			} catch (IOException e) {
				e.printStackTrace();
				main.disconnectedByServer = true;
				closeConnection();
			}
			
		}
	}
	
	// TCP stream information is set out as (byte PacketID, int length ..... rest of data
	
	private byte[] recieveData() throws IOException {
		byte packetID = in.readByte();
		if (packetID == -1) return new byte[0];
		
		if (packetID == Packet.DISCONNECT.getID()) {
			main.disconnectedByServer = true;
			return new byte[0];
		}
		
		var len = in.readInt();
		
		var packet = Packet.getPacketByID(packetID);
		if (packet == null || len > packet.getMaxPayload() || len < 0) return new byte[0];
		
		byte[] data = new byte[len + 1];
		data[0] = packetID;
		
		byte[] payload = in.readNBytes(len);
		
		System.arraycopy(payload, 0, data, 1, len);
		
		return data;
	}
	
	public void sendData(byte[] data) throws IOException {
		out.write(data);
	}
	
	// Handles all available data in the buffer
	public void handleAllTCPData() throws IOException {
		
		// Handles each type of packet. (Plan to add methods for specific data handling)
		while (!dataBuffer.isEmpty()) {
			byte[] data = dataBuffer.poll();
			
			switch (data[0]) {
			case 70:
				//Last thing sent.
				//Other Players info.
				var ID = Long.parseLong(Bitmaths.bytesToString(data, 1, 4));
				
				var nameLength = Integer.valueOf(Bitmaths.bytesToString(data,  1 + 4, 2)) - 10;
				String name = Bitmaths.bytesToString(data, 1 + 4 + 2, nameLength);
				
				var joining = (Bitmaths.bytesToString(data, 1 + 4 + 2 + nameLength, 1).equals("1"));
				
				var colourLength = data.length - ( 1 + 4 + 2 + nameLength + 1);
				var colour = Integer.valueOf(Bitmaths.bytesToString(data, 1 + 4 + 2 + nameLength + 1, colourLength)) - (((255 << 8) + 255) << 8) + 255 + 1;
				
				
				if(joining) main.players.addPlayer(new Player(false, ID, name, new Color(colour), new Vec2f()));
				else main.players.removePlayer(ID);
				
				break;
				
			case 5:
				var receiveTime = System.nanoTime();
				
				// Client is the sender when the packet contains both client and server time.
				var isClientSender = data.length == Packet.PING.getObjectSize() + 1;
				
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
					echoData = Bitmaths.pushByteToData(Packet.PING.getID(), echoData);
					out.write(echoData);
				}
				break;
				
			default:
				return;
			}
		}
		
	}
	
	private void readIDfromServer() {
		try {
			Thread.sleep(1000);
			ID = in.readLong();
			
			PlayerHandler.Me.ID = ID;
			
			System.out.println("ID: " + ID);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void sendMyData() {
		var nameLength = Integer.toString(PlayerHandler.Me.name.length() + 10);
		var name = PlayerHandler.Me.name;
		var colour = Integer.toString(PlayerHandler.Me.colour.getRGB());
		
		int payload = 2 + name.length() + colour.length();
		
		String[] myData = {nameLength, name, colour};
		
		byte[] myDataBytes = Bitmaths.stringArrayToBytes(myData);
		myDataBytes = Bitmaths.pushByteArrayToData(Bitmaths.intToBytes(payload), myDataBytes);
		myDataBytes = Bitmaths.pushByteToData((byte) 70, myDataBytes);
		
		try {
			sendData(myDataBytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void connectToServer(InetAddress serverIP) throws IOException{
		socketTCP = new Socket(serverIP, SERVER_PORT);
		
		in = new DataInputStream(socketTCP.getInputStream());
		out = new DataOutputStream(socketTCP.getOutputStream());
		
		myPort = socketTCP.getLocalPort();
		System.out.println("my TCP port: " + myPort);
		
		serverConnection = true;
		threadTCP.start();
	}
	
	private void closeConnection(){
		
		try {
			serverConnection = false;
			
			socketTCP.close();
			System.out.println("Closed TCP socket");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void stopRunningTCP() {
		if (! serverConnection && ! threadTCP.isAlive()) return;
		
		// Kills thread if not already dead
		try {
			closeConnection();
			System.out.println("ThreadTCP Joining");
			// Wait for thread to die
			threadTCP.join();
			// Death confirmed
			System.out.println("ThreadTCP has been killed");
		} catch (InterruptedException e) {
			e.printStackTrace();
			threadTCP.interrupt();
		}
	}

	public int getMyPort() {
		return myPort;
	}
	
	public boolean isSocketClosed() {
		return ((socketTCP != null) && socketTCP.isClosed());
	}
	
	public boolean getServerConnection() {
		return serverConnection;
	}
	
}
