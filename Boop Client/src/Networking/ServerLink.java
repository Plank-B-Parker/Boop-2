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

import Math.Vec2f;
import Mian.Display;
import Mian.main;

public class ServerLink implements Runnable{
	
	Socket socketTCP;
	private int myPort = 0;
	
	public static final int PORT = 23000;
	
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
	}

	@Override
	public void run() {
		readIDfromServer();
		
		while(connected) {
			try {
				// Read data in stream and store in buffer	
				byte[] data = recieveData();
				if (data.length != 0) dataBuffer.add(data);
				
			} catch (IOException e) {
				e.printStackTrace();
				connected = false;
				// close connection method here
			}
			
			handleAllTCPData();
		}
	}
	
	// TCP stream information is set out as (byte PacketID, int length ..... rest of data
	
	private byte[] recieveData() throws IOException {
		int packetID = in.read();
		if (packetID == -1) return new byte[0];
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
				Display.centreInServer.x = convertBytestoFloat(data, 1);
				Display.centreInServer.y = convertBytestoFloat(data, 5);
				Display.screenHeightOnServer = 2f*convertBytestoFloat(data, 9);
				System.out.println("TCP WORKS");
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
	
	public static int convertBytestoInt(byte[] bytes, int start) {
		return  ((int) bytes[start] << 24) + ((int) bytes[start + 1] << 16) + ((int) bytes[start + 2] << 8) + bytes[start + 3];
	}
	
	public static float convertBytestoFloat(byte[] bytes, int start) {
	    int intBits = 
	    		// Bit shifting and 
	    	      bytes[start] << 24 | (bytes[start + 1] & 0xFF) << 16 | (bytes[start + 2] & 0xFF) << 8 | (bytes[start + 3] & 0xFF);
	    return Float.intBitsToFloat(intBits); 
	}

	public int getMyPort() {
		return myPort;
	}
	
}
