package networking;

public enum Packet {
	
	NEW_BALLS	((byte) 2, 1372, 49, 7),
	OLD_BALLS	((byte) 3, 1392, 58, 6),
	CLIENTJOIN  ((byte) 71, 1352, 13, 5),
	CLIENTDATA	((byte) 70, 896, 32, 6),
	CLOCK_SYN	((byte) 7, 8, 1, 1),
	PACKET_LOSS	((byte) 15, 4, 1, 1),
	PING		((byte) 5, 16, 1, 2),
	DISCONNECT	((byte) -5, 0, 0, 0),
	DUMMY		((byte) 8, 8, 1, 1);
	
	
	public static final int MAX_PAYLOAD_SIZE = 1400;
	public static final int FREE_PAYLOAD_SIZE = 1395;
	
	private byte packetID = 0;
	private int maxPayload = 0;
	private int numObj = 0;
	private int numberOfItems = 0;
	private int objectSize = 0;
	
	private Packet(byte packetID, int maxPayload, int numObj, int numberOfItems) {
		this.packetID = packetID;
		this.maxPayload = maxPayload;
		this.numObj = numObj;
		this.numberOfItems = numberOfItems;
		
		if (numObj != 0) objectSize = maxPayload / numObj;
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
	
	public int getObjectSize() {
		return objectSize;
	}
	
	public static Packet getPacketByID(byte id) {
		Packet packet = null;
		
		for (var p : Packet.values()) {
			if (p.getID() == id) {
				packet = p;
				break;
			}
		}
		
		return packet;
	}

}
