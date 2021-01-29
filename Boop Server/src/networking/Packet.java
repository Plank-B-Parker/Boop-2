package networking;

public enum Packet {
	NEW_BALLS((byte) 2, 1372, 49, 7),
	OLD_BALLS((byte) 3, 1392, 58, 6),
	CLOCK_SYN((byte) 7, 8, 1, 1),
	ACK((byte) 8, 8, 1, 1),
	DISCONNECT((byte) -5, 0, 0, 0);
	
	
	public static final int MAX_PAYLOAD_SIZE = 1400;
	public static final int FREE_PAYLOAD_SIZE = 1395;
	
	private byte packetID = 0;
	private int maxPayload = 0;
	private int numObj = 0;
	private int numberOfItems = 0;
	
	private Packet(byte packetID, int maxPayload, int numObj, int numberOfItems) {
		this.packetID = packetID;
		this.maxPayload = maxPayload;
		this.numObj = numObj;
		this.numberOfItems = numberOfItems;
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

}
