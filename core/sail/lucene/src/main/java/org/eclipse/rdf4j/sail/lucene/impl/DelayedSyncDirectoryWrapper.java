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
package org.eclipse.rdf4j.sail.lucene.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FilterDirectory;
import org.eclipse.rdf4j.sail.SailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper around a Lucene Directory that batches sync and metadata sync calls to be executed at a fixed interval.
 *
 * @author Piotr Sowiński
 */
class DelayedSyncDirectoryWrapper extends FilterDirectory {

	final private Logger logger = LoggerFactory.getLogger(getClass());

	final private ScheduledThreadPoolExecutor scheduler;

	final private AtomicBoolean needsMetadataSync = new AtomicBoolean(false);

	final private AtomicReference<Throwable> lastSyncThrowable = new AtomicReference<>(null);

	final private HashSet<String> pendingSyncs = new HashSet<>();

	final private int maxPendingSyncs;

	private final Object syncMonitor = new Object();

	private boolean closed = false;

	/**
	 * Creates a new instance of LuceneDirectoryWrapper.
	 *
	 * @param in              the underlying directory
	 * @param fsyncInterval   the interval in milliseconds writes after which a fsync is performed
	 * @param maxPendingSyncs the maximum number of pending syncs to accumulate before forcing a sync
	 */
	DelayedSyncDirectoryWrapper(Directory in, long fsyncInterval, int maxPendingSyncs) {
		super(in);

		assert fsyncInterval > 0;
		assert maxPendingSyncs > 0;

		this.maxPendingSyncs = maxPendingSyncs;

		// Use a daemon thread so the scheduler does not prevent JVM shutdown.
		this.scheduler = new ScheduledThreadPoolExecutor(1, r -> {
			Thread t = Executors.defaultThreadFactory().newThread(r);
			t.setDaemon(true);
			t.setName("rdf4j-lucene-sync-" + t.getId());
			return t;
		});

		// Help GC by removing cancelled tasks from the queue.
		this.scheduler.setRemoveOnCancelPolicy(true);

		this.scheduler.scheduleAtFixedRate(
				() -> {
					try {
						doSync();
					} catch (Throwable e) {
						// keep scheduling even if Errors occur
						logger.error(e.getClass().getSimpleName() + " during a periodic sync of Lucene index", e);
						// Throwable is recorded and rethrown on next sync()/syncMetaData() call by checkException().
						try {
							Thread.sleep(10); // slight throttle to avoid busy looping
						} catch (InterruptedException ex) {
							Thread.currentThread().interrupt();
						}
					}
				},
				fsyncInterval,
				fsyncInterval,
				TimeUnit.MILLISECONDS
		);
	}

	private void doSync() throws IOException {
		logger.debug("Performing periodic sync of Lucene index");
		List<String> toSync;
		synchronized (pendingSyncs) {
			toSync = new ArrayList<>(pendingSyncs);
			pendingSyncs.clear();
		}

		boolean metaRequestedInitial = this.needsMetadataSync.get();
		boolean metaToProcess = false;

		try {
			if (toSync.isEmpty() && !metaRequestedInitial) {
				logger.debug("Nothing to sync");
				// Nothing to sync
				return;
			}

			synchronized (syncMonitor) {
				// Process metadata first if requested
				metaToProcess = this.needsMetadataSync.getAndSet(false);
				if (metaToProcess) {
					try {
						logger.debug("Syncing metadata");
						super.syncMetaData();
					} catch (Throwable e) {
						logger.error(e.getClass().getSimpleName() + " during a periodic sync of Lucene index metadata",
								e);
						throw e;
					}
				}

				if (!toSync.isEmpty()) {
					try {
						logger.debug("Syncing files");
						super.sync(toSync);
					} catch (Throwable e) {
						// Lucene files may be merged/removed between scheduling and sync.
						// Treat missing files as benign and attempt per-file sync, ignoring those missing.
						if (e instanceof java.nio.file.NoSuchFileException || e instanceof FileNotFoundException) {
							for (String name : toSync) {
								try {
									super.sync(Collections.singleton(name));
								} catch (java.nio.file.NoSuchFileException | FileNotFoundException ignore) {
									// File disappeared before fsync: safe to ignore
								} catch (Throwable t) {
									logger.error(t.getClass().getSimpleName()
											+ " during a periodic sync of Lucene index files (per-file)", t);
									throw t;
								}
							}
							// Consider sync successful when only missing files were encountered.
						} else {
							logger.error(e.getClass().getSimpleName() + " during a periodic sync of Lucene index files",
									e);
							throw e;
						}
					}
				}
			}
		} catch (Throwable t) {
			lastSyncThrowable.set(t);
			synchronized (pendingSyncs) {
				pendingSyncs.addAll(toSync);
				if (metaToProcess) {
					needsMetadataSync.set(true);
				}
			}
			throw t;
		}

	}

	@Override
	public void sync(Collection<String> names) throws IOException {
		checkException();
		if (closed) {
			throw new SailException("DelayedSyncDirectoryWrapper is closed");
		}

		boolean doImmediateSync = false;
		synchronized (pendingSyncs) {
			pendingSyncs.addAll(names);
			if (pendingSyncs.size() >= maxPendingSyncs) {
				// If we have accumulated too many pending syncs, do a sync right away
				// to avoid excessive memory usage
				doImmediateSync = true;
			}
		}
		if (doImmediateSync) {
			doSync();
		}
	}

	@Override
	public void syncMetaData() throws IOException {
		// Request a metadata sync even if a previous error is pending
		needsMetadataSync.set(true);
		checkException();
		if (closed) {
			throw new SailException("DelayedSyncDirectoryWrapper is closed");
		}
	}

	private void checkException() throws IOException {
		final Throwable t = lastSyncThrowable.getAndSet(null);
		if (t != null) {
			// Rethrow the last exception if there was one.
			// This will fail the current transaction, and not the one that caused the original exception.
			// But there is no other way to notify the caller of the error, as the sync is done asynchronously.

			if (t instanceof IOException) {
				throw ((IOException) t);
			} else {
				throw new SailException(t);
			}
		}
	}

	@Override
	public void close() throws IOException {
		closed = true;

		// Finish the current sync task, if in progress and then shut down
		try {
			scheduler.shutdown();
			if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
				logger.error("Failed to shut down Lucene directory sync scheduler within 10s");
				throw new SailException("Failed to shut down Lucene directory sync scheduler within 10s");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new SailException(e);
		} finally {
			// Do a final sync of any remaining files
			try {
				doSync();
			} finally {
				super.close();
			}
		}
	}
}
