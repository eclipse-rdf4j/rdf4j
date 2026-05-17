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

import org.eclipse.rdf4j.common.concurrent.locks.Lock;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.IterationWrapper;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.SailReadOnlyException;
import org.eclipse.rdf4j.sail.base.SailDataset;
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
	private volatile boolean hasPendingChanges;

	/*--------------*
	 * Constructors *
	 *--------------*/

	protected LmdbStoreConnection(LmdbStore sail) {
		super(sail, sail.getSailStore(), sail.getConnectionEvaluationStrategyFactory());
		this.lmdbStore = sail;
		sailChangedEvent = new DefaultSailChangedEvent(sail);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected TripleSource createTripleSource(ValueFactory vf, SailDataset rdfDataset) {
		return new LmdbSailDatasetTripleSource(vf, rdfDataset);
	}

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
			hasPendingChanges = false;
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
			hasPendingChanges = false;
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
		hasPendingChanges = true;
	}

	@Override
	public boolean addInferredStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		boolean ret = super.addInferredStatement(subj, pred, obj, contexts);
		// assume the triple is not yet present in the triple store
		sailChangedEvent.setStatementsAdded(true);
		hasPendingChanges = true;
		return ret;
	}

	@Override
	protected CloseableIteration<? extends BindingSet> evaluateInternal(TupleExpr tupleExpr,
			Dataset dataset,
			BindingSet bindings, boolean includeInferred) throws SailException {
		boolean flagPushed = false;
		SailDataset forcedDataset = null;
		boolean success = false;
		if (hasPendingChanges) {
			LmdbEvaluationStrategy.pushConnectionChangesFlag(true);
			flagPushed = true;
		} else {
			LmdbEvaluationStrategy.pushConnectionChangesFlag(false);
			flagPushed = true;
			forcedDataset = createForcedEvaluationDataset(includeInferred);
			if (forcedDataset != null) {
				LmdbEvaluationStrategy.setForcedDataset((LmdbEvaluationDataset) forcedDataset);
			}
		}
		try {
			CloseableIteration<? extends BindingSet> base = super.evaluateInternal(tupleExpr, dataset, bindings,
					includeInferred);
			success = true;
			SailDataset datasetToClose = forcedDataset;
			// Do not force materialization of lazy values; simply ensure we pop the thread-local flag.
			return new IterationWrapper<BindingSet>(base) {
				@Override
				protected void handleClose() throws QueryEvaluationException {
					try {
						super.handleClose();
					} finally {
						try {
							if (datasetToClose != null) {
								datasetToClose.close();
							}
						} finally {
							LmdbEvaluationStrategy.clearForcedDataset();
							LmdbEvaluationStrategy.popConnectionChangesFlag();
						}
					}
				}
			};
		} catch (RuntimeException e) {
			throw e;
		} finally {
			if (!success) {
				try {
					if (forcedDataset != null) {
						forcedDataset.close();
					}
				} finally {
					LmdbEvaluationStrategy.clearForcedDataset();
					if (flagPushed) {
						LmdbEvaluationStrategy.popConnectionChangesFlag();
					}
				}
			}
		}
	}

	private SailDataset createForcedEvaluationDataset(boolean includeInferred) throws SailException {
		IsolationLevel isolationLevel = effectiveIsolationLevel();
		if (!LmdbEvaluationStrategy.idJoinsAllowedForIsolation(isolationLevel)) {
			return null;
		}
		LmdbSailStore backingStore = lmdbStore.getBackingStore();
		if (includeInferred) {
			SailDataset inferred = null;
			SailDataset explicit = null;
			try {
				inferred = backingStore.getInferredSailSource().dataset(isolationLevel);
				explicit = backingStore.getExplicitSailSource().dataset(isolationLevel);
				return new LmdbUnionSailDataset(inferred, explicit, backingStore.getValueStore());
			} catch (RuntimeException e) {
				try {
					if (inferred != null) {
						inferred.close();
					}
				} finally {
					if (explicit != null) {
						explicit.close();
					}
				}
				throw e;
			}
		}
		SailDataset explicit = backingStore.getExplicitSailSource().dataset(isolationLevel);
		if (explicit instanceof LmdbEvaluationDataset) {
			return explicit;
		}
		explicit.close();
		return null;
	}

	private IsolationLevel effectiveIsolationLevel() {
		return isActive() ? getTransactionIsolation() : lmdbStore.getDefaultIsolationLevel();
	}

	@Override
	protected CloseableIteration<? extends Statement> getStatementsInternal(Resource subj, IRI pred,
			Value obj,
			boolean includeInferred, Resource... contexts) throws SailException {
		boolean flagPushed = false;
		if (hasPendingChanges) {
			LmdbEvaluationStrategy.pushConnectionChangesFlag(true);
			flagPushed = true;
		} else {
			LmdbEvaluationStrategy.pushConnectionChangesFlag(false);
			flagPushed = true;
		}
		try {
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

				@Override
				protected void handleClose() throws SailException {
					try {
						super.handleClose();
					} finally {
						LmdbEvaluationStrategy.popConnectionChangesFlag();
					}
				}
			};
		} catch (RuntimeException e) {
			if (flagPushed) {
				LmdbEvaluationStrategy.popConnectionChangesFlag();
			}
			throw e;
		}
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
		hasPendingChanges = true;
	}

	@Override
	public boolean removeInferredStatement(Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		boolean ret = super.removeInferredStatement(subj, pred, obj, contexts);
		sailChangedEvent.setStatementsRemoved(true);
		hasPendingChanges = true;
		return ret;
	}

	@Override
	protected void clearInternal(Resource... contexts) throws SailException {
		super.clearInternal(contexts);
		sailChangedEvent.setStatementsRemoved(true);
		hasPendingChanges = true;
	}

	@Override
	public void clearInferred(Resource... contexts) throws SailException {
		super.clearInferred(contexts);
		sailChangedEvent.setStatementsRemoved(true);
		hasPendingChanges = true;
	}

	@Override
	protected void closeInternal() throws SailException {
		super.closeInternal();
		// release thread-local pool
		Pool.release();
	}
}
