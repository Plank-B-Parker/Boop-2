package Math;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public final class Bitmaths {
	
	private Bitmaths() { }
	
	public static byte[] floatArrayToBytes(float[] floats) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(floats.length * 4);
		FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
		
		for (float i: floats) {
			floatBuffer.put(i);
		}
		return byteBuffer.array();
	}
	
	public static byte[] intArrayToBytes(int[] ints) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(ints.length * 4);
		IntBuffer intBuffer = byteBuffer.asIntBuffer();
		
		for (int i: ints) {
			intBuffer.put(i);
		}
		return byteBuffer.array();
	}
	
	public static byte[] floatToBytes(float value) {
		return ByteBuffer.allocate(4).putFloat(value).array();
	}
	
	public static byte[] intToBytes(int value) {
		return ByteBuffer.allocate(4).putInt(value).array();
	}
	
	public static float bytesToFloat(byte[] bytes) {
		return ByteBuffer.wrap(bytes).getFloat();
	}
	
	public static float bytesToFloat(byte[] bytes, int offset) {
		if (offset > bytes.length - 4) return -1;
		return ByteBuffer.wrap(bytes).getFloat(offset);
	}
	
	public static float[] bytesToFloatArray(byte[] bytes) {
		return ByteBuffer.wrap(bytes).asFloatBuffer().array();
	}
	
	public static float bytesToInt(byte[] bytes) {
		return ByteBuffer.wrap(bytes).getInt();
	}
	
	public static float bytesToInt(byte[] bytes, int offset) {
		if (offset > bytes.length - 4) return -1;
		return ByteBuffer.wrap(bytes).getInt(offset);
	}
	
	public static int[] bytesToIntArray(byte[] bytes) {
		return ByteBuffer.wrap(bytes).asIntBuffer().array();
	}
	
	public static byte[] pushByteToData(byte frontByte, byte[] data) {
		byte[] newData = new byte[data.length + 1];
		System.arraycopy(data, 0, newData, 1, data.length);
		
		newData[0] = frontByte;
		
		return newData;
	}
	
}
