package Networking;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import Math.Bitmaths;
import Mian.main;

public class UdpLink implements Runnable{
	
	private DatagramSocket socket;
	private InetAddress serverIP;
	volatile boolean connected = false;
	public static final int MAX_PAYLOAD_SIZE = 1400;
	
	//Contains all the packets sent from server before they are processed.
	private ArrayList<byte[]> updateQueue = new ArrayList<>();
	
	private Thread threadUDP;
	main main;
	
	public UdpLink(main main) {
		this.main = main;
		
		threadUDP = new Thread(this, "UDP-Thread");
		threadUDP.setDaemon(true);
	}
	
	public AtomicInteger recievedPacketsUDP = new AtomicInteger(0);
	public AtomicInteger sentPacketsUDP = new AtomicInteger(0);
	
	@Override
	public void run() {
		while (connected) {
			byte[] data = new byte[MAX_PAYLOAD_SIZE];
			
			DatagramPacket packet = new DatagramPacket(data, data.length);
			try {
				socket.receive(packet);
				recievedPacketsUDP.incrementAndGet();
				handleData(packet.getData());
				//updateQueue.add(packet.getData());
			} catch (IOException e) {
				e.printStackTrace();
				if (e.getClass() == SocketTimeoutException.class) {
					System.out.println("UDP Socket Timeout");
				}
				else {closeConnection();}
				
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
			this.socket = new DatagramSocket(localPort);
			this.socket.setSoTimeout(5000);
			
			System.out.println("My UDP port: " + socket.getLocalPort());
			System.out.println("My Local IP: " + InetAddress.getLocalHost());
			
			byte[] test = Bitmaths.intToBytes(localPort);
			byte[] test2 = Bitmaths.pushByteToData((byte) 2, test);
			
			sendData(test2);
			sendData(test2);
			
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
		
		if (! socket.isClosed()) {
			socket.close();
			System.out.println("closed UDP Socket");
		} 
	}
	
	public void stopRunningUDP() {
		if (! connected && ! threadUDP.isAlive()) return;
		
		// Kills thread if not already dead
		connected = false;
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
}
