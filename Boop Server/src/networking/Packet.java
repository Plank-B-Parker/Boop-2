package networking;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

import math.Bitmaths;

/**
 * Contains the id of the packetData it represents and only in TCP connections
 * does it specify the length
 */
public class Packet {
	long clientID = 0;
	SocketAddress sourceAddress = null;

	PacketData identity = PacketData.INVALID;
	int headerLength = identity.getProtocol().getHeaderLength();
	byte[] payload;
	int packetSequence = 0;

	// CurrentLength maximum should be expectedLength + headerLength
	int currentLength = 0;
	// Expected length of the payload specified at the start of the packet
	int expectedLength = identity.getHeaderlessPayloadSize();
	int bytesWritten = 0;

	public Packet() {
		payload = new byte[PacketData.MAX_PAYLOAD_SIZE];
	}

	public Packet(byte[] data, long clientID) {
		this.clientID = clientID;
		payload = data;
		currentLength = payload.length;
		identity = PacketData.getEnumByID(payload[0]);
		headerLength = identity.getProtocol().getHeaderLength();
		expectedLength = Bitmaths.bytesToInt(payload, 1);
	}

	/**
	 * Reads as much data from the buffer as it can and writes into the Packet byte
	 * array
	 * <p>
	 * If there is enough data read then the PacketData type and expected length is
	 * also determined. However the packet may be filled with more data than
	 * expected so a call to {@link #writeExtraDataToPacket(Packet)} is needed to
	 * move the extra data.
	 * 
	 * @param byteBuffer the buffer to read from
	 */
	public void writeToPacket(ByteBuffer byteBuffer) {
		int remaining = byteBuffer.remaining();

		int bytesToCopy = Math.min(remaining, headerLength + expectedLength - currentLength);

		// Packet is already full with expected data
		if (bytesToCopy <= 0) {
			return;
		}

		try {
			byteBuffer.get(payload, currentLength, bytesToCopy);

		} catch (IndexOutOfBoundsException e) {
			System.out.println("Big problem man: Packet class");
			System.out.println("Current length: " + currentLength);
			System.out.println("Left To Write: " + (headerLength + expectedLength - currentLength));
			System.out.println("Remaining: " + remaining);
			System.out.println("BytesToCopy: " + bytesToCopy);
		}
		currentLength += bytesToCopy;

		// If all of the header information has been read
		if (identity == PacketData.INVALID && currentLength > 4) {
			identity = PacketData.getEnumByID(payload[0]);
			headerLength = identity.getProtocol().getHeaderLength();
			expectedLength = Bitmaths.bytesToInt(payload, 1);
		}

	}

	/**
	 * 
	 * @param source
	 */
	public void writeExtraDataToPacket(Packet source) {
		// The source packet contains more data than it should
		if (source.currentLength > source.expectedLength + source.headerLength) {
			this.currentLength = source.currentLength - source.expectedLength - source.headerLength;
			// Copy extra data from source to this packet (data in source does not need to
			// be removed)
			System.arraycopy(source.payload, source.expectedLength + source.headerLength, this.payload, 0,
					this.currentLength);

			// Adjust pointer for source (may not be necessary when reading the data)
			source.currentLength = source.expectedLength + source.headerLength;

			// See if there's enough data to work out the header information
			if (this.currentLength > 4) {
				identity = PacketData.getEnumByID(payload[0]);
				headerLength = identity.getProtocol().getHeaderLength();
				expectedLength = Bitmaths.bytesToInt(payload, 1);
			}

		}

	}

	public void writeToUdpPacket(ByteBuffer byteBuffer, SocketAddress sourceAddress) {
		this.sourceAddress = sourceAddress;

		int remaining = byteBuffer.remaining();

		byteBuffer.get(payload, currentLength, remaining);
		currentLength += remaining;

		// If all of the header information has been read
		if (identity == PacketData.INVALID && currentLength > 4) {
			identity = PacketData.getEnumByID(payload[0]);
			packetSequence = Bitmaths.bytesToInt(payload, 1);
			headerLength = identity.getProtocol().getHeaderLength();
			expectedLength = currentLength - headerLength;
		}
	}

	public boolean isFilled() {
		return currentLength >= expectedLength + headerLength;
	}

	public boolean isWritten() {
		return bytesWritten >= expectedLength + headerLength;
	}

}