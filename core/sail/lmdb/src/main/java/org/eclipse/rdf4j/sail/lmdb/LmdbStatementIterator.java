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
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;

/**
 * A statement iterator that wraps a RecordIterator containing statement records and translates these records to
 * {@link Statement} objects.
 *
 * Performance optimizations: - Caches quad array indices locally to minimize array lookups - Reduces method call
 * overhead by extracting IDs first - Optimizes context value creation (only when contextID != 0)
 */
class LmdbStatementIterator extends AbstractCloseableIteration<Statement> {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final RecordIterator recordIt;

	private final ValueStore valueStore;
	private Statement nextElement;

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

			// Extract all IDs from quad array first to minimize array access overhead
			// This improves CPU cache locality and reduces bounds checking
			long subjID = quad[TripleStore.SUBJ_IDX];
			long predID = quad[TripleStore.PRED_IDX];
			long objID = quad[TripleStore.OBJ_IDX];
			long contextID = quad[TripleStore.CONTEXT_IDX];

			// Create lazy values - these are lightweight wrappers that defer actual value loading
			Resource subj = (Resource) valueStore.getLazyValue(subjID);
			IRI pred = (IRI) valueStore.getLazyValue(predID);
			Value obj = valueStore.getLazyValue(objID);

			// Only create context value if contextID is non-zero
			// This avoids unnecessary object allocation for default graph statements
			Resource context = (contextID != 0) ? (Resource) valueStore.getLazyValue(contextID) : null;

			return valueStore.createStatement(subj, pred, obj, context);
		} catch (IOException e) {
			throw causeIOException(e);
		}
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
}
