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
package org.eclipse.rdf4j.sail.s3.storage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Encodes and decodes unsigned values using variable-length encoding.
 *
 * <p>
 * Uses the <a href="https://sqlite.org/src4/doc/trunk/www/varint.wiki">variable-length encoding of SQLite</a> which
 * preserves lexicographic ordering: smaller values use fewer bytes, and lexicographic byte order matches numeric order.
 * </p>
 *
 * <p>
 * Adapted from the LMDB Varint implementation with LMDB-specific dependencies removed (no SignificantBytesBE, no
 * GroupMatcher). All reads use heap-based byte-by-byte decoding.
 * </p>
 */
public final class Varint {

	static final byte[] ENCODED_LONG_MAX = new byte[] {
			(byte) 0xFF, // header: 8 payload bytes
			0x7F, // MSB of Long.MAX_VALUE
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
	};

	static final byte[] ENCODED_LONG_MAX_QUAD = new byte[] {
			(byte) 0xFF, 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF,
			(byte) 0xFF, 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF,
			(byte) 0xFF, 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF,
			(byte) 0xFF, 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF
	};

	static final byte[] ALL_ZERO_QUAD = new byte[] { 0, 0, 0, 0 };

	private Varint() {
	}

	/**
	 * Encodes a value using the variable-length encoding of SQLite.
	 *
	 * <p>
	 * The encoding has the following properties:
	 * <ol>
	 * <li>Smaller (and more common) values use fewer bytes.</li>
	 * <li>The length of any varint can be determined by looking at just the first byte.</li>
	 * <li>Lexicographical and numeric ordering for varints are the same.</li>
	 * </ol>
	 * </p>
	 *
	 * @param bb    buffer for writing bytes
	 * @param value value to encode
	 */
	public static void writeUnsigned(final ByteBuffer bb, final long value) {
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
			long v = value - 240;
			final ByteOrder prev = bb.order();
			if (prev != ByteOrder.BIG_ENDIAN) {
				bb.order(ByteOrder.BIG_ENDIAN);
			}
			try {
				int hi = (int) (v >>> 8) + 241;
				int lo = (int) (v & 0xFF);
				bb.putShort((short) ((hi << 8) | lo));
			} finally {
				if (prev != ByteOrder.BIG_ENDIAN) {
					bb.order(prev);
				}
			}
		} else if (value <= 67823) {
			long v = value - 2288;
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
			int bytes = descriptor(value) + 1;
			bb.put((byte) (250 + (bytes - 3)));
			writeSignificantBits(bb, value, bytes);
		}
	}

	private static void writeSignificantBits(ByteBuffer bb, long value, int bytes) {
		final ByteOrder prev = bb.order();
		if (prev != ByteOrder.BIG_ENDIAN) {
			bb.order(ByteOrder.BIG_ENDIAN);
		}
		try {
			int i = bytes;

			if ((i & 1) != 0) {
				bb.put((byte) (value >>> ((i - 1) * 8)));
				i--;
			}

			if (i == 8) {
				bb.putLong(value);
				return;
			}

			if (i >= 4) {
				int shift = (i - 4) * 8;
				bb.putInt((int) (value >>> shift));
				i -= 4;
			}

			while (i >= 2) {
				int shift = (i - 2) * 8;
				bb.putShort((short) (value >>> shift));
				i -= 2;
			}
		} finally {
			if (prev != ByteOrder.BIG_ENDIAN) {
				bb.order(prev);
			}
		}
	}

	/**
	 * Calculates required length in bytes to encode the given long value.
	 *
	 * @param value the value
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
	 * Calculates required length in bytes to encode a list of four long values.
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
	 * The number of bytes required to represent the given number minus one.
	 */
	private static byte descriptor(long value) {
		return value == 0 ? 0 : (byte) (7 - Long.numberOfLeadingZeros(value) / 8);
	}

	/** Lookup: lead byte (0..255) -> number of additional bytes (0..8). */
	private static final byte[] VARINT_EXTRA_BYTES = buildVarintExtraBytes();

	private static byte[] buildVarintExtraBytes() {
		final byte[] t = new byte[256];
		for (int i = 0; i <= 240; i++) {
			t[i] = 0;
		}
		for (int i = 241; i <= 248; i++) {
			t[i] = 1;
		}
		t[249] = 2;
		for (int i = 250; i <= 255; i++) {
			t[i] = (byte) (i - 247);
		}
		return t;
	}

	/**
	 * Decodes a value using SQLite's variable-length integer encoding.
	 *
	 * @param bb buffer for reading bytes
	 * @return decoded value
	 * @throws IllegalArgumentException if encoded varint is longer than 9 bytes
	 */
	public static long readUnsigned(ByteBuffer bb) throws IllegalArgumentException {
		int a0 = bb.get() & 0xFF;

		if (a0 <= 240) {
			return a0;
		} else if (a0 <= 248) {
			int a1 = bb.get() & 0xFF;
			return 240L + ((long) (a0 - 241) << 8) + a1;
		} else if (a0 == 249) {
			int a1 = bb.get() & 0xFF;
			int a2 = bb.get() & 0xFF;
			return 2288L + ((long) a1 << 8) + a2;
		} else {
			int bytes = a0 - 250 + 3;
			return readSignificantBits(bb, bytes);
		}
	}

	/**
	 * Skips over a single varint in the buffer.
	 *
	 * @param bb buffer to advance
	 */
	public static void skipUnsigned(ByteBuffer bb) {
		final int a0 = bb.get() & 0xFF;
		if (a0 <= 240) {
			return;
		}
		final int extra = VARINT_EXTRA_BYTES[a0];
		bb.position(bb.position() + extra);
	}

	/**
	 * Decodes a value at an absolute position without advancing the buffer position.
	 *
	 * @param bb  buffer for reading bytes
	 * @param pos absolute position in the buffer
	 * @return decoded value
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
			int a2 = bb.get(pos + 2) & 0xFF;
			return 2288 + 256 * a1 + a2;
		} else {
			int bytes = a0 - 250 + 3;
			return readSignificantBitsAbsolute(bb, pos + 1, bytes);
		}
	}

	private static final int[] FIRST_TO_LENGTH = buildFirstToLength();

	private static int[] buildFirstToLength() {
		int[] t = new int[256];
		for (int i = 0; i <= 240; i++) {
			t[i] = 1;
		}
		for (int i = 241; i <= 248; i++) {
			t[i] = 2;
		}
		t[249] = 3;
		for (int i = 250; i <= 255; i++) {
			t[i] = i - 246;
		}
		return t;
	}

	/**
	 * Determines length of an encoded varint value by inspecting the first byte.
	 *
	 * @param a0 first byte of varint value
	 * @return total length in bytes
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
		for (long value : values) {
			writeUnsigned(bb, value);
		}
	}

	/**
	 * Decodes multiple values using variable-length encoding from the given buffer.
	 *
	 * @param bb     buffer for reading bytes
	 * @param values array for the result values
	 */
	public static void readListUnsigned(ByteBuffer bb, long[] values) {
		for (int i = 0; i < values.length; i++) {
			values[i] = readUnsigned(bb);
		}
	}

	/**
	 * Decodes exactly 4 values (a quad) from the given buffer.
	 *
	 * @param bb     buffer for reading bytes
	 * @param values array of length 4 for the result values
	 */
	public static void readQuadUnsigned(ByteBuffer bb, long[] values) {
		values[0] = readUnsigned(bb);
		values[1] = readUnsigned(bb);
		values[2] = readUnsigned(bb);
		values[3] = readUnsigned(bb);
	}

	/**
	 * Decodes multiple values using variable-length encoding, placing each value into the position specified by the
	 * index map.
	 *
	 * @param bb       buffer for reading bytes
	 * @param indexMap map for indexes of values within values array
	 * @param values   array for the result values
	 */
	public static void readListUnsigned(ByteBuffer bb, int[] indexMap, long[] values) {
		for (int i = 0; i < values.length; i++) {
			values[indexMap[i]] = readUnsigned(bb);
		}
	}

	/**
	 * Decodes exactly 4 values (a quad) from the given buffer, placing each value at the index specified by the map.
	 *
	 * @param bb       buffer for reading bytes
	 * @param indexMap map for indexes of values within values array
	 * @param values   array of length 4 for the result values
	 */
	public static void readQuadUnsigned(ByteBuffer bb, int[] indexMap, long[] values) {
		values[indexMap[0]] = readUnsigned(bb);
		values[indexMap[1]] = readUnsigned(bb);
		values[indexMap[2]] = readUnsigned(bb);
		values[indexMap[3]] = readUnsigned(bb);
	}

	/**
	 * Reads n significant bytes from the buffer in big-endian order (byte-by-byte, heap-safe).
	 */
	private static long readSignificantBits(ByteBuffer bb, int n) {
		long value = 0;
		for (int i = 0; i < n; i++) {
			value = (value << 8) | (bb.get() & 0xFFL);
		}
		return value;
	}

	/**
	 * Reads n significant bytes from the buffer at an absolute position in big-endian order.
	 */
	private static long readSignificantBitsAbsolute(ByteBuffer bb, int pos, int bytes) {
		long value = 0;
		for (int i = 0; i < bytes; i++) {
			value = (value << 8) | (bb.get(pos + i) & 0xFFL);
		}
		return value;
	}
}
