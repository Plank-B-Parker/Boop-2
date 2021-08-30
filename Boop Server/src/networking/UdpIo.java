package networking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Deals with the receiving and sending of packets using the UDP protocol.
 * <p>
 * Packets received are put into a linked blocking queue for the handler class
 * to deal with.
 */
public class UdpIo implements Runnable {

	private ClientHandler clientHandler;
	private Server server;
	private DatagramChannel channel;

	private BlockingQueue<Packet> inboundPackets;
	private BlockingQueue<Packet> outboundPackets;

	ByteBuffer readBuffer = ByteBuffer.allocate(PacketData.MAX_PAYLOAD_SIZE);
	ByteBuffer writeBuffer = ByteBuffer.allocate(PacketData.MAX_PAYLOAD_SIZE);

	public static final AtomicInteger recievedPacketsUDP = new AtomicInteger(0);
	public static final AtomicInteger sentPacketsUDP = new AtomicInteger(0);

	UdpIo(Server server, ClientHandler clientHandler) {
		this.server = server;
		this.clientHandler = clientHandler;

		inboundPackets = new LinkedBlockingQueue<>();
		outboundPackets = new LinkedBlockingQueue<>();

		try {
			channel = DatagramChannel.open();
			channel.configureBlocking(false);
			channel.socket().bind(new InetSocketAddress(Server.PORT));
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Udp IO socket could not be created");
		}
	}

	@Override
	public void run() {
		while (server.serverON) {
			try {
				readDatagramPackets();
				writeDatagramPackets();

			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("UDP recieve problem");
				Thread.currentThread().interrupt();
				break;
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

			boolean offered = inboundPackets.offer(packet);
			if (!offered) {
				System.out.println("Failed to offer packet to inboundUdpQueue");
				System.out.println("Abandoning packet");
			}

			readBuffer.clear();
			sourceAddress = channel.receive(readBuffer);
		}
	}

	private void writeDatagramPackets() throws IOException {
		Packet packet = outboundPackets.poll();

		while (packet != null) {
			writePacketToBuffer(packet, writeBuffer);
			writeBuffer.flip();

			channel.send(writeBuffer, packet.sourceAddress);

			writeBuffer.clear();
			packet = outboundPackets.poll();
		}
	}

	private void writePacketToBuffer(Packet packet, ByteBuffer byteBuffer) {
		byteBuffer.put(packet.payload);
	}

	public boolean addOutboundPacket(Packet packet) {
		return outboundPackets.offer(packet);
	}

	public void stopProcessingUdp() throws IOException {
		channel.close();
		inboundPackets.clear();
		outboundPackets.clear();
	}

	public BlockingQueue<Packet> getCompletePackets() {
		return inboundPackets;
	}

}
