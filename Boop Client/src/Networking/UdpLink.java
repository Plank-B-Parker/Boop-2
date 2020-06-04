package Networking;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import Balls.Ball;
import Mian.main;

public class UdpLink implements Runnable{
	
	private ServerLink serverLink;
	private int port;
	private DatagramSocket socket;
	private InetAddress ipv4Address;
	public static final int MAX_PAYLOAD_SIZE = 1400;
	
	public Thread threadUDP;
	main main;
	
	public UdpLink(ServerLink serverLink, main main) {
		this.serverLink = serverLink;
		this.main = main;
		
		threadUDP = new Thread(this, "UDP-Thread");
	}

	@Override
	public void run() {
		while (serverLink.connected) {
			byte[] data = new byte[MAX_PAYLOAD_SIZE];
			
			DatagramPacket packet = new DatagramPacket(data, data.length);
			
			try {
				socket.receive(packet);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			handleData(packet.getData());
			
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
			int numberOfEntities = data.length / bytesPerBall;
			
			// update balls
			for (int i = 0; i < numberOfEntities; i++) {
				int offset = i * numberOfItems;
				main.balls.setBallData((int) ballData[offset],(int) ballData[offset + 1], ballData[offset + 2], ballData[offset + 3],
						ballData[offset + 4], ballData[offset + 5],(int) ballData[offset + 6]);
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

	public void connectToServerUDP(InetAddress ipv4Address) {
		this.ipv4Address = ipv4Address;
		
		try {
			this.socket = new DatagramSocket();
			
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		threadUDP.start();
	}
}
