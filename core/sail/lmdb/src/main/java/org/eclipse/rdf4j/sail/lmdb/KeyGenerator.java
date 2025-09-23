/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import org.lwjgl.system.MemoryUtil;

final class KeyGenerator implements AutoCloseable {

	static final int CACHE_THRESHOLD = 10;
	static final int WINDOW_SIZE = 1000;

	private static final int FILTER_BITS = 1 << 18;
	private static final int FILTER_MASK = FILTER_BITS - 1;
	private static final int COUNTER_SLOTS = 1 << 14;
	private static final int COUNTER_MASK = COUNTER_SLOTS - 1;

	@FunctionalInterface
	interface BufferSupplier {
		KeyBuffer acquire();
	}

	static final class KeyBuffer {
		private final ByteBuffer buffer;
		private final boolean pooled;
		private final boolean cached;

		KeyBuffer(ByteBuffer buffer, boolean pooled) {
			this(buffer, pooled, false);
		}

		KeyBuffer(ByteBuffer buffer, boolean pooled, boolean cached) {
			this.buffer = Objects.requireNonNull(buffer, "buffer");
			this.pooled = pooled;
			this.cached = cached;
		}

		ByteBuffer buffer() {
			return buffer;
		}

		boolean pooled() {
			return pooled;
		}

		boolean cached() {
			return cached;
		}
	}

	private final IndexKeyWriters.KeyWriter keyWriter;
	private static final ConcurrentHashMap<KeySignature, CacheEntry> cache = new ConcurrentHashMap<>();
	private static final long[] filterBits = new long[FILTER_BITS >>> 6];
	private static final int[] counters = new int[COUNTER_SLOTS];
	private static final int[] counterEpoch = new int[COUNTER_SLOTS];
	private static int epoch;
	private static int windowCallCount;

	private static LongAdder hits = new LongAdder();
	private static LongAdder requests = new LongAdder();

	KeyGenerator(IndexKeyWriters.KeyWriter keyWriter) {
		this.keyWriter = Objects.requireNonNull(keyWriter, "keyWriter");
	}

	KeyBuffer keyFor(long subj, long pred, long obj, long context, BufferSupplier supplier) {
		return keyFor(subj, pred, obj, context, supplier, true);
	}

	int count = 0;

	KeyBuffer keyFor(long subj, long pred, long obj, long context, BufferSupplier supplier, boolean allowCache) {
//		if(count++ % 1000000 == 0) {
//			System.out.println("KeyGenerator cache hits: " + hits.sum() + " / " + requests.sum() + " (" + Math.floor(hits.sum() * 100.0 / requests.sum()) + "%)");
//		}

//		requests.increment();
		long sum = subj + pred + obj + context;
		int filterIndex = (int) (sum & FILTER_MASK);
		if (allowCache && isFilterHit(filterIndex)) {
			KeySignature signature = allowCache ? new KeySignature(subj, pred, obj, context) : null;

			CacheEntry entry = cache.get(signature);
			if (entry != null) {
//				hits.increment();
				return new KeyBuffer(entry.stored, false, true);
			}
		}

		KeyBuffer supplied = supplier.acquire();
		ByteBuffer buffer = supplied.buffer();
		buffer.clear();
		keyWriter.write(buffer, subj, pred, obj, context);
		buffer.flip();

		if (allowCache) {
			maybePromote(subj, pred, obj, context, supplied, buffer, sum, filterIndex);
		}

		return new KeyBuffer(buffer, supplied.pooled(), false);
	}

	private boolean isFilterHit(int filterIndex) {
		int word = filterIndex >>> 6;
		long mask = 1L << (filterIndex & 63);
		return (filterBits[word] & mask) != 0L;
	}

	private void setFilterBit(int filterIndex) {
		int word = filterIndex >>> 6;
		long mask = 1L << (filterIndex & 63);
		filterBits[word] |= mask;
	}

	private void maybePromote(long subj, long pred, long obj, long context, KeyBuffer supplied, ByteBuffer buffer,
			long sum,
			int filterIndex) {
		int counterIndex = (int) (sum & COUNTER_MASK);
		if (counterEpoch[counterIndex] != epoch) {
			counters[counterIndex] = 0;
			counterEpoch[counterIndex] = epoch;
		}
		int newCount = counters[counterIndex] + 1;
		if (newCount >= CACHE_THRESHOLD) {
			counters[counterIndex] = 0;
			cache.compute(new KeySignature(subj, pred, obj, context), (key, existing) -> {
				if (existing == null) {
					CacheEntry entry = createEntry(buffer, supplied.pooled());
					setFilterBit(filterIndex);
					return entry;
				}
				setFilterBit(filterIndex);
				return existing;
			});
		} else {
			counters[counterIndex] = newCount;
		}
		windowCallCount++;
		if (windowCallCount >= WINDOW_SIZE) {
			windowCallCount = 0;
			epoch = (epoch + 1) & Integer.MAX_VALUE;
		}

	}

	private CacheEntry createEntry(ByteBuffer buffer, boolean pooled) {
		ByteBuffer duplicate = buffer.duplicate();
		ByteBuffer copy;
		boolean direct;
		if (buffer.hasArray()) {
			copy = ByteBuffer.allocate(duplicate.remaining());
			direct = false;
		} else {
			copy = MemoryUtil.memAlloc(duplicate.remaining());
			direct = true;
		}
		copy.put(duplicate);
		copy.flip();
		buffer.rewind();
		return new CacheEntry(copy, direct);
	}

	@Override
	public void close() {
		cache.values().forEach(CacheEntry::close);
		cache.clear();
		synchronized (this) {
			Arrays.fill(filterBits, 0L);
			Arrays.fill(counters, 0);
			Arrays.fill(counterEpoch, 0);
			epoch = 0;
			windowCallCount = 0;
		}
	}

	private static final class CacheEntry {
		private final ByteBuffer stored;
		private final boolean direct;

		CacheEntry(ByteBuffer stored, boolean direct) {
			this.stored = stored;
			this.direct = direct;
		}

		ByteBuffer duplicate() {
			ByteBuffer duplicate = stored.duplicate();
			duplicate.position(0);
			duplicate.limit(stored.limit());
			return duplicate;
		}

		void close() {
			if (direct) {
				MemoryUtil.memFree(stored);
			}
		}
	}

	private static final class KeySignature {
		private final long subj;
		private final long pred;
		private final long obj;
		private final long context;
		private final int hash;

		KeySignature(long subj, long pred, long obj, long context) {
			this.subj = subj;
			this.pred = pred;
			this.obj = obj;
			this.context = context;
			this.hash = (int) (subj + pred + obj + context);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof KeySignature)) {
				return false;
			}
			KeySignature other = (KeySignature) obj;
			return subj == other.subj && pred == other.pred && this.obj == other.obj && context == other.context;
		}

		@Override
		public int hashCode() {
			return hash;
		}
	}
}
