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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FilterDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.jupiter.api.Test;

/**
 * @author Piotr Sowiński
 */
public class DelayedSyncDirectoryWrapperTest {

	private static final class TrackingDirectory extends FilterDirectory {
		private AtomicInteger syncCount = new AtomicInteger(0);
		private AtomicInteger metaSyncCount = new AtomicInteger(0);
		private AtomicInteger closeCount = new AtomicInteger(0);

		TrackingDirectory(Directory in) {
			super(in);
		}

		@Override
		public void sync(Collection<String> names) throws IOException {
			syncCount.getAndIncrement();
			super.sync(names);
		}

		@Override
		public void syncMetaData() throws IOException {
			metaSyncCount.getAndIncrement();
			super.syncMetaData();
		}

		@Override
		public void close() throws IOException {
			closeCount.getAndIncrement();
			super.close();
		}

		public int getSyncCount() {
			return syncCount.get();
		}

		public int getMetaSyncCount() {
			return metaSyncCount.get();
		}

		public int getCloseCount() {
			return closeCount.get();
		}
	}

	@Test
	public void testSyncData() throws IOException {
		final var dir = new TrackingDirectory(new RAMDirectory());
		final var delayedDir = new DelayedSyncDirectoryWrapper(dir, 500, 100);

		assertEquals(0, dir.getSyncCount());
		assertEquals(0, dir.getMetaSyncCount());

		delayedDir.sync(List.of("file1", "file2"));

		// The sync should be delayed, so the count should still be 0
		assertEquals(0, dir.getSyncCount());

		// Wait for more than the fsync interval to allow the scheduled task to run
		waitFor(700);
		assertEquals(1, dir.getSyncCount());
		// Meta sync should still be 0
		assertEquals(0, dir.getMetaSyncCount());

		delayedDir.close();

		// No additional syncs should have occurred after close
		assertEquals(1, dir.getSyncCount());
		assertEquals(0, dir.getMetaSyncCount());
		assertEquals(1, dir.getCloseCount());
	}

	@Test
	public void testSyncMetaData() throws IOException {
		final var dir = new TrackingDirectory(new RAMDirectory());
		final var delayedDir = new DelayedSyncDirectoryWrapper(dir, 500, 100);

		assertEquals(0, dir.getSyncCount());
		assertEquals(0, dir.getMetaSyncCount());

		delayedDir.syncMetaData();

		// The meta sync should be delayed, so the count should still be 0
		assertEquals(0, dir.getMetaSyncCount());

		// Wait for more than the fsync interval to allow the scheduled task to run
		waitFor(700);
		assertEquals(1, dir.getMetaSyncCount());
		// Regular sync should still be 0
		assertEquals(0, dir.getSyncCount());

		delayedDir.close();

		// No additional syncs should have occurred after close
		assertEquals(0, dir.getSyncCount());
		assertEquals(1, dir.getMetaSyncCount());
		assertEquals(1, dir.getCloseCount());
	}

	@Test
	public void testSyncMixed() throws IOException {
		final var dir = new TrackingDirectory(new RAMDirectory());
		final var delayedDir = new DelayedSyncDirectoryWrapper(dir, 500, 100);

		assertEquals(0, dir.getSyncCount());
		assertEquals(0, dir.getMetaSyncCount());

		delayedDir.sync(List.of("file1", "file2"));
		delayedDir.sync(List.of("file2", "file456"));
		delayedDir.syncMetaData();
		delayedDir.syncMetaData();
		delayedDir.syncMetaData();

		// The syncs should be delayed, so the counts should still be 0
		assertEquals(0, dir.getSyncCount());
		assertEquals(0, dir.getMetaSyncCount());

		// Wait for more than the fsync interval to allow the scheduled task to run
		waitFor(700);
		assertEquals(1, dir.getSyncCount());
		assertEquals(1, dir.getMetaSyncCount());

		delayedDir.sync(List.of("file2", "file456"));
		delayedDir.syncMetaData();

		waitFor(700);
		assertEquals(2, dir.getSyncCount());
		assertEquals(2, dir.getMetaSyncCount());

		// Wait again to ensure no extra syncs occur
		waitFor(700);
		assertEquals(2, dir.getSyncCount());
		assertEquals(2, dir.getMetaSyncCount());

		delayedDir.close();

		// No additional syncs should have occurred after close
		assertEquals(2, dir.getSyncCount());
		assertEquals(2, dir.getMetaSyncCount());
		assertEquals(1, dir.getCloseCount());
	}

	@Test
	public void testSyncMixed_afterClose() throws IOException {
		final var dir = new TrackingDirectory(new RAMDirectory());
		final var delayedDir = new DelayedSyncDirectoryWrapper(dir, 500, 100);

		assertEquals(0, dir.getSyncCount());
		assertEquals(0, dir.getMetaSyncCount());

		delayedDir.sync(List.of("file1", "file2"));
		delayedDir.sync(List.of("file2", "file456"));
		delayedDir.syncMetaData();
		delayedDir.syncMetaData();
		delayedDir.syncMetaData();

		assertEquals(0, dir.getSyncCount());
		assertEquals(0, dir.getMetaSyncCount());

		delayedDir.close();

		// The syncs should be executed on close
		assertEquals(1, dir.getSyncCount());
		assertEquals(1, dir.getMetaSyncCount());
		assertEquals(1, dir.getCloseCount());
	}

	@Test
	public void testCloseOnIndexShutDown() throws IOException {
		final var dir = new TrackingDirectory(new RAMDirectory());
		final var delayedDir = new DelayedSyncDirectoryWrapper(dir, 500, 100);
		final var index = new LuceneIndex(delayedDir, new StandardAnalyzer());
		assertEquals(0, dir.getCloseCount());

		index.shutDown();

		assertEquals(1, dir.getCloseCount());
	}

	@Test
	public void testRethrowLastSyncException() throws IOException {
		final var dir = new FilterDirectory(new RAMDirectory()) {
			@Override
			public void sync(Collection<String> names) throws IOException {
				throw new IOException("Simulated IO exception during sync");
			}

			@Override
			public void syncMetaData() throws IOException {
				throw new IOException("Simulated IO exception during syncMetaData");
			}
		};
		final var delayedDir = new DelayedSyncDirectoryWrapper(dir, 300, 100);

		// This should not throw immediately
		delayedDir.sync(List.of("file1", "file2"));
		waitFor(500);
		try {
			delayedDir.syncMetaData();
		} catch (IOException e) {
			// The exception from the previous sync should be rethrown here
			assertEquals("Simulated IO exception during sync", e.getMessage());
		}

		waitFor(500);
		try {
			delayedDir.sync(List.of("file3"));
		} catch (IOException e) {
			// The exception from the previous syncMetaData should be rethrown here
			assertEquals("Simulated IO exception during syncMetaData", e.getMessage());
		}
	}

	@Test
	public void testSyncIfOverSyncLimit() throws IOException {
		final var dir = new TrackingDirectory(new RAMDirectory());
		final var delayedDir = new DelayedSyncDirectoryWrapper(
				dir,
				100_000, // Large interval to prevent scheduled syncs during the test
				5 // Low max pending syncs to trigger sync quickly
		);

		assertEquals(0, dir.getSyncCount());
		delayedDir.sync(List.of("file1", "file2", "file3", "file4"));
		// Still no sync should have occurred, 4 < 5
		assertEquals(0, dir.getSyncCount());
		// Sync the same files again, should still be 4 unique files
		delayedDir.sync(List.of("file1", "file2", "file3", "file4"));
		assertEquals(0, dir.getSyncCount());
		// Sync one more file, should trigger the sync as we now have 5 unique files
		delayedDir.sync(List.of("file5"));
		assertEquals(1, dir.getSyncCount());
	}

	private void waitFor(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
