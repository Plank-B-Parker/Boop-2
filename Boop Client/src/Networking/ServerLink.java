package Networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerLink implements Runnable{
	
	Socket socketTCP;
	
	public long ID;
	
	public volatile boolean connected = false;
	
	DataInputStream in;
	DataOutputStream out;
	
	public LinkedBlockingQueue<byte[]> dataBuffer;
	
	public ServerLink(){
		
		dataBuffer = new LinkedBlockingQueue<>(60);
		
		Thread threadTCP = new Thread("TCP-Thread");
		threadTCP.start();
	}

	@Override
	public void run() {
		while(connected) {
			try {
				// Read data in stream and store in buffer			
				dataBuffer.add(recieveData());
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	// TCP stream information is set out as (byte PacketID, int length ..... rest of data
	
	public byte[] recieveData() throws IOException {
		int packetID = in.read();
		int len = in.readInt();
		
		byte[] data = new byte[len + 1];
		data[0] = (byte) packetID;
		
		byte[] payload = in.readNBytes(len);
		
		for (int i = 0; i < len; i++) {
			data[i + 1] = payload[0];
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
			case 5:
				ID = data[2];
				break;
			default:
				return;
			}
		}
		
	}
	
	public void connectToServer(String ipV4Address, int port) throws IOException{
		socketTCP = new Socket(ipV4Address, port);
		
		in = new DataInputStream(socketTCP.getInputStream());
		out = new DataOutputStream(socketTCP.getOutputStream());
		
		connected = true;
	}
	
	public static int convertBytestoInt(byte[] bytes) {
		return  ((int) bytes[0] << 24) + ((int) bytes[1] << 16) + ((int) bytes[2] << 8) + bytes[3];
	}
	
}
