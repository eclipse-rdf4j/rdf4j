/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import static org.eclipse.rdf4j.sail.nativerdf.NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.eclipse.rdf4j.common.io.ByteArrayUtil;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.nativerdf.btree.RecordIterator;
import org.eclipse.rdf4j.sail.nativerdf.model.CorruptIRI;
import org.eclipse.rdf4j.sail.nativerdf.model.CorruptIRIOrBNode;
import org.eclipse.rdf4j.sail.nativerdf.model.CorruptUnknownValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A statement iterator that wraps a RecordIterator containing statement records and translates these records to
 * {@link Statement} objects.
 */
class NativeStatementIterator implements CloseableIteration<Statement> {

	private static final Logger logger = LoggerFactory.getLogger(NativeStatementIterator.class);

	private final RecordIterator btreeIter;
	private final ValueStore valueStore;

	private Statement nextElement;
	private boolean closed = false;

	public NativeStatementIterator(RecordIterator btreeIter, ValueStore valueStore) {
		this.btreeIter = btreeIter;
		this.valueStore = valueStore;
	}

	public Statement getNextElement() throws SailException {
		try {
			byte[] nextValue;
			try {
				nextValue = btreeIter.next();
			} catch (AssertionError | Exception e) {
				logger.error("Error while reading next value from btree iterator for {}", btreeIter.toString(), e);
				throw e;
			}

			if (nextValue == null) {
				return null;
			}

			int subjID = ByteArrayUtil.getInt(nextValue, TripleStore.SUBJ_IDX);
			Resource subj = valueStore.getResource(subjID);

			int predID = ByteArrayUtil.getInt(nextValue, TripleStore.PRED_IDX);
			IRI pred = valueStore.getIRI(predID);

			int objID = ByteArrayUtil.getInt(nextValue, TripleStore.OBJ_IDX);
			Value obj = valueStore.getValue(objID);

			Resource context = null;
			int contextID = ByteArrayUtil.getInt(nextValue, TripleStore.CONTEXT_IDX);
			if (contextID != 0) {
				context = valueStore.getResource(contextID);
			}
			if (SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES) {
				if (subj == null) {
					subj = new CorruptIRIOrBNode(valueStore.getRevision(), subjID, null);
				}
				if (pred == null) {
					pred = new CorruptIRI(valueStore.getRevision(), predID, null, null);
				}
				if (obj == null) {
					obj = new CorruptUnknownValue(valueStore.getRevision(), objID, null);
				}
			}

			return valueStore.createStatement(subj, pred, obj, context);
		} catch (IOException e) {
			throw causeIOException(e);
		}
	}

	protected void handleClose() throws SailException {
		try {
			btreeIter.close();
		} catch (IOException e) {
			throw causeIOException(e);
		}
	}

	protected SailException causeIOException(IOException e) {
		return new SailException(e);
	}

	@Override
	public final boolean hasNext() {
		if (isClosed()) {
			return false;
		}

		try {
			return lookAhead() != null;
		} catch (NoSuchElementException logged) {
			// The lookAhead() method shouldn't throw a NoSuchElementException since it should return null when there
			// are no more elements.
			logger.trace("LookAheadIteration threw NoSuchElementException:", logged);
			return false;
		}
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
}
