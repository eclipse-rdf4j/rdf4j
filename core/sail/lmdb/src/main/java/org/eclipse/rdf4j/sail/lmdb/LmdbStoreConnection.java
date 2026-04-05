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
import java.util.Set;

import org.eclipse.rdf4j.common.concurrent.locks.Lock;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.IterationWrapper;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.SailReadOnlyException;
import org.eclipse.rdf4j.sail.base.SailDataset;
import org.eclipse.rdf4j.sail.base.SailDatasetTripleSource;
import org.eclipse.rdf4j.sail.base.SailSourceConnection;
import org.eclipse.rdf4j.sail.helpers.DefaultSailChangedEvent;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;

/**
 * Connection to an {@link LmdbStore}.
 */
public class LmdbStoreConnection extends SailSourceConnection {

	/*-----------*
	 * Constants *
	 *-----------*/

	protected final LmdbStore lmdbStore;

	/*-----------*
	 * Variables *
	 *-----------*/

	private volatile DefaultSailChangedEvent sailChangedEvent;

	/**
	 * The transaction lock held by this connection during transactions.
	 */
	private volatile Lock txnLock;

	/*--------------*
	 * Constructors *
	 *--------------*/

	protected LmdbStoreConnection(LmdbStore sail) {
		super(sail, sail.getSailStore(), sail.getEvaluationStrategyFactory());
		this.lmdbStore = sail;
		sailChangedEvent = new DefaultSailChangedEvent(sail);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected void startTransactionInternal() throws SailException {
		if (!lmdbStore.isWritable()) {
			throw new SailReadOnlyException("Unable to start transaction: data file is locked or read-only");
		}
		boolean releaseLock = true;
		try {
			if (txnLock == null || !txnLock.isActive()) {
				txnLock = lmdbStore.getTransactionLock(getTransactionIsolation());
				if (lmdbStore.isIsolationDisabled()) {
					// if the transaction isn't isolated then we need to keep holding our exclusive lock until commit
					releaseLock = false;
				}
			}
			super.startTransactionInternal();
		} finally {
			if (releaseLock && txnLock != null && txnLock.isActive()) {
				txnLock.release();
			}
		}
	}

	@Override
	protected void commitInternal() throws SailException {
		try {
			super.commitInternal();
		} finally {
			if (txnLock != null && txnLock.isActive()) {
				txnLock.release();
			}
		}

		lmdbStore.notifySailChanged(sailChangedEvent);

		// create a fresh event object.
		sailChangedEvent = new DefaultSailChangedEvent(lmdbStore);
	}

	@Override
	protected void rollbackInternal() throws SailException {
		try {
			super.rollbackInternal();
		} finally {
			if (txnLock != null && txnLock.isActive()) {
				txnLock.release();
			}
		}
		// create a fresh event object.
		sailChangedEvent = new DefaultSailChangedEvent(lmdbStore);
	}

	@Override
	protected void addStatementInternal(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		// assume the triple is not yet present in the triple store
		sailChangedEvent.setStatementsAdded(true);
	}

	@Override
	public boolean addInferredStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		boolean ret = super.addInferredStatement(subj, pred, obj, contexts);
		// assume the triple is not yet present in the triple store
		sailChangedEvent.setStatementsAdded(true);
		return ret;
	}

	@Override
	protected CloseableIteration<? extends BindingSet> evaluateInternal(TupleExpr tupleExpr,
			Dataset dataset,
			BindingSet bindings, boolean includeInferred) throws SailException {
		// ensure that all elements of the binding set are initialized (lazy values are resolved)
		return new IterationWrapper<BindingSet>(
				evaluateWithTripleSource(tupleExpr, dataset, bindings, includeInferred,
						rdfDataset -> createTripleSource(rdfDataset, dataset, includeInferred))) {
			@Override
			public BindingSet next() throws QueryEvaluationException {
				BindingSet bs = super.next();
				bs.forEach(b -> initValue(b.getValue()));
				return bs;
			}
		};
	}

	private TripleSource createTripleSource(SailDataset rdfDataset, Dataset dataset, boolean includeInferred) {
		TripleSource delegate = new SailDatasetTripleSource(lmdbStore.getValueFactory(), rdfDataset);
		if (!isLftjRuntimeSafe(dataset)) {
			return delegate;
		}
		return new LmdbLftjTripleSource(delegate, createQueryAccess(includeInferred));
	}

	private boolean isLftjRuntimeSafe(Dataset dataset) {
		return lmdbStore.getLmdbStoreConfig().isLftjEnabled()
				&& hasStrongLftjIndexCoverage()
				&& !hasPendingLocalChangesForLftj()
				&& isDefaultDataset(dataset);
	}

	private boolean hasStrongLftjIndexCoverage() {
		return LmdbLftjPlanner.hasRequiredIndexCoverage(lmdbStore.getBackingStore()
				.getTripleStore()
				.getConfiguredIndexSpecs());
	}

	private boolean hasPendingLocalChangesForLftj() {
		return sailChangedEvent.statementsAdded() || sailChangedEvent.statementsRemoved();
	}

	private boolean isDefaultDataset(Dataset dataset) {
		return dataset == null || (dataset.getDefaultGraphs().isEmpty()
				&& dataset.getNamedGraphs().isEmpty()
				&& dataset.getDefaultRemoveGraphs().isEmpty()
				&& dataset.getDefaultInsertGraph() == null);
	}

	private LmdbQueryAccess createQueryAccess(boolean includeInferred) {
		LmdbSailStore backingStore = lmdbStore.getBackingStore();
		TripleStore tripleStore = backingStore.getTripleStore();
		ValueStore valueStore = backingStore.getValueStore();
		return new LmdbQueryAccess() {
			@Override
			public TripleStore tripleStore() {
				return tripleStore;
			}

			@Override
			public TxnManager.Txn acquireReadTxn() {
				try {
					return tripleStore.getTxnManager().createReadTxn();
				} catch (IOException e) {
					throw new SailException(e);
				}
			}

			@Override
			public void releaseReadTxn(TxnManager.Txn txn) {
				txn.close();
			}

			@Override
			public long resolveId(Value value) {
				try {
					return valueStore.getId(value);
				} catch (IOException e) {
					throw new SailException(e);
				}
			}

			@Override
			public Value resolveValue(long id) {
				try {
					return valueStore.getValue(id);
				} catch (IOException e) {
					throw new SailException(e);
				}
			}

			@Override
			public boolean includeInferred() {
				return includeInferred;
			}

			@Override
			public Set<String> configuredIndexes() {
				try {
					return tripleStore.getConfiguredIndexSpecs();
				} catch (SailException e) {
					throw e;
				}
			}

			@Override
			public RecordIterator openScan(TxnManager.Txn txn, String indexName, long subj, long pred, long obj,
					long context, boolean explicit) {
				try {
					return tripleStore.getTriples(txn, indexName, subj, pred, obj, context, explicit);
				} catch (IOException e) {
					throw new SailException(e);
				}
			}

			@Override
			public LmdbTrieKeyCursor openTrieCursor(TxnManager.Txn txn, String indexName, boolean explicit) {
				return tripleStore.openTrieCursor(txn, indexName, explicit);
			}
		};
	}

	@Override
	protected CloseableIteration<? extends Statement> getStatementsInternal(Resource subj, IRI pred,
			Value obj,
			boolean includeInferred, Resource... contexts) throws SailException {
		return new IterationWrapper<Statement>(
				super.getStatementsInternal(subj, pred, obj, includeInferred, contexts)) {
			@Override
			public Statement next() throws SailException {
				// ensure that all elements of the statement are initialized (lazy values are resolved)
				Statement stmt = super.next();
				initValue(stmt.getSubject());
				initValue(stmt.getPredicate());
				initValue(stmt.getObject());
				initValue(stmt.getContext());
				return stmt;
			}
		};
	}

	/**
	 * Ensures that all components of the value are initialized from the underlying database.
	 *
	 * @param value The value that should be initialized
	 */
	protected void initValue(Value value) {
		if (value instanceof LmdbValue) {
			((LmdbValue) value).init();
		}
	}

	@Override
	protected void removeStatementsInternal(Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		sailChangedEvent.setStatementsRemoved(true);
	}

	@Override
	public boolean removeInferredStatement(Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		boolean ret = super.removeInferredStatement(subj, pred, obj, contexts);
		sailChangedEvent.setStatementsRemoved(true);
		return ret;
	}

	@Override
	protected void clearInternal(Resource... contexts) throws SailException {
		super.clearInternal(contexts);
		sailChangedEvent.setStatementsRemoved(true);
	}

	@Override
	public void clearInferred(Resource... contexts) throws SailException {
		super.clearInferred(contexts);
		sailChangedEvent.setStatementsRemoved(true);
	}

	@Override
	protected void closeInternal() throws SailException {
		super.closeInternal();
		// release thread-local pool
		Pool.release();
	}
}
