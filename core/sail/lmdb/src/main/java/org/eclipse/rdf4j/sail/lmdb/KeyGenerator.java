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
import java.util.function.Supplier;

import org.lwjgl.system.MemoryUtil;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;

/**
 * KeyEncoder with a lightweight probabilistic admission filter.
 */
final class KeyGenerator implements AutoCloseable {

	static final int CACHE_THRESHOLD = 2;
	static final int WINDOW_SIZE = 50;

	static final int CACHE_MAX_ENTRIES = 1 << 12;

	private static final int FILTER_BITS = 1 << 16;
	private static final int FILTER_MASK = FILTER_BITS - 1;
	private static final int COUNTER_SLOTS = CACHE_MAX_ENTRIES;
	private static final int COUNTER_MASK = COUNTER_SLOTS - 1;

	private final IndexKeyWriters.KeyWriter keyWriter;
	private final Cache<KeySignature, CacheEntry> cache;
	private final long[] filterBits = new long[FILTER_BITS >>> 6];
	private final int[] counters = new int[COUNTER_SLOTS];
	private final int[] counterEpoch = new int[COUNTER_SLOTS];
	private int epoch;
	private int windowCallCount;

	KeyGenerator(IndexKeyWriters.KeyWriter keyWriter) {
		this.keyWriter = Objects.requireNonNull(keyWriter, "keyWriter");
		this.cache = CacheBuilder.newBuilder()
				.maximumSize(CACHE_MAX_ENTRIES)
				.removalListener(this::onCacheRemoval)
				.build();
	}

	ByteBuffer keyFor(long subj, long pred, long obj, long context, Supplier<ByteBuffer> supplier, boolean pooled) {
		return keyFor(subj, pred, obj, context, supplier, pooled, true);
	}

	ByteBuffer keyFor(long subj, long pred, long obj, long context, Supplier<ByteBuffer> supplier, boolean pooled,
			boolean allowCache) {
		long sum = subj + pred + obj + context;
		int filterIndex = (int) (sum & FILTER_MASK);
		KeySignature signature = null;

		if (allowCache && isFilterHit(filterIndex)) {
			signature = new KeySignature(subj, pred, obj, context);
			CacheEntry entry = cache.getIfPresent(signature);
			if (entry != null) {
				return entry.stored;
			}
		}

		ByteBuffer buffer = supplier.get();
		buffer.clear();
		keyWriter.write(buffer, subj, pred, obj, context);
		buffer.flip();

		if (allowCache) {
			maybePromote(signature, buffer, sum, filterIndex, subj, pred, obj, context);
		}

		return buffer;
	}

	private boolean isFilterHit(int index) {
		int word = index >>> 6;
		long mask = 1L << (index & 63);
		return (filterBits[word] & mask) != 0L;
	}

	private void setFilterBit(int index) {
		int word = index >>> 6;
		long mask = 1L << (index & 63);
		filterBits[word] |= mask;
	}

	private void maybePromote(KeySignature signature, ByteBuffer buffer, long sum, int filterIndex, long subj,
			long pred,
			long obj, long context) {
		int counterIndex = (int) (sum & COUNTER_MASK);
		if (counterEpoch[counterIndex] != epoch) {
			counters[counterIndex] = 0;
			counterEpoch[counterIndex] = epoch;
		}

		int newCount = counters[counterIndex] + 1;
		if (newCount >= CACHE_THRESHOLD) {
			counters[counterIndex] = 0;
			KeySignature key = signature;
			if (key == null) {
				key = new KeySignature(subj, pred, obj, context);
			}
			if (cache.getIfPresent(key) == null) {
				cache.put(key, createEntry(buffer));
			}
			setFilterBit(filterIndex);
		} else {
			counters[counterIndex] = newCount;
		}

		windowCallCount++;
		if (windowCallCount >= WINDOW_SIZE) {
			windowCallCount = 0;
			epoch = (epoch + 1) & Integer.MAX_VALUE;
		}
	}

	private CacheEntry createEntry(ByteBuffer buffer) {
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
		ByteBuffer readOnlyView = copy.asReadOnlyBuffer();
		readOnlyView.position(0);
		return new CacheEntry(readOnlyView, direct ? copy : null);
	}

	@Override
	public void close() {
		cache.invalidateAll();
		cache.cleanUp();
		Arrays.fill(filterBits, 0L);
		Arrays.fill(counters, 0);
		Arrays.fill(counterEpoch, 0);
		epoch = 0;
		windowCallCount = 0;
	}

	private void onCacheRemoval(RemovalNotification<KeySignature, CacheEntry> notification) {
		CacheEntry entry = notification.getValue();
		if (entry != null) {
			entry.close();
		}
	}

	private static final class CacheEntry {
		private final ByteBuffer stored;
		private final ByteBuffer resource;
		private final boolean direct;

		CacheEntry(ByteBuffer stored, ByteBuffer resource) {
			this.stored = stored;
			this.resource = resource;
			this.direct = resource != null;
		}

		void close() {
			if (direct) {
				MemoryUtil.memFree(resource);
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
