package networking;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicInteger;

import math.Bitmaths;

public class UDP implements Runnable{
	
	DatagramSocket socket;
	public static final int MAX_PAYLOAD_SIZE = 1400;
	
	Thread threadUDP;
	
	public AtomicInteger recievedPacketsUDP = new AtomicInteger(0);
	public AtomicInteger sentPacketsUDP = new AtomicInteger(0);
	
	private ClientAccept clientAcceptor;

	public UDP(ClientAccept clientAcceptor) {
		this.clientAcceptor = clientAcceptor;
		
		try {
			this.socket = new DatagramSocket(ClientAccept.PORT);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		threadUDP = new Thread(this, "UDP-thread");
		
	}
	
	@Override
	public void run() {
		while(ClientAccept.serverON) {
			byte[] data = new byte[MAX_PAYLOAD_SIZE];
			
			DatagramPacket packet = new DatagramPacket(data, data.length);
			
			try {
				socket.receive(packet);
				recievedPacketsUDP.incrementAndGet();
				handleData(packet);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("UDP problem");
				break;
			}
		}
		
	}
	
	private void handleData(DatagramPacket packet) {

		Client client = clientAcceptor.getClientByAddressAndPort(packet.getAddress(), packet.getPort());

		if (client == null) {
			System.out.println("UDP packet sent from unknown client: "+packet.getAddress().toString());
			return;
		}

		switch (packet.getData()[0]) {
		case 1:
			byte data = packet.getData()[1];
			boolean pressed = 1 == ((data >> 6) & 1); // Determines whether a key has been pressed or released
			data = (byte) (data & ~(1 << 6)); // Resets the pressed bit to revert to the key number
			client.handleKey(pressed, data);
			break;
		case 10:
			byte[] test = Bitmaths.intToBytes(packet.getPort());
			byte[] test2 = Bitmaths.pushByteToData((byte) 10, test);
			System.out.println(packet.getPort());
			sendData(test2, packet.getAddress(), packet.getPort());
			break;
		default:
			return;
		}
	}
	
	public void sendData(byte[] data, InetAddress ipAddress, int port) {
		DatagramPacket packet = new DatagramPacket(data, data.length, ipAddress, port);
		
		try {
			socket.send(packet);
			sentPacketsUDP.incrementAndGet();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
