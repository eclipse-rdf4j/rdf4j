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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FilterDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper around a Lucene Directory that batches sync and metadata sync calls to be executed at a fixed interval.
 *
 * @author Piotr Sowiński
 */
class DelayedSyncDirectoryWrapper extends FilterDirectory {

	final private Logger logger = LoggerFactory.getLogger(getClass());

	final private ScheduledExecutorService scheduler;

	final private AtomicBoolean needsMetadataSync = new AtomicBoolean(false);

	final private AtomicReference<IOException> lastSyncException = new AtomicReference<>(null);

	final private HashSet<String> pendingSyncs = new HashSet<>();

	final private int maxPendingSyncs;

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
		scheduler = Executors.newScheduledThreadPool(1);
		scheduler.scheduleAtFixedRate(
				this::doSync,
				fsyncInterval,
				fsyncInterval,
				TimeUnit.MILLISECONDS
		);
	}

	private void doSync() {
		List<String> toSync;
		synchronized (pendingSyncs) {
			toSync = new ArrayList<>(pendingSyncs);
			pendingSyncs.clear();
		}
		if (!toSync.isEmpty()) {
			try {
				super.sync(toSync);
			} catch (IOException e) {
				lastSyncException.set(e);
				logger.error("IO error during a periodic sync of Lucene index files", e);
			}
		}
		if (this.needsMetadataSync.getAndSet(false)) {
			try {
				super.syncMetaData();
			} catch (IOException e) {
				lastSyncException.set(e);
				logger.error("IO error during a periodic sync of Lucene index metadata", e);
			}
		}
	}

	@Override
	public void sync(Collection<String> names) throws IOException {
		final IOException ex = lastSyncException.getAndSet(null);
		if (ex != null) {
			// Rethrow the last exception if there was one.
			// This will fail the current transaction, and not the one that caused the original exception.
			// But there is no other way to notify the caller of the error, as the sync is done asynchronously.
			throw ex;
		}
		synchronized (pendingSyncs) {
			pendingSyncs.addAll(names);
			if (pendingSyncs.size() >= maxPendingSyncs) {
				// If we have accumulated too many pending syncs, do a sync right away
				// to avoid excessive memory usage
				doSync();
			}
		}
	}

	@Override
	public void syncMetaData() throws IOException {
		needsMetadataSync.set(true);
	}

	@Override
	public void close() throws IOException {
		// Finish the current sync task, if in progress and then shut down
		try {
			scheduler.shutdown();
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
