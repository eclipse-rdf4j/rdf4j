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

/**
 * KeyEncoder with a lightweight probabilistic admission filter.
 */
final class KeyGenerator implements AutoCloseable {

	static int CACHE_THRESHOLD = 2;
	static final int WINDOW_SIZE = 10000;

	private static final int FILTER_BITS = 1 << 12;
	private static final int FILTER_MASK = FILTER_BITS - 1;

	private final IndexKeyWriters.KeyWriter keyWriter;
	private final CacheEntry[] cacheEntries = new CacheEntry[FILTER_BITS];
	private final int[] counters = new int[cacheEntries.length];
	private final int[] counterEpoch = new int[counters.length];
	private int epoch;
	private int windowCallCount;

	KeyGenerator(IndexKeyWriters.KeyWriter keyWriter) {
		this.keyWriter = Objects.requireNonNull(keyWriter, "keyWriter");
	}

	ByteBuffer keyFor(long subj, long pred, long obj, long context, Supplier<ByteBuffer> supplier, boolean pooled) {
		return keyFor(subj, pred, obj, context, supplier, pooled, true);
	}

	ByteBuffer keyFor(long subj, long pred, long obj, long context, Supplier<ByteBuffer> supplier, boolean pooled,
					  boolean allowCache) {
		long sum = subj * 3 + (subj + pred) * 5 + (subj + obj) * 7 + context * 11;
		int filterIndex = (int) (sum & FILTER_MASK);

		if (allowCache) {
			CacheEntry entry = cacheEntries[filterIndex];
			if (entry != null && entry.matches(subj, pred, obj, context)) {
//				System.out.println("hit");
				return entry.buffer();
			}
//			System.out.println("miss");
		}

		ByteBuffer buffer = supplier.get();
		buffer.clear();
		keyWriter.write(buffer, subj, pred, obj, context);
		buffer.flip();

		if (allowCache) {
			if (timeSinceLastCacheThresholdIncrease + 5 > epoch && epoch% 32 == 0) {
				// Don't promote to cache too soon after a threshold increase
			} else {
				maybePromote(buffer, sum, filterIndex, subj, pred, obj, context);
			}
			if (windowCallCount++ % WINDOW_SIZE == 0) {
				epoch = (epoch + 1) & Integer.MAX_VALUE;
				churn /= 2;
				if(epoch % 128 == 0 && churn < CACHE_THRESHOLD / 2 && CACHE_THRESHOLD > 2) {
					CACHE_THRESHOLD--;
					System.out.println("Decreased cache threshold to: " + CACHE_THRESHOLD);
					timeSinceLastCacheThresholdIncrease = epoch;
				}
			}
		}


		return buffer;
	}


	int max = 0;

	int prev = 0;
	int prev2 = 0;

	int churn = 0;
	int timeSinceLastCacheThresholdIncrease = 0;

	private void maybePromote(ByteBuffer buffer, long sum, int filterIndex, long subj, long pred, long obj,
							  long context) {

		// prev and prev2 store the two previous filter indices we saw, if ours mataches any of these then we can continue, if not we return
		if (filterIndex != prev && filterIndex != prev2) {
			prev2 = prev;
			prev = filterIndex;
		} else {
			// System.out.println("Skipping due to locality");
			return;
		}

		if (counterEpoch[filterIndex] != epoch) {
			counters[filterIndex] = 0;
			counterEpoch[filterIndex] = epoch;
		}

		int newCount = counters[filterIndex]++;
		if (newCount >= CACHE_THRESHOLD) {
//			if(max < newCount) {
//				max = newCount;
//				System.out.println("New max: " + max);
//			}
			CacheEntry existing = cacheEntries[filterIndex];
			boolean matches = existing != null ? existing.matches(subj, pred, obj, context) : false;
			if (!matches) {
				if (existing != null) {
					churn++;
					if (churn % 50 == 0) {
						System.out.println("Cache churn: " + churn + ", current threshold: " + CACHE_THRESHOLD);
						CACHE_THRESHOLD++;
						timeSinceLastCacheThresholdIncrease = epoch;
					}
				}

//				if(existing != null) {
//					System.out.println("Churn");
//				}
				CacheEntry entry = createEntry(subj, pred, obj, context, buffer);
				cacheEntries[filterIndex] = entry;
			}
//			setFilterBit(filterIndex);
		} else {
			// counters[filterIndex] = newCount;
		}


	}

	private CacheEntry createEntry(long subj, long pred, long obj, long context, ByteBuffer buffer) {
//		System.out.println("Promoting to cache: " + subj + " " + pred + " " + obj + " " + context);
		ByteBuffer duplicate = buffer.duplicate();
		ByteBuffer readOnlyView = duplicate.asReadOnlyBuffer();
		readOnlyView.position(0);
		return new CacheEntry(subj, pred, obj, context, readOnlyView);
	}

	@Override
	public void close() {
		Arrays.fill(counters, 0);
		Arrays.fill(counterEpoch, 0);
		epoch = 0;
		windowCallCount = 0;
	}

	private static final class CacheEntry {
		private final long subj;
		private final long pred;
		private final long obj;
		private final long context;
		private final ByteBuffer stored;

		CacheEntry(long subj, long pred, long obj, long context, ByteBuffer stored) {
			this.subj = subj;
			this.pred = pred;
			this.obj = obj;
			this.context = context;
			this.stored = stored;
		}

		boolean matches(long subj, long pred, long obj, long context) {
			return this.subj == subj && this.pred == pred && this.obj == obj && this.context == context;
		}

		ByteBuffer buffer() {
			return stored;
		}

	}
}
