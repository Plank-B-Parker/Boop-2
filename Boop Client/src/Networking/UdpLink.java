package Networking;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import Mian.main;

public class UdpLink implements Runnable{
	
	private int port;
	private DatagramSocket socket;
	private InetAddress ipv4Address;
	volatile boolean connected = false;
	public static final int MAX_PAYLOAD_SIZE = 1400;
	
	//Contains all the packets sent from server before they are processed.
	private ArrayList<byte[]> updateQueue = new ArrayList<>();
	
	public Thread threadUDP;
	main main;
	
	public UdpLink(main main) {
		this.main = main;
		
		threadUDP = new Thread(this, "UDP-Thread");
		threadUDP.setDaemon(true);
	}

	
	@Override
	public void run() {
		while (connected) {
			byte[] data = new byte[MAX_PAYLOAD_SIZE];
			
			DatagramPacket packet = new DatagramPacket(data, data.length);
			
			try {
				socket.receive(packet);
				handleData(packet.getData());
				//updateQueue.add(packet.getData());
			} catch (IOException e) {
				e.printStackTrace();
				closeConnection();
			}
			
			
		}
		
	}
	
	public void processServerUpdate() {
		if (! connected) return;
		
		int numPackets = updateQueue.size();
		
		byte[][] updates = new byte[numPackets][];
		updateQueue.toArray(updates);
		updateQueue = new ArrayList<>();
		
		for(int i = 0; i < numPackets; i++) {
			byte[] data = updates[i];
			handleData(data);
		}
	}
	
	
	private void handleData(byte[] data) {
		switch (data[0]) {
		case 2:
			byte[] newData = Arrays.copyOfRange(data, 1, data.length);
			
			float[] ballData = new float[newData.length / 4];
			ByteBuffer.wrap(newData).asFloatBuffer().get(ballData);
			
			int numberOfItems = 7;
			int bytesPerBall = 28;
			int numberOfEntities = newData.length / bytesPerBall;
			
			// update balls
			float currentBall[] = new float[numberOfItems];
			for (int i = 0; i < numberOfEntities; i++) {
				int offset = i * numberOfItems;
				
				//Put all data into currentBall.
				for(int j = 0; j < numberOfItems; j++) {
						currentBall[j] = ballData[offset + j];
					}
				
				main.balls.setBallData(currentBall);
			}
			
			break;
		default:
			return;
		}
	}
	
	public void sendData(byte[] data) {
		DatagramPacket packet = new DatagramPacket(data, data.length, ipv4Address, port);
		
		try {
			socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void connectToServerUDP(InetAddress ipv4Address, int localPort) {
		this.ipv4Address = ipv4Address;
		
		try {
			this.socket = new DatagramSocket(localPort);
			
			System.out.println("My port: " + socket.getLocalPort());
			
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		this.connected = true;
		threadUDP.start();
	}
	
	private void closeConnection() {
		this.connected = false;
		
		if (! socket.isClosed()) {
			socket.close();
			System.out.println("closed UDP Socket");
		} 
	}
	
	public void stopRunningUDP() {
		if (! connected && ! threadUDP.isAlive()) return;
		
		connected = false;
		try {
			closeConnection();
			System.out.println("Thread UDP joining");
			threadUDP.join();
			System.out.println("ThreadUDP has been killed");
		} catch (InterruptedException e) {
			e.printStackTrace();
			threadUDP.interrupt();
		}
	}
}
