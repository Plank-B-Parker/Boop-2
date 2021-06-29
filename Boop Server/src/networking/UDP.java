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
	
	
	private ClientAccept clientAcceptor;
	private ClientHandler clientHanlder;

	public UDP(ClientAccept clientAcceptor) {
		this.clientAcceptor = clientAcceptor;
		clientHanlder = clientAcceptor.clientHandler;
		
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

		Client client = clientHanlder.getClientByAddressAndPort(packet.getAddress(), packet.getPort());

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
		case 5:
			float directionY = Bitmaths.bytesToFloat(packet.getData(),1);
			float directionX = Bitmaths.bytesToFloat(packet.getData(),5);
			
//			System.out.println("(" + client.centrePos.x + ", " + client.centrePos.y + ")");
			//System.out.println("(" +  directionX + ", " +  directionY + ")");
			
			client.setDirection(directionX, directionY);
			break;
		case 10:
			
			
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
		
		try {
			socket.send(packet);
			SentPacketsUDP.incrementAndGet();
		} catch (IOException e) {
			e.printStackTrace();
		}
		int sequence = Bitmaths.bytesToInt(data);
		//System.out.println("Packet sequence: " + Bitmaths.bytesToInt(data));
		//System.out.println("Total packets sent: " + SentPacketsUDP.get());
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
