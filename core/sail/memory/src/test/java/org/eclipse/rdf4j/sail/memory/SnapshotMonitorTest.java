/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

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

				while (memorySailStore.snapshotMonitor.getFirstUnusedOrElse(Integer.MIN_VALUE) != Integer.MIN_VALUE) {
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

				while (memorySailStore.snapshotMonitor.getFirstUnusedOrElse(Integer.MIN_VALUE) != Integer.MIN_VALUE) {
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
							memorySailStore.snapshotMonitor.getFirstUnusedOrElse(Integer.MIN_VALUE));
				}
				Assertions.assertEquals(Integer.MIN_VALUE,
						memorySailStore.snapshotMonitor.getFirstUnusedOrElse(Integer.MIN_VALUE));
			}

		}

	}

	@Test
	public void testReservationAndReleaseDatasetNone() {
		try (MemorySailStore memorySailStore = new MemorySailStore(false)) {

			try (SailSource explicitSailSource = memorySailStore.getExplicitSailSource()) {
				try (SailDataset dataset = explicitSailSource.dataset(IsolationLevels.NONE)) {
					Assertions.assertEquals(Integer.MIN_VALUE,
							memorySailStore.snapshotMonitor.getFirstUnusedOrElse(Integer.MIN_VALUE));
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
							memorySailStore.snapshotMonitor.getFirstUnusedOrElse(Integer.MIN_VALUE));
				}
				Assertions.assertEquals(Integer.MIN_VALUE,
						memorySailStore.snapshotMonitor.getFirstUnusedOrElse(Integer.MIN_VALUE));
			}

		}

	}

	@Test
	public void testReservationAndReleaseSink() {
		try (MemorySailStore memorySailStore = new MemorySailStore(false)) {

			try (SailSource explicitSailSource = memorySailStore.getExplicitSailSource()) {
				try (SailSink sink = explicitSailSource.sink(IsolationLevels.SNAPSHOT)) {
					Assertions.assertEquals(Integer.MIN_VALUE,
							memorySailStore.snapshotMonitor.getFirstUnusedOrElse(Integer.MIN_VALUE));
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
							memorySailStore.snapshotMonitor.getFirstUnusedOrElse(Integer.MIN_VALUE));
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
					Assertions.assertEquals(0, memorySailStore.snapshotMonitor.getFirstUnusedOrElse(Integer.MIN_VALUE));
					try (SailDataset dataset2 = explicitSailSource.dataset(IsolationLevels.SNAPSHOT)) {
						try (SailSink sink = explicitSailSource.sink(IsolationLevels.SNAPSHOT)) {
							sink.prepare();
							sink.flush();
						}
						Assertions.assertEquals(0,
								memorySailStore.snapshotMonitor.getFirstUnusedOrElse(Integer.MIN_VALUE));
					}
				}

				try (SailDataset dataset = explicitSailSource.dataset(IsolationLevels.SNAPSHOT)) {
					Assertions.assertEquals(2, memorySailStore.snapshotMonitor.getFirstUnusedOrElse(Integer.MIN_VALUE));
				}

				Assertions.assertEquals(Integer.MIN_VALUE,
						memorySailStore.snapshotMonitor.getFirstUnusedOrElse(Integer.MIN_VALUE));
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
				Assertions.assertEquals(-1, memorySailStore.snapshotMonitor.getFirstUnusedOrElse(Integer.MIN_VALUE));

				dataset.close();

				Assertions.assertEquals(1, memorySailStore.snapshotMonitor.getFirstUnusedOrElse(Integer.MIN_VALUE));

				sink.prepare();
				sink.flush();

				dataset = explicitSailSource.dataset(IsolationLevels.SNAPSHOT);

				Assertions.assertEquals(1, memorySailStore.snapshotMonitor.getFirstUnusedOrElse(Integer.MIN_VALUE));

				sink.close();

				Assertions.assertEquals(2, memorySailStore.snapshotMonitor.getFirstUnusedOrElse(Integer.MIN_VALUE));

				dataset.close();

				Assertions.assertEquals(Integer.MIN_VALUE,
						memorySailStore.snapshotMonitor.getFirstUnusedOrElse(Integer.MIN_VALUE));

			}

		}

	}

	private void getAndAbandonDataset(SailSource explicitSailSource, MemorySailStore.SnapshotMonitor snapshotMonitor) {
		SailDataset dataset = explicitSailSource.dataset(IsolationLevels.SNAPSHOT);
		CloseableIteration<? extends Resource, SailException> contextIDs = dataset.getContextIDs();
		contextIDs.close();
		Assertions.assertNotEquals(Integer.MIN_VALUE, snapshotMonitor.getFirstUnusedOrElse(Integer.MIN_VALUE));
	}

	private void getAndAbandonSink(SailSource explicitSailSource, MemorySailStore.SnapshotMonitor snapshotMonitor) {
		SailSink sink = explicitSailSource.sink(IsolationLevels.SERIALIZABLE);
		sink.prepare();
		sink.flush();
		Assertions.assertNotEquals(Integer.MIN_VALUE, snapshotMonitor.getFirstUnusedOrElse(Integer.MIN_VALUE));
	}
}
