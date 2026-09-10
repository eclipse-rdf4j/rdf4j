/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import java.util.Arrays;
import java.util.function.IntUnaryOperator;

import org.eclipse.rdf4j.common.annotation.Experimental;

@Experimental
final class PrimitiveHashJoinTable {
	final int keyWidth;
	final int payloadWidth;
	final IntUnaryOperator hashHook;
	long[] keys;
	byte[] occupied;
	byte[] fingerprints;
	int[] fullHashes;
	int[] heads;
	int[] tails;
	/** Duplicate-chain length per bucket — the §6.2 build-time chain statistic, maintained as rows arrive. */
	int[] chainCounts;
	int maxChainLength;
	int distinctKeys;
	boolean uniqueKeys = true;
	long[] payloads;
	int[] next;
	int payloadCount;
	private boolean sealed;

	PrimitiveHashJoinTable(int keyWidth, int payloadWidth) {
		this(keyWidth, payloadWidth, IntUnaryOperator.identity());
	}

	PrimitiveHashJoinTable(int keyWidth, int payloadWidth, IntUnaryOperator hashHook) {
		this.keyWidth = keyWidth;
		this.payloadWidth = payloadWidth;
		this.hashHook = hashHook;
		this.keys = new long[keyWidth * 32];
		this.occupied = new byte[32];
		this.fingerprints = new byte[32];
		this.fullHashes = new int[32];
		this.heads = new int[32];
		this.tails = new int[32];
		Arrays.fill(heads, -1);
		Arrays.fill(tails, -1);
		this.chainCounts = new int[32];
		this.payloads = new long[Math.max(32, payloadWidth * 32)];
		this.next = new int[32];
		Arrays.fill(next, -1);
	}

	/** Physical data bytes across every backing array (headers excluded; the admission estimate absorbs them). */
	long byteSize() {
		return 8L * keys.length + occupied.length + fingerprints.length + 4L * fullHashes.length + 4L * heads.length
				+ 4L * tails.length + 4L * chainCounts.length + 8L * payloads.length + 4L * next.length;
	}

	/** Longest duplicate chain observed at build time — the §6.2 skew statistic. */
	int maxChainLength() {
		return maxChainLength;
	}

	/** Mean duplicate-chain length (payload rows per distinct key), the §6.3 conditional-fanout ingredient. */
	double meanChainLength() {
		return distinctKeys == 0 ? 0D : (double) payloadCount / distinctKeys;
	}

	/** The duplicate-chain length behind one bucket — the chain handle's count, consumable without walking it. */
	int chainLength(int bucket) {
		return chainCounts[bucket];
	}

	/** Group views are published only after the existing build has completed. */
	void seal() {
		sealed = true;
	}

	boolean isSealed() {
		return sealed;
	}

	/** Positioned lookup: retain the bucket so consumers can select count, payload or tuple-group demand. */
	int lookupBucket(long[] row, int[] keySlots) {
		int bucket = find(row, keySlots, hash(row, keySlots));
		return occupied[bucket] == 0 ? -1 : bucket;
	}

	int lookupPreparedBucket(NativeBatch batch, int row, int[] keySlots, int hash, int bucket) {
		int mask = occupied.length - 1;
		byte fingerprint = fingerprint(hash);
		while (occupied[bucket] != 0) {
			if (fingerprints[bucket] == fingerprint && fullHashes[bucket] == hash
					&& matches(batch, row, keySlots, bucket))
				return bucket;
			bucket = (bucket + 1) & mask;
		}
		return -1;
	}

	void add(long[] row, int[] keySlots, int[] payloadSlots) {
		if (sealed)
			throw new IllegalStateException("hash table is borrowed and immutable");
		if ((distinctKeys + 1) * 4 > occupied.length * 3) {
			growBuckets();
		}
		int hash = hash(row, keySlots);
		int bucket = find(row, keySlots, hash);
		if (occupied[bucket] == 0) {
			occupied[bucket] = 1;
			fingerprints[bucket] = fingerprint(hash);
			fullHashes[bucket] = hash;
			int offset = bucket * keyWidth;
			for (int i = 0; i < keyWidth; i++) {
				keys[offset + i] = row[keySlots[i]];
			}
			distinctKeys++;
		} else {
			uniqueKeys = false;
		}
		ensurePayloadCapacity(payloadCount + 1);
		int payload = payloadCount++;
		for (int i = 0; i < payloadWidth; i++) {
			payloads[payload * payloadWidth + i] = row[payloadSlots[i]];
		}
		if (heads[bucket] < 0) {
			heads[bucket] = payload;
		} else {
			next[tails[bucket]] = payload;
		}
		tails[bucket] = payload;
		int length = ++chainCounts[bucket];
		if (length > maxChainLength) {
			maxChainLength = length;
		}
	}

	/**
	 * Chain-count lookup for probes that consume the duplicate chain as a multiplicity instead of walking it: the
	 * resolved bucket's chain length, or 0 on a miss.
	 */
	int lookupPreparedChainCount(NativeBatch batch, int row, int[] keySlots, int hash, int bucket) {
		int mask = occupied.length - 1;
		byte fingerprint = fingerprint(hash);
		while (occupied[bucket] != 0) {
			if (fingerprints[bucket] == fingerprint && fullHashes[bucket] == hash
					&& matches(batch, row, keySlots, bucket)) {
				return chainCounts[bucket];
			}
			bucket = (bucket + 1) & mask;
		}
		return 0;
	}

	int lookup(NativeBatch batch, int row, int[] keySlots) {
		int hash = hash(batch, row, keySlots);
		int bucket = hash & (occupied.length - 1);
		return lookupPrepared(batch, row, keySlots, hash, bucket, heads[bucket]);
	}

	void hashBatch(NativeBatch batch, int[] rows, int rowCount, int[] keySlots, long[] hashState,
			int[] hashes) {
		Arrays.fill(hashState, 0, rowCount, 0x9e3779b97f4a7c15L);
		for (int keySlot : keySlots) {
			int columnOffset = keySlot * batch.capacity;
			for (int i = 0; i < rowCount; i++) {
				hashState[i] = mix(hashState[i] ^ batch.slots[columnOffset + rows[i]]);
			}
		}
		for (int i = 0; i < rowCount; i++) {
			hashes[i] = hashHook.applyAsInt((int) hashState[i]);
		}
	}

	void headBatch(int[] hashes, int rowCount, int[] buckets, int[] candidateHeads) {
		int mask = occupied.length - 1;
		for (int i = 0; i < rowCount; i++) {
			int bucket = hashes[i] & mask;
			buckets[i] = bucket;
			candidateHeads[i] = heads[bucket];
		}
	}

	int lookupPrepared(NativeBatch batch, int row, int[] keySlots, int hash, int bucket, int candidateHead) {
		int mask = occupied.length - 1;
		byte fingerprint = fingerprint(hash);
		boolean initialBucket = true;
		while (occupied[bucket] != 0) {
			if (fingerprints[bucket] == fingerprint && fullHashes[bucket] == hash
					&& matches(batch, row, keySlots, bucket)) {
				return initialBucket ? candidateHead : heads[bucket];
			}
			bucket = (bucket + 1) & mask;
			initialBucket = false;
		}
		return -1;
	}

	long payload(int payload, int offset) {
		return payloads[payload * payloadWidth + offset];
	}

	int find(long[] row, int[] keySlots, int hash) {
		int mask = occupied.length - 1;
		int bucket = hash & mask;
		byte fingerprint = fingerprint(hash);
		while (occupied[bucket] != 0 && (fingerprints[bucket] != fingerprint || fullHashes[bucket] != hash
				|| !matches(row, keySlots, bucket))) {
			bucket = (bucket + 1) & mask;
		}
		return bucket;
	}

	boolean matches(long[] row, int[] keySlots, int bucket) {
		int offset = bucket * keyWidth;
		for (int i = 0; i < keyWidth; i++) {
			if (keys[offset + i] != row[keySlots[i]]) {
				return false;
			}
		}
		return true;
	}

	boolean matches(NativeBatch batch, int row, int[] keySlots, int bucket) {
		int offset = bucket * keyWidth;
		for (int i = 0; i < keyWidth; i++) {
			if (keys[offset + i] != batch.get(keySlots[i], row)) {
				return false;
			}
		}
		return true;
	}

	int hash(long[] row, int[] keySlots) {
		long hash = 0x9e3779b97f4a7c15L;
		for (int keySlot : keySlots) {
			hash = mix(hash ^ row[keySlot]);
		}
		return hashHook.applyAsInt((int) hash);
	}

	int hash(NativeBatch batch, int row, int[] keySlots) {
		long hash = 0x9e3779b97f4a7c15L;
		for (int keySlot : keySlots) {
			hash = mix(hash ^ batch.get(keySlot, row));
		}
		return hashHook.applyAsInt((int) hash);
	}

	static byte fingerprint(int hash) {
		return (byte) (hash ^ (hash >>> 8) ^ (hash >>> 16) ^ (hash >>> 24));
	}

	static long mix(long value) {
		value ^= value >>> 33;
		value *= 0xff51afd7ed558ccdL;
		value ^= value >>> 33;
		value *= 0xc4ceb9fe1a85ec53L;
		return value ^ (value >>> 33);
	}

	void ensurePayloadCapacity(int requiredRows) {
		if (requiredRows <= next.length) {
			return;
		}
		int oldLength = next.length;
		int newLength = oldLength << 1;
		while (newLength < requiredRows) {
			newLength <<= 1;
		}
		next = Arrays.copyOf(next, newLength);
		Arrays.fill(next, oldLength, newLength, -1);
		payloads = Arrays.copyOf(payloads, Math.max(newLength, payloadWidth * newLength));
	}

	void growBuckets() {
		long[] oldKeys = keys;
		byte[] oldOccupied = occupied;
		byte[] oldFingerprints = fingerprints;
		int[] oldFullHashes = fullHashes;
		int[] oldHeads = heads;
		int[] oldTails = tails;
		int[] oldChainCounts = chainCounts;
		int newLength = oldOccupied.length << 1;
		keys = new long[keyWidth * newLength];
		occupied = new byte[newLength];
		fingerprints = new byte[newLength];
		fullHashes = new int[newLength];
		heads = new int[newLength];
		tails = new int[newLength];
		chainCounts = new int[newLength];
		Arrays.fill(heads, -1);
		Arrays.fill(tails, -1);
		for (int oldBucket = 0; oldBucket < oldOccupied.length; oldBucket++) {
			if (oldOccupied[oldBucket] == 0) {
				continue;
			}
			int mask = newLength - 1;
			int hash = oldFullHashes[oldBucket];
			int bucket = hash & mask;
			while (occupied[bucket] != 0) {
				bucket = (bucket + 1) & mask;
			}
			occupied[bucket] = 1;
			fingerprints[bucket] = oldFingerprints[oldBucket];
			fullHashes[bucket] = hash;
			System.arraycopy(oldKeys, oldBucket * keyWidth, keys, bucket * keyWidth, keyWidth);
			heads[bucket] = oldHeads[oldBucket];
			tails[bucket] = oldTails[oldBucket];
			chainCounts[bucket] = oldChainCounts[oldBucket];
		}
	}
}
