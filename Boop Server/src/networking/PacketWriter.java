package networking;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class PacketWriter {

	// Buffers for writing
	BlockingQueue<Packet> writeQueue;
	Packet packetInProgress = null;

	PacketWriter(int queueCapacity) {
		writeQueue = new LinkedBlockingQueue<>(queueCapacity);
	}

	public void write(Client client, ByteBuffer byteBuffer) throws IOException {
		// Move data from packet into the byteBuffer
		int bytesToWrite = packetInProgress.headerLength + packetInProgress.expectedLength
				- packetInProgress.bytesWritten;

		byteBuffer.put(packetInProgress.payload, packetInProgress.bytesWritten, bytesToWrite);
		byteBuffer.flip();

		packetInProgress.bytesWritten += client.write(byteBuffer);
		byteBuffer.clear();

		if (packetInProgress.bytesWritten >= packetInProgress.expectedLength) {
			if (!writeQueue.isEmpty()) {
				packetInProgress = writeQueue.poll();
			} else {
				packetInProgress = null;
			}
		}
	}

	public boolean isWriterEmpty() {
		return writeQueue.isEmpty() && packetInProgress == null;
	}

	public void enqueueWritePacket(Packet packet) {
		if (packetInProgress == null) {
			packetInProgress = packet;
		} else {
			boolean offered = writeQueue.offer(packet);
			if (!offered)
				System.out.println("TCP packet could not be offered to this client: " + packet.clientID);
		}
	}

	public Packet getPacketInProgress() {
		return packetInProgress;
	}
}
