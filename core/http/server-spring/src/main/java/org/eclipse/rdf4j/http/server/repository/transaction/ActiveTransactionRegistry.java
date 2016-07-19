/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.repository.transaction;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

/**
 * Registry keeping track of active transactions identified by a {@link UUID} and the
 * {@link RepositoryConnection} that corresponds to the given transaction.
 * 
 * @author Jeen Broekstra
 */
public enum ActiveTransactionRegistry {

	/**
	 * Singleton instance
	 */
	INSTANCE;

	private final Logger logger = LoggerFactory.getLogger(ActiveTransactionRegistry.class);

	/**
	 * Configurable system property {@code rdf4j.server.txn.registry.timeout} for specifying the transaction
	 * cache timeout (in seconds).
	 */
	public static final String CACHE_TIMEOUT_PROPERTY = "rdf4j.server.txn.registry.timeout";

	/**
	 * Default timeout setting for transaction cache entries (in seconds).
	 */
	public final static int DEFAULT_TIMEOUT = 60;

	/**
	 * primary cache for transactions, accessible via transaction ID. Cache entries are kept until a
	 * transaction signals it has ended, or until the secondary cache finds an "orphaned" transaction entry.
	 */
	private final Cache<UUID, CacheEntry> primaryCache;

	/**
	 * The secondary cache does automatic cleanup of its entries based on the configured timeout. If an
	 * expired entry is no longer locked by any thread, it is considered "orphaned" and discarded from the
	 * primary cache.
	 */
	private final Cache<UUID, CacheEntry> secondaryCache;

	static class CacheEntry {

		private final RepositoryConnection connection;

		private final ReentrantLock lock = new ReentrantLock();

		public CacheEntry(RepositoryConnection connection) {
			this.connection = connection;
		}

		/**
		 * @return Returns the connection.
		 */
		public RepositoryConnection getConnection() {
			return connection;
		}

		/**
		 * @return Returns the lock.
		 */
		public ReentrantLock getLock() {
			return lock;
		}

	}

	/**
	 * private constructor. Access via {@link ActiveTransactionRegistry#INSTANCE}
	 */
	private ActiveTransactionRegistry() {
		int timeout = DEFAULT_TIMEOUT;

		final String configuredValue = System.getProperty(CACHE_TIMEOUT_PROPERTY);
		if (configuredValue != null) {
			try {
				timeout = Integer.parseInt(configuredValue);
			}
			catch (NumberFormatException e) {
				logger.warn("Expected integer value for property {}. Timeout will default to {} seconds. ",
						CACHE_TIMEOUT_PROPERTY, DEFAULT_TIMEOUT);
			}
		}

		primaryCache = CacheBuilder.newBuilder().removalListener(new RemovalListener<UUID, CacheEntry>() {

			@Override
			public void onRemoval(RemovalNotification<UUID, CacheEntry> notification) {
				CacheEntry entry = notification.getValue();
				try {
					entry.getConnection().close();
				}
				catch (RepositoryException e) {
					// fall through
				}
			}
		}).build();

		secondaryCache = CacheBuilder.newBuilder().removalListener(new RemovalListener<UUID, CacheEntry>() {

			@Override
			public void onRemoval(RemovalNotification<UUID, CacheEntry> notification) {
				if (RemovalCause.EXPIRED.equals(notification.getCause())) {
					final UUID transactionId = notification.getKey();
					final CacheEntry entry = notification.getValue();
					synchronized (primaryCache) {
						if (!entry.getLock().isLocked()) {
							// no operation active, we can decommission this entry
							primaryCache.invalidate(transactionId);
							logger.warn("deregistered expired transaction {}", transactionId);
						}
						else {
							// operation still active. Reinsert in secondary cache.
							secondaryCache.put(transactionId, entry);
						}
					}
				}
			}
		}).expireAfterAccess(timeout, TimeUnit.SECONDS).build();

	}

	/**
	 * Register a new transaction with the given id and connection.
	 * 
	 * @param transactionId
	 *        the transaction id
	 * @param conn
	 *        the {@link RepositoryConnection} to use for handling the transaction.
	 * @throws RepositoryException
	 *         if a transaction is already registered with the given transaction id.
	 */
	public void register(UUID transactionId, RepositoryConnection conn)
		throws RepositoryException
	{
		synchronized (primaryCache) {
			if (primaryCache.getIfPresent(transactionId) == null) {
				final CacheEntry cacheEntry = new CacheEntry(conn);
				primaryCache.put(transactionId, cacheEntry);
				secondaryCache.put(transactionId, cacheEntry);
				logger.debug("registered transaction {} ", transactionId);
			}
			else {
				logger.error("transaction already registered: {}", transactionId);
				throw new RepositoryException(
						"transaction with id " + transactionId.toString() + " already registered.");
			}
		}
	}

	/**
	 * Remove the given transaction from the registry
	 * 
	 * @param transactionId
	 *        the transaction id
	 * @throws RepositoryException
	 *         if no registered transaction with the given id could be found.
	 */
	public void deregister(UUID transactionId)
		throws RepositoryException
	{
		synchronized (primaryCache) {
			CacheEntry entry = primaryCache.getIfPresent(transactionId);
			if (entry == null) {
				throw new RepositoryException(
						"transaction with id " + transactionId.toString() + " not registered.");
			}
			else {
				primaryCache.invalidate(transactionId);
				secondaryCache.invalidate(transactionId);
				logger.debug("deregistered transaction {}", transactionId);
			}
		}
	}

	/**
	 * Obtain the {@link RepositoryConnection} associated with the given transaction. This method will block
	 * if another thread currently has access to the connection.
	 * 
	 * @param transactionId
	 *        a transaction ID
	 * @return the RepositoryConnection belonging to this transaction.
	 * @throws RepositoryException
	 *         if no transaction with the given id is registered.
	 * @throws InterruptedException
	 *         if the thread is interrupted while acquiring a lock on the transaction.
	 */
	public RepositoryConnection getTransactionConnection(UUID transactionId)
		throws RepositoryException, InterruptedException
	{
		Lock txnLock = null;
		synchronized (primaryCache) {
			CacheEntry entry = primaryCache.getIfPresent(transactionId);
			if (entry == null) {
				throw new RepositoryException(
						"transaction with id " + transactionId.toString() + " not registered.");
			}

			txnLock = entry.getLock();
		}

		txnLock.lockInterruptibly();
		/* Another thread might have deregistered the transaction while we were acquiring the lock */
		final CacheEntry entry = primaryCache.getIfPresent(transactionId);
		if (entry == null) {
			throw new RepositoryException(
					"transaction with id " + transactionId + " is no longer registered!");
		}
		updateSecondaryCache(transactionId, entry);

		return entry.getConnection();
	}

	/**
	 * Unlocks the {@link RepositoryConnection} associated with the given transaction for use by other
	 * threads. If the transaction is no longer registered, this will method will exit silently.
	 * 
	 * @param transactionId
	 *        a transaction identifier.
	 */
	public void returnTransactionConnection(UUID transactionId) {
		final CacheEntry entry = primaryCache.getIfPresent(transactionId);
		if (entry != null) {
			updateSecondaryCache(transactionId, entry);
			final ReentrantLock txnLock = entry.getLock();
			if (txnLock.isHeldByCurrentThread()) {
				txnLock.unlock();
			}
		}
	}

	/**
	 * Checks if the given transaction entry is still in the secondary cache (resetting its last access time
	 * in the process) and if not reinserts it.
	 * 
	 * @param transactionId
	 *        the id for the transaction to check
	 * @param entry
	 *        the cache entry to insert if necessary.
	 */
	private void updateSecondaryCache(UUID transactionId, final CacheEntry entry) {
		try {
			secondaryCache.get(transactionId, new Callable<CacheEntry>() {

				@Override
				public CacheEntry call()
					throws Exception
				{
					return entry;
				}
			});
		}
		catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
}
