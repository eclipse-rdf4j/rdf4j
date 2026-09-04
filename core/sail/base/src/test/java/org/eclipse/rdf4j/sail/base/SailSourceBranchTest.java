/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.SailException;
import org.junit.jupiter.api.Test;

class SailSourceBranchTest {

	@Test
	void closeDefersSnapshotUntilLastObserverCloses() {
		AtomicInteger closeCount = new AtomicInteger();
		SailSource branch = backingSource(closeCount).fork();
		SailDataset first = branch.dataset(IsolationLevels.SNAPSHOT);
		SailDataset second = branch.dataset(IsolationLevels.SNAPSHOT);

		branch.close();
		assertEquals(0, closeCount.get());

		first.close();
		assertEquals(0, closeCount.get());

		second.close();
		assertEquals(1, closeCount.get());
	}

	@Test
	void closeReleasesSnapshotWithoutObservers() {
		AtomicInteger closeCount = new AtomicInteger();
		SailSource branch = backingSource(closeCount).fork();
		SailDataset observer = branch.dataset(IsolationLevels.SNAPSHOT);
		observer.close();

		branch.close();

		assertEquals(1, closeCount.get());
	}

	@Test
	void observerCloseFailureDoesNotPinRetiredSnapshot() {
		AtomicInteger datasetCalls = new AtomicInteger();
		AtomicInteger failedCloseCount = new AtomicInteger();
		AtomicInteger snapshotCloseCount = new AtomicInteger();
		BackingSailSource backing = new BackingSailSource() {
			@Override
			public SailSink sink(IsolationLevel level) throws SailException {
				return new NoopSailSink();
			}

			@Override
			public SailDataset dataset(IsolationLevel level) throws SailException {
				return datasetCalls.getAndIncrement() == 0
						? new CloseCountingDataset(failedCloseCount, new SailException("delegate close"))
						: new CloseCountingDataset(snapshotCloseCount);
			}
		};
		SailSource branch = backing.fork();
		SailDataset failingObserver = branch.dataset(IsolationLevels.NONE);

		SailException failure = assertThrows(SailException.class, failingObserver::close);
		assertEquals("delegate close", failure.getMessage());
		assertEquals(1, failedCloseCount.get());

		SailDataset snapshotObserver = branch.dataset(IsolationLevels.SNAPSHOT);
		branch.close();
		assertEquals(0, snapshotCloseCount.get());

		snapshotObserver.close();
		assertEquals(1, snapshotCloseCount.get());
	}

	@Test
	void borrowerAfterFlushSeesFlushedChanges() {
		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI subj = vf.createIRI("urn:s");
		IRI pred = vf.createIRI("urn:p");
		IRI obj = vf.createIRI("urn:o");
		PinnedBackingSource backing = new PinnedBackingSource();
		SailSource branch = backing.fork();

		SailDataset before = branch.dataset(IsolationLevels.SNAPSHOT);
		assertFalse(hasStatement(before, subj, pred, obj));

		SailSink sink = branch.sink(IsolationLevels.SNAPSHOT);
		sink.approve(subj, pred, obj, null);
		sink.flush();
		sink.close();
		before.close();

		branch.flush();
		assertTrue(hasStatement(backing.dataset(IsolationLevels.NONE), subj, pred, obj));

		SailDataset after = branch.dataset(IsolationLevels.SNAPSHOT);
		try {
			assertTrue(hasStatement(after, subj, pred, obj),
					"borrower created after flush must not observe the pre-flush snapshot");
		} finally {
			after.close();
			branch.close();
		}
	}

	@Test
	void openObserverKeepsRetiredSnapshotUntilClosed() {
		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI subj = vf.createIRI("urn:s");
		IRI pred = vf.createIRI("urn:p");
		IRI obj = vf.createIRI("urn:o");
		PinnedBackingSource backing = new PinnedBackingSource();
		SailSource branch = backing.fork();

		SailDataset before = branch.dataset(IsolationLevels.SNAPSHOT);
		SailSink sink = branch.sink(IsolationLevels.SNAPSHOT);
		sink.approve(subj, pred, obj, null);
		sink.flush();
		sink.close();
		branch.flush();

		assertEquals(0, backing.datasetCloseCount.get());
		assertFalse(hasStatement(before, subj, pred, obj));

		SailDataset after = branch.dataset(IsolationLevels.SNAPSHOT);
		assertTrue(hasStatement(after, subj, pred, obj));

		before.close();
		assertEquals(0, backing.datasetCloseCount.get());
		after.close();
		branch.close();
		assertEquals(2, backing.datasetCloseCount.get());
	}

	private static boolean hasStatement(SailDataset dataset, Resource subj, IRI pred, Value obj) {
		try (CloseableIteration<? extends Statement> iter = dataset.getStatements(subj, pred, obj)) {
			return iter.hasNext();
		}
	}

	/**
	 * Backing source whose datasets are pinned to the committed state at creation time, like a store with native
	 * snapshot support.
	 */
	private static final class PinnedBackingSource extends BackingSailSource {
		private final AtomicInteger datasetCloseCount = new AtomicInteger();
		private volatile List<Statement> committed = List.of();

		@Override
		public SailSink sink(IsolationLevel level) throws SailException {
			return new NoopSailSink() {
				private final List<Statement> pending = new ArrayList<>();

				@Override
				public void approve(Resource subj, IRI pred, Value obj, Resource ctx) throws SailException {
					pending.add(SimpleValueFactory.getInstance().createStatement(subj, pred, obj, ctx));
				}

				@Override
				public void flush() throws SailException {
					List<Statement> next = new ArrayList<>(committed);
					next.addAll(pending);
					pending.clear();
					committed = List.copyOf(next);
				}
			};
		}

		@Override
		public SailDataset dataset(IsolationLevel level) throws SailException {
			List<Statement> pinned = committed;
			return new CloseCountingDataset(datasetCloseCount) {
				@Override
				public CloseableIteration<? extends Statement> getStatements(Resource subj, IRI pred, Value obj,
						Resource... contexts) throws SailException {
					List<Statement> matches = new ArrayList<>();
					for (Statement st : pinned) {
						if ((subj == null || subj.equals(st.getSubject()))
								&& (pred == null || pred.equals(st.getPredicate()))
								&& (obj == null || obj.equals(st.getObject()))) {
							matches.add(st);
						}
					}
					return new CloseableIteratorIteration<>(matches.iterator());
				}
			};
		}
	}

	private BackingSailSource backingSource(AtomicInteger closeCount) {
		return new BackingSailSource() {
			@Override
			public SailSink sink(IsolationLevel level) throws SailException {
				return new NoopSailSink();
			}

			@Override
			public SailDataset dataset(IsolationLevel level) throws SailException {
				return new CloseCountingDataset(closeCount);
			}
		};
	}

	private static class CloseCountingDataset implements SailDataset {
		private final AtomicInteger closeCount;
		private final SailException closeFailure;

		private CloseCountingDataset(AtomicInteger closeCount) {
			this(closeCount, null);
		}

		private CloseCountingDataset(AtomicInteger closeCount, SailException closeFailure) {
			this.closeCount = closeCount;
			this.closeFailure = closeFailure;
		}

		@Override
		public void close() throws SailException {
			closeCount.incrementAndGet();
			if (closeFailure != null) {
				throw closeFailure;
			}
		}

		@Override
		public CloseableIteration<? extends Namespace> getNamespaces() throws SailException {
			return new EmptyIteration<>();
		}

		@Override
		public String getNamespace(String prefix) throws SailException {
			return null;
		}

		@Override
		public CloseableIteration<? extends Resource> getContextIDs() throws SailException {
			return new EmptyIteration<>();
		}

		@Override
		public CloseableIteration<? extends Statement> getStatements(Resource subj, IRI pred, Value obj,
				Resource... contexts) throws SailException {
			return new EmptyIteration<>();
		}
	}

	private static class NoopSailSink implements SailSink {
		@Override
		public void prepare() throws SailException {
		}

		@Override
		public void flush() throws SailException {
		}

		@Override
		public void setNamespace(String prefix, String name) throws SailException {
		}

		@Override
		public void removeNamespace(String prefix) throws SailException {
		}

		@Override
		public void clearNamespaces() throws SailException {
		}

		@Override
		public void clear(Resource... contexts) throws SailException {
		}

		@Override
		public void observe(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		}

		@Override
		public void approve(Resource subj, IRI pred, Value obj, Resource ctx) throws SailException {
		}

		@Override
		public void deprecate(Statement statement) throws SailException {
		}

		@Override
		public void close() throws SailException {
		}
	}
}
