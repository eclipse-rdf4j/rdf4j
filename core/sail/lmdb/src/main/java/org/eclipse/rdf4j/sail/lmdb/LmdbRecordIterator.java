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

import static org.eclipse.rdf4j.sail.lmdb.LmdbUtil.E;
import static org.lwjgl.util.lmdb.LMDB.MDB_NEXT;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOTFOUND;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET_RANGE;
import static org.lwjgl.util.lmdb.LMDB.MDB_SUCCESS;
import static org.lwjgl.util.lmdb.LMDB.mdb_cmp;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_get;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_renew;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

import org.eclipse.rdf4j.common.concurrent.locks.StampedLongAdderLockManager;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lmdb.TripleStore.TripleIndex;
import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.eclipse.rdf4j.sail.lmdb.util.GroupMatcher;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.lmdb.MDBVal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A record iterator that wraps a native LMDB iterator.
 */
class LmdbRecordIterator implements RecordIterator {
	private static final Logger log = LoggerFactory.getLogger(LmdbRecordIterator.class);
	private static final List<Character> COMPONENT_ORDER = List.of('s', 'p', 'o', 'c');
	private static final ConcurrentMap<String, LongAdder> RECOMMENDED_INDEX_TRACKER = new ConcurrentHashMap<>();
	private static final ConcurrentMap<LmdbRecordIterator, String> INDEX_NAME_CACHE = new ConcurrentHashMap<>();
	private static final Set<LmdbRecordIterator> PENDING_RECOMMENDATIONS = ConcurrentHashMap.newKeySet();
	private static final Set<LmdbRecordIterator> RECORDED_RECOMMENDATIONS = ConcurrentHashMap.newKeySet();

	private final Pool pool;

	private final TripleIndex index;

	private final long subj;
	private final long pred;
	private final long obj;
	private final long context;

	private final long cursor;

	private final MDBVal maxKey;

	private final boolean matchValues;
	private GroupMatcher groupMatcher;

	private final Txn txnRef;

	private long txnRefVersion;

	private final long txn;

	private final int dbi;

	private volatile boolean closed = false;

	private final MDBVal keyData;

	private final MDBVal valueData;

	private ByteBuffer minKeyBuf;

	private ByteBuffer maxKeyBuf;

	private int lastResult;

	private final long[] quad;
	private final long[] originalQuad;

	private boolean fetchNext = false;

	private final StampedLongAdderLockManager txnLockManager;

	private final Thread ownerThread = Thread.currentThread();

	LmdbRecordIterator(TripleIndex index, boolean rangeSearch, long subj, long pred, long obj,
			long context, boolean explicit, Txn txnRef) throws IOException {
		this.subj = subj;
		this.pred = pred;
		this.obj = obj;
		this.context = context;
		this.originalQuad = new long[] { subj, pred, obj, context };
		this.quad = new long[] { subj, pred, obj, context };
		this.pool = Pool.get();
		this.keyData = pool.getVal();
		this.valueData = pool.getVal();
		this.index = index;
		if (rangeSearch) {
			minKeyBuf = pool.getKeyBuffer();
			index.getMinKey(minKeyBuf, subj, pred, obj, context);
			minKeyBuf.flip();

			this.maxKey = pool.getVal();
			this.maxKeyBuf = pool.getKeyBuffer();
			index.getMaxKey(maxKeyBuf, subj, pred, obj, context);
			maxKeyBuf.flip();
			this.maxKey.mv_data(maxKeyBuf);
		} else {
			minKeyBuf = null;
			this.maxKey = null;
		}

		this.matchValues = subj > 0 || pred > 0 || obj > 0 || context >= 0;

		this.dbi = index.getDB(explicit);
		this.txnRef = txnRef;
		this.txnLockManager = txnRef.lockManager();

		long readStamp;
		try {
			readStamp = txnLockManager.readLock();
		} catch (InterruptedException e) {
			throw new SailException(e);
		}
		try {
			this.txnRefVersion = txnRef.version();
			this.txn = txnRef.get();

			try (MemoryStack stack = MemoryStack.stackPush()) {
				PointerBuffer pp = stack.mallocPointer(1);
				E(mdb_cursor_open(txn, dbi, pp));
				cursor = pp.get(0);
			}
		} finally {
			txnLockManager.unlockRead(readStamp);
		}
	}

	static ConcurrentMap<String, LongAdder> getRecommendedIndexTracker() {
		return RECOMMENDED_INDEX_TRACKER;
	}

	static void resetIndexRecommendationTracker() {
		RECOMMENDED_INDEX_TRACKER.clear();
		INDEX_NAME_CACHE.clear();
		PENDING_RECOMMENDATIONS.clear();
		RECORDED_RECOMMENDATIONS.clear();
	}

	private static String computeIndexName(TripleIndex index, long subj, long pred, long obj, long context,
			boolean recordUsage) {
		String actual = new String(index.getFieldSeq());
		int boundCount = countBound(subj, pred, obj, context);
		if (boundCount <= 0) {
			return actual;
		}

		int score = index.getPatternScore(subj, pred, obj, context);
		if (score >= boundCount) {
			return actual;
		}

		CandidateIndex recommendation = selectRecommendedIndex(actual, subj, pred, obj, context, recordUsage);
		if (recommendation == null) {
			return actual;
		}

		return actual + " (scan; consider " + recommendation.name + ")";
	}

	private static int countBound(long subj, long pred, long obj, long context) {
		int count = 0;
		if (subj >= 0) {
			count++;
		}
		if (pred >= 0) {
			count++;
		}
		if (obj >= 0) {
			count++;
		}
		if (context >= 0) {
			count++;
		}
		return count;
	}

	private static CandidateIndex selectRecommendedIndex(String actual, long subj, long pred, long obj,
			long context, boolean recordUsage) {
		List<CandidateIndex> candidates = buildCandidateIndexes(subj, pred, obj, context, recordUsage);
		if (candidates.isEmpty()) {
			return null;
		}

		CandidateIndex best = null;
		for (CandidateIndex candidate : candidates) {
			if (candidate.name.equals(actual)) {
				continue;
			}

			if (best == null || candidate.count > best.count
					|| (candidate.count == best.count && candidate.patternScore > best.patternScore)
					|| (candidate.count == best.count && candidate.patternScore == best.patternScore
							&& candidate.orderDeviation < best.orderDeviation)
					|| (candidate.count == best.count && candidate.patternScore == best.patternScore
							&& candidate.orderDeviation == best.orderDeviation
							&& candidate.name.compareTo(best.name) < 0)) {
				best = candidate;
			}
		}

		if (best == null) {
			best = candidates.get(0);
		}

		if (recordUsage) {
			for (CandidateIndex candidate : candidates) {
				if (candidate.counter != null) {
					candidate.counter.increment();
				}
			}
		}

		return best;
	}

	private static List<CandidateIndex> buildCandidateIndexes(long subj, long pred, long obj, long context,
			boolean recordUsage) {
		List<Character> boundComponents = gatherBoundComponents(subj, pred, obj, context);
		if (boundComponents.isEmpty()) {
			return Collections.emptyList();
		}

		List<Character> preferredOrder = determinePreferredOrder(subj, pred, obj, context, boundComponents);

		List<String> boundPermutations = permuteBoundComponents(boundComponents);
		List<String> unboundPermutations = permuteUnboundComponents(boundComponents);

		if (unboundPermutations.isEmpty()) {
			unboundPermutations = Collections.singletonList("");
		}

		List<CandidateIndex> result = new ArrayList<>(boundPermutations.size() * unboundPermutations.size());

		for (String bound : boundPermutations) {
			for (String suffix : unboundPermutations) {
				String candidate = bound + suffix;
				addCandidate(result, candidate, preferredOrder, subj, pred, obj, context, recordUsage);
			}
		}

		return result;
	}

	private static void addCandidate(Collection<CandidateIndex> candidates, String candidate,
			List<Character> preferredOrder, long subj, long pred, long obj, long context, boolean recordUsage) {
		LongAdder counter = RECOMMENDED_INDEX_TRACKER.get(candidate);
		if (counter == null && recordUsage) {
			counter = RECOMMENDED_INDEX_TRACKER.computeIfAbsent(candidate, key -> new LongAdder());
		}
		long count = counter != null ? counter.sum() : 0L;
		int score = computePatternScore(candidate, subj, pred, obj, context);
		int deviation = computeDeviation(candidate, preferredOrder);
		candidates.add(new CandidateIndex(candidate, count, score, deviation, counter));
	}

	private static boolean shouldRecordUsage() {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		for (StackTraceElement element : stackTrace) {
			String className = element.getClassName();
			String methodName = element.getMethodName();
			if (("org.eclipse.rdf4j.query.algebra.StatementPattern".equals(className)
					&& "getIndexName".equals(methodName))
					|| ("org.eclipse.rdf4j.repository.sail.SailQuery".equals(className) && "explain".equals(methodName))
					|| ("org.eclipse.rdf4j.sail.base.SailSourceConnection".equals(className)
							&& "explain".equals(methodName))
					|| "org.eclipse.rdf4j.query.algebra.helpers.QueryModelTreeToGenericPlanNode".equals(className)
					|| ("org.eclipse.rdf4j.query.explanation.Explanation".equals(className)
							&& "toString".equals(methodName))) {
				return true;
			}
		}
		return false;
	}

	private static List<Character> gatherBoundComponents(long subj, long pred, long obj, long context) {
		List<Character> bound = new ArrayList<>(4);
		if (subj >= 0) {
			bound.add('s');
		}
		if (pred >= 0) {
			bound.add('p');
		}
		if (obj >= 0) {
			bound.add('o');
		}
		if (context >= 0) {
			bound.add('c');
		}
		return bound;
	}

	private static List<Character> determinePreferredOrder(long subj, long pred, long obj, long context,
			List<Character> boundComponents) {
		List<Character> order = new ArrayList<>(COMPONENT_ORDER.size());

		if (subj >= 0) {
			order.add('s');
		}

		boolean subjectBound = subj >= 0;
		boolean contextBound = context >= 0;
		boolean predicateBound = pred >= 0;
		boolean objectBound = obj >= 0;

		if (contextBound && !subjectBound) {
			if (objectBound) {
				order.add('o');
			}
			if (predicateBound) {
				order.add('p');
			}
			if (contextBound) {
				order.add('c');
			}
		} else {
			if (predicateBound) {
				order.add('p');
			}
			if (objectBound) {
				order.add('o');
			}
			if (contextBound) {
				order.add('c');
			}
		}

		for (Character component : boundComponents) {
			if (!order.contains(component)) {
				order.add(component);
			}
		}

		for (Character component : COMPONENT_ORDER) {
			if (!order.contains(component)) {
				order.add(component);
			}
		}

		return order;
	}

	private static List<String> permuteBoundComponents(List<Character> boundComponents) {
		char[] items = toCharArray(boundComponents);
		boolean[] used = new boolean[items.length];
		StringBuilder current = new StringBuilder(items.length);
		List<String> result = new ArrayList<>();
		permuteCharacters(items, used, current, result);
		return result;
	}

	private static List<String> permuteUnboundComponents(List<Character> boundComponents) {
		List<Character> unbound = new ArrayList<>(COMPONENT_ORDER);
		for (Character component : boundComponents) {
			unbound.remove(component);
		}

		if (unbound.isEmpty()) {
			return Collections.emptyList();
		}

		char[] items = toCharArray(unbound);
		boolean[] used = new boolean[items.length];
		StringBuilder current = new StringBuilder(items.length);
		List<String> permutations = new ArrayList<>();
		permuteCharacters(items, used, current, permutations);
		return permutations;
	}

	private static void permuteCharacters(char[] items, boolean[] used, StringBuilder current,
			List<String> result) {
		if (current.length() == items.length) {
			result.add(current.toString());
			return;
		}

		for (int i = 0; i < items.length; i++) {
			if (!used[i]) {
				used[i] = true;
				current.append(items[i]);
				permuteCharacters(items, used, current, result);
				current.deleteCharAt(current.length() - 1);
				used[i] = false;
			}
		}
	}

	private static int computeDeviation(String sequence, List<Character> preferredOrder) {
		int deviation = 0;
		for (int i = 0; i < sequence.length(); i++) {
			char component = sequence.charAt(i);
			int preferred = preferredOrder.indexOf(component);
			if (preferred < 0) {
				preferred = preferredOrder.size();
			}
			deviation += Math.abs(preferred - i);
		}
		return deviation;
	}

	private static int computePatternScore(String indexName, long subj, long pred, long obj, long context) {
		int score = 0;
		for (int i = 0; i < indexName.length(); i++) {
			char component = indexName.charAt(i);
			switch (component) {
			case 's':
				if (subj >= 0) {
					score++;
				} else {
					return score;
				}
				break;
			case 'p':
				if (pred >= 0) {
					score++;
				} else {
					return score;
				}
				break;
			case 'o':
				if (obj >= 0) {
					score++;
				} else {
					return score;
				}
				break;
			case 'c':
				if (context >= 0) {
					score++;
				} else {
					return score;
				}
				break;
			default:
				throw new IllegalArgumentException("invalid component '" + component + "' in index: "
						+ indexName);
			}
		}
		return score;
	}

	private static char[] toCharArray(List<Character> components) {
		char[] items = new char[components.size()];
		for (int i = 0; i < components.size(); i++) {
			items[i] = components.get(i);
		}
		return items;
	}

	private static final class CandidateIndex {
		final String name;
		final long count;
		final int patternScore;
		final int orderDeviation;
		final LongAdder counter;

		CandidateIndex(String name, long count, int patternScore, int orderDeviation, LongAdder counter) {
			this.name = name;
			this.count = count;
			this.patternScore = patternScore;
			this.orderDeviation = orderDeviation;
			this.counter = counter;
		}
	}

	@Override
	public long[] next() {
		long readStamp;
		try {
			readStamp = txnLockManager.readLock();
		} catch (InterruptedException e) {
			throw new SailException(e);
		}

		try {
			if (closed) {
				log.debug("Calling next() on an LmdbRecordIterator that is already closed, returning null");
				return null;
			}

			if (txnRefVersion != txnRef.version()) {
				// TODO: None of the tests in the LMDB Store cover this case!
				// cursor must be renewed
				mdb_cursor_renew(txn, cursor);
				if (fetchNext) {
					// cursor must be positioned on last item, reuse minKeyBuf if available
					if (minKeyBuf == null) {
						minKeyBuf = pool.getKeyBuffer();
					}
					minKeyBuf.clear();
					index.toKey(minKeyBuf, quad[0], quad[1], quad[2], quad[3]);
					minKeyBuf.flip();
					keyData.mv_data(minKeyBuf);
					lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_SET);
					if (lastResult != MDB_SUCCESS) {
						// use MDB_SET_RANGE if key was deleted
						lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_SET_RANGE);
					}
					if (lastResult != MDB_SUCCESS) {
						closeInternal(false);
						return null;
					}
				}
				// update version of txn ref
				this.txnRefVersion = txnRef.version();
			}

			if (fetchNext) {
				lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT);
				fetchNext = false;
			} else {
				if (minKeyBuf != null) {
					// set cursor to min key
					keyData.mv_data(minKeyBuf);
					lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_SET_RANGE);
				} else {
					// set cursor to first item
					lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT);
				}
			}

			while (lastResult == MDB_SUCCESS) {
				// if (maxKey != null && TripleStore.COMPARATOR.compare(keyData.mv_data(), maxKey.mv_data()) > 0) {
				if (maxKey != null && mdb_cmp(txn, dbi, keyData, maxKey) > 0) {
					lastResult = MDB_NOTFOUND;
				} else if (matches()) {
					// value doesn't match search key/mask, fetch next value
					lastResult = mdb_cursor_get(cursor, keyData, valueData, MDB_NEXT);
				} else {
					// Matching value found
					index.keyToQuad(keyData.mv_data(), originalQuad, quad);
					// fetch next value
					fetchNext = true;
					return quad;
				}
			}
			closeInternal(false);
			return null;
		} finally {
			txnLockManager.unlockRead(readStamp);
		}
	}

	@Override
	public String getIndexName() {
		while (true) {
			boolean explanationContext = shouldRecordUsage();
			String cached = INDEX_NAME_CACHE.get(this);
			if (cached != null) {
				if (explanationContext && RECORDED_RECOMMENDATIONS.add(this)) {
					String computed = computeIndexName(index, subj, pred, obj, context, true);
					INDEX_NAME_CACHE.put(this, computed);
					return computed;
				}
				return cached;
			}

			if (PENDING_RECOMMENDATIONS.add(this)) {
				try {
					boolean recordUsage = explanationContext;
					String computed = computeIndexName(index, subj, pred, obj, context, recordUsage);
					INDEX_NAME_CACHE.put(this, computed);
					if (recordUsage) {
						RECORDED_RECOMMENDATIONS.add(this);
					}
					return computed;
				} finally {
					PENDING_RECOMMENDATIONS.remove(this);
				}
			}

			Thread.onSpinWait();
		}
	}

	private boolean matches() {

		if (groupMatcher != null) {
			return !this.groupMatcher.matches(keyData.mv_data());
		} else if (matchValues) {
			this.groupMatcher = index.createMatcher(subj, pred, obj, context);
			return !this.groupMatcher.matches(keyData.mv_data());
		} else {
			return false;
		}
	}

	private void closeInternal(boolean maybeCalledAsync) {
		if (!closed) {
			INDEX_NAME_CACHE.remove(this);
			PENDING_RECOMMENDATIONS.remove(this);
			RECORDED_RECOMMENDATIONS.remove(this);
			long writeStamp = 0L;
			boolean writeLocked = false;
			if (maybeCalledAsync && ownerThread != Thread.currentThread()) {
				try {
					writeStamp = txnLockManager.writeLock();
					writeLocked = true;
				} catch (InterruptedException e) {
					throw new SailException(e);
				}
			}
			try {
				if (!closed) {
					mdb_cursor_close(cursor);
					pool.free(keyData);
					pool.free(valueData);
					if (minKeyBuf != null) {
						pool.free(minKeyBuf);
					}
					if (maxKey != null) {
						pool.free(maxKeyBuf);
						pool.free(maxKey);
					}
				}
			} finally {
				closed = true;
				if (writeLocked) {
					txnLockManager.unlockWrite(writeStamp);
				}
			}
		}
	}

	@Override
	public void close() {
		closeInternal(true);
	}
}
