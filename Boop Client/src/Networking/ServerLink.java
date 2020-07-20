package Networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

import Math.Bitmaths;
import Math.Vec2f;
import Mian.Display;
import Mian.main;

public class ServerLink implements Runnable{
	
	Socket socketTCP;
	private int myPort = 0;
	
	public static final int PORT = 2300;
	public static final byte DISCONNECT_ID = -5;
	
	public long ID = -1;
	
	volatile boolean connected = false;
	
	DataInputStream in;
	DataOutputStream out;
	
	Thread threadTCP;
	
	public LinkedBlockingQueue<byte[]> dataBuffer;
	
	main main;
	
	public ServerLink(main main){
		this.main = main;
		
		dataBuffer = new LinkedBlockingQueue<>(60);
		
		threadTCP = new Thread(this, "TCP-Thread");
		threadTCP.setDaemon(true);
	}

	@Override
	public void run() {
		readIDfromServer();
		
		while(connected) {
			try {
				// Read data in stream and store in buffer	
				byte[] data = recieveData();
				if (data.length != 0) dataBuffer.add(data);
				handleAllTCPData();
				
			} catch (IOException e) {
				e.printStackTrace();
				closeConnection();
				main.disconnectedByServer = true;
			}
			
		}
	}
	
	// TCP stream information is set out as (byte PacketID, int length ..... rest of data
	
	private byte[] recieveData() throws IOException {
		int packetID = in.read();
		if (packetID == -1) return new byte[0];
		if (packetID == DISCONNECT_ID) throw new IOException();
		float len = in.readFloat();
		System.out.println("Payload length: " + len);
		
		byte[] data = new byte[(int)len + 1];
		data[0] = (byte) packetID;
		
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
				Display.centreInServer.x = Bitmaths.bytesToFloat(data, 1);
				Display.centreInServer.y = Bitmaths.bytesToFloat(data, 5);
				Display.screenHeightOnServer = 2f*Bitmaths.bytesToFloat(data, 9);
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
			System.out.println("ID: " + ID);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void connectToServer(InetAddress ipV4Address) throws IOException{
		socketTCP = new Socket(ipV4Address, PORT);
		
		in = new DataInputStream(socketTCP.getInputStream());
		out = new DataOutputStream(socketTCP.getOutputStream());
		
		myPort = socketTCP.getLocalPort();
		
		connected = true;
		threadTCP.start();
	}
	
	private void closeConnection(){
		connected = false;
		try {
			if (! socketTCP.isClosed()) {
				socketTCP.close();
				System.out.println("Closed TCP socket");
			} 
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void stopRunningTCP() {
		if (! connected && ! threadTCP.isAlive()) return;
		
		connected = false;
		try {
			closeConnection();
			System.out.println("ThreadTCP Joining");
			threadTCP.join();
			System.out.println("ThreadTCP has been killed");
		} catch (InterruptedException e) {
			e.printStackTrace();
			threadTCP.interrupt();
		}
	}

	public int getMyPort() {
		return myPort;
	}
	
}
