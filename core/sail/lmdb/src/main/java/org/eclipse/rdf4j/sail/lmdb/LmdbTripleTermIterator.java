/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
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
import org.eclipse.rdf4j.model.TripleTerm;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbTripleTerm;

/**
 * An iterator that wraps a RecordIterator containing triple term records and translates these records to
 * {@link TripleTerm} objects.
 */
class LmdbTripleTermIterator extends AbstractCloseableIteration<TripleTerm> implements IndexReportingIterator {

	private final RecordIterator recordIt;

	private final ValueStore valueStore;
	private TripleTerm nextElement;

	/**
	 * Creates a new LmdbTripleTermIterator.
	 */
	public LmdbTripleTermIterator(RecordIterator recordIt, ValueStore valueStore) {
		this.recordIt = recordIt;
		this.valueStore = valueStore;
	}

	public TripleTerm getNextElement() throws SailException {
		try {
			long[] quad = recordIt.next();
			if (quad == null) {
				return null;
			}

			long subjID = quad[TripleIndex.SUBJ_IDX];
			Resource subj = (Resource) valueStore.getLazyValue(subjID);

			long predID = quad[TripleIndex.PRED_IDX];
			IRI pred = (IRI) valueStore.getLazyValue(predID);

			long objID = quad[TripleIndex.OBJ_IDX];
			Value obj = valueStore.getLazyValue(objID);

			long termID = quad[TripleIndex.CONTEXT_IDX];
			return new LmdbTripleTerm(valueStore.getRevision(), subj, pred, obj, termID);
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
	public final TripleTerm next() {
		if (isClosed()) {
			throw new NoSuchElementException("The iteration has been closed.");
		}
		TripleTerm result = lookAhead();

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
	private TripleTerm lookAhead() {
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
