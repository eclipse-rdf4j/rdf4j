/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.eclipse.rdf4j.common.io.ByteArrayUtil;
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

	private final long[] quad = new long[4];

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
			Record nextRecord = recordIt.next();

			if (nextRecord == null) {
				return null;
			}

			ByteBuffer key = nextRecord.key;
			Varint.readGroupUnsigned(key, quad);

			long subjID = quad[TripleStore.SUBJ_IDX];
			Resource subj = (Resource) valueStore.getValue(subjID);

			long predID = quad[TripleStore.PRED_IDX];
			IRI pred = (IRI) valueStore.getValue(predID);

			long objID = quad[TripleStore.OBJ_IDX];
			Value obj = valueStore.getValue(objID);

			Resource context = null;
			long contextID = quad[TripleStore.CONTEXT_IDX];
			if (contextID != 0) {
				context = (Resource) valueStore.getValue(contextID);
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
