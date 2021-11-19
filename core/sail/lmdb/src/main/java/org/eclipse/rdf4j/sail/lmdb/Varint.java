package org.eclipse.rdf4j.sail.lmdb;

import java.nio.ByteBuffer;
import java.util.Comparator;

/**
 * Encodes and decodes unsigned values using variable-length encoding.
 */
public final class Varint {

	private Varint() {
	}

	/**
	 * Encodes a value using the <a href="https://sqlite.org/src4/doc/trunk/www/varint.wiki">variable-length encoding of
	 * SQLite</a>.
	 *
	 * <p>
	 * The encoding has the following properties:
	 * <ol>
	 * <li>Smaller (and more common) values use fewer bytes and take up less space than larger (and less common)
	 * values.</li>
	 * <li>The length of any varint can be determined by looking at just the first byte of the encoding.</li>
	 * <li>Lexicographical and numeric ordering for varints are the same. Hence if a group of varints are ordered
	 * lexicographically (that is to say, if they are ordered by {@link java.util.Arrays#compare(byte[], byte[])} with
	 * shorter varints coming first) then those varints will also be in numeric order. This property means that varints
	 * can be used as keys in the key/value backend storage and the records will occur in numerical order of the
	 * keys.</li>
	 * </ol>
	 * </p>
	 * <p>
	 * <table>
	 * <thead>
	 * <tr>
	 * <th>A0</th>
	 * <th>Value</th>
	 * </tr>
	 * </thead> <tbody>
	 * <tr>
	 * <td>0–240</td>
	 * <td>A0</td>
	 * </tr>
	 * <tr>
	 * <td>241–248</td>
	 * <td>240 + 256 × (A0 – 241) + A1</td>
	 * </tr>
	 * <tr>
	 * <td>249</td>
	 * <td>2288 + 256 × A1 + A2</td>
	 * </tr>
	 * <tr>
	 * <td>250</td>
	 * <td>A1…A3 as a 3-byte big-endian integer</td>
	 * </tr>
	 * <tr>
	 * <td>251</td>
	 * <td>A1…A4 as a 4-byte big-endian integer</td>
	 * </tr>
	 * <tr>
	 * <td>252</td>
	 * <td>A1…A5 as a 5-byte big-endian integer</td>
	 * </tr>
	 * <tr>
	 * <td>253</td>
	 * <td>A1…A6 as a 6-byte big-endian integer</td>
	 * </tr>
	 * <tr>
	 * <td>254</td>
	 * <td>A1…A7 as a 7-byte big-endian integer</td>
	 * </tr>
	 * <tr>
	 * <td>255</td>
	 * <td>A1…A8 as a 8-byte big-endian integer</td>
	 * </tr>
	 * </tbody>
	 * </table>
	 * </p>
	 *
	 * @param bb    buffer for writing bytes
	 * @param value value to encode
	 */
	public static void writeUnsigned(ByteBuffer bb, long value) {
		if (value <= 240) {
			byte a0 = (byte) value;
			bb.put(a0);
		} else if (value <= 2287) {
			byte a0 = (byte) ((value - 240) / 256 + 241);
			byte a1 = (byte) ((value - 240) % 256);
			bb.put(a0);
			bb.put(a1);
		} else if (value <= 67823) {
			byte a0 = (byte) 249;
			byte a1 = (byte) ((value - 2288) / 256);
			byte a2 = (byte) ((value - 2288) % 256);
			bb.put(a0);
			bb.put(a1);
			bb.put(a2);
		} else {
			int bytes = descriptor(value) + 1;
			byte a0 = (byte) (250 + (bytes - 3));
			bb.put(a0);
			writeSignificantBits(bb, value, bytes);
		}
	}

	/**
	 * Decodes a value using the <a href="https://sqlite.org/src4/doc/trunk/www/varint.wiki">variable-length encoding of
	 * SQLite</a>.
	 *
	 * @param bb buffer for reading bytes
	 * @return decoded value
	 * @throws IllegalArgumentException if encoded varint is longer than 9 bytes
	 * @see #writeUnsigned(ByteBuffer, long)
	 */
	public static long readUnsigned(ByteBuffer bb) throws IllegalArgumentException {
		int a0 = bb.get() & 0xFF;
		if (a0 <= 240) {
			return a0;
		} else if (a0 <= 248) {
			int a1 = bb.get() & 0xFF;
			return 240 + 256 * (a0 - 241) + a1;
		} else if (a0 == 249) {
			int a1 = bb.get() & 0xFF;
			int a2 = bb.get() & 0xFF;
			return 2288 + 256 * a1 + a2;
		} else {
			int bytes = a0 - 250 + 3;
			return readSignificantBits(bb, bytes);
		}
	}

	/**
	 * Writes only the significant bytes of the given value in big-endian order.
	 *
	 * @param bb    buffer for writing bytes
	 * @param value value to encode
	 * @param bytes number of significant bytes
	 */
	private static void writeSignificantBits(ByteBuffer bb, long value, int bytes) {
		while (bytes-- > 0) {
			bb.put((byte) (0xFF & (value >>> (bytes * 8))));
		}
	}

	/**
	 * Reads only the significant bytes of the given value in big-endian order.
	 *
	 * @param bb    buffer for reading bytes
	 * @param bytes number of significant bytes
	 */
	private static long readSignificantBits(ByteBuffer bb, int bytes) {
		bytes--;
		long value = (long) (bb.get() & 0xFF) << (bytes * 8);
		while (bytes-- > 0) {
			value |= (long) (bb.get() & 0xFF) << (bytes * 8);
		}
		return value;
	}

	/**
	 * Reads only the significant bytes of the given value in big-endian order.
	 *
	 * @param bb    buffer for reading bytes
	 * @param pos   position within the buffer
	 * @param bytes number of significant bytes
	 */
	private static long readSignificantBits(ByteBuffer bb, int pos, int bytes) {
		bytes--;
		long value = (long) (bb.get(pos++) & 0xFF) << (bytes * 8);
		while (bytes-- > 0) {
			value |= (long) (bb.get(pos++) & 0xFF) << (bytes * 8);
		}
		return value;
	}

	/**
	 * The number of bytes required to represent the given number minus one. The descriptor is encoded in 3 bits.
	 *
	 * <p>
	 * 000 = 1, 001 = 2, ..., 111 = 8
	 * </p>
	 *
	 * @param value the long value
	 * @return the descriptor encoded as byte
	 */
	private static byte descriptor(long value) {
		return value == 0 ? 0 : (byte) (7 - Long.numberOfLeadingZeros(value) / 8);
	}

	/**
	 * Encodes a group of five long values using group variable-length encoding with size prefix into the given buffer.
	 *
	 * @param bb buffer for writing bytes
	 * @param a  first value
	 * @param b  second value
	 * @param c  third value
	 * @param d  fourth value
	 * @param e  fifth value
	 */
	public static void writeGroupUnsigned(ByteBuffer bb, long a, long b, long c, long d, long e) {
		byte aDesc = descriptor(a);
		byte bDesc = descriptor(b);
		byte cDesc = descriptor(c);
		byte dDesc = descriptor(d);
		byte eDesc = descriptor(e);

		short desc = (short) ((aDesc << 12) | (bDesc << 9) | (cDesc << 6) | (dDesc << 3) |
				eDesc);
		bb.put((byte) (0xff & (desc >>> 8)));
		bb.put((byte) (0xff & desc));

		writeSignificantBits(bb, a, aDesc + 1);
		writeSignificantBits(bb, b, bDesc + 1);
		writeSignificantBits(bb, c, cDesc + 1);
		writeSignificantBits(bb, d, dDesc + 1);
		writeSignificantBits(bb, e, eDesc + 1);
	}

	/**
	 * Calculates required length in bytes to encode a group of five long values using group variable-length encoding
	 * with size prefix.
	 *
	 * @param a first value
	 * @param b second value
	 * @param c third value
	 * @param d fourth value
	 * @return length in bytes
	 */
	public static int calcGroupLengthUnsigned(long a, long b, long c, long d, long e) {
		return 2 + 5 + descriptor(a) + descriptor(b) + descriptor(c) + descriptor(d) + descriptor(e);
	}

	/**
	 * Calculates required length in bytes to encode a group of four long values using group variable-length encoding
	 * with size prefix.
	 *
	 * @param a first value
	 * @param b second value
	 * @param c third value
	 * @param d fourth value
	 * @return length in bytes
	 */
	public static int calcGroupLengthUnsigned4(long a, long b, long c, long d) {
		return 2 + 4 + descriptor(a) + descriptor(b) + descriptor(c) + descriptor(d);
	}

	/**
	 * Encodes a group of five values using group variable-length encoding with size prefix into the given buffer.
	 *
	 * @param bb     buffer for writing bytes
	 * @param values array with 5 values to write
	 */
	public static void writeGroupUnsigned(ByteBuffer bb, long[] values) {
		writeGroupUnsigned(bb, values[0], values[1], values[2], values[3], values[4]);
	}

	/**
	 * Encodes a group of four five values using group variable-length encoding with size prefix into the given buffer.
	 *
	 * @param bb     buffer for writing bytes
	 * @param values array with 4 values to write
	 */
	public static void writeGroupUnsigned4(ByteBuffer bb, long[] values) {
		writeGroupUnsigned4(bb, values[0], values[1], values[2], values[3]);
	}

	/**
	 * Encodes a group of four long values using group variable-length encoding with size prefix into the given buffer.
	 *
	 * @param bb buffer for writing bytes
	 * @param a  first value
	 * @param b  second value
	 * @param c  third value
	 * @param d  fourth value
	 */
	public static void writeGroupUnsigned4(ByteBuffer bb, long a, long b, long c, long d) {
		byte aDesc = descriptor(a);
		byte bDesc = descriptor(b);
		byte cDesc = descriptor(c);
		byte dDesc = descriptor(d);

		short desc = (short) ((aDesc << 12) | (bDesc << 9) | (cDesc << 6) | (dDesc << 3));
		bb.put((byte) (0xff & (desc >>> 8)));
		bb.put((byte) (0xff & desc));

		writeSignificantBits(bb, a, aDesc + 1);
		writeSignificantBits(bb, b, bDesc + 1);
		writeSignificantBits(bb, c, cDesc + 1);
		writeSignificantBits(bb, d, dDesc + 1);
	}

	/**
	 * Decodes a group of four or five variable-length long values with size prefix from a buffer.
	 *
	 * @param bb     buffer for reading bytes
	 * @param values array for decoded values
	 */
	public static void readGroupUnsigned(ByteBuffer bb, long[] values) {
		short desc = (short) (((bb.get() & 0xff) << 8) | (bb.get() & 0xff));

		values[0] = readSignificantBits(bb, ((desc >> 12) & 7) + 1);
		values[1] = readSignificantBits(bb, ((desc >> 9) & 7) + 1);
		values[2] = readSignificantBits(bb, ((desc >> 6) & 7) + 1);
		values[3] = readSignificantBits(bb, ((desc >> 3) & 7) + 1);
		if (values.length > 4) {
			values[4] = readSignificantBits(bb, (desc & 7) + 1);
		}
	}

	/**
	 * Decodes a single element of group of variable-length long values with size prefix from a buffer.
	 *
	 * @param bb    buffer for reading bytes
	 * @param index the element's index
	 * @return the decoded value
	 */
	public static long readGroupElementUnsigned(ByteBuffer bb, int index) {
		short desc = (short) (((bb.get(0) & 0xff) << 8) | (bb.get(1) & 0xff));
		int pos = 2;
		for (int j = 0; j < index; j++) {
			pos += ((desc >> (12 - 3 * j)) & 7) + 1;
		}
		return readSignificantBits(bb, pos, ((desc >> (12 - index * 3)) & 7) + 1);
	}

	/**
	 * A matcher for partial equality tests of group varints.
	 */
	public static class GroupMatcher {

		final ByteBuffer value;
		final boolean[] shouldMatch;
		final int[] lengths;
		final byte[] lengthMask;
		final short valueDesc;

		public GroupMatcher(ByteBuffer value, boolean a, boolean b, boolean c, boolean d, boolean e) {
			this.shouldMatch = new boolean[] { a, b, c, d, e };
			this.value = value;
			this.valueDesc = (short) (((value.get(0) & 0xff) << 8) | (value.get(1) & 0xff));
			this.lengths = new int[] {
					((valueDesc >> 12) & 7) + 1,
					((valueDesc >> 9) & 7) + 1,
					((valueDesc >> 6) & 7) + 1,
					((valueDesc >> 3) & 7) + 1,
					(valueDesc & 7) + 1
			};
			this.lengthMask = lengthMask();
		}

		private byte[] lengthMask() {
			int desc = 0;
			for (int i = 0; i < shouldMatch.length; i++) {
				desc |= shouldMatch[i] ? (7 << (12 - i * 3)) : 0;
			}
			return new byte[] { (byte) (0xff & (desc >>> 8)), (byte) (0xff & desc) };
		}

		public boolean matches(ByteBuffer other) {
			for (int i = 0; i < 2; i++) {
				if (((value.get(i) ^ other.get(i)) & lengthMask[i]) != 0) {
					return false;
				}
			}
			short otherDesc = (short) (((other.get(0) & 0xff) << 8) | (other.get(1) & 0xff));
			int thisPos = 2;
			int otherPos = 2;
			for (int i = 0; i < shouldMatch.length; i++) {
				int length = lengths[i];
				int otherLength = ((otherDesc >> (12 - 3 * i)) & 7) + 1;
				if (shouldMatch[i]) {
					if (length != otherLength || compareRegion(value, thisPos, other, otherPos, length) != 0) {
						return false;
					}
				}
				thisPos += length;
				otherPos += otherLength;
			}
			return true;
		}
	}

	/**
	 * A comparator for element-wise comparison of group varints.
	 */
	public static class GroupComparator implements Comparator<ByteBuffer> {

		private final int[] indexes;

		public GroupComparator(int[] indexes) {
			this.indexes = indexes;
		}

		public int compare(ByteBuffer group1, ByteBuffer group2) {
			short group1Desc = (short) (((group1.get(0) & 0xff) << 8) | (group1.get(1) & 0xff));
			short group2Desc = (short) (((group2.get(0) & 0xff) << 8) | (group2.get(1) & 0xff));
			for (int i = 0; i < indexes.length; i++) {
				int index = indexes[i];
				int value1Length = ((group1Desc >> (12 - 3 * index)) & 7) + 1;
				int value2Length = ((group2Desc >> (12 - 3 * index)) & 7) + 1;
				if (value1Length < value2Length) {
					return -1;
				} else if (value1Length > value2Length) {
					return 1;
				} else {
					// two independent for loops seem to be
					// faster as one combined loop
					int group1Pos = 2;
					for (int j = 0; j < index; j++) {
						group1Pos += ((group1Desc >> (12 - 3 * j)) & 7) + 1;
					}
					int group2Pos = 2;
					for (int j = 0; j < index; j++) {
						group2Pos += ((group2Desc >> (12 - 3 * j)) & 7) + 1;
					}

					int diff = compareRegion(group1, group1Pos, group2, group2Pos, value1Length);
					if (diff != 0) {
						return diff;
					}
				}
			}
			return 0;
		}
	}

	private static int compareRegion(ByteBuffer bb1, int startIdx1, ByteBuffer bb2, int startIdx2, int length) {
		int result = 0;
		for (int i = 0; result == 0 && i < length; i++) {
			result = (bb1.get(startIdx1 + i) & 0xff) - (bb2.get(startIdx2 + i) & 0xff);
		}
		return result;
	}
}