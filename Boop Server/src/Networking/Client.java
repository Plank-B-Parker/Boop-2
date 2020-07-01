package Networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

import Math.Bitmaths;
import Math.Vec2f;
import Mian.main;

public class Client implements Runnable{
	public static final byte DISCONNECT_ID = -5;
	
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
	
	
	public Client() {
		clientThread = new Thread(this, "Client-Thread");
		
		dataBuffer = new LinkedBlockingQueue<>(60);
		
		Random random = new Random();
		
		centrePos.set(-1f, -0.25f);
		
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
		int packetID = in.read();
		int len = in.readInt();
		
		byte[] data = new byte[len + 1];
		data[0] = (byte) packetID;
		
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
		
		connected = true;
		clientThread.start();
	}
	
	public void disconnect(){
		if (!myClientSocket.isClosed() && connected) {
			connected = false;
			byte[] disconnect = {DISCONNECT_ID};
			try {
				out.write(disconnect);
				myClientSocket.close();
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
