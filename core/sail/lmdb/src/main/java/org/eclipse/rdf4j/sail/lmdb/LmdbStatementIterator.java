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

import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;

/**
 * A statement iterator that wraps a RecordIterator containing statement records and translates these records to
 * {@link Statement} objects.
 */
class LmdbStatementIterator extends LookAheadIteration<Statement, SailException> {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final RecordIterator recordIt;

	private final ValueStore valueStore;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new LmdbStatementIterator.
	 */
	public LmdbStatementIterator(RecordIterator recordIt, ValueStore valueStore) throws IOException {
		this.recordIt = recordIt;
		this.valueStore = valueStore;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public Statement getNextElement() throws SailException {
		try {
			long[] quad = recordIt.next();
			if (quad == null) {
				return null;
			}

			long subjID = quad[TripleStore.SUBJ_IDX];
			Resource subj = (Resource) valueStore.getLazyValue(subjID);

			long predID = quad[TripleStore.PRED_IDX];
			IRI pred = (IRI) valueStore.getLazyValue(predID);

			long objID = quad[TripleStore.OBJ_IDX];
			Value obj = valueStore.getLazyValue(objID);

			Resource context = null;
			long contextID = quad[TripleStore.CONTEXT_IDX];
			if (contextID != 0) {
				context = (Resource) valueStore.getLazyValue(contextID);
			}

			return valueStore.createStatement(subj, pred, obj, context);
		} catch (IOException e) {
			throw causeIOException(e);
		}
	}

	@Override
	protected void handleClose() throws SailException {
		try {
			super.handleClose();
		} finally {
			try {
				recordIt.close();
			} catch (IOException e) {
				throw causeIOException(e);
			}
		}
	}

	protected SailException causeIOException(IOException e) {
		return new SailException(e);
	}
}
