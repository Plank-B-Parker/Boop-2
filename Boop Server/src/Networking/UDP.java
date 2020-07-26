package Networking;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicInteger;

public class UDP implements Runnable{
	
	DatagramSocket socket;
	public static final int MAX_PAYLOAD_SIZE = 1400;
	
	Thread threadUDP;
	
	public AtomicInteger recievedPacketsUDP = new AtomicInteger(0);
	public AtomicInteger sentPacketsUDP = new AtomicInteger(0);
	
	public UDP() {
		
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
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			handleData(data);
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
			threadUDP.join();
			socket.close();
		} catch (InterruptedException e) {
			threadUDP.interrupt();
			e.printStackTrace();
		}
	}
}
