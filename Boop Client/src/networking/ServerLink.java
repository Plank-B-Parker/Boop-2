package networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

import main.Main;
import main.PlayerHandler;
import math.Bitmaths;
import math.Vec2f;
import display.Display;

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
		
		dataBuffer = new LinkedBlockingQueue<>(60);
		
		threadTCP = new Thread(this, "TCP-Thread");
		threadTCP.setDaemon(true);
	}

	@Override
	public void run() {
		readIDfromServer();
		
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
		
		float len = in.readFloat();
		System.out.println("Payload length: " + len);
		
		byte[] data = new byte[(int)len + 1];
		data[0] = packetID;
		
		byte[] payload = in.readNBytes((int)len);
		
		for (int i = 0; i < len; i++) {
			data[i + 1] = payload[i];
		}
		
		return data;
	}
	
	public void sendData(byte[] data) throws IOException {
		out.write(data);
	}
	
	// Handles all available data in the buffer
	public void handleAllTCPData() {
		if (dataBuffer.isEmpty()) {return;}
		
		// Handles each type of packet. (Plan to add methods for specific data handling)
		for (byte[] data: dataBuffer) {
			switch (data[0]) {
			case 70:
				Display.diameterInServer = 2f*Bitmaths.bytesToFloat(data, 9);
				
				PlayerHandler.Me.centrePos.x = Bitmaths.bytesToFloat(data, 1);
				PlayerHandler.Me.centrePos.y = Bitmaths.bytesToFloat(data, 5);	
				
//				System.out.println(PlayerHandler.Me.centrePos.x + ", " + PlayerHandler.Me.centrePos.y);
				System.out.println("Hello");
				
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
