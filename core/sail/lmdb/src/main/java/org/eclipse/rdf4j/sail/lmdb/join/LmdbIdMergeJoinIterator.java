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
package org.eclipse.rdf4j.sail.lmdb.join;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.sail.lmdb.IdBindingInfo;
import org.eclipse.rdf4j.sail.lmdb.RecordIterator;
import org.eclipse.rdf4j.sail.lmdb.TripleStore;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;

/**
 * Merge join iterator that operates entirely on LMDB ID records. Materialization to
 * {@link org.eclipse.rdf4j.query.BindingSet} happens in a separate {@link LmdbIdFinalBindingSetIteration}.
 */
public class LmdbIdMergeJoinIterator implements RecordIterator {

	private final PeekMarkRecordIterator leftIterator;
	private final PeekMarkRecordIterator rightIterator;
	private final String mergeVariable;
	private final IdBindingInfo bindingInfo;
	private final Set<String> sharedVariables;
	private final int mergeIndex;
	private final int bindingSize;
	private static final int COLUMN_COUNT = TripleStore.CONTEXT_IDX + 1;

	private long[] currentLeftRecord;
	private long currentLeftKey;
	private boolean hasCurrentLeftKey;

	private long[] leftPeekRecord;
	private long leftPeekKey;
	private boolean hasLeftPeekKey;
	private int currentLeftValueAndPeekEquals = -1;
	private boolean closed;
	private final int[] leftColumnBindingIndex;
	private final int[] rightColumnBindingIndex;
	private static final boolean DEBUG = Boolean.getBoolean("rdf4j.lmdb.mergeJoinDebug");
	private final AtomicInteger debugCounter = DEBUG ? new AtomicInteger() : null;

	public LmdbIdMergeJoinIterator(RecordIterator leftIterator, RecordIterator rightIterator,
			LmdbIdJoinIterator.PatternInfo leftInfo, LmdbIdJoinIterator.PatternInfo rightInfo, String mergeVariable,
			IdBindingInfo bindingInfo) {
		if (mergeVariable == null || mergeVariable.isEmpty()) {
			throw new IllegalArgumentException("Merge variable must be provided for LMDB merge join");
		}
		if (bindingInfo == null) {
			throw new IllegalArgumentException("Binding info must be provided for LMDB merge join");
		}
		if (leftInfo.getRecordIndex(mergeVariable) < 0 || rightInfo.getRecordIndex(mergeVariable) < 0) {
			throw new IllegalArgumentException("Merge variable " + mergeVariable
					+ " must be present in both join operands for LMDB merge join");
		}

		this.leftIterator = new PeekMarkRecordIterator(leftIterator);
		this.rightIterator = new PeekMarkRecordIterator(rightIterator);
		this.mergeVariable = mergeVariable;
		this.bindingInfo = bindingInfo;

		int idx = bindingInfo.getIndex(mergeVariable);
		if (idx < 0) {
			throw new IllegalArgumentException(
					"Merge variable " + mergeVariable + " is not tracked in the binding info for LMDB merge join");
		}
		this.mergeIndex = idx;
		this.bindingSize = bindingInfo.size();

		Set<String> shared = new HashSet<>(leftInfo.getVariableNames());
		shared.retainAll(rightInfo.getVariableNames());
		this.sharedVariables = Collections.unmodifiableSet(shared);
		this.leftColumnBindingIndex = computeColumnBindingMap(leftInfo, bindingInfo);
		this.rightColumnBindingIndex = computeColumnBindingMap(rightInfo, bindingInfo);
		if (DEBUG) {
			System.out.println("DEBUG bindingSize=" + bindingSize + " mergeVar=" + mergeVariable + " shared="
					+ shared + " bindingVars=" + bindingInfo.getVariableNames());
		}
	}

	@Override
	public long[] next() {
		if (closed) {
			return null;
		}
		try {
			long[] nextRecord = computeNextElement();
			if (nextRecord == null) {
				close();
			}
			return nextRecord;
		} catch (RuntimeException e) {
			close();
			throw e;
		}
	}

	private long[] computeNextElement() throws QueryEvaluationException {
		if (closed) {
			return null;
		}
		if (!ensureCurrentLeft()) {
			return null;
		}

		while (true) {
			if (!rightIterator.hasNext()) {
				if (rightIterator.isResettable() && leftIterator.hasNext()) {
					rightIterator.reset();
					if (!advanceLeft()) {
						return null;
					}
					continue;
				}
				return null;
			}

			long[] rightPeek = rightIterator.peek();
			if (rightPeek == null) {
				return null;
			}

			int compare = compare(currentLeftKey, key(rightPeek));
			if (compare == 0) {
				long[] result = equal();
				if (result != null) {
					return result;
				}
			} else if (compare < 0) {
				if (leftIterator.hasNext()) {
					if (!lessThan()) {
						return null;
					}
				} else {
					return null;
				}
			} else {
				rightIterator.next();
			}
		}
	}

	private boolean ensureCurrentLeft() {
		if (currentLeftRecord != null) {
			return true;
		}
		return advanceLeft();
	}

	private boolean advanceLeft() {
		leftPeekRecord = null;
		hasLeftPeekKey = false;
		currentLeftValueAndPeekEquals = -1;

		while (leftIterator.hasNext()) {
			long[] candidate = leftIterator.next();
			if (candidate == null) {
				continue;
			}
			if (DEBUG && debugCounter.getAndIncrement() < 5) {
				System.out.println("DEBUG advanceLeft len=" + candidate.length + " record="
						+ Arrays.toString(candidate));
			}
			currentLeftRecord = candidate;
			currentLeftKey = key(candidate);
			hasCurrentLeftKey = true;
			return true;
		}

		currentLeftRecord = null;
		hasCurrentLeftKey = false;
		return false;
	}

	private boolean lessThan() {
		long previous = hasCurrentLeftKey ? currentLeftKey : Long.MIN_VALUE;
		if (!advanceLeft()) {
			return false;
		}
		if (hasCurrentLeftKey && previous == currentLeftKey) {
			if (rightIterator.isResettable()) {
				rightIterator.reset();
			}
		} else {
			rightIterator.unmark();
		}
		return true;
	}

	private long[] equal() {
		while (rightIterator.hasNext()) {
			if (rightIterator.isResettable()) {
				long[] result = joinWithCurrentLeft(rightIterator.next());
				if (result != null) {
					return result;
				}
			} else {
				doLeftPeek();
				if (currentLeftValueAndPeekEquals == 0 && !rightIterator.isMarked()) {
					rightIterator.mark();
				}
				long[] result = joinWithCurrentLeft(rightIterator.next());
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}

	private void doLeftPeek() {
		if (leftPeekRecord == null) {
			leftPeekRecord = leftIterator.peek();
			if (leftPeekRecord != null) {
				leftPeekKey = key(leftPeekRecord);
				hasLeftPeekKey = true;
			} else {
				hasLeftPeekKey = false;
			}
			currentLeftValueAndPeekEquals = -1;
		}

		if (currentLeftValueAndPeekEquals == -1) {
			boolean equals = hasLeftPeekKey && hasCurrentLeftKey && currentLeftKey == leftPeekKey;
			currentLeftValueAndPeekEquals = equals ? 0 : 1;
		}
	}

	private long[] joinWithCurrentLeft(long[] rightRecord) {
		if (rightRecord == null) {
			return null;
		}

		long[] combined = new long[bindingSize];
		Arrays.fill(combined, LmdbValue.UNKNOWN_ID);

		if (!mergeRecordInto(combined, currentLeftRecord, leftColumnBindingIndex)) {
			return null;
		}
		if (!mergeRecordInto(combined, rightRecord, rightColumnBindingIndex)) {
			return null;
		}
		if (DEBUG && debugCounter.get() < 50) {
			System.out.println("DEBUG combined=" + Arrays.toString(combined));
		}
		return combined;
	}

	private static int[] computeColumnBindingMap(LmdbIdJoinIterator.PatternInfo patternInfo, IdBindingInfo bindingInfo) {
		int[] map = new int[COLUMN_COUNT];
		Arrays.fill(map, -1);
		for (String var : patternInfo.getVariableNames()) {
			int bindingIdx = bindingInfo.getIndex(var);
			if (bindingIdx < 0) {
				continue;
			}
			int mask = patternInfo.getPositionsMask(var);
			for (int pos = 0; pos < COLUMN_COUNT; pos++) {
				if (((mask >> pos) & 1) != 0) {
					map[pos] = bindingIdx;
				}
			}
		}
		return map;
	}

	private boolean mergeRecordInto(long[] target, long[] source, int[] colMap) {
		if (source == null) {
			return true;
		}
		for (int col = 0; col < colMap.length; col++) {
			int bindingIdx = colMap[col];
			if (bindingIdx < 0 || bindingIdx >= target.length) {
				continue;
			}
			long value = col < source.length ? source[col] : LmdbValue.UNKNOWN_ID;
			if (col == TripleStore.CONTEXT_IDX && value == 0L) {
				value = LmdbValue.UNKNOWN_ID;
			}
			long existing = target[bindingIdx];
			if (existing == LmdbValue.UNKNOWN_ID) {
				if (value != LmdbValue.UNKNOWN_ID) {
					target[bindingIdx] = value;
				}
			} else if (value != LmdbValue.UNKNOWN_ID && existing != value) {
				return false;
			}
		}
		return true;
	}

	private int compare(long left, long right) {
		return Long.compare(left, right);
	}

	private long key(long[] record) {
		long id = valueAt(record, mergeIndex);
		if (id == LmdbValue.UNKNOWN_ID) {
			throw new QueryEvaluationException(
					"Merge variable " + mergeVariable + " is unbound in the current record; cannot perform merge join");
		}
		return id;
	}

	@Override
	public void close() {
		if (closed) {
			return;
		}
		closed = true;
		currentLeftRecord = null;
		hasCurrentLeftKey = false;
		leftPeekRecord = null;
		hasLeftPeekKey = false;
		currentLeftValueAndPeekEquals = -1;
		try {
			leftIterator.close();
		} finally {
			rightIterator.close();
		}
	}

	private static long valueAt(long[] record, int index) {
		if (index < 0 || index >= record.length) {
			return LmdbValue.UNKNOWN_ID;
		}
		return record[index];
	}
}
