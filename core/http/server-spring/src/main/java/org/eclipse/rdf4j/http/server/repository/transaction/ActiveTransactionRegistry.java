/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.repository.transaction;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry keeping track of active transactions identified by a {@link UUID}
 * and the {@link RepositoryConnection} that corresponds to the given
 * transaction.
 * 
 * @author Jeen Broekstra
 * @since 2.8.0
 */
public enum ActiveTransactionRegistry {

	/**
	 * Singleton instance
	 */
	INSTANCE;

	private final Logger logger = LoggerFactory.getLogger(ActiveTransactionRegistry.class);

	/**
	 * Configurable system property {@code sesame.server.txn.registry.timeout}
	 * for specifying the transaction cache timeout (in seconds).
	 */
	public static final String CACHE_TIMEOUT_PROPERTY = "sesame.server.txn.registry.timeout";

	/**
	 * Default timeout setting for transaction cache entries (in seconds).
	 */
	public final static int DEFAULT_TIMEOUT = 60;

	private final Cache<UUID, CacheEntry> activeConnections;

	static class CacheEntry {

		private final RepositoryConnection connection;

		private final Lock lock = new ReentrantLock();

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
		public Lock getLock() {
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

		activeConnections = initializeCache(timeout, TimeUnit.SECONDS);
	}

	private final Cache<UUID, CacheEntry> initializeCache(int timeout, TimeUnit unit) {
		return CacheBuilder.newBuilder().removalListener(new RemovalListener<UUID, CacheEntry>() {

			@Override
			public void onRemoval(RemovalNotification<UUID, CacheEntry> notification) {
				if (RemovalCause.EXPIRED.equals(notification.getCause())) {
					logger.warn("transaction registry item {} removed after expiry", notification.getKey());
					CacheEntry entry = notification.getValue();
					try {
						entry.getConnection().close();
					}
					catch (RepositoryException e) {
						// fall through
					}
				}
				else {
					logger.debug("transaction {} removed from registry. cause: {}", notification.getKey(),
							notification.getCause());
				}
			}
		}).expireAfterAccess(timeout, unit).build();
	}

	/**
	 * Register a new transaction with the given id and connection.
	 * 
	 * @param transactionId
	 *        the transaction id
	 * @param conn
	 *        the {@link RepositoryConnection} to use for handling the
	 *        transaction.
	 * @throws RepositoryException
	 *         if a transaction is already registered with the given transaction
	 *         id.
	 */
	public void register(UUID transactionId, RepositoryConnection conn)
		throws RepositoryException
	{
		synchronized (activeConnections) {
			if (activeConnections.getIfPresent(transactionId) == null) {
				activeConnections.put(transactionId, new CacheEntry(conn));
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
		synchronized (activeConnections) {
			CacheEntry entry = activeConnections.getIfPresent(transactionId);
			if (entry == null) {
				throw new RepositoryException(
						"transaction with id " + transactionId.toString() + " not registered.");
			}
			else {
				activeConnections.invalidate(transactionId);
				final Lock txnLock = entry.getLock();
				txnLock.unlock();
				logger.debug("deregistered transaction {}", transactionId);
			}
		}
	}

	/**
	 * Obtain the {@link RepositoryConnection} associated with the given
	 * transaction. This method will block if another thread currently has access
	 * to the connection.
	 * 
	 * @param transactionId
	 *        a transaction ID
	 * @return the RepositoryConnection belonging to this transaction.
	 * @throws RepositoryException
	 *         if no transaction with the given id is registered.
	 * @throws InterruptedException
	 *         if the thread is interrupted while acquiring a lock on the
	 *         transaction.
	 */
	public RepositoryConnection getTransactionConnection(UUID transactionId)
		throws RepositoryException, InterruptedException
	{
		Lock txnLock = null;
		synchronized (activeConnections) {
			CacheEntry entry = activeConnections.getIfPresent(transactionId);
			if (entry == null) {
				throw new RepositoryException(
						"transaction with id " + transactionId.toString() + " not registered.");
			}

			txnLock = entry.getLock();
		}

		txnLock.lockInterruptibly();
		/* Another thread might have deregistered the transaction while we were acquiring the lock */
		final CacheEntry entry = activeConnections.getIfPresent(transactionId);
		if (entry == null) {
			throw new RepositoryException("transaction with id " + transactionId + " is no longer registered!");
		}
		return entry.getConnection();
	}

	/**
	 * Unlocks the {@link RepositoryConnection} associated with the given
	 * transaction for use by other threads. If the transaction is no longer
	 * registered, this will method will exit silently.
	 * 
	 * @param transactionId
	 *        a transaction identifier.
	 */
	public void returnTransactionConnection(UUID transactionId) {

		final CacheEntry entry = activeConnections.getIfPresent(transactionId);

		if (entry != null) {
			final Lock txnLock = entry.getLock();
			txnLock.unlock();
		}
	}
}
