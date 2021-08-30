package networking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import main.PlayerHandler;

/**
 * Deals with the receiving and sending of packets using the UDP protocol.
 * <p>
 * Packets received are put into a linked blocking queue for the handler class
 * to deal with.
 */
public class UdpLink implements Runnable {

	private DatagramChannel channel;
	private InetSocketAddress serverAddress;
	private InetSocketAddress localUdpAddress;

	private BlockingQueue<Packet> inboundPackets;
	private BlockingQueue<Packet> outboundPackets;

	// Buffers can hold a minimum of 4 max sized packets at once
	private ByteBuffer readBuffer = ByteBuffer.allocate(PacketData.MAX_PAYLOAD_SIZE * 4);
	private ByteBuffer writeBuffer = ByteBuffer.allocate(PacketData.MAX_PAYLOAD_SIZE * 4);

	Client client;
	PlayerHandler players;

	UdpLink(Client client, PlayerHandler players) {
		this.client = client;
		this.players = players;

		inboundPackets = new LinkedBlockingQueue<>(40);
		outboundPackets = new LinkedBlockingQueue<>(40);
	}

	public static final AtomicInteger recievedPacketsUDP = new AtomicInteger(0);
	public static final AtomicInteger sentPacketsUDP = new AtomicInteger(0);

	@Override
	public void run() {
		while (client.serverConnected) {
			try {
				readDatagramPackets();
				writeDatagramPackets();

			} catch (IOException e) {
				e.printStackTrace();
				if (e.getClass() == SocketTimeoutException.class) {
					System.out.println("UDP Socket Timeout");
				} else {
					break;
				}
			}

		}

	}

	private void readDatagramPackets() throws IOException {
		SocketAddress sourceAddress = channel.receive(readBuffer);

		while (sourceAddress != null) {
			recievedPacketsUDP.incrementAndGet();

			Packet packet = new Packet();
			readBuffer.flip();
			packet.writeToUdpPacket(readBuffer, sourceAddress);
			
			client.submitPacketToHandler(packet);
			
			/*boolean offered = inboundPackets.offer(packet);
			if (!offered) {
				System.out.println("Failed to offer packet to inboundUdpQueue");
				System.out.println("Abandoning packet");
			}*/

			readBuffer.clear();
			sourceAddress = channel.receive(readBuffer);
		}
	}

	private void writeDatagramPackets() throws IOException {
		Packet packet = outboundPackets.poll();

		while (packet != null) {
			writePacketToBuffer(packet, writeBuffer);
			writeBuffer.flip();

			channel.send(writeBuffer, serverAddress);

			writeBuffer.clear();
			packet = outboundPackets.poll();
		}
	}

	private void writePacketToBuffer(Packet packet, ByteBuffer byteBuffer) {
		byteBuffer.put(packet.payload);
	}

	public void connectToServerUDP(InetSocketAddress serverAddress, int localPort) throws IOException {
		this.serverAddress = serverAddress;
		localUdpAddress = new InetSocketAddress(localPort);

		channel = DatagramChannel.open();
		channel.configureBlocking(false);
		channel.socket().bind(localUdpAddress);

		System.out.println("My UDP port: " + localUdpAddress.getPort());
		System.out.println("My Local IP: " + localUdpAddress.getAddress());
	}

	public void closeConnection() throws IOException {
		channel.close();
		inboundPackets.clear();
		outboundPackets.clear();
		System.out.println("closed UDP Socket");
	}

	public BlockingQueue<Packet> getCompletePackets() {
		return inboundPackets;
	}

	public boolean offerOutboundPacket(Packet packet) {
		return outboundPackets.offer(packet);
	}
}
