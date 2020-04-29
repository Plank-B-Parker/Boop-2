package Networking;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class UdpLink implements Runnable{
	
	private ServerLink serverlink;
	private final int PORT;
	
	
	private DatagramSocket socket;
	private InetAddress ipv4Address;
	
	public static final int MAX_PAYLOAD_SIZE = 1472;
	
	public UdpLink(ServerLink serverlink, int port, InetAddress ipv4Address) {
		this.serverlink = serverlink;
		this.PORT = port;
		this.ipv4Address = ipv4Address;
		
		
		try {
			this.socket = new DatagramSocket(PORT, this.ipv4Address);
			
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		Thread threadUDP = new Thread("UDP-Thread");
		threadUDP.start();
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

}
