package math;

import java.nio.ByteBuffer;
import java.util.Arrays;

public final class Bitmaths {

	private Bitmaths() {
	}

	// Longs

	public static byte[] longToBytes(long value) {
		return ByteBuffer.allocate(8).putLong(value).array();
	}

	public static long bytesToLong(byte[] bytes) {
		return ByteBuffer.wrap(bytes).getLong();
	}

	public static long bytesToLong(byte[] bytes, int index) {
		if (index > bytes.length - 8)
			return -1;
		return ByteBuffer.wrap(bytes).getLong(index);
	}

	public static byte[] longArrayToBytes(long[] longs) {
		var byteBuffer = ByteBuffer.allocate(longs.length * 8);

		byteBuffer.asLongBuffer().put(longs);

		return byteBuffer.array();
	}

	public static long[] bytesToLongArray(byte[] bytes) {
		return ByteBuffer.wrap(bytes).asLongBuffer().array();
	}

	// Floats

	public static byte[] floatToBytes(float value) {
		return ByteBuffer.allocate(4).putFloat(value).array();
	}

	public static float bytesToFloat(byte[] bytes) {
		return ByteBuffer.wrap(bytes).getFloat();
	}

	public static float bytesToFloat(byte[] bytes, int index) {
		if (index > bytes.length - 4)
			return -1;
		return ByteBuffer.wrap(bytes).getFloat(index);
	}

	public static byte[] floatArrayToBytes(float[] floats) {
		var byteBuffer = ByteBuffer.allocate(floats.length * 4);

		byteBuffer.asFloatBuffer().put(floats);

		return byteBuffer.array();
	}

	public static float[] bytesToFloatArray(byte[] bytes) {
		return ByteBuffer.wrap(bytes).asFloatBuffer().array();
	}

	// Integers

	public static byte[] intToBytes(int value) {
		return ByteBuffer.allocate(4).putInt(value).array();
	}

	public static int bytesToInt(byte[] bytes) {
		return ByteBuffer.wrap(bytes).getInt();
	}

	public static int bytesToInt(byte[] bytes, int index) {
		if (index > bytes.length - 4)
			return -1;
		return ByteBuffer.wrap(bytes).getInt(index);
	}

	public static byte[] intArrayToBytes(int[] ints) {
		var byteBuffer = ByteBuffer.allocate(ints.length * 4);

		byteBuffer.asIntBuffer().put(ints);

		return byteBuffer.array();
	}

	public static int[] bytesToIntArray(byte[] bytes) {
		return ByteBuffer.wrap(bytes).asIntBuffer().array();
	}

	public static byte[] shortToBytes(short value) {
		return ByteBuffer.allocate(4).putInt(value).array();
	}

	// Shorts

	public static short bytesToShort(byte[] bytes) {
		return ByteBuffer.wrap(bytes).getShort();
	}

	public static short bytesToShort(byte[] bytes, int index) {
		if (index > bytes.length - 2)
			return -1;
		return ByteBuffer.wrap(bytes).getShort(index);
	}

	public static byte[] shortArrayToBytes(short[] shorts) {
		var byteBuffer = ByteBuffer.allocate(shorts.length * 2);

		byteBuffer.asShortBuffer().put(shorts);

		return byteBuffer.array();
	}

	public static short[] bytesToShortArray(byte[] bytes) {
		return ByteBuffer.wrap(bytes).asShortBuffer().array();
	}

	// Bytes

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

	/**
	 * Returns the bytes for the primitive type of the number.
	 */
	public static byte[] numberToBytes(Number number) {
		byte[] bytes = intToBytes(number.intValue());

		if (number.getClass() == Float.class) {
			bytes = floatToBytes(number.floatValue());
		} else if (number.getClass() == Long.class) {
			bytes = longToBytes(number.longValue());
		} else if (number.getClass() == Short.class) {
			bytes = shortToBytes(number.shortValue());
		}

		return bytes;
	}

	/**
	 * Converts the array of numbers into bytes for their respective primitive
	 * types.
	 */
	public static byte[] numberArrayToBytes(Number[] numbers) {
		byte[] bytes = new byte[0];

		for (int i = numbers.length - 1; i >= 0; i--) {
			bytes = pushByteArrayToData(numberToBytes(numbers[i]), bytes);
		}

		return bytes;
	}

	public static byte[] stringArrayToBytes(String[] string) {
		var bytes = new byte[0];

		for (int i = string.length - 1; i >= 0; i--) {
			bytes = pushByteArrayToData(string[i].getBytes(), bytes);
		}

		return bytes;
	}

	public static String bytesToString(byte[] bytes, int index, int length) {
		if (index > bytes.length - length)
			return null;
		byte[] stringInBytes = Arrays.copyOfRange(bytes, index, index + length);
		return new String(stringInBytes);
	}

	public static String bytesToString(byte[] bytes, int length) {
		return bytesToString(bytes, 0, length);
	}

}
