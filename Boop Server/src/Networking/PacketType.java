package Networking;

public enum PacketType {
	NEW_BALLS((byte) 2, 7, 28),
	OLD_BALLS((byte) 3, 5, 20),
	DISCONNECT((byte) -5, 0);
	
	
	private byte packetID = 0;
	private int  numberOfItems = 0;
	private int bytesPerBall = 0;
	
	private PacketType(byte packetID, int numberOfItems, int bytesPerBall) {
		this.packetID = packetID;
		this.numberOfItems = numberOfItems;
		this.bytesPerBall = bytesPerBall;
		// Experiment with Number abstract class to convert primitve bytes and int
		// to other numbers.
		// e.g. Make client convert the Number to its proper type rather than the server
		Number[] nums = new Number[10];
		nums[0].doubleValue();
	}
	
	private PacketType(byte packetID, int numberOfItems) {
		this.packetID = packetID;
		this.numberOfItems = numberOfItems;
	}
	
	private PacketType(byte packetID, int maxPayload, int numObj, int numberOfItems) {
		
	}

	public byte getPacketID() {
		return packetID;
	}

	public int getNumberOfItems() {
		return numberOfItems;
	}

	public int getBytesPerBall() {
		return bytesPerBall;
	}
	
}
