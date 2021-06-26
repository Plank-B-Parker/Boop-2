package math;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

public final class Bitmaths {
	
	private Bitmaths() { }
	
	public static byte[] longToBytes(long value) {
		return ByteBuffer.allocate(8).putLong(value).array();
	}
	
	public static long bytesToLong(byte[] bytes) {
		return ByteBuffer.wrap(bytes).getLong();
	}
	
	public static long bytesToLong(byte[] bytes, int index) {
		if (index > bytes.length - 8) return -1;
		return ByteBuffer.wrap(bytes).getLong(index);
	}
	
	public static byte[] longArrayToBytes(long[] longs) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(longs.length * 8);
		LongBuffer longBuffer = byteBuffer.asLongBuffer();
		
		for (long i: longs) {
			longBuffer.put(i);
		}
		return byteBuffer.array();
	}
	
	public static long[] bytesToLongArray(byte[] bytes) {
		return ByteBuffer.wrap(bytes).asLongBuffer().array();
	}
	
	public static byte[] floatToBytes(float value) {
		return ByteBuffer.allocate(4).putFloat(value).array();
	}
	
	public static float bytesToFloat(byte[] bytes) {
		return ByteBuffer.wrap(bytes).getFloat();
	}
	
	public static float bytesToFloat(byte[] bytes, int index) {
		if (index > bytes.length - 4) return -1;
		return ByteBuffer.wrap(bytes).getFloat(index);
	}
	
	public static byte[] floatArrayToBytes(float[] floats) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(floats.length * 4);
		FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
		
		for (float i: floats) {
			floatBuffer.put(i);
		}
		return byteBuffer.array();
	}
	
	public static float[] bytesToFloatArray(byte[] bytes) {
		return ByteBuffer.wrap(bytes).asFloatBuffer().array();
	}
	
	public static byte[] intToBytes(int value) {
		return ByteBuffer.allocate(4).putInt(value).array();
	}
	
	public static int bytesToInt(byte[] bytes) {
		return ByteBuffer.wrap(bytes).getInt();
	}
	
	public static int bytesToInt(byte[] bytes, int index) {
		if (index > bytes.length - 4) return -1;
		return ByteBuffer.wrap(bytes).getInt(index);
	}
	
	public static byte[] intArrayToBytes(int[] ints) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(ints.length * 4);
		IntBuffer intBuffer = byteBuffer.asIntBuffer();
		
		for (int i: ints) {
			intBuffer.put(i);
		}
		return byteBuffer.array();
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
	
	public static byte[] pushByteArrayToData(byte[] frontArray, byte[] data) {
		byte[] newData = new byte[data.length + frontArray.length];
		System.arraycopy(data, 0, newData, newData.length - data.length, data.length);
		
		System.arraycopy(frontArray, 0, newData, 0, frontArray.length);
		
		return newData;
	}
	
	public static byte[] numberToBytes(Number number) {
		byte[] bytes = intToBytes(number.intValue());
		

		if (number.getClass() == Float.class) {
			bytes = floatToBytes(number.floatValue());
		}
		else if (number.getClass() == Long.class) {
			bytes = longToBytes(number.longValue());
		}
		else if (number.getClass() == Short.class) {
			number.shortValue();
		}
		
		return bytes;
	}
	
	public static byte[] numberArrayToBytes(Number[] numbers) {
		byte[] bytes = new byte[0];
		
		for (int i = numbers.length - 1; i >= 0; i--) {
			bytes = pushByteArrayToData(numberToBytes(numbers[i]), bytes);
		}
		
		return bytes;
	}
}
