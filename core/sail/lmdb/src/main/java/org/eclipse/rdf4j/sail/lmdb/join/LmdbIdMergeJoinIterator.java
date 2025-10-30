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

import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.sail.lmdb.RecordIterator;
import org.eclipse.rdf4j.sail.lmdb.ValueStore;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;

public class LmdbIdMergeJoinIterator extends LookAheadIteration<BindingSet> {

	private final PeekMarkRecordIterator leftIterator;
	private final PeekMarkRecordIterator rightIterator;
	private final LmdbIdJoinIterator.PatternInfo leftInfo;
	private final LmdbIdJoinIterator.PatternInfo rightInfo;
	private final String mergeVariable;
	private final QueryEvaluationContext context;
	private final BindingSet initialBindings;
	private final ValueStore valueStore;

	private long[] currentLeftRecord;
	private MutableBindingSet currentLeftBinding;
	private long currentLeftKey;
	private boolean hasCurrentLeftKey;

	private long[] leftPeekRecord;
	private long leftPeekKey;
	private boolean hasLeftPeekKey;
	private int currentLeftValueAndPeekEquals = -1;

	public LmdbIdMergeJoinIterator(RecordIterator leftIterator, RecordIterator rightIterator,
			LmdbIdJoinIterator.PatternInfo leftInfo, LmdbIdJoinIterator.PatternInfo rightInfo, String mergeVariable,
			QueryEvaluationContext context, BindingSet initialBindings, ValueStore valueStore) {
		this.leftIterator = new PeekMarkRecordIterator(leftIterator);
		this.rightIterator = new PeekMarkRecordIterator(rightIterator);
		this.leftInfo = leftInfo;
		this.rightInfo = rightInfo;
		this.mergeVariable = mergeVariable;
		this.context = context;
		this.initialBindings = initialBindings;
		this.valueStore = valueStore;

		if (mergeVariable == null || mergeVariable.isEmpty()) {
			throw new IllegalArgumentException("Merge variable must be provided for LMDB merge join");
		}
		if (leftInfo.getRecordIndex(mergeVariable) < 0 || rightInfo.getRecordIndex(mergeVariable) < 0) {
			throw new IllegalArgumentException("Merge variable " + mergeVariable
					+ " must be present in both join operands for LMDB merge join");
		}
	}

	@Override
	protected BindingSet getNextElement() throws QueryEvaluationException {
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

			int compare = compare(currentLeftKey, key(rightInfo, rightPeek));
			if (compare == 0) {
				BindingSet result = equal();
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

	private boolean ensureCurrentLeft() throws QueryEvaluationException {
		if (currentLeftRecord != null) {
			return true;
		}
		return advanceLeft();
	}

	private boolean advanceLeft() throws QueryEvaluationException {
		leftPeekRecord = null;
		hasLeftPeekKey = false;
		currentLeftValueAndPeekEquals = -1;

		while (leftIterator.hasNext()) {
			long[] candidate = leftIterator.next();
			// Defer left materialization; keep only the ID record.
			currentLeftRecord = candidate;
			currentLeftBinding = null;
			currentLeftKey = key(leftInfo, candidate);
			hasCurrentLeftKey = true;
			return true;
		}

		currentLeftRecord = null;
		currentLeftBinding = null;
		hasCurrentLeftKey = false;
		return false;
	}

	private boolean lessThan() throws QueryEvaluationException {
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

	private BindingSet equal() throws QueryEvaluationException {
		while (rightIterator.hasNext()) {
			if (rightIterator.isResettable()) {
				BindingSet result = joinWithCurrentLeft(rightIterator.next());
				if (result != null) {
					return result;
				}
			} else {
				doLeftPeek();
				if (currentLeftValueAndPeekEquals == 0 && !rightIterator.isMarked()) {
					rightIterator.mark();
				}
				BindingSet result = joinWithCurrentLeft(rightIterator.next());
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}

	private void doLeftPeek() throws QueryEvaluationException {
		if (leftPeekRecord == null) {
			leftPeekRecord = leftIterator.peek();
			if (leftPeekRecord != null) {
				leftPeekKey = key(leftInfo, leftPeekRecord);
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

	private BindingSet joinWithCurrentLeft(long[] rightRecord) throws QueryEvaluationException {
		MutableBindingSet result = context.createBindingSet(initialBindings);
		if (!leftInfo.applyRecord(currentLeftRecord, result, valueStore)) {
			return null;
		}
		if (!rightInfo.applyRecord(rightRecord, result, valueStore)) {
			return null;
		}
		return result;
	}

	private int compare(long left, long right) {
		return Long.compare(left, right);
	}

	private long key(LmdbIdJoinIterator.PatternInfo info, long[] record) throws QueryEvaluationException {
		long id = info.getId(record, mergeVariable);
		if (id == LmdbValue.UNKNOWN_ID) {
			throw new QueryEvaluationException(
					"Merge variable " + mergeVariable + " is unbound in the current record; cannot perform merge join");
		}
		return id;
	}

	@Override
	protected void handleClose() throws QueryEvaluationException {
		try {
			leftIterator.close();
		} finally {
			rightIterator.close();
		}
	}
}
