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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.collection.factory.api.CollectionFactory;
import org.eclipse.rdf4j.collection.factory.mapdb.MapDb3CollectionFactory;
import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.concurrent.locks.Lock;
import org.eclipse.rdf4j.common.concurrent.locks.LockManager;
import org.eclipse.rdf4j.common.io.MavenUtil;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
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
import org.eclipse.rdf4j.sail.helpers.DirectoryLockManager;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A SAIL implementation using LMDB for storing and querying its data.
 *
 * @implNote the LMDB store is is in an experimental state: its existence, signature or behavior may change without
 *           warning from one release to the next.
 */
@Experimental
public class LmdbStore extends AbstractNotifyingSail implements FederatedServiceResolverClient {

	private static final Logger logger = LoggerFactory.getLogger(LmdbStore.class);

	/*-----------*
	 * Variables *
	 *-----------*/

	private static final String VERSION = MavenUtil.loadVersion("org.eclipse.rdf4j", "rdf4j-sail-lmdb", "devel");

	/**
	 * Specifies which triple indexes this lmdb store must use.
	 */
	private final LmdbStoreConfig config;

	private SailStore store;

	private LmdbSailStore backingStore;

	// used to decide if store is writable, is true if the store was writable during initialization
	private boolean isWritable;

	// indicates if a datadir is temporary (i.e. will be deleted on shutdown)
	private boolean isTmpDatadir = false;

	/**
	 * Data directory lock.
	 */
	private volatile Lock dirLock;

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

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new LmdbStore with default settings.
	 */
	public LmdbStore() {
		this(new LmdbStoreConfig());
	}

	/**
	 * Creates a new LmdbStore.
	 */
	public LmdbStore(LmdbStoreConfig config) {
		super();
		this.config = config;
		setSupportedIsolationLevels(IsolationLevels.NONE, IsolationLevels.READ_COMMITTED, IsolationLevels.SNAPSHOT_READ,
				IsolationLevels.SNAPSHOT, IsolationLevels.SERIALIZABLE);
		setDefaultIsolationLevel(IsolationLevels.SNAPSHOT_READ);
		config.getDefaultQueryEvaluationMode().ifPresent(this::setDefaultQueryEvaluationMode);
		if (config.getIterationCacheSyncThreshold() > 0) {
			setIterationCacheSyncThreshold(config.getIterationCacheSyncThreshold());
		}
		EvaluationStrategyFactory evalStrategyFactory = config.getEvaluationStrategyFactory();
		if (evalStrategyFactory != null) {
			setEvaluationStrategyFactory(evalStrategyFactory);
		}
	}

	/**
	 * Creates a new LmdbStore with default settings.
	 */
	public LmdbStore(File dataDir) {
		this(dataDir, new LmdbStoreConfig());
	}

	public LmdbStore(File dataDir, LmdbStoreConfig config) {
		this(config);
		setDataDir(dataDir);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public void setDataDir(File dataDir) {
		super.setDataDir(dataDir);
		isTmpDatadir = (dataDir == null);
	}

	/**
	 * @return Returns the {@link EvaluationStrategy}.
	 */
	public synchronized EvaluationStrategyFactory getEvaluationStrategyFactory() {
		if (evalStratFactory == null) {
			evalStratFactory = new StrictEvaluationStrategyFactory(getFederatedServiceResolver());
		}
		evalStratFactory.setQuerySolutionCacheThreshold(getIterationCacheSyncThreshold());
		evalStratFactory.setTrackResultSize(isTrackResultSize());
		evalStratFactory.setCollectionFactory(getCollectionFactory());
		return evalStratFactory;
	}

	public boolean getPageCardinalityEstimator() {
		return config.getPageCardinalityEstimator();
	}

	/**
	 * Sets the {@link EvaluationStrategy} to use.
	 */
	public synchronized void setEvaluationStrategyFactory(EvaluationStrategyFactory factory) {
		evalStratFactory = factory;
	}

	/**
	 * @return Returns the SERVICE resolver.
	 */
	public synchronized FederatedServiceResolver getFederatedServiceResolver() {
		if (serviceResolver == null) {
			if (dependentServiceResolver == null) {
				dependentServiceResolver = new SPARQLServiceResolver();
			}
			setFederatedServiceResolver(dependentServiceResolver);
		}
		return serviceResolver;
	}

	/**
	 * Overrides the {@link FederatedServiceResolver} used by this instance, but the given resolver is not shutDown when
	 * this instance is.
	 *
	 * @param resolver The SERVICE resolver to set.
	 */
	@Override
	public synchronized void setFederatedServiceResolver(FederatedServiceResolver resolver) {
		this.serviceResolver = resolver;
		if (resolver != null && evalStratFactory instanceof FederatedServiceResolverClient) {
			((FederatedServiceResolverClient) evalStratFactory).setFederatedServiceResolver(resolver);
		}
	}

	/**
	 * Initializes this LmdbStore.
	 *
	 * @throws SailException If this LmdbStore could not be initialized using the parameters that have been set.
	 */
	@Override
	protected void initializeInternal() throws SailException {
		logger.debug("Initializing LmdbStore...");

		// Check initialization parameters
		File dataDir = getDataDir();

		if (dataDir == null) {
			try {
				setDataDir(Files.createTempDirectory("rdf4j-lmdb-tmp").toFile());
				isTmpDatadir = true;
			} catch (IOException ioe) {
				throw new SailException("Temp data dir could not be created");
			}
			dataDir = getDataDir();
		} else if (!dataDir.exists()) {
			boolean success = dataDir.mkdirs();
			if (!success) {
				throw new SailException("Unable to create data directory: " + dataDir);
			}
		} else if (!dataDir.isDirectory()) {
			throw new SailException("The specified path does not denote a directory: " + dataDir);
		} else if (!dataDir.canRead()) {
			throw new SailException("Not allowed to read from the specified directory: " + dataDir);
		}

		// try to lock the directory or fail
		dirLock = new DirectoryLockManager(dataDir).lockOrFail();

		logger.debug("Data dir is " + dataDir);

		try {
			File versionFile = new File(dataDir, "lmdbrdf.ver");
			String version = versionFile.exists() ? FileUtils.readFileToString(versionFile, StandardCharsets.UTF_8)
					: null;
			if (!VERSION.equals(version) && upgradeStore(dataDir, version)) {
				FileUtils.writeStringToFile(versionFile, VERSION, StandardCharsets.UTF_8);
			}
			backingStore = new LmdbSailStore(dataDir, config);
			this.store = new SnapshotSailStore(backingStore, () -> new MemoryOverflowModel(false) {
				@Override
				protected LmdbSailStore createSailStore(File dataDir) throws IOException, SailException {
					// Model can't fit into memory, use another LmdbSailStore to store delta
					LmdbSailStore lmdbSailStore = new LmdbSailStore(dataDir, config);
					lmdbSailStore.enableMultiThreading = false;
					return lmdbSailStore;
				}
			}) {

				@Override
				public SailSource getExplicitSailSource() {
					if (isIsolationDisabled()) {
						// no isolation, use LmdbSailStore directly
						return backingStore.getExplicitSailSource();
					} else {
						return super.getExplicitSailSource();
					}
				}

				@Override
				public SailSource getInferredSailSource() {
					if (isIsolationDisabled()) {
						// no isolation, use LmdbSailStore directly
						return backingStore.getInferredSailSource();
					} else {
						return super.getInferredSailSource();
					}
				}
			};
		} catch (Throwable e) {
			// LmdbStore initialization failed, release any allocated files
			dirLock.release();

			throw new SailException(e);
		}

		isWritable = getDataDir().canWrite();

		logger.debug("LmdbStore initialized");
	}

	@Override
	protected void shutDownInternal() throws SailException {
		logger.debug("Shutting down LmdbStore...");

		try {
			store.close();
		} finally {
			dirLock.release();
			if (dependentServiceResolver != null) {
				dependentServiceResolver.shutDown();
			}
		}

		if (isTmpDatadir) {
			File dataDir = getDataDir();
			if (dataDir != null) {
				try {
					try (Stream<Path> walk = Files.walk(dataDir.toPath())) {
						walk
								.map(Path::toFile)
								.sorted(Comparator.reverseOrder()) // delete files before directory
								.forEach(File::delete);
					}

				} catch (IOException ioe) {
					logger.error("Could not delete temp file " + dataDir);
				}
			}
		}
		logger.debug("LmdbStore shut down");
	}

	@Override
	public void shutDown() throws SailException {
		super.shutDown();
		// edge case when re-initialize after shutdown
		if (isTmpDatadir) {
			setDataDir(null);
		}
	}

	@Override
	public boolean isWritable() {
		return isWritable;
	}

	@Override
	protected NotifyingSailConnection getConnectionInternal() throws SailException {
		return new LmdbStoreConnection(this);
	}

	@Override
	public ValueFactory getValueFactory() {
		return store.getValueFactory();
	}

	/**
	 * This call will block when {@link IsolationLevels#NONE} is provided when there are active transactions with a
	 * higher isolation and block when a higher isolation is provided when there are active transactions with
	 * {@link IsolationLevels#NONE} isolation. Store is either exclusively in {@link IsolationLevels#NONE} isolation
	 * with potentially zero or more transactions, or exclusively in higher isolation mode with potentially zero or more
	 * transactions.
	 *
	 * @param level indicating desired mode {@link IsolationLevels#NONE} or higher
	 * @return Lock used to prevent Store from switching isolation modes
	 * @throws SailException
	 */
	protected Lock getTransactionLock(IsolationLevel level) throws SailException {
		txnLockManager.lock();
		try {
			if (IsolationLevels.NONE.isCompatibleWith(level)) {
				// make sure no isolated transaction are active
				isolatedLockManager.waitForActiveLocks();
				// mark isolation as disabled
				return disabledIsolationLockManager.createLock(level.toString());
			} else {
				// make sure isolation is not disabled
				disabledIsolationLockManager.waitForActiveLocks();
				// mark isolated transaction as active
				return isolatedLockManager.createLock(level.toString());
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new InterruptedSailException(e);
		} finally {
			txnLockManager.unlock();
		}
	}

	/**
	 * Checks if any {@link IsolationLevels#NONE} isolation transactions are active.
	 *
	 * @return <code>true</code> if at least one transaction has direct access to the indexes
	 */
	boolean isIsolationDisabled() {
		return disabledIsolationLockManager.isActiveLock();
	}

	SailStore getSailStore() {
		return store;
	}

	LmdbSailStore getBackingStore() {
		return backingStore;
	}

	private boolean upgradeStore(File dataDir, String version) throws SailException {
		// nothing to do, just update version number
		return true;
	}

	@Override
	public Supplier<CollectionFactory> getCollectionFactory() {
		return () -> new MapDb3CollectionFactory(getIterationCacheSyncThreshold());
	}

}
