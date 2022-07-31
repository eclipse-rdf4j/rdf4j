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

import java.io.File;
import java.io.IOException;
import java.lang.ref.Cleaner;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.FileUtils;
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
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailSource;
import org.eclipse.rdf4j.sail.base.SailStore;
import org.eclipse.rdf4j.sail.base.SnapshotSailStore;
import org.eclipse.rdf4j.sail.helpers.AbstractNotifyingSail;
import org.eclipse.rdf4j.sail.helpers.DirectoryLockManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A SAIL implementation using B-Tree indexing on disk for storing and querying its data.
 *
 * The NativeStore is designed for datasets between 100,000 and 100 million triples. On most operating systems, if there
 * is sufficient physical memory, the NativeStore will act like the MemoryStore, because the read/write commands will be
 * cached by the OS. This technique allows the NativeStore to operate quite well for millions of triples.
 *
 * @author Arjohn Kampman
 * @author jeen
 */
public class NativeStore extends AbstractNotifyingSail implements FederatedServiceResolverClient {

	private static final Logger logger = LoggerFactory.getLogger(NativeStore.class);

	private static final String VERSION = MavenUtil.loadVersion("org.eclipse.rdf4j", "rdf4j-sail-nativerdf", "devel");

	private static final Cleaner REMOVE_STORES_USED_FOR_MEMORY_OVERFLOW = Cleaner.create();

	/**
	 * When we are close to running out of memory we start using a native store instead of a model in memory.
	 * Performance craters to near zero. So it is dubious if this is worth the effort. The class is static to avoid
	 * taking a pointer which might make it hard to get a phantom reference.
	 */
	final static class MemoryOverflowIntoNativeStore extends MemoryOverflowModel {
		private static final long serialVersionUID = 1L;

		/**
		 * The class is static to avoid taking a pointer which might make it hard to get a phantom reference.
		 */
		private static final class OverFlowStoreCleaner implements Runnable {
			private final NativeSailStore nativeSailStore;
			private final File dataDir;

			private OverFlowStoreCleaner(NativeSailStore nativeSailStore, File dataDir) {
				this.nativeSailStore = nativeSailStore;
				this.dataDir = dataDir;
			}

			@Override
			public void run() {
				try {
					nativeSailStore.close();
				} finally {
					try {
						FileUtils.deleteDirectory(dataDir);
					} catch (IOException e) {
						NativeStore.logger.error("Could not remove data dir of overflow model store", e);
					}
				}
			}
		}

		@Override
		protected SailStore createSailStore(File dataDir) throws IOException, SailException {
			// Model can't fit into memory, use another NativeSailStore to store delta
			NativeSailStore nativeSailStore = new NativeSailStore(dataDir, "spoc");
			// Once the model is no longer reachable (i.e. phantom reference we can close the
			// backingstore.
			REMOVE_STORES_USED_FOR_MEMORY_OVERFLOW.register(this, new OverFlowStoreCleaner(nativeSailStore, dataDir));
			return nativeSailStore;
		}
	}

	/**
	 * Specifies which triple indexes this native store must use.
	 */
	private volatile String tripleIndexes;

	/**
	 * Flag indicating whether updates should be synced to disk forcefully. This may have a severe impact on write
	 * performance. By default, this feature is disabled.
	 */
	private volatile boolean forceSync = false;

	private volatile int valueCacheSize = ValueStore.VALUE_CACHE_SIZE;

	private volatile int valueIDCacheSize = ValueStore.VALUE_ID_CACHE_SIZE;

	private volatile int namespaceCacheSize = ValueStore.NAMESPACE_CACHE_SIZE;

	private volatile int namespaceIDCacheSize = ValueStore.NAMESPACE_ID_CACHE_SIZE;

	private SailStore store;

	// used to decide if store is writable, is true if the store was writable during initialization
	private boolean isWritable;

	// indicates if a datadir is temporary (i.e. will be deleted on shutdown)
	private boolean isTmpDatadir = false;

	/**
	 * Data directory lock.
	 */
	private volatile Lock dirLock;

	private EvaluationStrategyFactory evalStratFactory;

	/** independent life cycle */
	private FederatedServiceResolver serviceResolver;

	/** dependent life cycle */
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
	 * Creates a new NativeStore.
	 */
	public NativeStore() {
		super();
		setSupportedIsolationLevels(IsolationLevels.NONE, IsolationLevels.READ_COMMITTED, IsolationLevels.SNAPSHOT_READ,
				IsolationLevels.SNAPSHOT, IsolationLevels.SERIALIZABLE);
		setDefaultIsolationLevel(IsolationLevels.SNAPSHOT_READ);
	}

	public NativeStore(File dataDir) {
		this();
		setDataDir(dataDir);
	}

	public NativeStore(File dataDir, String tripleIndexes) {
		this(dataDir);
		setTripleIndexes(tripleIndexes);
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
	 * Sets the triple indexes for the native store, must be called before initialization.
	 *
	 * @param tripleIndexes An index strings, e.g. <var>spoc,posc</var>.
	 */
	public void setTripleIndexes(String tripleIndexes) {
		if (isInitialized()) {
			throw new IllegalStateException("sail has already been intialized");
		}

		this.tripleIndexes = tripleIndexes;
	}

	public String getTripleIndexes() {
		return tripleIndexes;
	}

	/**
	 * Specifies whether updates should be synced to disk forcefully, must be called before initialization. Enabling
	 * this feature may prevent corruption in case of events like power loss, but can have a severe impact on write
	 * performance. By default, this feature is disabled.
	 */
	public void setForceSync(boolean forceSync) {
		this.forceSync = forceSync;
	}

	public boolean getForceSync() {
		return forceSync;
	}

	public void setValueCacheSize(int valueCacheSize) {
		this.valueCacheSize = valueCacheSize;
	}

	public void setValueIDCacheSize(int valueIDCacheSize) {
		this.valueIDCacheSize = valueIDCacheSize;
	}

	public void setNamespaceCacheSize(int namespaceCacheSize) {
		this.namespaceCacheSize = namespaceCacheSize;
	}

	public void setNamespaceIDCacheSize(int namespaceIDCacheSize) {
		this.namespaceIDCacheSize = namespaceIDCacheSize;
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
		return evalStratFactory;
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
	 * Initializes this NativeStore.
	 *
	 * @exception SailException If this NativeStore could not be initialized using the parameters that have been set.
	 */
	@Override
	protected void initializeInternal() throws SailException {
		logger.debug("Initializing NativeStore...");

		// Check initialization parameters
		File dataDir = getDataDir();

		if (dataDir == null) {
			try {
				setDataDir(Files.createTempDirectory("rdf4j-native-tmp").toFile());
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
			Path versionPath = new File(dataDir, "nativerdf.ver").toPath();
			String version = versionPath.toFile().exists() ? Files.readString(versionPath, StandardCharsets.UTF_8)
					: null;
			if (!VERSION.equals(version) && upgradeStore(dataDir, version)) {
				logger.debug("Data store upgraded to version " + VERSION);
				Files.writeString(versionPath, VERSION, StandardCharsets.UTF_8,
						StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
			}
			final NativeSailStore mainStore = new NativeSailStore(dataDir, tripleIndexes, forceSync, valueCacheSize,
					valueIDCacheSize, namespaceCacheSize, namespaceIDCacheSize);
			this.store = new SnapshotSailStore(mainStore, () -> new MemoryOverflowIntoNativeStore()) {

				@Override
				public SailSource getExplicitSailSource() {
					if (isIsolationDisabled()) {
						// no isolation, use NativeSailStore directly
						return mainStore.getExplicitSailSource();
					} else {
						return super.getExplicitSailSource();
					}
				}

				@Override
				public SailSource getInferredSailSource() {
					if (isIsolationDisabled()) {
						// no isolation, use NativeSailStore directly
						return mainStore.getInferredSailSource();
					} else {
						return super.getInferredSailSource();
					}
				}
			};
		} catch (Throwable e) {
			// NativeStore initialization failed, release any allocated files
			dirLock.release();

			throw new SailException(e);
		}

		isWritable = getDataDir().canWrite();

		logger.debug("NativeStore initialized");
	}

	@Override
	protected void shutDownInternal() throws SailException {
		logger.debug("Shutting down NativeStore...");

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
					Files.walk(dataDir.toPath())
							.map(Path::toFile)
							.sorted(Comparator.reverseOrder()) // delete files before directory
							.forEach(File::delete);
				} catch (IOException ioe) {
					logger.error("Could not delete temp file " + dataDir);
				}
			}
		}
		logger.debug("NativeStore shut down");
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
		try {
			return new NativeStoreConnection(this);
		} catch (IOException e) {
			throw new SailException(e);
		}
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
			throw new SailException(e);
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

	private boolean upgradeStore(File dataDir, String version) throws IOException, SailException {
		if (version == null) {
			// either a new store or a pre-2.8.2 store
			ValueStore valueStore = new ValueStore(dataDir);
			try {
				valueStore.checkConsistency();
				return true; // good enough
			} catch (SailException e) {
				// valueStore is not consistent - possibly contains two entries for
				// string-literals with the same lexical value (e.g. "foo" and
				// "foo"^^xsd:string). Log an error and indicate upgrade should
				// not be executed.
				logger.error(
						"VALUE INCONSISTENCY: could not automatically upgrade native store to RDF 1.1-compatibility: {}. Failure to upgrade may result in inconsistent query results when comparing literal values.",
						e.getMessage());
				return false;
			} finally {
				valueStore.close();
			}
		} else {
			return false; // no upgrade needed
		}
	}
}
