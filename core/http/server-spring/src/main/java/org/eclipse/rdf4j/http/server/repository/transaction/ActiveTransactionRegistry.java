/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
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

import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

/**
 * Registry keeping track of active transactions identified by a {@link UUID}.
 * 
 * @author Jeen Broekstra
 */
public enum ActiveTransactionRegistry {

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
	private final Cache<UUID, Transaction> primaryCache;

	/**
	 * The secondary cache does automatic cleanup of its entries based on the configured timeout. If an
	 * expired transaction is no longer active, it is considered "orphaned" and discarded from the primary
	 * cache.
	 */
	private final Cache<UUID, Transaction> secondaryCache;

	/**
	 * private constructor.
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

		primaryCache = CacheBuilder.newBuilder().removalListener(new RemovalListener<UUID, Transaction>() {

			@Override
			public void onRemoval(RemovalNotification<UUID, Transaction> notification) {
				Transaction entry = notification.getValue();
				try {
					entry.close();
				}
				catch (RepositoryException | InterruptedException | ExecutionException e) {
					// fall through
				}
			}
		}).build();

		secondaryCache = CacheBuilder.newBuilder().removalListener(new RemovalListener<UUID, Transaction>() {

			@Override
			public void onRemoval(RemovalNotification<UUID, Transaction> notification) {
				if (RemovalCause.EXPIRED.equals(notification.getCause())) {
					final UUID transactionId = notification.getKey();
					final Transaction entry = notification.getValue();
					synchronized (primaryCache) {
						if (!entry.hasActiveOperations()) {
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
	 * @param txnId
	 * @param txn
	 */
	public void register(Transaction txn) {
		synchronized (primaryCache) {
			Transaction existingTxn = primaryCache.getIfPresent(txn.getID());
			if (existingTxn == null) {
				primaryCache.put(txn.getID(), txn);
				secondaryCache.put(txn.getID(), txn);
				logger.debug("registered transaction {} ", txn.getID());
			}
			else {
				logger.error("transaction already registered: {}", txn.getID());
				throw new RepositoryException(
						"transaction with id " + txn.getID().toString() + " already registered.");
			}
		}
	}

	public Transaction getTransaction(UUID id) {
		synchronized (primaryCache) {
			Transaction entry = primaryCache.getIfPresent(id);
			if (entry == null) {
				throw new RepositoryException("transaction with id " + id.toString() + " not registered.");
			}
			updateSecondaryCache(entry);
			return entry;
		}
	}

	/**
	 * @param transaction
	 */
	public void deregister(Transaction transaction) {

		synchronized (primaryCache) {
			Transaction entry = primaryCache.getIfPresent(transaction.getID());
			if (entry == null) {
				throw new RepositoryException(
						"transaction with id " + transaction.getID().toString() + " not registered.");
			}
			else {
				primaryCache.invalidate(transaction.getID());
				secondaryCache.invalidate(transaction.getID());
				logger.debug("deregistered transaction {}", transaction.getID());
			}
		}
	}

	/**
	 * Checks if the given transaction entry is still in the secondary cache (resetting its last access time
	 * in the process) and if not reinserts it.
	 * 
	 * @param transaction
	 *        the transaction to check
	 */
	private void updateSecondaryCache(final Transaction transaction) {
		try {
			secondaryCache.get(transaction.getID(), new Callable<Transaction>() {

				@Override
				public Transaction call()
					throws Exception
				{
					return transaction;
				}
			});
		}
		catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
}
