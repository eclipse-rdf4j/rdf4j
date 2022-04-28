/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.util.Arrays;

import org.eclipse.rdf4j.common.io.ByteArrayUtil;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.AbstractStatement;
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

			return new LazyNativeStatement(nextValue);
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

	/**
	 * Allow to postpone fetching values from the backing store to the moment they are needed. e.g. in a query like
	 * SELECT ?s WHERE {?s ?p ?o} only the ?s value is required saving up to three value look ups (?p ?o and context).
	 */

	private final class LazyNativeStatement extends AbstractStatement {
		private final byte[] nextValue;
		private static final long serialVersionUID = 1L;

		public LazyNativeStatement(byte[] nextValue) {

			this.nextValue = nextValue;
		}

		@Override
		public Resource getSubject() {
			int subjID = ByteArrayUtil.getInt(nextValue, TripleStore.SUBJ_IDX);
			try {
				return (Resource) valueStore.getValue(subjID);
			} catch (IOException e) {
				throw causeIOException(e);
			}
		}

		@Override
		public IRI getPredicate() {
			int predID = ByteArrayUtil.getInt(nextValue, TripleStore.PRED_IDX);
			try {
				return (IRI) valueStore.getValue(predID);
			} catch (IOException e) {
				throw causeIOException(e);
			}
		}

		@Override
		public Value getObject() {
			int objID = ByteArrayUtil.getInt(nextValue, TripleStore.OBJ_IDX);
			try {
				return (Value) valueStore.getValue(objID);
			} catch (IOException e) {
				throw causeIOException(e);
			}
		}

		@Override
		public Resource getContext() {
			int contextID = ByteArrayUtil.getInt(nextValue, TripleStore.CONTEXT_IDX);
			try {
				if (contextID != 0) {
					return (Resource) valueStore.getValue(contextID);
				}
			} catch (IOException e) {
				throw causeIOException(e);
			}
			return null;
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof LazyNativeStatement) {
				return Arrays.equals(nextValue, ((LazyNativeStatement) o).nextValue);
			}
			return super.equals(o);
		}

		protected Object writeReplace() throws ObjectStreamException {
			return valueStore.createStatement(getSubject(), getPredicate(), getObject(), getContext());
		}
	}
}
