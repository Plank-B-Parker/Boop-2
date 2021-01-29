package networking;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import math.Bitmaths;

public class UDP implements Runnable{
	
	DatagramSocket socket;

	Thread threadUDP;
	
	ArrayList<InetAddress> confirmedIPs;
	ArrayList<InetAddress> problemIPs;
	ArrayList<InetAddress> maliciousIPs;
	
	public AtomicInteger RecievedPacketsUDP = new AtomicInteger(0);
	public AtomicInteger SentPacketsUDP = new AtomicInteger(0);
	
	private ClientAccept clientAccept;
	
	public UDP(ClientAccept clientAccept) {
		try {
			this.socket = new DatagramSocket(ClientAccept.PORT);
			this.clientAccept = clientAccept;
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		threadUDP = new Thread(this, "UDP-thread");
		
	}
	
	@Override
	public void run() {
		while(ClientAccept.serverON) {
			byte[] data = new byte[Packet.MAX_PAYLOAD_SIZE];
			
			DatagramPacket packet = new DatagramPacket(data, data.length);
			
			
			try {
				socket.receive(packet);
				RecievedPacketsUDP.incrementAndGet();
				handleData(packet);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("UDP problem");
				break;
			}
		}
		
	}
	
	private void handleData(DatagramPacket packet) {
		switch (packet.getData()[0]) {
		case 10:
			
			Client client = clientAccept.getClientbyIPAddress(packet.getAddress());
			
			byte[] test = Bitmaths.intToBytes(packet.getPort());
			test = Bitmaths.pushByteToData((byte) 10, test);
			test = Bitmaths.pushByteArrayToData(Bitmaths.intToBytes(client.udpPacketsSent.incrementAndGet()), test);
			
			System.out.println(packet.getPort());
			sendData(test, packet.getAddress(), packet.getPort());
			break;
		default:
			return;
		}
	}
	
	public void sendData(byte[] data, InetAddress ipAddress, int port) {
		DatagramPacket packet = new DatagramPacket(data, data.length, ipAddress, port);
		packet.getAddress(); // check if packet is actually from a client
		try {
			socket.send(packet);
			SentPacketsUDP.incrementAndGet();
		} catch (IOException e) {
			e.printStackTrace();
		}
		int sequence = Bitmaths.bytesToInt(data);
		System.out.println("Packet sequence: " + Bitmaths.bytesToInt(data));
		System.out.println("Total packets sent: " + SentPacketsUDP.get());
	}
	
	public void startUDP() {
		threadUDP.start();
	}
	
	public void disconnect(){
		try {
			socket.close();
			threadUDP.join();
		} catch (InterruptedException e) {
			threadUDP.interrupt();
			e.printStackTrace();
		}
	}
}
