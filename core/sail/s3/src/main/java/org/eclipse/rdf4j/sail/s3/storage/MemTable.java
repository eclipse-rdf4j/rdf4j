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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In-memory sorted store for quads using a {@link ConcurrentSkipListMap}. Stores quads as varint-encoded byte[] keys
 * (in the order defined by a {@link QuadIndex}) with a 1-byte flag value:
 * <ul>
 * <li>{@code 0x01} = explicit</li>
 * <li>{@code 0x02} = inferred</li>
 * <li>{@code 0x00} = tombstone (deleted)</li>
 * </ul>
 *
 * <p>
 * The unsigned byte comparison on keys preserves the varint lexicographic ordering, which in turn preserves the numeric
 * ordering of the encoded IDs.
 * </p>
 */
public class MemTable {

	public static final byte FLAG_TOMBSTONE = 0x00;
	public static final byte FLAG_EXPLICIT = 0x01;
	public static final byte FLAG_INFERRED = 0x02;

	private static final byte[] VALUE_EXPLICIT = new byte[] { FLAG_EXPLICIT };
	private static final byte[] VALUE_INFERRED = new byte[] { FLAG_INFERRED };
	private static final byte[] VALUE_TOMBSTONE = new byte[] { FLAG_TOMBSTONE };

	private final QuadIndex index;
	private final ConcurrentSkipListMap<byte[], byte[]> data;
	private final AtomicBoolean frozen = new AtomicBoolean(false);

	/**
	 * Creates a new MemTable backed by the given index for key encoding.
	 *
	 * @param index the QuadIndex that determines key encoding order
	 */
	public MemTable(QuadIndex index) {
		this.index = index;
		this.data = new ConcurrentSkipListMap<>(Arrays::compareUnsigned);
	}

	/**
	 * Creates a frozen (immutable) MemTable from an existing data map. Used internally by {@link #freeze()}.
	 */
	private MemTable(QuadIndex index, ConcurrentSkipListMap<byte[], byte[]> data, boolean frozen) {
		this.index = index;
		this.data = data;
		this.frozen.set(frozen);
	}

	/**
	 * Stores a quad in the table.
	 *
	 * @param s        subject ID
	 * @param p        predicate ID
	 * @param o        object ID
	 * @param c        context ID
	 * @param explicit true for explicit, false for inferred
	 * @throws IllegalStateException if the table is frozen
	 */
	public void put(long s, long p, long o, long c, boolean explicit) {
		checkNotFrozen();
		byte[] key = index.toKeyBytes(s, p, o, c);
		data.put(key, explicit ? VALUE_EXPLICIT : VALUE_INFERRED);
	}

	/**
	 * Removes a quad by writing a tombstone.
	 *
	 * @param s        subject ID
	 * @param p        predicate ID
	 * @param o        object ID
	 * @param c        context ID
	 * @param explicit true for explicit, false for inferred (currently unused; tombstone applies to either)
	 * @throws IllegalStateException if the table is frozen
	 */
	public void remove(long s, long p, long o, long c, boolean explicit) {
		checkNotFrozen();
		byte[] key = index.toKeyBytes(s, p, o, c);
		data.put(key, VALUE_TOMBSTONE);
	}

	/**
	 * Checks if a quad exists (is not a tombstone).
	 *
	 * @param s        subject ID
	 * @param p        predicate ID
	 * @param o        object ID
	 * @param c        context ID
	 * @param explicit true to check explicit, false for inferred
	 * @return true if the quad exists with the matching flag
	 */
	public boolean get(long s, long p, long o, long c, boolean explicit) {
		byte[] key = index.toKeyBytes(s, p, o, c);
		byte[] value = data.get(key);
		if (value == null || value[0] == FLAG_TOMBSTONE) {
			return false;
		}
		return explicit ? value[0] == FLAG_EXPLICIT : value[0] == FLAG_INFERRED;
	}

	/**
	 * Returns an iterator over matching quads using range scan. Bound components (>= 0) form a prefix; unbound
	 * components (-1) are wildcards.
	 *
	 * @param s        subject ID, or -1 for wildcard
	 * @param p        predicate ID, or -1 for wildcard
	 * @param o        object ID, or -1 for wildcard
	 * @param c        context ID, or -1 for wildcard
	 * @param explicit true for explicit, false for inferred
	 * @return an iterator over matching quads as long[4] arrays in SPOC order
	 */
	public Iterator<long[]> scan(long s, long p, long o, long c, boolean explicit) {
		byte[] minKey = index.getMinKeyBytes(s, p, o, c);
		byte[] maxKey = index.getMaxKeyBytes(s, p, o, c);
		byte expectedFlag = explicit ? FLAG_EXPLICIT : FLAG_INFERRED;

		ConcurrentNavigableMap<byte[], byte[]> range = data.subMap(minKey, true, maxKey, true);

		return new ScanIterator(range, index, expectedFlag, s, p, o, c);
	}

	/**
	 * Returns the number of entries in the table (including tombstones).
	 */
	public int size() {
		return data.size();
	}

	/**
	 * Returns a rough estimate of memory consumption in bytes.
	 */
	public long approximateSizeInBytes() {
		long size = 0;
		for (Map.Entry<byte[], byte[]> entry : data.entrySet()) {
			// key array overhead (16 bytes) + key data + value array overhead (16 bytes) + value data
			// + map entry overhead (~64 bytes for skip list node)
			size += 16 + entry.getKey().length + 16 + entry.getValue().length + 64;
		}
		return size;
	}

	/**
	 * Returns a frozen (immutable) snapshot of this table. After freezing, no further writes are accepted on this
	 * instance.
	 *
	 * @return this MemTable, now frozen
	 */
	public MemTable freeze() {
		frozen.set(true);
		return this;
	}

	/**
	 * Returns whether this table is frozen (immutable).
	 */
	public boolean isFrozen() {
		return frozen.get();
	}

	/**
	 * Clears all entries from the table.
	 *
	 * @throws IllegalStateException if the table is frozen
	 */
	public void clear() {
		checkNotFrozen();
		data.clear();
	}

	/**
	 * Returns the QuadIndex used by this table.
	 */
	public QuadIndex getIndex() {
		return index;
	}

	/**
	 * Returns an unmodifiable view of the underlying data map.
	 */
	public Map<byte[], byte[]> getData() {
		return Collections.unmodifiableMap(data);
	}

	/**
	 * Returns a {@link RawEntrySource} over the given key range. Includes tombstones (no flag filtering). Used by
	 * {@link MergeIterator}.
	 */
	public RawEntrySource asRawSource(long s, long p, long o, long c) {
		byte[] minKey = index.getMinKeyBytes(s, p, o, c);
		byte[] maxKey = index.getMaxKeyBytes(s, p, o, c);
		ConcurrentNavigableMap<byte[], byte[]> range = data.subMap(minKey, true, maxKey, true);
		return new RawSourceImpl(range);
	}

	private static class RawSourceImpl implements RawEntrySource {
		private final Iterator<Map.Entry<byte[], byte[]>> delegate;
		private Map.Entry<byte[], byte[]> current;

		RawSourceImpl(ConcurrentNavigableMap<byte[], byte[]> range) {
			this.delegate = range.entrySet().iterator();
			if (delegate.hasNext()) {
				current = delegate.next();
			}
		}

		@Override
		public boolean hasNext() {
			return current != null;
		}

		@Override
		public byte[] peekKey() {
			return current.getKey();
		}

		@Override
		public byte peekFlag() {
			return current.getValue()[0];
		}

		@Override
		public void advance() {
			if (delegate.hasNext()) {
				current = delegate.next();
			} else {
				current = null;
			}
		}
	}

	/**
	 * Returns a {@link RawEntrySource} over MemTable entries matching the given predicate, encoded as 3-varint keys in
	 * the specified partition sort order. Used by {@link PartitionMergeIterator} to merge MemTable entries with Parquet
	 * partition files.
	 *
	 * @param predId    the predicate ID to filter by
	 * @param subj      subject filter, or -1 for wildcard
	 * @param obj       object filter, or -1 for wildcard
	 * @param ctx       context filter, or -1 for wildcard
	 * @param sortOrder the partition sort order ("soc", "osc", or "cso")
	 * @return a RawEntrySource with 3-varint keys in the specified sort order
	 */
	public RawEntrySource asPartitionRawSource(long predId, long subj, long obj, long ctx, String sortOrder) {
		// Scan the SPOC MemTable for entries matching the given predicate
		byte[] minKey = index.getMinKeyBytes(subj <= 0 ? 0 : subj, predId, obj <= 0 ? 0 : obj,
				ctx < 0 ? 0 : ctx);
		byte[] maxKey = index.getMaxKeyBytes(subj <= 0 ? Long.MAX_VALUE : subj, predId,
				obj <= 0 ? Long.MAX_VALUE : obj, ctx < 0 ? Long.MAX_VALUE : ctx);
		ConcurrentNavigableMap<byte[], byte[]> range = data.subMap(minKey, true, maxKey, true);

		// Collect matching entries, re-encode as 3-varint partition keys
		List<PartitionEntry> entries = new ArrayList<>();
		long[] quad = new long[4];
		for (Map.Entry<byte[], byte[]> entry : range.entrySet()) {
			index.keyToQuad(entry.getKey(), quad);
			// Verify predicate matches (range scan may include adjacent predicates)
			if (quad[QuadIndex.PRED_IDX] != predId) {
				continue;
			}
			// Apply additional filters
			if (subj >= 0 && quad[QuadIndex.SUBJ_IDX] != subj) {
				continue;
			}
			if (obj >= 0 && quad[QuadIndex.OBJ_IDX] != obj) {
				continue;
			}
			if (ctx >= 0 && quad[QuadIndex.CONTEXT_IDX] != ctx) {
				continue;
			}
			byte[] partitionKey = ParquetQuadSource.encodeKey(sortOrder,
					quad[QuadIndex.SUBJ_IDX], quad[QuadIndex.OBJ_IDX], quad[QuadIndex.CONTEXT_IDX]);
			entries.add(new PartitionEntry(partitionKey, entry.getValue()[0]));
		}

		// Sort by partition key (entries may not be in partition sort order)
		entries.sort((a, b) -> java.util.Arrays.compareUnsigned(a.key, b.key));

		return new PartitionRawSourceImpl(entries);
	}

	private static class PartitionEntry {
		final byte[] key;
		final byte flag;

		PartitionEntry(byte[] key, byte flag) {
			this.key = key;
			this.flag = flag;
		}
	}

	private static class PartitionRawSourceImpl implements RawEntrySource {
		private final List<PartitionEntry> entries;
		private int pos;

		PartitionRawSourceImpl(List<PartitionEntry> entries) {
			this.entries = entries;
			this.pos = 0;
		}

		@Override
		public boolean hasNext() {
			return pos < entries.size();
		}

		@Override
		public byte[] peekKey() {
			return entries.get(pos).key;
		}

		@Override
		public byte peekFlag() {
			return entries.get(pos).flag;
		}

		@Override
		public void advance() {
			pos++;
		}
	}

	/**
	 * Partitions entries by predicate ID. Returns a map from predicate ID to a list of {@link QuadEntry} records
	 * containing (subject, object, context, flag). Used during Parquet flush to write per-predicate partition files.
	 *
	 * @return map from predicate ID to list of quad entries (without predicate column)
	 */
	public Map<Long, List<QuadEntry>> partitionByPredicate() {
		Map<Long, List<QuadEntry>> result = new LinkedHashMap<>();
		long[] quad = new long[4];
		for (Map.Entry<byte[], byte[]> entry : data.entrySet()) {
			index.keyToQuad(entry.getKey(), quad);
			long predId = quad[QuadIndex.PRED_IDX];
			result.computeIfAbsent(predId, k -> new ArrayList<>())
					.add(new QuadEntry(quad[QuadIndex.SUBJ_IDX], quad[QuadIndex.OBJ_IDX],
							quad[QuadIndex.CONTEXT_IDX], entry.getValue()[0]));
		}
		return result;
	}

	/**
	 * A quad entry with predicate removed (implicit in partition). Used for Parquet file writing.
	 */
	public static class QuadEntry {
		public final long subject;
		public final long object;
		public final long context;
		public final byte flag;

		public QuadEntry(long subject, long object, long context, byte flag) {
			this.subject = subject;
			this.object = object;
			this.context = context;
			this.flag = flag;
		}
	}

	private void checkNotFrozen() {
		if (frozen.get()) {
			throw new IllegalStateException("MemTable is frozen and cannot accept writes");
		}
	}

	/**
	 * Iterator that filters range scan results by flag value and pattern match. Skips tombstones and entries where
	 * bound components don't match the query pattern. Returns quads in SPOC order.
	 */
	private static class ScanIterator implements Iterator<long[]> {
		private final Iterator<Map.Entry<byte[], byte[]>> delegate;
		private final QuadIndex quadIndex;
		private final byte expectedFlag;
		private final long patternS, patternP, patternO, patternC;
		private long[] next;

		ScanIterator(ConcurrentNavigableMap<byte[], byte[]> range, QuadIndex quadIndex, byte expectedFlag,
				long s, long p, long o, long c) {
			this.delegate = range.entrySet().iterator();
			this.quadIndex = quadIndex;
			this.expectedFlag = expectedFlag;
			this.patternS = s;
			this.patternP = p;
			this.patternO = o;
			this.patternC = c;
			advance();
		}

		private void advance() {
			next = null;
			while (delegate.hasNext()) {
				Map.Entry<byte[], byte[]> entry = delegate.next();
				byte flag = entry.getValue()[0];
				if (flag != expectedFlag) {
					continue;
				}
				long[] quad = new long[4];
				quadIndex.keyToQuad(entry.getKey(), quad);
				if ((patternS >= 0 && quad[QuadIndex.SUBJ_IDX] != patternS)
						|| (patternP >= 0 && quad[QuadIndex.PRED_IDX] != patternP)
						|| (patternO >= 0 && quad[QuadIndex.OBJ_IDX] != patternO)
						|| (patternC >= 0 && quad[QuadIndex.CONTEXT_IDX] != patternC)) {
					continue;
				}
				next = quad;
				return;
			}
		}

		@Override
		public boolean hasNext() {
			return next != null;
		}

		@Override
		public long[] next() {
			if (next == null) {
				throw new NoSuchElementException();
			}
			long[] result = next;
			advance();
			return result;
		}
	}
}
