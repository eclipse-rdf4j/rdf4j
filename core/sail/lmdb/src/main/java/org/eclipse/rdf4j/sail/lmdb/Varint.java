/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import java.nio.ByteBuffer;

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
	public static void writeUnsigned(final ByteBuffer bb, final long value) {
		if (value <= 240) {
			bb.put((byte) value);
		} else if (value <= 2287) {
			bb.put((byte) ((value - 240) / 256 + 241));
			bb.put((byte) ((value - 240) % 256));
		} else if (value <= 67823) {
			bb.put((byte) 249);
			bb.put((byte) ((value - 2288) / 256));
			bb.put((byte) ((value - 2288) % 256));
		} else {
			int bytes = descriptor(value) + 1;
			bb.put((byte) (250 + (bytes - 3)));
			writeSignificantBits(bb, value, bytes);
		}
	}

	/**
	 * Calculates required length in bytes to encode the given long value using variable-length encoding.
	 *
	 * @param value the value value
	 * @return length in bytes
	 */
	public static int calcLengthUnsigned(long value) {
		if (value <= 240) {
			return 1;
		} else if (value <= 2287) {
			return 2;
		} else if (value <= 67823) {
			return 3;
		} else {
			int bytes = descriptor(value) + 1;
			return 1 + bytes;
		}
	}

	/**
	 * Calculates required length in bytes to encode a list of four long values using variable-length encoding.
	 *
	 * @param a first value
	 * @param b second value
	 * @param c third value
	 * @param d fourth value
	 * @return length in bytes
	 */
	public static int calcListLengthUnsigned(long a, long b, long c, long d) {
		return calcLengthUnsigned(a) + calcLengthUnsigned(b) + calcLengthUnsigned(c) + calcLengthUnsigned(d);
	}

	/**
	 * The number of bytes required to represent the given number minus one. The descriptor can be encoded in 3 bits.
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
	 * Decodes a value using the <a href="https://sqlite.org/src4/doc/trunk/www/varint.wiki">variable-length encoding of
	 * SQLite</a>.
	 *
	 * @param bb buffer for reading bytes
	 * @return decoded value
	 * @throws IllegalArgumentException if encoded varint is longer than 9 bytes
	 * @see #writeUnsigned(ByteBuffer, long)
	 */
	public static long readUnsigned(ByteBuffer bb, int pos) throws IllegalArgumentException {
		int a0 = bb.get(pos) & 0xFF;
		if (a0 <= 240) {
			return a0;
		} else if (a0 <= 248) {
			int a1 = bb.get(pos + 1) & 0xFF;
			return 240 + 256 * (a0 - 241) + a1;
		} else if (a0 == 249) {
			int a1 = bb.get(pos + 1) & 0xFF;
			int a2 = bb.get(pos + 1) & 0xFF;
			return 2288 + 256 * a1 + a2;
		} else {
			int bytes = a0 - 250 + 3;
			return readSignificantBits(bb, pos + 1, bytes);
		}
	}

	/**
	 * Determines length of an encoded varint value by inspecting the first byte.
	 *
	 * @param a0 first byte of varint value
	 * @return decoded value
	 */
	public static int firstToLength(byte a0) {
		int a0Unsigned = a0 & 0xFF;
		if (a0Unsigned <= 240) {
			return 1;
		} else if (a0Unsigned <= 248) {
			return 2;
		} else if (a0Unsigned == 249) {
			return 3;
		} else {
			int bytes = a0Unsigned - 250 + 3;
			return 1 + bytes;
		}
	}

	/**
	 * Decodes a single element of a list of variable-length long values from a buffer.
	 *
	 * @param bb    buffer for reading bytes
	 * @param index the element's index
	 * @return the decoded value
	 */
	public static long readListElementUnsigned(ByteBuffer bb, int index) {
		int pos = 0;
		for (int i = 0; i < index; i++) {
			pos += firstToLength(bb.get(pos));
		}
		return readUnsigned(bb, pos);
	}

	/**
	 * Encodes multiple values using variable-length encoding into the given buffer.
	 *
	 * @param bb     buffer for writing bytes
	 * @param values array with values to write
	 */
	public static void writeListUnsigned(final ByteBuffer bb, final long[] values) {
		for (int i = 0; i < values.length; i++) {
			final long value = values[i];
			if (value <= 240) {
				bb.put((byte) value);
			} else if (value <= 2287) {
				bb.put((byte) ((value - 240) / 256 + 241));
				bb.put((byte) ((value - 240) % 256));
			} else if (value <= 67823) {
				bb.put((byte) 249);
				bb.put((byte) ((value - 2288) / 256));
				bb.put((byte) ((value - 2288) % 256));
			} else {
				int bytes = descriptor(value) + 1;
				bb.put((byte) (250 + (bytes - 3)));
				writeSignificantBits(bb, value, bytes);
			}
		}
	}

	/**
	 * Decodes multiple values using variable-length encoding from the given buffer.
	 *
	 * @param bb     buffer for writing bytes
	 * @param values array for the result values
	 */
	public static void readListUnsigned(ByteBuffer bb, long[] values) {
		for (int i = 0; i < values.length; i++) {
			values[i] = readUnsigned(bb);
		}
	}

	/**
	 * Decodes multiple values using variable-length encoding from the given buffer.
	 *
	 * @param bb       buffer for writing bytes
	 * @param indexMap map for indexes of values within values array
	 * @param values   array for the result values
	 */
	public static void readListUnsigned(ByteBuffer bb, int[] indexMap, long[] values) {
		for (int i = 0; i < values.length; i++) {
			values[indexMap[i]] = readUnsigned(bb);
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

	private static int compareRegion(ByteBuffer bb1, int startIdx1, ByteBuffer bb2, int startIdx2, int length) {
		int result = 0;
		for (int i = 0; result == 0 && i < length; i++) {
			result = (bb1.get(startIdx1 + i) & 0xff) - (bb2.get(startIdx2 + i) & 0xff);
		}
		return result;
	}

	/**
	 * A matcher for partial equality tests of varint lists.
	 */
	public static class GroupMatcher {

		final ByteBuffer value;
		final boolean[] shouldMatch;
		final int[] lengths;

		public GroupMatcher(ByteBuffer value, boolean[] shouldMatch) {
			this.value = value;
			this.shouldMatch = shouldMatch;
			this.lengths = new int[shouldMatch.length];
			int pos = 0;
			for (int i = 0; i < lengths.length; i++) {
				int length = firstToLength(value.get(pos));
				lengths[i] = length;
				pos += length;
			}
		}

		public GroupMatcher(ByteBuffer value, boolean a, boolean b, boolean c, boolean d, boolean e) {
			this(value, new boolean[] { a, b, c, d, e });
		}

		public boolean matches(ByteBuffer other) {
			int thisPos = 0;
			int otherPos = 0;
			for (int i = 0; i < shouldMatch.length; i++) {
				int length = lengths[i];
				int otherLength = firstToLength(other.get(otherPos));
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
}
