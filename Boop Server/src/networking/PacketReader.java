package networking;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class PacketReader {

	// Buffers for reading
	private Packet nextPacket;
	private BlockingQueue<Packet> completePackets;

	PacketReader(int queueCapacity) {
		completePackets = new LinkedBlockingQueue<>(queueCapacity);
		nextPacket = new Packet();
	}

	public void read(Client client, ByteBuffer byteBuffer) throws IOException {
		SocketChannel channel = client.getClientChannel();

		// Reads data from the socket and write into the buffer
		int bytesRead = channel.read(byteBuffer);

		// Continuously reads until there is no more data
		// Buffer may be filled with multiple packets worth of data or even a partial
		// packet
		while (bytesRead > 0) {
			bytesRead = channel.read(byteBuffer);
		}

		// The end of the stream has been reached i.e. client disconnected
		if (bytesRead == -1) {
			client.setEndOfStream(true);
			return;
		}

		// switch from write to read mode for the buffer.
		byteBuffer.flip();

		// No data was read into the buffer and so there's no data to process
		// Early exit and clear buffer for reuse
		if (byteBuffer.remaining() == 0) {
			byteBuffer.clear();
			return;
		}

		// Convert data in buffer into a Packet object
		nextPacket.writeToPacket(byteBuffer);

		// Move extra data read to a new packet object
		var packet = new Packet();
		packet.writeExtraDataToPacket(nextPacket);

		nextPacket.clientID = client.getIdentity();

		boolean offered = completePackets.offer(nextPacket);
		if (!offered) {
			System.out.println("Failed to offer packet to completePackets queue");
			System.out.println("Abandoning packet");
		}
		nextPacket = packet;

		// Clear buffer for reuse
		byteBuffer.clear();
	}

	public BlockingQueue<Packet> getCompletePackets() {
		return completePackets;
	}
}
