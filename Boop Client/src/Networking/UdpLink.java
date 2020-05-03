package Networking;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class UdpLink implements Runnable{
	
	private ServerLink serverlink;
	private int serverport;
	private DatagramSocket socket;
	private InetAddress serveripv4Address;
	public static final int MAX_PAYLOAD_SIZE = 1472;
	
	public Thread threadUDP;
	
	public UdpLink(ServerLink serverlink) {
		this.serverlink = serverlink;
		
		threadUDP = new Thread("UDP-Thread");
	}

	@Override
	public void run() {
		while (serverlink.connected) {
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
			break;
		default:
			return;
		}
	}
	
	public void sendData(byte[] data) {
		DatagramPacket packet = new DatagramPacket(data, data.length, serveripv4Address, serverport);
		
		try {
			socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void connectToServerUDP(int serverport, InetAddress serveripv4Address) {
		this.serveripv4Address = serveripv4Address;
		this.serverport = serverport;
		
		
		try {
			this.socket = new DatagramSocket(this.serverport, this.serveripv4Address);
			
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		threadUDP.start();
	}
}
