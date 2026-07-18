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

import java.io.IOException;
import java.util.NoSuchElementException;

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.common.iteration.IndexReportingIterator;
import org.eclipse.rdf4j.common.order.StatementOrder;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;

/**
 * A statement iterator that wraps a RecordIterator containing statement records and translates these records to
 * {@link Statement} objects.
 */
class LmdbStatementIterator extends AbstractCloseableIteration<Statement> implements IndexReportingIterator {

	/**
	 * Number of times the same advisory minimum bound must be hinted before an actual cursor seek is issued. Short lags
	 * are cheaper to resolve by plain iteration; this mirrors the skip-scan threshold in {@link LmdbRecordIterator}.
	 */
	private static final int SEEK_MIN_CALLS = 4;

	private static final String SEEK_ENABLED_PROPERTY = "rdf4j.lmdb.statementSeek.enabled";

	/*-----------*
	 * Variables *
	 *-----------*/

	private final RecordIterator recordIt;

	private final ValueStore valueStore;
	private Statement nextElement;

	private long cachedId1 = Long.MIN_VALUE;
	private long cachedId2 = Long.MIN_VALUE;
	private long cachedId3 = Long.MIN_VALUE;
	private long cachedId4 = Long.MIN_VALUE;
	private Value cachedValue1;
	private Value cachedValue2;
	private Value cachedValue3;
	private Value cachedValue4;

	// Ordered-scan state used to honor advisory seek hints; order == null means hints are ignored.
	private final StatementOrder order;
	private final int orderIdx;
	private final long matchSubj;
	private final long matchPred;
	private final long matchObj;
	private final long matchContext;
	private final boolean seekEnabled;
	private long lastFetchedOrderId = LmdbValue.UNKNOWN_ID;
	private Value lastSeekTarget;
	private int seekCalls;
	private boolean hasMaxId;
	private long maxId;
	private boolean maxIdInclusive;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new LmdbStatementIterator.
	 */
	public LmdbStatementIterator(RecordIterator recordIt, ValueStore valueStore) {
		this(recordIt, valueStore, null, LmdbValue.UNKNOWN_ID, LmdbValue.UNKNOWN_ID, LmdbValue.UNKNOWN_ID,
				LmdbValue.UNKNOWN_ID);
	}

	/**
	 * Creates a new LmdbStatementIterator over an ordered scan that can honor advisory seek hints. The match IDs are
	 * the resolved pattern IDs the scan was opened with ({@link LmdbValue#UNKNOWN_ID} for unbound fields, context bound
	 * iff {@code >= 0}).
	 */
	public LmdbStatementIterator(RecordIterator recordIt, ValueStore valueStore, StatementOrder order, long matchSubj,
			long matchPred, long matchObj, long matchContext) {
		this.recordIt = recordIt;
		this.valueStore = valueStore;
		this.order = order;
		this.orderIdx = orderIdx(order);
		this.matchSubj = matchSubj;
		this.matchPred = matchPred;
		this.matchObj = matchObj;
		this.matchContext = matchContext;
		this.seekEnabled = order != null
				&& Boolean.parseBoolean(System.getProperty(SEEK_ENABLED_PROPERTY, "true"));
	}

	private static int orderIdx(StatementOrder order) {
		if (order == null) {
			return -1;
		}
		switch (order) {
		case S:
			return TripleIndex.SUBJ_IDX;
		case P:
			return TripleIndex.PRED_IDX;
		case O:
			return TripleIndex.OBJ_IDX;
		case C:
			return TripleIndex.CONTEXT_IDX;
		default:
			throw new IllegalArgumentException("Unknown statement order: " + order);
		}
	}

	/*---------*
	 * Methods *
	 *---------*/

	public Statement getNextElement() throws SailException {
		try {
			long[] quad = recordIt.next();
			if (quad == null) {
				return null;
			}

			if (order != null) {
				// the quad array is a reused buffer, so the order component must be copied out
				long orderId = quad[orderIdx];
				if (hasMaxId) {
					int compareToMax = Long.compareUnsigned(orderId, maxId);
					if (compareToMax > 0 || (compareToMax == 0 && !maxIdInclusive)) {
						// the caller declared elements beyond the max bound unusable: report exhaustion
						return null;
					}
				}
				lastFetchedOrderId = orderId;
			}

			long subjID = quad[TripleIndex.SUBJ_IDX];
			Resource subj = (Resource) getLazyValue(subjID);

			long predID = quad[TripleIndex.PRED_IDX];
			IRI pred = (IRI) getLazyValue(predID);

			long objID = quad[TripleIndex.OBJ_IDX];
			Value obj = getLazyValue(objID);

			Resource context = null;
			long contextID = quad[TripleIndex.CONTEXT_IDX];
			if (contextID != 0) {
				context = (Resource) getLazyValue(contextID);
			}

			return valueStore.createStatement(subj, pred, obj, context);
		} catch (IOException e) {
			throw causeIOException(e);
		}
	}

	private Value getLazyValue(long id) throws IOException {
		if (id == cachedId1) {
			return cachedValue1;
		}
		if (id == cachedId2) {
			return cachedValue2;
		}
		if (id == cachedId3) {
			return cachedValue3;
		}
		if (id == cachedId4) {
			return cachedValue4;
		}

		Value value = valueStore.getLazyValue(id);
		cachedId4 = cachedId3;
		cachedValue4 = cachedValue3;
		cachedId3 = cachedId2;
		cachedValue3 = cachedValue2;
		cachedId2 = cachedId1;
		cachedValue2 = cachedValue1;
		cachedId1 = id;
		cachedValue1 = value;
		return value;
	}

	@Override
	protected void handleClose() throws SailException {
		recordIt.close();
	}

	private SailException causeIOException(IOException e) {
		return new SailException(e);
	}

	@Override
	public final boolean hasNext() {
		if (isClosed()) {
			return false;
		}

		return lookAhead() != null;
	}

	@Override
	public final Statement next() {
		if (isClosed()) {
			throw new NoSuchElementException("The iteration has been closed.");
		}
		Statement result = lookAhead();

		if (result != null) {
			nextElement = null;
			return result;
		} else {
			throw new NoSuchElementException();
		}
	}

	/**
	 * Fetches the next element if it hasn't been fetched yet and stores it in {@link #nextElement}.
	 *
	 * @return The next element, or null if there are no more results.
	 */
	private Statement lookAhead() {
		if (nextElement == null) {
			nextElement = getNextElement();

			if (nextElement == null) {
				close();
			}
		}
		return nextElement;
	}

	/**
	 * Throws an {@link UnsupportedOperationException}.
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void seek(Value minValue, boolean minInclusive, Value maxValue, boolean maxInclusive) {
		if (!seekEnabled || isClosed()) {
			return;
		}

		if (maxValue != null) {
			try {
				long id = valueStore.getId(maxValue);
				if (id != LmdbValue.UNKNOWN_ID) {
					hasMaxId = true;
					maxId = id;
					maxIdInclusive = maxInclusive;
				}
			} catch (IOException e) {
				// advisory hint: an unresolvable max bound is simply not applied
			}
		}

		if (minValue == null || nextElement != null) {
			return;
		}

		// LMDB decides whether an actual cursor seek is worth it: only after the same minimum bound has been
		// hinted SEEK_MIN_CALLS times does the lag justify a MDB_SET_RANGE plus target resolution.
		if (minValue == lastSeekTarget || minValue.equals(lastSeekTarget)) {
			seekCalls++;
			if (seekCalls < SEEK_MIN_CALLS) {
				return;
			}
		} else {
			lastSeekTarget = minValue;
			seekCalls = 1;
			return;
		}

		try {
			long targetId = valueStore.getId(minValue);
			if (targetId == LmdbValue.UNKNOWN_ID) {
				return;
			}
			if (!minInclusive) {
				// ids are compared unsigned; UNKNOWN_ID (-1) is the unsigned maximum and was excluded above, so
				// incrementing cannot wrap
				targetId++;
			}
			if (lastFetchedOrderId != LmdbValue.UNKNOWN_ID
					&& Long.compareUnsigned(lastFetchedOrderId, targetId) >= 0) {
				// seekForward requires targets that do not precede the current position
				return;
			}

			// bound match ids (which may be negative as signed longs for inlined values) are kept verbatim;
			// only unbound fields become 0, the minimal key component
			long subj = orderIdx == TripleIndex.SUBJ_IDX ? targetId : boundOrZero(matchSubj);
			long pred = orderIdx == TripleIndex.PRED_IDX ? targetId : boundOrZero(matchPred);
			long obj = orderIdx == TripleIndex.OBJ_IDX ? targetId : boundOrZero(matchObj);
			long context = orderIdx == TripleIndex.CONTEXT_IDX ? targetId : boundOrZero(matchContext);

			if (recordIt.seekForward(subj, pred, obj, context)) {
				// require a fresh miss run before the next seek
				lastSeekTarget = null;
				seekCalls = 0;
			}
		} catch (IOException e) {
			// advisory hint: never fail the query for a declined seek
		}
	}

	private static long boundOrZero(long matchId) {
		return matchId == LmdbValue.UNKNOWN_ID ? 0 : matchId;
	}

	@Override
	public boolean supportsSeek() {
		return seekEnabled && !isClosed();
	}

	@Override
	public String getIndexName() {
		return recordIt.getIndexName();
	}

	@Override
	public long getSourceRowsScannedActual() {
		return recordIt.getSourceRowsScannedActual();
	}

	@Override
	public long getSourceRowsMatchedActual() {
		return recordIt.getSourceRowsMatchedActual();
	}

	@Override
	public long getSourceRowsFilteredActual() {
		return recordIt.getSourceRowsFilteredActual();
	}
}
