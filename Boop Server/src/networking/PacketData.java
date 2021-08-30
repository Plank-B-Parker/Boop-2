package networking;

/**
 * Contains information about how data is structured within certain packets
 */
public enum PacketData {
	
	CLOCK_SYN	 ((byte) 1, 8, 1, 1, Protocol.TCP),
	PING		 ((byte) 2, 16, 1, 2, Protocol.TCP),
	PACKET_LOSS	 ((byte) 3, 4, 1, 1, Protocol.TCP),
	
	NEW_BALLS	 ((byte) 60, 1372, 49, 7, Protocol.UDP),
	OLD_BALLS	 ((byte) 61, 1392, 58, 6, Protocol.UDP),
	
	CLIENT_DATA	 ((byte) 70, 896, 32, 6, Protocol.UDP),
	CLIENT_SETUP ((byte) 71, 8, 1, 1, Protocol.TCP),
	CLIENT_JOIN  ((byte) 72, 1352, 13, 5, Protocol.TCP),
	CLIENT_INPUT ((byte) 73, 5, 1, 5, Protocol.UDP), // numberOfItems == number of Enums in Key class.
	CLIENT_DIR	 ((byte) 74, 8, 1, 2, Protocol.UDP),
	
	DISCONNECT	 ((byte) -5, 0, 0, 0, Protocol.TCP),
	DUMMY		 ((byte) -8, 8, 1, 1, Protocol.UDP),
	
	/**
	 * Returned by {@link #getEnumById(byte)} if an enum object does not exist for the given packetID.
	 */
	INVALID		((byte) -127, 0, 0, 0, Protocol.UDP);
	
	public static final int MAX_PAYLOAD_SIZE = 1400;
	
	// Cache array to save time and in this case elements are immutable.
	private static final PacketData[] values = values();
	
	// Enforce immutability to stop setter methods from changing values.
	private final byte packetID;
	private final int maxPayload;
	private final int numObj;
	private final int numberOfItems;
	private final int objectSize;
	private final Protocol protocol;
	
	private PacketData(byte packetID, int maxPayload, int numObj, int numberOfItems, Protocol protocol) {
		this.packetID = packetID;
		this.maxPayload = maxPayload;
		this.numObj = numObj;
		this.numberOfItems = numberOfItems;
		this.protocol = protocol;
		
		objectSize = numObj != 0 ? maxPayload / numObj : 0;
	}

	public byte getID() {
		return packetID;
	}

	public int getMaxPayload() {
		return maxPayload;
	}
	
	public int getNumObj() {
		return numObj;
	}

	public int getNumberOfItems() {
		return numberOfItems;
	}
	
	public Protocol getProtocol() {
		return protocol;
	}
	
	public int getObjectSize() {
		return objectSize;
	}
	
	public int getHeaderlessPayloadSize() {
		return MAX_PAYLOAD_SIZE - protocol.getHeaderLength();
	}
	
	public static PacketData[] getEnums() {
		return values;
	}
	
	/**
	 * Returns an enum that exists with that packetID else an INVALID enum object is returned
	 * @param packetID identifies a specific enum object
	 */
	public static PacketData getEnumByID(byte packetID) {
		PacketData packet = INVALID;
		
		for (var p : values) {
			if (p.getID() == packetID) {
				packet = p;
				break;
			}
		}
		
		return packet;
	}

	public enum Protocol{
		TCP(5), // PacketID(byte) + Payload length(int) 
		UDP(5); // PacketID(byte) + Packet Sequence(int)
		
		private final int headerLength;
		
		private Protocol(int headerLength) {
			this.headerLength = headerLength;
		}
		
		public int getHeaderLength() {
			return headerLength;
		}
		
	}
	
} // End of PacketData Enum
