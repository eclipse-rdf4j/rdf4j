/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.s3.storage;

import java.util.Base64;

/**
 * A simple bit-array bloom filter for long values. Uses two independent hash functions derived from a single
 * murmur3-style hash to set/test bits.
 *
 * <p>
 * Each Parquet file gets one bloom filter keyed on the <b>leading component</b> of the file's sort order (e.g. subject
 * IDs for SPOC files, object IDs for OPSC files, context IDs for CSPO files).
 * </p>
 */
public final class BloomFilter {

	private static final int MIN_BITS = 64;
	private final long[] bits;
	private final int numBits;
	private final int numHashFunctions;

	/**
	 * Creates a bloom filter sized for the expected number of insertions and false positive probability.
	 *
	 * @param expectedInsertions expected number of distinct elements
	 * @param fpp                desired false positive probability (e.g. 0.01 for 1%)
	 */
	public BloomFilter(int expectedInsertions, double fpp) {
		if (expectedInsertions <= 0) {
			expectedInsertions = 1;
		}
		this.numBits = Math.max(MIN_BITS, optimalNumBits(expectedInsertions, fpp));
		this.numHashFunctions = optimalNumHashFunctions(expectedInsertions, numBits);
		this.bits = new long[(numBits + 63) >>> 6];
	}

	private BloomFilter(long[] bits, int numBits, int numHashFunctions) {
		this.bits = bits;
		this.numBits = numBits;
		this.numHashFunctions = numHashFunctions;
	}

	/**
	 * Adds a value to the bloom filter.
	 */
	public void add(long value) {
		long hash1 = murmurHash(value);
		long hash2 = murmurHash(value ^ 0x9E3779B97F4A7C15L);
		for (int i = 0; i < numHashFunctions; i++) {
			int bit = (int) (((hash1 + (long) i * hash2) & Long.MAX_VALUE) % numBits);
			bits[bit >>> 6] |= 1L << (bit & 63);
		}
	}

	/**
	 * Tests whether a value might be in the set.
	 *
	 * @return {@code true} if the value might be present; {@code false} if it is definitely not present
	 */
	public boolean mightContain(long value) {
		long hash1 = murmurHash(value);
		long hash2 = murmurHash(value ^ 0x9E3779B97F4A7C15L);
		for (int i = 0; i < numHashFunctions; i++) {
			int bit = (int) (((hash1 + (long) i * hash2) & Long.MAX_VALUE) % numBits);
			if ((bits[bit >>> 6] & (1L << (bit & 63))) == 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Serializes this bloom filter to a Base64-encoded string for JSON storage.
	 */
	public String toBase64() {
		// Format: [numBits (4 bytes)] [numHashFunctions (4 bytes)] [bits array (8 bytes each)]
		byte[] data = new byte[8 + bits.length * 8];
		writeInt(data, 0, numBits);
		writeInt(data, 4, numHashFunctions);
		for (int i = 0; i < bits.length; i++) {
			writeLong(data, 8 + i * 8, bits[i]);
		}
		return Base64.getEncoder().encodeToString(data);
	}

	/**
	 * Deserializes a bloom filter from a Base64-encoded string.
	 */
	public static BloomFilter fromBase64(String encoded) {
		byte[] data = Base64.getDecoder().decode(encoded);
		int numBits = readInt(data, 0);
		int numHash = readInt(data, 4);
		int arrayLen = (data.length - 8) / 8;
		long[] bits = new long[arrayLen];
		for (int i = 0; i < arrayLen; i++) {
			bits[i] = readLong(data, 8 + i * 8);
		}
		return new BloomFilter(bits, numBits, numHash);
	}

	private static long murmurHash(long value) {
		long h = value;
		h ^= h >>> 33;
		h *= 0xFF51AFD7ED558CCDL;
		h ^= h >>> 33;
		h *= 0xC4CEB9FE1A85EC53L;
		h ^= h >>> 33;
		return h;
	}

	private static int optimalNumBits(int n, double fpp) {
		return (int) (-n * Math.log(fpp) / (Math.log(2) * Math.log(2)));
	}

	private static int optimalNumHashFunctions(int n, int m) {
		return Math.max(1, (int) Math.round((double) m / n * Math.log(2)));
	}

	private static void writeInt(byte[] buf, int offset, int value) {
		buf[offset] = (byte) (value >>> 24);
		buf[offset + 1] = (byte) (value >>> 16);
		buf[offset + 2] = (byte) (value >>> 8);
		buf[offset + 3] = (byte) value;
	}

	private static void writeLong(byte[] buf, int offset, long value) {
		buf[offset] = (byte) (value >>> 56);
		buf[offset + 1] = (byte) (value >>> 48);
		buf[offset + 2] = (byte) (value >>> 40);
		buf[offset + 3] = (byte) (value >>> 32);
		buf[offset + 4] = (byte) (value >>> 24);
		buf[offset + 5] = (byte) (value >>> 16);
		buf[offset + 6] = (byte) (value >>> 8);
		buf[offset + 7] = (byte) value;
	}

	private static int readInt(byte[] buf, int offset) {
		return ((buf[offset] & 0xFF) << 24) | ((buf[offset + 1] & 0xFF) << 16)
				| ((buf[offset + 2] & 0xFF) << 8) | (buf[offset + 3] & 0xFF);
	}

	private static long readLong(byte[] buf, int offset) {
		return ((long) (buf[offset] & 0xFF) << 56) | ((long) (buf[offset + 1] & 0xFF) << 48)
				| ((long) (buf[offset + 2] & 0xFF) << 40) | ((long) (buf[offset + 3] & 0xFF) << 32)
				| ((long) (buf[offset + 4] & 0xFF) << 24) | ((long) (buf[offset + 5] & 0xFF) << 16)
				| ((long) (buf[offset + 6] & 0xFF) << 8) | (long) (buf[offset + 7] & 0xFF);
	}
}
