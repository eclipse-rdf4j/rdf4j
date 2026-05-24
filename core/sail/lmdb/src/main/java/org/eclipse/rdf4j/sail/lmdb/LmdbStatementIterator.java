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

	private static final long EMPTY_CACHE_ID = Long.MIN_VALUE;

	/*-----------*
	 * Variables *
	 *-----------*/

	private final RecordIterator recordIt;

	private final ValueStore valueStore;
	private Statement nextElement;
	private long previousSubjectID = EMPTY_CACHE_ID;
	private Resource previousSubject;
	private long previousPredicateID = EMPTY_CACHE_ID;
	private IRI previousPredicate;
	private long previousObjectID = EMPTY_CACHE_ID;
	private Value previousObject;
	private long previousContextID = EMPTY_CACHE_ID;
	private Resource previousContext;

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

	public LmdbStatementIterator(RecordIterator recordIt, ValueStore valueStore,
			long subjectID, Resource subject, long predicateID, IRI predicate, long objectID, Value object,
			long contextID, Resource context) {
		this(recordIt, valueStore);
		if (subject != null) {
			previousSubjectID = subjectID;
			previousSubject = subject;
		}
		if (predicate != null) {
			previousPredicateID = predicateID;
			previousPredicate = predicate;
		}
		if (object != null) {
			previousObjectID = objectID;
			previousObject = object;
		}
		if (contextID == 0 || context != null) {
			previousContextID = contextID;
			previousContext = context;
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

			long subjID = quad[TripleStore.SUBJ_IDX];
			Resource subj = getSubject(subjID);

			long predID = quad[TripleStore.PRED_IDX];
			IRI pred = getPredicate(predID);

			long objID = quad[TripleStore.OBJ_IDX];
			Value obj = getObject(objID);

			long contextID = quad[TripleStore.CONTEXT_IDX];
			Resource context = getContext(contextID);

			return valueStore.createStatement(subj, pred, obj, context);
		} catch (IOException e) {
			throw causeIOException(e);
		}
	}

	private Resource getSubject(long subjectID) throws IOException {
		if (subjectID == previousSubjectID) {
			return previousSubject;
		}
		Resource subject = (Resource) valueStore.getLazyValue(subjectID);
		previousSubjectID = subjectID;
		previousSubject = subject;
		return subject;
	}

	private IRI getPredicate(long predicateID) throws IOException {
		if (predicateID == previousPredicateID) {
			return previousPredicate;
		}
		IRI predicate = valueStore.getLazyPredicate(predicateID);
		previousPredicateID = predicateID;
		previousPredicate = predicate;
		return predicate;
	}

	private Value getObject(long objectID) throws IOException {
		if (objectID == previousObjectID) {
			return previousObject;
		}
		Value object = valueStore.getLazyValue(objectID);
		previousObjectID = objectID;
		previousObject = object;
		return object;
	}

	private Resource getContext(long contextID) throws IOException {
		if (contextID == previousContextID) {
			return previousContext;
		}
		Resource context = null;
		if (contextID != 0) {
			context = (Resource) valueStore.getLazyValue(contextID);
		}
		previousContextID = contextID;
		previousContext = context;
		return context;
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

	@Override
	public long getDistinctCursorSkipCountActual() {
		return recordIt.getDistinctCursorSkipCountActual();
	}

	@Override
	public long getDistinctCursorSkipSeekCountActual() {
		return recordIt.getDistinctCursorSkipSeekCountActual();
	}
}
