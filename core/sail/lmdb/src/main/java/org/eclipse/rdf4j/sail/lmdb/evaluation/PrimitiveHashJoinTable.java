/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntUnaryOperator;

import org.eclipse.rdf4j.common.annotation.Experimental;

@Experimental
final class PrimitiveHashJoinTable {
	final int keyWidth;
	final int payloadWidth;
	final IntUnaryOperator hashHook;
	long[] keys;
	/** Low 32 bits: full hash. High 32 bits: first payload row + 1; zero word is empty. */
	long[] entries;
	/** Occupancy and seven hash bits share one byte, keeping negative scalar probes compact. */
	byte[] controls;
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
		if (keyWidth < 0 || payloadWidth < 0) {
			throw new IllegalArgumentException("negative hash table width");
		}
		this.keyWidth = keyWidth;
		this.payloadWidth = payloadWidth;
		this.hashHook = Objects.requireNonNull(hashHook);
		this.keys = new long[Math.multiplyExact(keyWidth, 32)];
		this.entries = new long[32];
		this.controls = new byte[32];
		this.tails = new int[32];
		this.chainCounts = new int[32];
		this.payloads = new long[Math.multiplyExact(payloadWidth, 32)];
		this.next = new int[32];
	}

	/** Backing-array data bytes; object/array headers, cursor scratch and inputs are excluded. */
	long byteSize() {
		return 8L * keys.length + 8L * entries.length + controls.length + 4L * tails.length + 4L * chainCounts.length
				+ 8L * payloads.length + 4L * next.length;
	}

	int capacity() {
		return entries.length;
	}

	boolean occupied(int bucket) {
		return entries[bucket] != 0L;
	}

	int head(int bucket) {
		return (int) (entries[bucket] >>> Integer.SIZE) - 1;
	}

	private static long entry(int hash, int head) {
		return (head + 1L) << Integer.SIZE | (hash & 0xffff_ffffL);
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
		return entries[bucket] == 0L ? -1 : bucket;
	}

	int lookupPreparedBucket(NativeBatch batch, int row, int[] keySlots, int hash, int bucket) {
		return lookupPreparedBucket(batch, row, keySlots, hash, bucket, entries[bucket]);
	}

	int lookupPreparedBucket(NativeBatch batch, int row, int[] keySlots, int hash, int bucket, long entry) {
		long[] index = entries;
		int mask = index.length - 1;
		while (entry != 0L) {
			if ((int) entry == hash && matches(batch, row, keySlots, bucket)) {
				return bucket;
			}
			bucket = (bucket + 1) & mask;
			entry = index[bucket];
		}
		return -1;
	}

	void add(long[] row, int[] keySlots, int[] payloadSlots) {
		if (sealed) {
			throw new IllegalStateException("hash table is borrowed and immutable");
		}
		int hash = hash(row, keySlots);
		int bucket = find(row, keySlots, hash);
		boolean inserted = entries[bucket] == 0L;
		// Duplicate payload rows do not consume a bucket. In particular a duplicate at 75% load
		// must not double the index and all key/statistic arrays.
		if (inserted && distinctKeys >= entries.length - (entries.length >>> 2)) {
			growBuckets();
			bucket = find(row, keySlots, hash);
		}
		ensurePayloadCapacity(Math.addExact(payloadCount, 1));
		int payload = payloadCount;
		int payloadAt = payload * payloadWidth;
		for (int i = 0; i < payloadWidth; i++) {
			payloads[payloadAt + i] = row[payloadSlots[i]];
		}
		if (inserted) {
			int offset = bucket * keyWidth;
			for (int i = 0; i < keyWidth; i++) {
				keys[offset + i] = row[keySlots[i]];
			}
			entries[bucket] = entry(hash, payload);
			controls[bucket] = control(hash);
			distinctKeys++;
		} else {
			uniqueKeys = false;
			next[tails[bucket]] = payload;
		}
		next[payload] = -1;
		tails[bucket] = payload;
		payloadCount = payload + 1;
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
		return lookupPreparedChainCount(batch, row, keySlots, hash, bucket, entries[bucket]);
	}

	int lookupPreparedChainCount(NativeBatch batch, int row, int[] keySlots, int hash, int bucket, long entry) {
		int found = lookupPreparedBucket(batch, row, keySlots, hash, bucket, entry);
		return found < 0 ? 0 : chainCounts[found];
	}

	int lookup(NativeBatch batch, int row, int[] keySlots) {
		if (keyWidth == 1 && keySlots.length == 1) {
			long key = batch.get(keySlots[0], row);
			return lookupScalarSingle(key, hashHook.applyAsInt((int) mix(0x9e3779b97f4a7c15L ^ key)));
		}
		int hash = hash(batch, row, keySlots);
		byte expected = control(hash);
		byte[] tags = controls;
		int mask = tags.length - 1;
		int bucket = hash & mask;
		byte tag;
		while ((tag = tags[bucket]) != 0) {
			if (tag == expected && (int) entries[bucket] == hash && matches(batch, row, keySlots, bucket)) {
				return head(bucket);
			}
			bucket = (bucket + 1) & mask;
		}
		return -1;
	}

	private int lookupScalarSingle(long key, int hash) {
		byte[] tags = controls;
		byte expected = control(hash), tag;
		int mask = tags.length - 1;
		int bucket = hash & mask;
		while ((tag = tags[bucket]) != 0) {
			if (tag == expected) {
				long entry = entries[bucket];
				if ((int) entry == hash && keys[bucket] == key) {
					return (int) (entry >>> Integer.SIZE) - 1;
				}
			}
			bucket = (bucket + 1) & mask;
		}
		return -1;
	}

	private static byte control(int hash) {
		return (byte) (0x80 | (hash >>> 25));
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
		int mask = entries.length - 1;
		for (int i = 0; i < rowCount; i++) {
			int bucket = hashes[i] & mask;
			buckets[i] = bucket;
			candidateHeads[i] = head(bucket);
		}
	}

	/**
	 * Prefetch first probe words. The table must not change before they are consumed. Hash scratch is dead after
	 * hashBatch(), so callers can reuse it here instead of allocating heads.
	 */
	void entryBatch(int[] hashes, int rowCount, int[] buckets, long[] candidates) {
		long[] index = entries;
		int mask = index.length - 1;
		for (int i = 0; i < rowCount; i++) {
			int bucket = hashes[i] & mask;
			buckets[i] = bucket;
			candidates[i] = index[bucket];
		}
	}

	int lookupPrepared(NativeBatch batch, int row, int[] keySlots, int hash, int bucket, int candidateHead) {
		return lookupPrepared(batch, row, keySlots, hash, bucket, entries[bucket]);
	}

	int lookupPrepared(NativeBatch batch, int row, int[] keySlots, int hash, int bucket, long entry) {
		if (keyWidth == 1) {
			return lookupSingle(batch.get(keySlots[0], row), hash, bucket, entry);
		}
		long[] index = entries;
		int mask = index.length - 1;
		while ((int) entry != hash || !matches(batch, row, keySlots, bucket)) {
			if (entry == 0L) {
				return -1;
			}
			bucket = (bucket + 1) & mask;
			entry = index[bucket];
		}
		// The empty word also decodes to -1 (including a zero-hash/zero-key probe).
		return (int) (entry >>> Integer.SIZE) - 1;
	}

	/** A scalar key needs neither a tuple loop nor repeated batch column addressing. */
	private int lookupSingle(long key, int hash, int bucket, long entry) {
		long[] index = entries;
		long[] values = keys;
		int mask = index.length - 1;
		while ((int) entry != hash || values[bucket] != key) {
			if (entry == 0L) {
				return -1;
			}
			bucket = (bucket + 1) & mask;
			entry = index[bucket];
		}
		return (int) (entry >>> Integer.SIZE) - 1;
	}

	long payload(int payload, int offset) {
		return payloads[payload * payloadWidth + offset];
	}

	int find(long[] row, int[] keySlots, int hash) {
		byte[] tags = controls;
		int mask = tags.length - 1;
		int bucket = hash & mask;
		byte expected = control(hash), tag;
		while ((tag = tags[bucket]) != 0) {
			if (tag == expected && (int) entries[bucket] == hash && matches(row, keySlots, bucket)) {
				return bucket;
			}
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
		if (requiredRows < 0) {
			throw new IllegalArgumentException("negative payload capacity");
		}
		if (requiredRows <= next.length) {
			return;
		}
		int newLength = next.length;
		while (newLength < requiredRows) {
			newLength = Math.multiplyExact(newLength, 2);
		}
		long[] newPayloads = payloadWidth == 0 ? payloads
				: Arrays.copyOf(payloads, Math.multiplyExact(payloadWidth, newLength));
		int[] newNext = Arrays.copyOf(next, newLength);
		// add() assigns the new row's sentinel; unwritten rows are not part of any chain.
		payloads = newPayloads;
		next = newNext;
	}

	void growBuckets() {
		int newLength = Math.multiplyExact(entries.length, 2);
		long[] newKeys = new long[Math.multiplyExact(keyWidth, newLength)];
		long[] newEntries = new long[newLength];
		byte[] newControls = new byte[newLength];
		int[] newTails = new int[newLength];
		int[] newChainCounts = new int[newLength];
		int mask = newLength - 1;
		for (int oldBucket = 0; oldBucket < entries.length; oldBucket++) {
			long entry = entries[oldBucket];
			if (entry == 0L) {
				continue;
			}
			int bucket = (int) entry & mask;
			while (newEntries[bucket] != 0L) {
				bucket = (bucket + 1) & mask;
			}
			newEntries[bucket] = entry;
			newControls[bucket] = controls[oldBucket];
			System.arraycopy(keys, oldBucket * keyWidth, newKeys, bucket * keyWidth, keyWidth);
			newTails[bucket] = tails[oldBucket];
			newChainCounts[bucket] = chainCounts[oldBucket];
		}
		keys = newKeys;
		entries = newEntries;
		controls = newControls;
		tails = newTails;
		chainCounts = newChainCounts;
	}
}
