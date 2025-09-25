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
import java.nio.ByteOrder;

import org.eclipse.rdf4j.sail.lmdb.util.Bytes;
import org.eclipse.rdf4j.sail.lmdb.util.SignificantBytesBE;

/**
 * Encodes and decodes unsigned values using variable-length encoding.
 */
public final class Varint {

	private static final byte[] ENCODED_LONG_MAX = new byte[] {
			(byte) 0xFF, // header: 8 payload bytes
			0x7F, // MSB of Long.MAX_VALUE
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
	};

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
		// Fast path for Long.MAX_VALUE (0xFF header + 8 data bytes)
		if (value == Long.MAX_VALUE) {
			final ByteOrder prev = bb.order();
			if (prev != ByteOrder.BIG_ENDIAN) {
				bb.order(ByteOrder.BIG_ENDIAN);
			}
			try {
				bb.put(ENCODED_LONG_MAX);
			} finally {
				if (prev != ByteOrder.BIG_ENDIAN) {
					bb.order(prev);
				}
			}
			return;
		}

		if (value <= 240) {
			bb.put((byte) value);
		} else if (value <= 2287) {
			// header: 241..248, then 1 payload byte
			// Using bit ops instead of div/mod and putShort to batch the two bytes.
			long v = value - 240; // 1..2047
			final ByteOrder prev = bb.order();
			if (prev != ByteOrder.BIG_ENDIAN) {
				bb.order(ByteOrder.BIG_ENDIAN);
			}
			try {
				int hi = (int) (v >>> 8) + 241; // 241..248
				int lo = (int) (v & 0xFF); // 0..255
				bb.putShort((short) ((hi << 8) | lo));
			} finally {
				if (prev != ByteOrder.BIG_ENDIAN) {
					bb.order(prev);
				}
			}
		} else if (value <= 67823) {
			// header 249, then 2 payload bytes (value - 2288), big-endian
			long v = value - 2288; // 0..65535
			bb.put((byte) 249);
			final ByteOrder prev = bb.order();
			if (prev != ByteOrder.BIG_ENDIAN) {
				bb.order(ByteOrder.BIG_ENDIAN);
			}
			try {
				bb.putShort((short) v);
			} finally {
				if (prev != ByteOrder.BIG_ENDIAN) {
					bb.order(prev);
				}
			}
		} else {
			int bytes = descriptor(value) + 1; // 3..8
			bb.put((byte) (250 + (bytes - 3))); // header 250..255
			writeSignificantBits(bb, value, bytes); // payload (batched)
		}
	}

	// Writes the top `bytes` significant bytes of `value` in big-endian order.
// Uses putLong/putInt/putShort to batch writes and a single leading byte if needed.
	private static void writeSignificantBits(ByteBuffer bb, long value, int bytes) {
		final ByteOrder prev = bb.order();
		if (prev != ByteOrder.BIG_ENDIAN) {
			bb.order(ByteOrder.BIG_ENDIAN);
		}
		try {
			int i = bytes;

			// If odd number of bytes, write the leading MSB first
			if ((i & 1) != 0) {
				bb.put((byte) (value >>> ((i - 1) * 8)));
				i--;
			}

			// Now i is even: prefer largest chunks first
			if (i == 8) { // exactly 8 bytes
				bb.putLong(value);
				return;
			}

			if (i >= 4) { // write next 4 bytes, if any
				int shift = (i - 4) * 8;
				bb.putInt((int) (value >>> shift));
				i -= 4;
			}

			while (i >= 2) { // write remaining pairs
				int shift = (i - 2) * 8;
				bb.putShort((short) (value >>> shift));
				i -= 2;
			}
			// i must be 0 here.
		} finally {
			if (prev != ByteOrder.BIG_ENDIAN) {
				bb.order(prev);
			}
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
	 * Decodes a value using SQLite's variable-length integer encoding.
	 *
	 * Lead-byte layout → number of additional bytes: 0..240 → 0 241..248→ 1 249 → 2 250..255→ 3..8 (i.e., 250→3, 251→4,
	 * …, 255→8)
	 *
	 * @param bb buffer for reading bytes
	 * @return decoded value
	 * @throws IllegalArgumentException if encoded varint is longer than 9 bytes
	 * @see #writeUnsigned(ByteBuffer, long)
	 */
	public static long readUnsigned(ByteBuffer bb) throws IllegalArgumentException {
		final int a0 = bb.get() & 0xFF; // lead byte, unsigned

		if (a0 <= 240) {
			return a0;
		}

		final int extra = VARINT_EXTRA_BYTES[a0]; // 0..8 additional bytes

		switch (extra) {
		case 1: {
			// 1 extra byte; 241..248
			final int a1 = bb.get() & 0xFF;
			// 240 + 256*(a0-241) + a1
			return 240L + ((long) (a0 - 241) << 8) + a1;
		}

		case 2: {
			// 2 extra bytes; lead byte == 249
			final int a1 = bb.get() & 0xFF;
			final int a2 = bb.get() & 0xFF;
			// 2288 + 256*a1 + a2
			return 2288L + ((long) a1 << 8) + a2;
		}

		case 3:
		case 4:
		case 5:
		case 6:
		case 7:
		case 8:
			return readSignificantBitsDirect(bb, extra);
		// 3..8 extra bytes; 250..255
		default:
			throw new IllegalArgumentException("Bytes is higher than 8: " + extra);

		}
	}

	/** Lookup: lead byte (0..255) → number of additional bytes (0..8). */
	private static final byte[] VARINT_EXTRA_BYTES = buildVarintExtraBytes();

	private static byte[] buildVarintExtraBytes() {
		final byte[] t = new byte[256];

		// 0..240 → 0 extra bytes
		for (int i = 0; i <= 240; i++) {
			t[i] = 0;
		}

		// 241..248 → 1 extra byte
		for (int i = 241; i <= 248; i++) {
			t[i] = 1;
		}

		// 249 → 2 extra bytes
		t[249] = 2;

		// 250..255 → 3..8 extra bytes
		for (int i = 250; i <= 255; i++) {
			t[i] = (byte) (i - 247); // 250→3, …, 255→8
		}

		return t;
	}

	public static long readUnsignedHeap(ByteBuffer bb) throws IllegalArgumentException {
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
			return readSignificantBitsHeap(bb, bytes);
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

	private static final int[] FIRST_TO_LENGTH = buildFirstToLength();

	private static int[] buildFirstToLength() {
		int[] t = new int[256];
		// 0..240 → 1
		for (int i = 0; i <= 240; i++) {
			t[i] = 1;
		}
		// 241..248 → 2
		for (int i = 241; i <= 248; i++) {
			t[i] = 2;
		}
		// 249 → 3
		t[249] = 3;
		// 250..255 → 4..9
		for (int i = 250; i <= 255; i++) {
			t[i] = i - 246; // 250→4, 255→9
		}
		return t;
	}

	/**
	 * Determines length of an encoded varint value by inspecting the first byte.
	 *
	 * @param a0 first byte of varint value
	 * @return decoded value
	 */
	public static int firstToLength(byte a0) {
		return FIRST_TO_LENGTH[a0 & 0xFF];
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
		// TODO: Optimise for quads and also call writeUnsigned
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

	public static void readQuadUnsigned(ByteBuffer bb, int[] indexMap, long[] values) {
		assert indexMap.length == 4;
		values[indexMap[0]] = readUnsigned(bb);
		values[indexMap[1]] = readUnsigned(bb);
		values[indexMap[2]] = readUnsigned(bb);
		values[indexMap[3]] = readUnsigned(bb);
	}

	/**
	 * Reads only the significant bytes of the given value in big-endian order.
	 *
	 * @param bb buffer for reading bytes
	 * @param n  number of significant bytes
	 */
	private static long readSignificantBits(ByteBuffer bb, int n) {
		if (bb.isDirect()) {
			return readSignificantBitsDirect(bb, n);
		} else {
			return readSignificantBitsHeap(bb, n);
		}
	}

	private static long readSignificantBitsDirect(ByteBuffer bb, int n) {
		return SignificantBytesBE.readDirect(bb, n);
	}

	private static long readSignificantBitsHeap(ByteBuffer bb, int n) {
		return SignificantBytesBE.read(bb, n);
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

		final int[] lengths;
		final Bytes.RegionComparator[] cmps;
		private final byte[] firstBytes;
		private final boolean[] shouldMatch;
		private MatchFn matcher;

		public GroupMatcher(ByteBuffer value, boolean[] shouldMatch) {
			assert shouldMatch.length == 4;
			this.lengths = new int[4];
			assert value.hasArray();
			byte[] valueArray = value.array();
			int baseOffset = value.arrayOffset();
			if (baseOffset != 0) {
				throw new AssertionError();
			}
			this.shouldMatch = shouldMatch;
			this.cmps = new Bytes.RegionComparator[4];
			this.firstBytes = new byte[4];

			// Loop is unrolled for performance. Do not change back to a loop, do not extract into method, unless you
			// benchmark with QueryBenchmark first!
			{
				byte fb = valueArray[0];
				firstBytes[0] = fb;
				int len = firstToLength(fb);
				lengths[0] = len;

				cmps[0] = Bytes.capturedComparator(valueArray, 0, len);

				baseOffset += len;
			}
			{
				byte fb = valueArray[baseOffset];
				firstBytes[1] = fb;
				int len = firstToLength(fb);
				lengths[1] = len;

				cmps[1] = Bytes.capturedComparator(valueArray, baseOffset, len);

				baseOffset += len;
			}
			{
				byte fb = valueArray[baseOffset];
				firstBytes[2] = fb;
				int len = firstToLength(fb);
				lengths[2] = len;

				cmps[2] = Bytes.capturedComparator(valueArray, baseOffset, len);

				baseOffset += len;
			}
			{
				byte fb = valueArray[baseOffset];
				firstBytes[3] = fb;
				int len = firstToLength(fb);
				lengths[3] = len;

				cmps[3] = Bytes.capturedComparator(valueArray, baseOffset, len);
			}

		}

		public GroupMatcher(ByteBuffer value, boolean a, boolean b, boolean c, boolean d, boolean e) {
			this(value, new boolean[] { a, b, c, d, e });
		}

		public boolean matches(ByteBuffer other) {
			if (this.matcher == null) {
				this.matcher = selectMatcher(shouldMatch);
			}

			return matcher.matches(other);
		}

		@FunctionalInterface
		private interface MatchFn {
			boolean matches(ByteBuffer other);
		}

		private MatchFn selectMatcher(boolean[] shouldMatch) {
			byte mask = 0;
			if (shouldMatch[0]) {
				mask |= 0b0001;
			}
			if (shouldMatch[1]) {
				mask |= 0b0010;
			}
			if (shouldMatch[2]) {
				mask |= 0b0100;
			}
			if (shouldMatch[3]) {
				mask |= 0b1000;
			}

			switch (mask) {
			case 0b0000:
				return this::match0000;
			case 0b0001:
				return this::match0001;
			case 0b0010:
				return this::match0010;
			case 0b0011:
				return this::match0011;
			case 0b0100:
				return this::match0100;
			case 0b0101:
				return this::match0101;
			case 0b0110:
				return this::match0110;
			case 0b0111:
				return this::match0111;
			case 0b1000:
				return this::match1000;
			case 0b1001:
				return this::match1001;
			case 0b1010:
				return this::match1010;
			case 0b1011:
				return this::match1011;
			case 0b1100:
				return this::match1100;
			case 0b1101:
				return this::match1101;
			case 0b1110:
				return this::match1110;
			case 0b1111:
				return this::match1111;
			default:
				throw new IllegalStateException("Unsupported matcher mask: " + mask);
			}
		}

		private boolean match0000(ByteBuffer other) {
			return true;
		}

		private boolean match0001(ByteBuffer other) {
			byte otherFirst0 = other.get(0);
			if (firstBytes[0] == otherFirst0) {
				return cmps[0].compare(otherFirst0, other, 0) == 0;
			}
			return false;
		}

		private boolean match0010(ByteBuffer other) {
			int pos = 0;
			pos += skipAhead(other, pos);

			byte otherFirst1 = other.get(pos);
			if (firstBytes[1] == otherFirst1) {
				return cmps[1].compare(otherFirst1, other, pos) == 0;
			}
			return false;
		}

		private boolean match0011(ByteBuffer other) {
			byte otherFirst0 = other.get(0);
			if (firstBytes[0] == otherFirst0) {
				if (cmps[0].compare(otherFirst0, other, 0) == 0) {
					int pos = lengths[0];
					byte otherFirst1 = other.get(pos);
					if (firstBytes[1] == otherFirst1) {
						return cmps[1].compare(otherFirst1, other, pos) == 0;
					}
				}
			}

			return false;
		}

		private boolean match0100(ByteBuffer other) {
			int pos = 0;
			pos += skipAhead(other, pos);
			pos += skipAhead(other, pos);

			byte otherFirst2 = other.get(pos);
			if (firstBytes[2] == otherFirst2) {
				return cmps[2].compare(otherFirst2, other, pos) == 0;
			}
			return false;
		}

		private boolean match0101(ByteBuffer other) {
			int pos;

			byte otherFirst0 = other.get(0);
			if (firstBytes[0] == otherFirst0) {
				if (cmps[0].compare(otherFirst0, other, 0) == 0) {
					pos = lengths[0];
				} else {
					return false;
				}
			} else {
				return false;
			}
			pos += skipAhead(other, pos);

			byte otherFirst2 = other.get(pos);
			if (firstBytes[2] == otherFirst2) {
				return cmps[2].compare(otherFirst2, other, pos) == 0;
			}
			return false;
		}

		private boolean match0110(ByteBuffer other) {
			int pos = 0;
			pos += skipAhead(other, pos);

			byte otherFirst1 = other.get(pos);
			if (firstBytes[1] == otherFirst1) {
				if (cmps[1].compare(otherFirst1, other, pos) == 0) {
					pos += lengths[1];
				} else {
					return false;
				}
			} else {
				return false;
			}

			byte otherFirst2 = other.get(pos);
			if (firstBytes[2] == otherFirst2) {
				return cmps[2].compare(otherFirst2, other, pos) == 0;
			}
			return false;
		}

		private int skipAhead(ByteBuffer other, int pos) {
			return firstToLength(other.get(pos));
		}

		private boolean match0111(ByteBuffer other) {
			int pos;

			byte otherFirst0 = other.get(0);
			if (firstBytes[0] == otherFirst0) {
				if (cmps[0].compare(otherFirst0, other, 0) == 0) {
					pos = lengths[0];
				} else {
					return false;
				}
			} else {
				return false;
			}

			byte otherFirst1 = other.get(pos);
			if (firstBytes[1] == otherFirst1) {
				if (cmps[1].compare(otherFirst1, other, pos) == 0) {
					pos += lengths[1];
				} else {
					return false;
				}
			} else {
				return false;
			}

			byte otherFirst2 = other.get(pos);
			if (firstBytes[2] == otherFirst2) {
				return cmps[2].compare(otherFirst2, other, pos) == 0;
			}
			return false;
		}

		private boolean match1000(ByteBuffer other) {
			int pos = 0;
			pos += skipAhead(other, pos);
			pos += skipAhead(other, pos);
			pos += skipAhead(other, pos);

			byte otherFirst3 = other.get(pos);
			if (firstBytes[3] == otherFirst3) {
				return cmps[3].compare(otherFirst3, other, pos) == 0;
			}
			return false;
		}

		private boolean match1001(ByteBuffer other) {
			int pos;

			byte otherFirst0 = other.get(0);
			if (firstBytes[0] == otherFirst0) {
				if (cmps[0].compare(otherFirst0, other, 0) == 0) {
					pos = lengths[0];
				} else {
					return false;
				}
			} else {
				return false;
			}
			pos += skipAhead(other, pos);
			pos += skipAhead(other, pos);

			byte otherFirst3 = other.get(pos);
			if (firstBytes[3] == otherFirst3) {
				return cmps[3].compare(otherFirst3, other, pos) == 0;
			}
			return false;
		}

		private boolean match1010(ByteBuffer other) {
			int pos = 0;
			pos += skipAhead(other, pos);
			byte otherFirst1 = other.get(pos);
			if (firstBytes[1] == otherFirst1) {
				if (cmps[1].compare(otherFirst1, other, pos) == 0) {
					pos += lengths[1];
				} else {
					return false;
				}
			} else {
				return false;
			}

			pos += skipAhead(other, pos);

			byte otherFirst3 = other.get(pos);
			if (firstBytes[3] == otherFirst3) {
				return cmps[3].compare(otherFirst3, other, pos) == 0;
			}
			return false;
		}

		private boolean match1011(ByteBuffer other) {
			int pos;

			byte otherFirst0 = other.get(0);
			if (firstBytes[0] == otherFirst0) {
				if (cmps[0].compare(otherFirst0, other, 0) == 0) {
					pos = lengths[0];
				} else {
					return false;
				}
			} else {
				return false;
			}

			byte otherFirst1 = other.get(pos);
			if (firstBytes[1] == otherFirst1) {
				if (cmps[1].compare(otherFirst1, other, pos) == 0) {
					pos += lengths[1];
				} else {
					return false;
				}
			} else {
				return false;
			}

			pos += skipAhead(other, pos);

			byte otherFirst3 = other.get(pos);
			if (firstBytes[3] == otherFirst3) {
				return cmps[3].compare(otherFirst3, other, pos) == 0;
			}
			return false;
		}

		private boolean match1100(ByteBuffer other) {
			int pos = 0;
			pos += skipAhead(other, pos);
			pos += skipAhead(other, pos);

			byte otherFirst2 = other.get(pos);
			if (firstBytes[2] == otherFirst2) {
				if (cmps[2].compare(otherFirst2, other, pos) == 0) {
					pos += lengths[2];
				} else {
					return false;
				}
			} else {
				return false;
			}

			byte otherFirst3 = other.get(pos);
			if (firstBytes[3] == otherFirst3) {
				return cmps[3].compare(otherFirst3, other, pos) == 0;
			}
			return false;
		}

		private boolean match1101(ByteBuffer other) {
			int pos;

			byte otherFirst0 = other.get(0);
			if (firstBytes[0] == otherFirst0) {
				if (cmps[0].compare(otherFirst0, other, 0) == 0) {
					pos = lengths[0];
				} else {
					return false;
				}
			} else {
				return false;
			}
			pos += skipAhead(other, pos);

			byte otherFirst2 = other.get(pos);
			if (firstBytes[2] == otherFirst2) {
				if (cmps[2].compare(otherFirst2, other, pos) == 0) {
					pos += lengths[2];
				} else {
					return false;
				}
			} else {
				return false;
			}

			byte otherFirst3 = other.get(pos);
			if (firstBytes[3] == otherFirst3) {
				return cmps[3].compare(otherFirst3, other, pos) == 0;
			}
			return false;
		}

		private boolean match1110(ByteBuffer other) {
			int pos = 0;
			pos += skipAhead(other, pos);

			byte otherFirst1 = other.get(pos);
			if (firstBytes[1] == otherFirst1) {
				if (cmps[1].compare(otherFirst1, other, pos) == 0) {
					pos += lengths[1];
				} else {
					return false;
				}
			} else {
				return false;
			}

			byte otherFirst2 = other.get(pos);
			if (firstBytes[2] == otherFirst2) {
				if (cmps[2].compare(otherFirst2, other, pos) == 0) {
					pos += lengths[2];
				} else {
					return false;
				}
			} else {
				return false;
			}

			byte otherFirst3 = other.get(pos);
			if (firstBytes[3] == otherFirst3) {
				return cmps[3].compare(otherFirst3, other, pos) == 0;
			}
			return false;
		}

		private boolean match1111(ByteBuffer other) {
			int pos;
			byte otherFirst0 = other.get(0);
			if (firstBytes[0] == otherFirst0) {
				if (cmps[0].compare(otherFirst0, other, 0) == 0) {
					pos = lengths[0];
				} else {
					return false;
				}
			} else {
				return false;
			}

			byte otherFirst1 = other.get(pos);
			if (firstBytes[1] == otherFirst1) {
				if (cmps[1].compare(otherFirst1, other, pos) == 0) {
					pos += lengths[1];
				} else {
					return false;
				}
			} else {
				return false;
			}

			byte otherFirst2 = other.get(pos);
			if (firstBytes[2] == otherFirst2) {
				if (cmps[2].compare(otherFirst2, other, pos) == 0) {
					pos += lengths[2];
				} else {
					return false;
				}
			} else {
				return false;
			}

			byte otherFirst3 = other.get(pos);
			if (firstBytes[3] == otherFirst3) {
				return cmps[3].compare(otherFirst3, other, pos) == 0;
			}
			return false;
		}

	}
}
