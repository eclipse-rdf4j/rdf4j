/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.s3;

import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.concurrent.locks.Lock;
import org.eclipse.rdf4j.common.concurrent.locks.LockManager;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategyFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolverClient;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategyFactory;
import org.eclipse.rdf4j.repository.sparql.federation.SPARQLServiceResolver;
import org.eclipse.rdf4j.sail.InterruptedSailException;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailSource;
import org.eclipse.rdf4j.sail.base.SailStore;
import org.eclipse.rdf4j.sail.base.SnapshotSailStore;
import org.eclipse.rdf4j.sail.helpers.AbstractNotifyingSail;
import org.eclipse.rdf4j.sail.s3.config.S3StoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A SAIL implementation that stores RDF data on S3-compatible object storage using an LSM-tree architecture with
 * Parquet files, stats-based pruning, and multi-tier caching (Caffeine heap + disk LRU + S3).
 *
 * <p>
 * Supports three modes: S3 persistence (bucket configured), local filesystem persistence (dataDir configured), or pure
 * in-memory (neither configured).
 * </p>
 *
 * @implNote the S3 store is in an experimental state: its existence, signature or behavior may change without warning
 *           from one release to the next.
 */
@Experimental
public class S3Store extends AbstractNotifyingSail implements FederatedServiceResolverClient {

	private static final Logger logger = LoggerFactory.getLogger(S3Store.class);

	private final S3StoreConfig config;
	private SailStore store;
	private S3SailStore backingStore;
	private EvaluationStrategyFactory evalStratFactory;

	/**
	 * independent life cycle
	 */
	private FederatedServiceResolver serviceResolver;

	/**
	 * dependent life cycle
	 */
	private SPARQLServiceResolver dependentServiceResolver;

	/**
	 * Lock manager used to prevent concurrent {@link #getTransactionLock(IsolationLevel)} calls.
	 */
	private final ReentrantLock txnLockManager = new ReentrantLock();

	/**
	 * Holds locks for all isolated transactions.
	 */
	private final LockManager isolatedLockManager = new LockManager(debugEnabled());

	/**
	 * Holds locks for all {@link IsolationLevels#NONE} isolation transactions.
	 */
	private final LockManager disabledIsolationLockManager = new LockManager(debugEnabled());

	public S3Store() {
		this(new S3StoreConfig());
	}

	public S3Store(S3StoreConfig config) {
		super();
		this.config = config;
		setSupportedIsolationLevels(IsolationLevels.NONE, IsolationLevels.READ_COMMITTED,
				IsolationLevels.SNAPSHOT_READ, IsolationLevels.SNAPSHOT, IsolationLevels.SERIALIZABLE);
		setDefaultIsolationLevel(IsolationLevels.SNAPSHOT_READ);
		config.getDefaultQueryEvaluationMode().ifPresent(this::setDefaultQueryEvaluationMode);
		EvaluationStrategyFactory evalFactory = config.getEvaluationStrategyFactory();
		if (evalFactory != null) {
			setEvaluationStrategyFactory(evalFactory);
		}
	}

	public synchronized EvaluationStrategyFactory getEvaluationStrategyFactory() {
		if (evalStratFactory == null) {
			evalStratFactory = new StrictEvaluationStrategyFactory(getFederatedServiceResolver());
		}
		evalStratFactory.setQuerySolutionCacheThreshold(getIterationCacheSyncThreshold());
		evalStratFactory.setTrackResultSize(isTrackResultSize());
		return evalStratFactory;
	}

	public synchronized void setEvaluationStrategyFactory(EvaluationStrategyFactory factory) {
		evalStratFactory = factory;
	}

	public synchronized FederatedServiceResolver getFederatedServiceResolver() {
		if (serviceResolver == null) {
			if (dependentServiceResolver == null) {
				dependentServiceResolver = new SPARQLServiceResolver();
			}
			setFederatedServiceResolver(dependentServiceResolver);
		}
		return serviceResolver;
	}

	@Override
	public synchronized void setFederatedServiceResolver(FederatedServiceResolver resolver) {
		this.serviceResolver = resolver;
		if (resolver != null && evalStratFactory instanceof FederatedServiceResolverClient) {
			((FederatedServiceResolverClient) evalStratFactory).setFederatedServiceResolver(resolver);
		}
	}

	@Override
	protected void initializeInternal() throws SailException {
		logger.debug("Initializing S3Store...");

		try {
			backingStore = new S3SailStore(config);
			this.store = new SnapshotSailStore(backingStore, () -> new org.eclipse.rdf4j.model.impl.LinkedHashModel()) {

				@Override
				public SailSource getExplicitSailSource() {
					if (isIsolationDisabled()) {
						return backingStore.getExplicitSailSource();
					} else {
						return super.getExplicitSailSource();
					}
				}

				@Override
				public SailSource getInferredSailSource() {
					if (isIsolationDisabled()) {
						return backingStore.getInferredSailSource();
					} else {
						return super.getInferredSailSource();
					}
				}
			};
		} catch (Exception e) {
			throw new SailException(e);
		}

		logger.debug("S3Store initialized");
	}

	@Override
	protected void shutDownInternal() throws SailException {
		logger.debug("Shutting down S3Store...");

		try {
			store.close();
		} finally {
			if (dependentServiceResolver != null) {
				dependentServiceResolver.shutDown();
			}
		}

		logger.debug("S3Store shut down");
	}

	@Override
	public boolean isWritable() {
		return true;
	}

	@Override
	protected NotifyingSailConnection getConnectionInternal() throws SailException {
		return new S3StoreConnection(this);
	}

	@Override
	public ValueFactory getValueFactory() {
		return store.getValueFactory();
	}

	/**
	 * This call will block when {@link IsolationLevels#NONE} is provided when there are active transactions with a
	 * higher isolation and block when a higher isolation is provided when there are active transactions with
	 * {@link IsolationLevels#NONE} isolation.
	 */
	Lock getTransactionLock(IsolationLevel level) throws SailException {
		txnLockManager.lock();
		try {
			if (IsolationLevels.NONE.isCompatibleWith(level)) {
				isolatedLockManager.waitForActiveLocks();
				return disabledIsolationLockManager.createLock(level.toString());
			} else {
				disabledIsolationLockManager.waitForActiveLocks();
				return isolatedLockManager.createLock(level.toString());
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new InterruptedSailException(e);
		} finally {
			txnLockManager.unlock();
		}
	}

	boolean isIsolationDisabled() {
		return disabledIsolationLockManager.isActiveLock();
	}

	SailStore getSailStore() {
		return store;
	}

	S3SailStore getBackingStore() {
		return backingStore;
	}
}
