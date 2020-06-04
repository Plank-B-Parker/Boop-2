package Networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

import Math.Vec2f;

public class Client implements Runnable{
	
	private Socket myClientSocket;
	
	DataInputStream in;
	DataOutputStream out;
	
	private long ID = 0;
	private InetAddress ipv4Address;
	private InetAddress ipv6Address;
	
	private int clientPort;
	
	public LinkedBlockingQueue<byte[]> dataBuffer;
	
	private volatile boolean connected = false;
	
	Thread clientThread;
	
	public Vec2f topLeftCorner = new Vec2f();
	public float width = 0.4f;
	public float height = 0.225f;
	public Vec2f botRightCorner = new Vec2f();
	
	
	public Client() {
		clientThread = new Thread(this, "Client-Thread");
		
		dataBuffer = new LinkedBlockingQueue<>(60);
		
		Random random = new Random();
		
		topLeftCorner.set(random.nextFloat() - width, random.nextFloat() - height);
		botRightCorner.set(topLeftCorner.x + width, topLeftCorner.y + height);
		
	}
	
	@Override
	public void run() {
		while(connected && ClientAccept.serverON) {
			// Read data in stream and store in buffer
			try {
				dataBuffer.add(recieveData());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public byte[] recieveData() throws IOException{
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
		
		connected = true;
		clientThread.start();
	}
	
	public void disconnect() throws IOException{
		if (!myClientSocket.isClosed()) {
			connected = false;
			myClientSocket.close();
			try {
				clientThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
				clientThread.interrupt();
			}

		}
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
}
