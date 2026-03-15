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

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.List;

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
	 * Builds a bloom filter for the leading component of the given sort order.
	 */
	public static BloomFilter buildForEntries(List<QuadEntry> entries, String sortSuffix) {
		BloomFilter bloom = new BloomFilter(Math.max(1, entries.size()), 0.01);
		for (QuadEntry entry : entries) {
			bloom.add(leadingComponent(entry, sortSuffix));
		}
		return bloom;
	}

	/**
	 * Extracts the leading component value from a quad entry based on the sort order suffix.
	 */
	static long leadingComponent(QuadEntry entry, String sortSuffix) {
		switch (sortSuffix.charAt(0)) {
		case 's':
			return entry.subject;
		case 'o':
			return entry.object;
		case 'c':
			return entry.context;
		case 'p':
			return entry.predicate;
		default:
			return entry.subject;
		}
	}

	/**
	 * Extracts the leading component value from raw quad IDs based on the sort order suffix.
	 */
	static long leadingComponent(long s, long p, long o, long c, String sortOrder) {
		if (sortOrder == null) {
			return -1;
		}
		switch (sortOrder.charAt(0)) {
		case 's':
			return s;
		case 'p':
			return p;
		case 'o':
			return o;
		case 'c':
			return c;
		default:
			return -1;
		}
	}

	/**
	 * Serializes this bloom filter to a Base64-encoded string for JSON storage.
	 */
	public String toBase64() {
		// Format: [numBits (4 bytes)] [numHashFunctions (4 bytes)] [bits array (8 bytes each)]
		ByteBuffer buf = ByteBuffer.allocate(8 + bits.length * 8);
		buf.putInt(numBits);
		buf.putInt(numHashFunctions);
		for (long word : bits) {
			buf.putLong(word);
		}
		return Base64.getEncoder().encodeToString(buf.array());
	}

	/**
	 * Deserializes a bloom filter from a Base64-encoded string.
	 */
	public static BloomFilter fromBase64(String encoded) {
		ByteBuffer buf = ByteBuffer.wrap(Base64.getDecoder().decode(encoded));
		int numBits = buf.getInt();
		int numHash = buf.getInt();
		int arrayLen = buf.remaining() / 8;
		long[] bits = new long[arrayLen];
		for (int i = 0; i < arrayLen; i++) {
			bits[i] = buf.getLong();
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

}
