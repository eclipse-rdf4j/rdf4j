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

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;

/**
 * A statement iterator that wraps a RecordIterator containing statement records and translates these records to
 * {@link Statement} objects.
 */
class LmdbStatementIterator implements CloseableIteration<Statement> {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final RecordIterator recordIt;

	private final ValueStore valueStore;
	private final StatementCreator statementCreator;
	private Statement nextElement;
	/**
	 * Flag indicating whether this iteration has been closed.
	 */
	private boolean closed = false;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new LmdbStatementIterator with the default statement creation logic.
	 */
	public LmdbStatementIterator(RecordIterator recordIt, ValueStore valueStore) {
		this.recordIt = recordIt;
		this.valueStore = valueStore;
		this.statementCreator = new StatementCreator(valueStore, false, false, false, false);
	}

	/**
	 * Creates a new LmdbStatementIterator using a custom statement creator. The provided {@link StatementCreator} is
	 * responsible for creating the Values and Statement for each record and can cache bound values so the iterator
	 * always returns LmdbValue-backed instances.
	 */
	public LmdbStatementIterator(RecordIterator recordIt, StatementCreator statementCreator) {
		this.recordIt = recordIt;
		this.valueStore = null;
		this.statementCreator = statementCreator;
	}

	/*---------*
	 * Methods *
	 *---------*/

	public Statement getNextElement() throws SailException {
		try {

			if (Thread.currentThread().isInterrupted()) {
				close();
				throw new SailException("The iteration has been interrupted");
			}

			long[] quad = recordIt.next();
			if (quad == null) {
				return null;
			}

			return statementCreator.create(quad);
		} catch (IOException e) {
			throw causeIOException(e);
		}
	}

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

	/**
	 * Checks whether this CloseableIteration has been closed.
	 *
	 * @return <var>true</var> if the CloseableIteration has been closed, <var>false</var> otherwise.
	 */
	public final boolean isClosed() {
		return closed;
	}

	/**
	 * Calls {@link #handleClose()} upon first call and makes sure the resource closures are only executed once.
	 */
	@Override
	public final void close() {
		if (!closed) {
			closed = true;
			handleClose();
		}
	}

	/**
	 * Statement creator that can cache bound values (S, P, O, and/or C) the first time they are seen via
	 * {@link ValueStore#getLazyValue(long)} and reuse them for subsequent records. This ensures bound values are always
	 * returned as LmdbValue-backed instances.
	 */
	static final class StatementCreator {
		private final ValueStore valueStore;
		private final boolean sBound;
		private final boolean pBound;
		private final boolean oBound;
		private final boolean cBound;

		private Resource cachedS;
		private IRI cachedP;
		private Value cachedO;
		private Resource cachedC;

		StatementCreator(ValueStore valueStore, boolean sBound, boolean pBound, boolean oBound, boolean cBound) {
			this.valueStore = valueStore;
			this.sBound = sBound;
			this.pBound = pBound;
			this.oBound = oBound;
			this.cBound = cBound;
		}

		StatementCreator(ValueStore valueStore, Resource cachedS, IRI cachedP, Value cachedO, Resource cachedC,
				boolean sBound, boolean pBound, boolean oBound, boolean cBound) {
			this.valueStore = valueStore;
			this.cachedS = cachedS;
			this.cachedP = cachedP;
			this.cachedO = cachedO;
			this.cachedC = cachedC;
			this.sBound = sBound;
			this.pBound = pBound;
			this.oBound = oBound;
			this.cBound = cBound;
		}

		Statement create(long[] quad) throws IOException {
			Resource s;
			if (sBound) {
				if (cachedS == null) {
					cachedS = (Resource) valueStore.getLazyValue(quad[TripleStore.SUBJ_IDX]);
				}
				s = cachedS;
			} else {
				s = (Resource) valueStore.getLazyValue(quad[TripleStore.SUBJ_IDX]);
			}

			IRI p;
			if (pBound) {
				if (cachedP == null) {
					cachedP = (IRI) valueStore.getLazyValue(quad[TripleStore.PRED_IDX]);
				}
				p = cachedP;
			} else {
				p = (IRI) valueStore.getLazyValue(quad[TripleStore.PRED_IDX]);
			}

			Value o;
			if (oBound) {
				if (cachedO == null) {
					cachedO = valueStore.getLazyValue(quad[TripleStore.OBJ_IDX]);
				}
				o = cachedO;
			} else {
				o = valueStore.getLazyValue(quad[TripleStore.OBJ_IDX]);
			}

			Resource c = null;
			long contextID = quad[TripleStore.CONTEXT_IDX];
			if (cBound) {
				if (cachedC == null) {
					if (contextID != 0) {
						cachedC = (Resource) valueStore.getLazyValue(contextID);
					} else {
						cachedC = null; // default graph
					}
				}
				c = cachedC;
			} else if (contextID != 0) {
				c = (Resource) valueStore.getLazyValue(contextID);
			}

			return valueStore.createStatement(s, p, o, c);
		}
	}
}
