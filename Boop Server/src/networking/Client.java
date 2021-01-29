package networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import jdk.jfr.Unsigned;
import main.Main;
import math.Bitmaths;
import math.Vec2f;

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
	
	public Vec2f centrePos = new Vec2f(); 	//centre of screen of client.
	public float radOfInf =  0.5f;        	//radius of region balls are sent to client.
	
	private long lastTime = 0;					//Last time when balls were sent;
	private float timeBetweenUpdates = 1f;	//Time between the balls being sent;
	private float delayUntilFirstSend = 3500;
	
	
	public AtomicInteger udpPacketsSent = new AtomicInteger(0);
	public AtomicInteger udpPacketsRecieved = new AtomicInteger(0);
	
	public Client() {
		clientThread = new Thread(this, "Client-Thread");
		
		dataBuffer = new LinkedBlockingQueue<>(60);
		
		Random random = new Random();
		
		centrePos.set(0f, 0f);
		
	}
	
	@Override
	public void run() {
		while(connected && ClientAccept.serverON) {
			// Read data in stream and store in buffer
			try {
				dataBuffer.add(recieveData());
			} catch (IOException e) {
				e.printStackTrace();
				disconnect();
			}
		}
	}
	
	public byte[] recieveData() throws IOException{
		byte packetID = in.readByte();
		if (packetID == Packet.DISCONNECT.getID()) throw new IOException();
		
		int len = in.readInt();
		
		byte[] data = new byte[len + 1];
		data[0] = packetID;
		
		byte[] payload = in.readNBytes(len);
		
		for (int i = 0; i < len; i++) {
			data[i + 1] = payload[i];
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
		
		float[] clientPosData = new float[4];
		clientPosData[1] = centrePos.x;
		clientPosData[2] = centrePos.y;
		clientPosData[3] = radOfInf;
		clientPosData[0] = (float) 3 * 4; // 3 floats * 4 bytes = 12 byte payload (length)
		
		byte[] clientPos = Bitmaths.floatArrayToBytes(clientPosData);
		clientPos = Bitmaths.pushByteToData((byte) 70, clientPos);
		
		
		out.write(clientPos);
		
		ipv4Address = socket.getInetAddress();
		clientPort = socket.getPort();
		
		System.out.println("Client ID: " + ID);
		System.out.println("Client port: " + clientPort);
		System.out.println("Client ip: " + ipv4Address);
		
		connected = true;
		clientThread.start();
	}
	
	public void disconnect(){
		if (connected) {
			connected = false;
			try {
				if (!myClientSocket.isClosed()) {
					byte[] disconnect = {Packet.DISCONNECT.getID()};
					out.write(disconnect);
					myClientSocket.close();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			System.out.println("Client " + ID + " has Disconnected");
			try {
				clientThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
				clientThread.interrupt();
			}

		}
	}
	
	/**
	 * Checks if client is ready for balls to be sent.
	 * @return
	 */
	public boolean isReadyForUpdate() {
		long currentTime = System.currentTimeMillis();
		long dt = currentTime - lastTime;
		if (lastTime == 0) {
			lastTime = currentTime;
			dt = currentTime - lastTime;
		}
		if(dt > timeBetweenUpdates*1000 && delayUntilFirstSend <= 0) {
			lastTime = currentTime;
			return true;
		}
		delayUntilFirstSend -= dt;
		return false;
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
	
	public boolean isConnected() {
		return connected;
	}
}
