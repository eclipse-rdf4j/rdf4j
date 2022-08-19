/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.memory;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailDataset;
import org.eclipse.rdf4j.sail.base.SailSink;
import org.eclipse.rdf4j.sail.base.SailSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public class SnapshotMonitorTest {

	@Test
	@Timeout(60)
	public void testAutomaticCleanupDataset() throws InterruptedException {
		try (MemorySailStore memorySailStore = new MemorySailStore(false)) {

			try (SailSource explicitSailSource = memorySailStore.getExplicitSailSource()) {
				getAndAbandonDataset(explicitSailSource, memorySailStore.snapshotMonitor);

				memorySailStore.snapshotMonitor.reserve(100, this).release();

				while (memorySailStore.snapshotMonitor.getFirstUnusedOrElse(100) != 99) {
					System.gc();
					Thread.sleep(1);
				}
			}

		}

	}

	@Test
	@Timeout(60)
	public void testAutomaticCleanupSink() throws InterruptedException {
		try (MemorySailStore memorySailStore = new MemorySailStore(false)) {

			try (SailSource explicitSailSource = memorySailStore.getExplicitSailSource()) {
				getAndAbandonSink(explicitSailSource, memorySailStore.snapshotMonitor);

				memorySailStore.snapshotMonitor.reserve(100, this).release();

				while (memorySailStore.snapshotMonitor.getFirstUnusedOrElse(100) != 99) {
					System.gc();
					Thread.sleep(1);
				}
			}

		}

	}

	@Test
	public void testReservationAndReleaseDataset() {
		try (MemorySailStore memorySailStore = new MemorySailStore(false)) {

			try (SailSource explicitSailSource = memorySailStore.getExplicitSailSource()) {
				try (SailDataset dataset = explicitSailSource.dataset(IsolationLevels.SNAPSHOT)) {
					Assertions.assertEquals(-1,
							memorySailStore.snapshotMonitor.getFirstUnusedOrElse(100));
				}

				memorySailStore.snapshotMonitor.reserve(100, this).release();

				Assertions.assertEquals(99,
						memorySailStore.snapshotMonitor.getFirstUnusedOrElse(100));
			}

		}

	}

	@Test
	public void testReservationAndReleaseDatasetNone() {
		try (MemorySailStore memorySailStore = new MemorySailStore(false)) {

			try (SailSource explicitSailSource = memorySailStore.getExplicitSailSource()) {
				try (SailDataset dataset = explicitSailSource.dataset(IsolationLevels.NONE)) {
					Assertions.assertEquals(99,
							memorySailStore.snapshotMonitor.getFirstUnusedOrElse(100));
				}
			}

		}

	}

	@Test
	public void testReservationAndReleaseSinkSerializable() {
		try (MemorySailStore memorySailStore = new MemorySailStore(false)) {

			try (SailSource explicitSailSource = memorySailStore.getExplicitSailSource()) {
				try (SailSink sink = explicitSailSource.sink(IsolationLevels.SERIALIZABLE)) {
					Assertions.assertEquals(-1,
							memorySailStore.snapshotMonitor.getFirstUnusedOrElse(100));
				}
				memorySailStore.snapshotMonitor.reserve(100, this).release();

				Assertions.assertEquals(99,
						memorySailStore.snapshotMonitor.getFirstUnusedOrElse(100));
			}

		}

	}

	@Test
	public void testReservationAndReleaseSink() {
		try (MemorySailStore memorySailStore = new MemorySailStore(false)) {

			try (SailSource explicitSailSource = memorySailStore.getExplicitSailSource()) {
				try (SailSink sink = explicitSailSource.sink(IsolationLevels.SNAPSHOT)) {
					Assertions.assertEquals(-1,
							memorySailStore.snapshotMonitor.getFirstUnusedOrElse(0));
				}
			}

		}

	}

	@Test
	public void testMultipleReservations() {
		try (MemorySailStore memorySailStore = new MemorySailStore(true)) {

			try (SailSource explicitSailSource = memorySailStore.getExplicitSailSource()) {
				try (SailDataset dataset = explicitSailSource.dataset(IsolationLevels.SNAPSHOT)) {
					Assertions.assertEquals(-1,
							memorySailStore.snapshotMonitor.getFirstUnusedOrElse(100));
				}
				try (SailSink sink = explicitSailSource.sink(IsolationLevels.SNAPSHOT)) {
					sink.prepare();
					sink.flush();
				}
				try (SailDataset dataset = explicitSailSource.dataset(IsolationLevels.SNAPSHOT)) {
					try (SailSink sink = explicitSailSource.sink(IsolationLevels.SNAPSHOT)) {
						sink.prepare();
						sink.flush();
					}
					Assertions.assertEquals(0, memorySailStore.snapshotMonitor.getFirstUnusedOrElse(100));
					try (SailDataset dataset2 = explicitSailSource.dataset(IsolationLevels.SNAPSHOT)) {
						try (SailSink sink = explicitSailSource.sink(IsolationLevels.SNAPSHOT)) {
							sink.prepare();
							sink.flush();
						}
						Assertions.assertEquals(0,
								memorySailStore.snapshotMonitor.getFirstUnusedOrElse(100));
					}
				}

				try (SailDataset dataset = explicitSailSource.dataset(IsolationLevels.SNAPSHOT)) {
					Assertions.assertEquals(2, memorySailStore.snapshotMonitor.getFirstUnusedOrElse(100));
				}

				memorySailStore.snapshotMonitor.reserve(100, this).release();

				Assertions.assertEquals(99,
						memorySailStore.snapshotMonitor.getFirstUnusedOrElse(100));
			}

		}

	}

	@Test
	public void testOverlappingReservations() {
		try (MemorySailStore memorySailStore = new MemorySailStore(true)) {

			try (SailSource explicitSailSource = memorySailStore.getExplicitSailSource()) {
				SailDataset dataset = explicitSailSource.dataset(IsolationLevels.SNAPSHOT);

				SailSink sink = explicitSailSource.sink(IsolationLevels.SNAPSHOT);
				sink.prepare();
				sink.flush();
				sink.close();

				sink = explicitSailSource.sink(IsolationLevels.SNAPSHOT);
				sink.prepare();
				sink.flush();
				sink.close();

				sink = explicitSailSource.sink(IsolationLevels.SERIALIZABLE);
				Assertions.assertEquals(-1, memorySailStore.snapshotMonitor.getFirstUnusedOrElse(100));

				dataset.close();

				Assertions.assertEquals(1, memorySailStore.snapshotMonitor.getFirstUnusedOrElse(100));

				sink.prepare();
				sink.flush();

				dataset = explicitSailSource.dataset(IsolationLevels.SNAPSHOT);

				Assertions.assertEquals(1, memorySailStore.snapshotMonitor.getFirstUnusedOrElse(100));

				sink.close();

				Assertions.assertEquals(2, memorySailStore.snapshotMonitor.getFirstUnusedOrElse(100));

				dataset.close();
				memorySailStore.snapshotMonitor.reserve(100, this).release();

				Assertions.assertEquals(99,
						memorySailStore.snapshotMonitor.getFirstUnusedOrElse(100));

			}

		}

	}

	private void getAndAbandonDataset(SailSource explicitSailSource, MemorySailStore.SnapshotMonitor snapshotMonitor) {
		SailDataset dataset = explicitSailSource.dataset(IsolationLevels.SNAPSHOT);
		CloseableIteration<? extends Resource, SailException> contextIDs = dataset.getContextIDs();
		contextIDs.close();
		Assertions.assertNotEquals(100, snapshotMonitor.getFirstUnusedOrElse(100));
	}

	private void getAndAbandonSink(SailSource explicitSailSource, MemorySailStore.SnapshotMonitor snapshotMonitor) {
		SailSink sink = explicitSailSource.sink(IsolationLevels.SERIALIZABLE);
		sink.prepare();
		sink.flush();
		Assertions.assertNotEquals(100, snapshotMonitor.getFirstUnusedOrElse(100));
	}
}
