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
package org.eclipse.rdf4j.sail.helpers;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract Sail implementation that takes care of common sail tasks, including proper closing of active connections
 * and a grace period for active connections during shutdown of the store.
 *
 * @author Herko ter Horst
 * @author jeen
 * @author Arjohn Kampman
 */
public abstract class AbstractSail implements Sail {

	/*-----------*
	 * Constants *
	 *-----------*/

	/**
	 * Default connection timeout on shutdown: 20,000 milliseconds.
	 */
	protected final static long DEFAULT_CONNECTION_TIMEOUT = 20000L;

	/**
	 * default transaction isolation level, set to {@link IsolationLevels#READ_COMMITTED }.
	 */
	private IsolationLevel defaultIsolationLevel = IsolationLevels.READ_COMMITTED;

	/**
	 * list of supported isolation levels. By default set to include {@link IsolationLevels#READ_UNCOMMITTED} and
	 * {@link IsolationLevels#SERIALIZABLE}. Specific store implementations are expected to alter this list according to
	 * their specific capabilities.
	 */
	private List<IsolationLevel> supportedIsolationLevels = new ArrayList<>();

	/**
	 * default value for the Iteration item sync threshold
	 */
	protected static final long DEFAULT_ITERATION_SYNC_THRESHOLD = 0L;

	// Note: the following variable and method are package protected so that they
	// can be removed when open connections no longer block other connections and
	// they can be closed silently (just like in JDBC).
	static final String DEBUG_PROP = "org.eclipse.rdf4j.repository.debug";

	protected static boolean debugEnabled() {
		try {
			String value = System.getProperty(DEBUG_PROP);
			return value != null && !value.equals("false");
		} catch (SecurityException e) {
			// Thrown when not allowed to read system properties, for example when
			// running in applets
			return false;
		}
	}

	/*-----------*
	 * Variables *
	 *-----------*/

	private static final Logger logger = LoggerFactory.getLogger(AbstractSail.class);

	/**
	 * Directory to store information related to this sail in (if any).
	 */
	private volatile File dataDir;

	/**
	 * Flag indicating whether the Sail has been initialized. Sails are initialized from {@link #init() initialization}
	 * until {@link #shutDown() shutdown}.
	 */
	private volatile boolean initialized = false;

	/**
	 * Lock used to synchronize the initialization state of a sail.
	 * <ul>
	 * <li>write lock: initialize(), shutDown()
	 * <li>read lock: getConnection()
	 * </ul>
	 */
	protected final ReentrantReadWriteLock initializationLock = new ReentrantReadWriteLock();

	/**
	 * Connection timeout on shutdown (in ms). Defaults to {@link #DEFAULT_CONNECTION_TIMEOUT}.
	 */
	protected volatile long connectionTimeOut = DEFAULT_CONNECTION_TIMEOUT;

	private long iterationCacheSyncThreshold = DEFAULT_ITERATION_SYNC_THRESHOLD;

	// track the results size that each node in the query plan produces during execution
	private boolean trackResultSize;

	/**
	 * Map used to track active connections and where these were acquired. The Throwable value may be null in case
	 * debugging was disable at the time the connection was acquired.
	 */
	private final Map<SailConnection, Throwable> activeConnections = new IdentityHashMap<>();

	/*
	 * constructors
	 */

	public AbstractSail() {
		super();
		this.addSupportedIsolationLevel(IsolationLevels.READ_UNCOMMITTED);
		this.addSupportedIsolationLevel(IsolationLevels.SERIALIZABLE);
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Set connection timeout on shutdown (in ms).
	 *
	 * @param connectionTimeOut timeout (in ms)
	 */
	public void setConnectionTimeOut(long connectionTimeOut) {
		this.connectionTimeOut = connectionTimeOut;
	}

	@Override
	public void setDataDir(File dataDir) {
		if (isInitialized()) {
			throw new IllegalStateException("sail has already been initialized");
		}

		this.dataDir = dataDir;
	}

	@Override
	public File getDataDir() {
		return dataDir;
	}

	@Override
	public String toString() {
		if (dataDir == null) {
			return super.toString();
		} else {
			return dataDir.toString();
		}
	}

	/**
	 * Checks whether the Sail has been initialized. Sails are initialized from {@link #init() initialization} until
	 * {@link #shutDown() shutdown}.
	 *
	 * @return <var>true</var> if the Sail has been initialized, <var>false</var> otherwise.
	 */
	protected boolean isInitialized() {
		return initialized;
	}

	@Override
	public void init() throws SailException {
		initializationLock.writeLock().lock();
		try {
			if (isInitialized()) {
				return; // skip silently
			}

			initializeInternal();

			initialized = true;
		} finally {
			initializationLock.writeLock().unlock();
		}
	}

	/**
	 * Do store-specific operations to initialize the store. The default implementation of this method does nothing.
	 */
	protected void initializeInternal() throws SailException {
	}

	@Override
	public void shutDown() throws SailException {
		initializationLock.writeLock().lock();
		try {
			if (!isInitialized()) {
				return;
			}

			Map<SailConnection, Throwable> activeConnectionsCopy;

			synchronized (activeConnections) {
				// Check if any active connections exist. If so, wait for a grace
				// period for them to finish.
				if (!activeConnections.isEmpty()) {
					logger.debug("Waiting for active connections to close before shutting down...");
					try {
						activeConnections.wait(connectionTimeOut);
					} catch (InterruptedException e) {
						// ignore and continue
					}
				}

				// Copy the current contents of the map so that we don't have to
				// synchronize on activeConnections. This prevents a potential
				// deadlock with concurrent calls to connectionClosed()
				activeConnectionsCopy = new IdentityHashMap<>(activeConnections);
			}

			// Forcefully close any connections that are still open
			for (Map.Entry<SailConnection, Throwable> entry : activeConnectionsCopy.entrySet()) {
				SailConnection con = entry.getKey();
				Throwable stackTrace = entry.getValue();

				if (stackTrace == null) {
					logger.warn("Closing active connection due to shut down; consider setting the {} system property",
							DEBUG_PROP);
				} else {
					logger.warn("Closing active connection due to shut down, connection was acquired in", stackTrace);
				}

				try {
					con.close();
				} catch (SailException e) {
					logger.error("Failed to close connection", e);
				}
			}

			// All connections should be closed now
			synchronized (activeConnections) {
				activeConnections.clear();
			}

			shutDownInternal();
		} finally {
			initialized = false;
			initializationLock.writeLock().unlock();
		}
	}

	/**
	 * Do store-specific operations to ensure proper shutdown of the store.
	 */
	protected abstract void shutDownInternal() throws SailException;

	@Override
	public SailConnection getConnection() throws SailException {
		if (!isInitialized()) {
			init();
		}
		initializationLock.readLock().lock();
		try {
			SailConnection connection = getConnectionInternal();

			Throwable stackTrace = debugEnabled() ? new Throwable() : null;
			synchronized (activeConnections) {
				activeConnections.put(connection, stackTrace);
			}

			return connection;
		} finally {
			initializationLock.readLock().unlock();
		}
	}

	/**
	 * Returns a store-specific SailConnection object.
	 *
	 * @return A connection to the store.
	 */
	protected abstract SailConnection getConnectionInternal() throws SailException;

	/**
	 * Signals to the store that the supplied connection has been closed; called by
	 * {@link AbstractSailConnection#close()}.
	 *
	 * @param connection The connection that has been closed.
	 */
	protected void connectionClosed(SailConnection connection) {
		synchronized (activeConnections) {
			if (activeConnections.containsKey(connection)) {
				activeConnections.remove(connection);

				if (activeConnections.isEmpty()) {
					// only notify waiting threads if all active connections have
					// been closed.
					activeConnections.notifyAll();
				}
			} else {
				logger.warn("tried to remove unknown connection object from store.");
			}
		}
	}

	/**
	 * Appends the provided {@link IsolationLevels} to the SAIL's list of supported isolation levels.
	 *
	 * @param level a supported IsolationLevel.
	 */
	protected void addSupportedIsolationLevel(IsolationLevels level) {
		this.supportedIsolationLevels.add(level);
	}

	/**
	 * Removes all occurrences of the provided {@link IsolationLevels} in the list of supported Isolation levels.
	 *
	 * @param level the isolation level to remove.
	 */
	protected void removeSupportedIsolationLevel(IsolationLevel level) {
		while (this.supportedIsolationLevels.remove(level)) {
		}
	}

	/**
	 * Sets the list of supported {@link IsolationLevels}s for this SAIL. The list is expected to be ordered in
	 * increasing complexity.
	 *
	 * @param supportedIsolationLevels a list of supported isolation levels.
	 */
	protected void setSupportedIsolationLevels(List<IsolationLevel> supportedIsolationLevels) {
		this.supportedIsolationLevels = supportedIsolationLevels;
	}

	/**
	 * Sets the list of supported {@link IsolationLevels}s for this SAIL. The list is expected to be ordered in
	 * increasing complexity.
	 *
	 * @param supportedIsolationLevels a list of supported isolation levels.
	 */
	protected void setSupportedIsolationLevels(IsolationLevel... supportedIsolationLevels) {
		this.supportedIsolationLevels = Arrays.asList(supportedIsolationLevels);
	}

	@Override
	public List<IsolationLevel> getSupportedIsolationLevels() {
		return Collections.unmodifiableList(supportedIsolationLevels);
	}

	@Override
	public IsolationLevel getDefaultIsolationLevel() {
		return defaultIsolationLevel;
	}

	/**
	 * Sets the default {@link IsolationLevel} on which transactions in this Sail operate.
	 *
	 * @param defaultIsolationLevel The defaultIsolationLevel to set.
	 */
	public void setDefaultIsolationLevel(IsolationLevel defaultIsolationLevel) {
		if (defaultIsolationLevel == null) {
			throw new IllegalArgumentException("default isolation level may not be null");
		}
		this.defaultIsolationLevel = defaultIsolationLevel;
	}

	/**
	 * Retrieves the currently configured threshold for syncing query evaluation iteration caches to disk.
	 *
	 * @return Returns the iterationCacheSyncThreshold.
	 */
	public long getIterationCacheSyncThreshold() {
		return iterationCacheSyncThreshold;
	}

	/**
	 * Set the threshold for syncing query evaluation iteration caches to disk.
	 *
	 * @param iterationCacheSyncThreshold The iterationCacheSyncThreshold to set.
	 */
	public void setIterationCacheSyncThreshold(long iterationCacheSyncThreshold) {
		this.iterationCacheSyncThreshold = iterationCacheSyncThreshold;
	}

	/**
	 * Returns the status of the result size tracking for the query plan. Useful to determine which parts of a query
	 * plan generated the most data.
	 *
	 * @return true if result size tracking is enabled.
	 */
	public boolean isTrackResultSize() {
		return trackResultSize;
	}

	/**
	 * Enable or disable results size tracking for the query plan. Useful to determine which parts of a query plan
	 * generated the most data.
	 *
	 * @param trackResultSize true to enable tracking.
	 */
	public void setTrackResultSize(boolean trackResultSize) {
		this.trackResultSize = trackResultSize;
	}
}
