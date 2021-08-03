package networking;

/**
 * Contains information about how data is structured within certain packets
 */
public enum PacketData {
	
	CLOCK_SYN	((byte) 1, 8, 1, 1),
	PING		((byte) 2, 16, 1, 2),
	PACKET_LOSS	((byte) 3, 4, 1, 1),
	
	NEW_BALLS	((byte) 60, 1372, 49, 7),
	OLD_BALLS	((byte) 61, 1392, 58, 6),
	
	CLIENT_DATA	((byte) 70, 896, 32, 6),
	CLIENT_JOIN ((byte) 71, 1352, 13, 5),
	CLIENT_INPUT((byte) 72, 5, 1, 5), // numberOfItems == number of Enums in Key class.
	CLIENT_DIR	((byte) 73, 8, 1, 2),
	
	DISCONNECT	((byte) -5, 0, 0, 0),
	DUMMY		((byte) -8, 8, 1, 1),
	
	/**
	 * Returned by {@link #getEnumById(byte)} if an enum object does not exist for the given packetID.
	 */
	INVALID		((byte) -127, 0, 0, 0);
	
	public static final int MAX_PAYLOAD_SIZE = 1400;
	public static final int FREE_PAYLOAD_SIZE = 1395;
	
	// Cache array to save time and in this case elements are immutable.
	private static final PacketData[] values = values();
	
	// Enforce immutability to stop setter methods from changing values.
	private final byte packetID;
	private final int maxPayload;
	private final int numObj;
	private final int numberOfItems;
	private final int objectSize;
	
	private PacketData(byte packetID, int maxPayload, int numObj, int numberOfItems) {
		this.packetID = packetID;
		this.maxPayload = maxPayload;
		this.numObj = numObj;
		this.numberOfItems = numberOfItems;
		
		objectSize = numObj != 0 ? maxPayload / numObj : 0;
	}

	public final byte getID() {
		return packetID;
	}

	public final int getMaxPayload() {
		return maxPayload;
	}
	
	public final int getNumObj() {
		return numObj;
	}

	public final int getNumberOfItems() {
		return numberOfItems;
	}
	
	public final int getObjectSize() {
		return objectSize;
	}
	
	public static final PacketData[] getEnums() {
		return values;
	}
	
	/**
	 * Returns an enum that exists with that packetID else an INVALID enum object is returned
	 * @param packetID identifies a specific enum object
	 */
	public static final PacketData getEnumByID(byte packetID) {
		PacketData packet = INVALID;
		
		for (var p : values) {
			if (p.getID() == packetID) {
				packet = p;
				break;
			}
		}
		
		return packet;
	}

}
