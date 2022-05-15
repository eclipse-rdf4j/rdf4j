/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.repository.transaction;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;

/**
 * Registry keeping track of active transactions identified by a {@link UUID}.
 *
 * @author Jeen Broekstra
 */
public enum ActiveTransactionRegistry {

	INSTANCE;

	private int timeout = DEFAULT_TIMEOUT;

	private final Logger logger = LoggerFactory.getLogger(ActiveTransactionRegistry.class);

	/**
	 * Configurable system property {@code rdf4j.server.txn.registry.timeout} for specifying the transaction cache
	 * timeout (in seconds).
	 *
	 * @deprecated since 2.3 use {@link Protocol#CACHE_TIMEOUT_PROPERTY}
	 */
	@Deprecated
	public static final String CACHE_TIMEOUT_PROPERTY = Protocol.TIMEOUT.CACHE_PROPERTY;

	/**
	 * Default timeout setting for transaction cache entries (in seconds).
	 *
	 * @deprecated since 2.3 use {@link Protocol#DEFAULT_TIMEOUT}
	 */
	@Deprecated
	public final static int DEFAULT_TIMEOUT = Protocol.TIMEOUT.DEFAULT;

	/**
	 * primary cache for transactions, accessible via transaction ID. Cache entries are kept until a transaction signals
	 * it has ended, or until the secondary cache finds an "orphaned" transaction entry.
	 */
	private final Cache<UUID, Transaction> primaryCache;

	/**
	 * The secondary cache does automatic cleanup of its entries based on the configured timeout. If an expired
	 * transaction is no longer active, it is considered "orphaned" and discarded from the primary cache.
	 */
	private final Cache<UUID, Transaction> secondaryCache;

	/**
	 * a scheduler that routinely cleanup the secondary cache there is no other way to remove stale transactions from
	 * there if remote clients are gone
	 */
	private final ScheduledExecutorService cleaupSecondaryCacheScheduler;
	private ScheduledFuture<?> cleanupTask;

	private Cache<UUID, Transaction> getSecondaryCache() {
		return secondaryCache;
	}

	/**
	 * private constructor.
	 */
	ActiveTransactionRegistry() {
		final String configuredValue = System.getProperty(Protocol.CACHE_TIMEOUT_PROPERTY);
		if (configuredValue != null) {
			try {
				timeout = Integer.parseInt(configuredValue);
			} catch (NumberFormatException e) {
				logger.warn("Expected integer value for property {}. Timeout will default to {} seconds. ",
						Protocol.CACHE_TIMEOUT_PROPERTY, Protocol.DEFAULT_TIMEOUT);
			}
		}
		primaryCache = CacheBuilder.newBuilder()
				.removalListener((RemovalNotification<UUID, Transaction> notification) -> {
					UUID transactionId = notification.getKey();
					Transaction entry = notification.getValue();
					try {
						logger.debug("primary cache removal txid {}", transactionId);
						entry.close();
					} catch (RepositoryException | InterruptedException | ExecutionException e) {
						// fall through
					}
				})
				.build();

		secondaryCache = CacheBuilder.newBuilder()
				.removalListener((RemovalNotification<UUID, Transaction> notification) -> {
					logger.debug("secondary cache removal");
					if (RemovalCause.EXPIRED.equals(notification.getCause())) {
						final UUID transactionId = notification.getKey();
						final Transaction entry = notification.getValue();
						logger.debug("expired transaction to be removed {}", transactionId);
						synchronized (primaryCache) {
							// no operation active, we can decommission this entry
							primaryCache.invalidate(transactionId);
							logger.debug("deregistered expired transaction {}", transactionId);
							try {
								logger.debug("try close() invoked on transaction !!!{}", transactionId);
								entry.close();
							} catch (Throwable t) {
								logger.debug("error on close when purging {}", t.getMessage());
							}
						}
					}
				})
				.expireAfterAccess(timeout, TimeUnit.SECONDS)
				.build();
		cleaupSecondaryCacheScheduler = Executors.newSingleThreadScheduledExecutor((

				Runnable runnable) -> {
			Thread thread = Executors.defaultThreadFactory().newThread(runnable);
			thread.setName("rdf4j-cleanup-stn-scheduler");
			thread.setDaemon(true);
			return thread;
		});

		// timeout + 10% to force cleanup
		cleanupTask = cleaupSecondaryCacheScheduler.schedule(() -> {
			cleanUpSecondaryCache();
		}, timeout + timeout / 10, TimeUnit.SECONDS);
		logger.debug("secondary cache expire time {} seconds", timeout);
	}

	protected void cleanUpSecondaryCache() {
		synchronized (primaryCache) {
			logger.debug("performing secondary cache cleanup. {}", getSecondaryCache().size());
			getSecondaryCache().cleanUp();
		}
		cleanupTask = cleaupSecondaryCacheScheduler.schedule(() -> {
			cleanUpSecondaryCache();
		}, timeout + timeout / 10, TimeUnit.SECONDS);
	}

	// stops the secondary cache cleanup scheduler. invoked by TransactionController.destroy()
	public void destroyScheduler() {
		if (cleanupTask != null)
			cleanupTask.cancel(false);
		cleanupTask = null;
		cleaupSecondaryCacheScheduler.shutdownNow();
		logger.debug("ActiveTransactionCache destroy invoked!");
	}

	public long getTimeout(TimeUnit unit) {
		return unit.convert(timeout, TimeUnit.SECONDS);
	}

	/**
	 * @param txn
	 */
	public void register(Transaction txn) {
		synchronized (primaryCache) {
			Transaction existingTxn = primaryCache.getIfPresent(txn.getID());
			if (existingTxn == null) {
				primaryCache.put(txn.getID(), txn);
				secondaryCache.put(txn.getID(), txn);
				logger.debug("registered transaction {} ", txn.getID());
			} else {
				logger.error("transaction already registered: {}", txn.getID());
				throw new RepositoryException("transaction with id " + txn.getID().toString() + " already registered.");
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
	 * Resets transaction timeout. If transaction has already timed-out, reinsert the transaction.
	 *
	 * @param txn
	 */
	public void active(Transaction txn) {
		synchronized (primaryCache) {
			updateSecondaryCache(txn);
			Transaction existingTxn = primaryCache.getIfPresent(txn.getID());
			if (existingTxn == null) {
				// reinstate transaction that timed-out too soon
				primaryCache.put(txn.getID(), txn);
				logger.debug("reinstated transaction {} ", txn.getID());
			}
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
			} else {
				primaryCache.invalidate(transaction.getID());
				secondaryCache.invalidate(transaction.getID());
				logger.debug("deregistered transaction {}", transaction.getID());
			}
		}
	}

	/**
	 * Checks if the given transaction entry is still in the secondary cache (resetting its last access time in the
	 * process) and if not reinserts it.
	 *
	 * @param transaction the transaction to check
	 */
	private void updateSecondaryCache(final Transaction transaction) {
		try {
			secondaryCache.get(transaction.getID(), () -> transaction);
			logger.debug("secondary cache update transaction {}", transaction.getID());
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
}
