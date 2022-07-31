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

import java.io.IOException;

import org.eclipse.rdf4j.common.io.ByteArrayUtil;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.nativerdf.btree.RecordIterator;

/**
 * A statement iterator that wraps a RecordIterator containing statement records and translates these records to
 * {@link Statement} objects.
 */
class NativeStatementIterator extends LookAheadIteration<Statement, SailException> {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final RecordIterator btreeIter;

	private final ValueStore valueStore;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new NativeStatementIterator.
	 */
	public NativeStatementIterator(RecordIterator btreeIter, ValueStore valueStore) throws IOException {
		this.btreeIter = btreeIter;
		this.valueStore = valueStore;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public Statement getNextElement() throws SailException {
		try {
			byte[] nextValue = btreeIter.next();

			if (nextValue == null) {
				return null;
			}

			int subjID = ByteArrayUtil.getInt(nextValue, TripleStore.SUBJ_IDX);
			Resource subj = (Resource) valueStore.getValue(subjID);

			int predID = ByteArrayUtil.getInt(nextValue, TripleStore.PRED_IDX);
			IRI pred = (IRI) valueStore.getValue(predID);

			int objID = ByteArrayUtil.getInt(nextValue, TripleStore.OBJ_IDX);
			Value obj = valueStore.getValue(objID);

			Resource context = null;
			int contextID = ByteArrayUtil.getInt(nextValue, TripleStore.CONTEXT_IDX);
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
				btreeIter.close();
			} catch (IOException e) {
				throw causeIOException(e);
			}
		}
	}

	protected SailException causeIOException(IOException e) {
		return new SailException(e);
	}
}
