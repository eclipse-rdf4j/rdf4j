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
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;

/**
 * A statement iterator that wraps a RecordIterator containing statement records and translates these records to
 * {@link Statement} objects.
 */
class LmdbStatementIterator extends AbstractCloseableIteration<Statement> implements IndexReportingIterator {

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

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new LmdbStatementIterator.
	 */
	public LmdbStatementIterator(RecordIterator recordIt, ValueStore valueStore) {
		this.recordIt = recordIt;
		this.valueStore = valueStore;
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
