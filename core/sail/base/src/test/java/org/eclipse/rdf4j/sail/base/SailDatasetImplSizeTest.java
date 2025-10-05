/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.base;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.SailException;
import org.junit.jupiter.api.Test;

/**
 * Verifies that SailDatasetImpl.size respects a pending clear() operation (statementCleared), and does not delegate to
 * the backing dataset when cleared with no contexts.
 */
public class SailDatasetImplSizeTest {

	/**
	 * Minimal backing dataset that reports a fixed size regardless of arguments.
	 */
	private static final class FixedSizeDataset implements SailDataset {
		private final long size;

		private FixedSizeDataset(long size) {
			this.size = size;
		}

		@Override
		public void close() throws SailException {
			// no-op
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

		@Override
		public long size(Resource subj, IRI pred, Value obj, Resource... contexts) {
			return size;
		}
	}

	@Test
	public void size_respects_statementCleared() {
		// backing dataset contains data (non-zero size)
		SailDataset backing = new FixedSizeDataset(5);

		// create a changeset and simulate clear() without contexts
		Changeset changes = new Changeset() {
			@Override
			public void flush() throws SailException {
				// not used in this test
			}

			@Override
			public org.eclipse.rdf4j.model.Model createEmptyModel() {
				return new org.eclipse.rdf4j.model.impl.LinkedHashModel();
			}
		};

		// clear() with zero contexts should mark statementCleared=true while leaving
		// hasApproved()/hasDeprecated() false
		changes.clear();

		// snapshot over backing with pending clear should report size 0
		SailDataset snapshot = new SailDatasetImpl(backing, changes);
		long snapshotSize = snapshot.size(null, null, null);

		assertEquals(0L, snapshotSize,
				"size() should respect statementCleared and return 0 when cleared without contexts");
	}

	/**
	 * Backing dataset that returns a concrete set of statements and supports filtering.
	 */
	private static final class ListBackedDataset implements SailDataset {
		private final java.util.List<Statement> data;

		private ListBackedDataset(java.util.List<Statement> data) {
			this.data = java.util.List.copyOf(data);
		}

		@Override
		public void close() throws SailException {
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
			java.util.stream.Stream<Statement> stream = data.stream();
			if (subj != null) {
				stream = stream.filter(st -> subj.equals(st.getSubject()));
			}
			if (pred != null) {
				stream = stream.filter(st -> pred.equals(st.getPredicate()));
			}
			if (obj != null) {
				stream = stream.filter(st -> obj.equals(st.getObject()));
			}
			if (contexts != null && contexts.length > 0) {
				java.util.Set<Resource> ctxs = new java.util.HashSet<>(java.util.Arrays.asList(contexts));
				stream = stream.filter(st -> ctxs.contains(st.getContext()));
			}
			java.util.Iterator<Statement> it = stream.iterator();
			return new CloseableIteratorIteration<>(it);
		}
	}

	private static final ValueFactory VF = SimpleValueFactory.getInstance();
	private static final Resource CTX_A = VF.createIRI("urn:ctx:A");
	private static final Resource CTX_B = VF.createIRI("urn:ctx:B");
	private static final IRI P = VF.createIRI("urn:p");

	private static Statement st(String s, String o, Resource ctx) {
		return VF.createStatement(VF.createIRI("urn:s:" + s), P, VF.createIRI("urn:o:" + o), ctx);
	}

	@Test
	public void size_afterGlobalClear_countsApprovedOnly() {
		SailDataset backing = new ListBackedDataset(java.util.List.of(
				st("1", "1", CTX_A),
				st("2", "2", CTX_B)
		));

		Changeset changes = new Changeset() {
			@Override
			public void flush() throws SailException {
			}

			@Override
			public org.eclipse.rdf4j.model.Model createEmptyModel() {
				return new org.eclipse.rdf4j.model.impl.LinkedHashModel();
			}
		};

		// global clear: remove all backing statements from view
		changes.clear();
		// approve two new statements (one per context)
		changes.approve(st("a", "a", CTX_A));
		changes.approve(st("b", "b", CTX_B));

		SailDataset snapshot = new SailDatasetImpl(backing, changes);

		assertAll(
				() -> assertEquals(2L, snapshot.size(null, null, null),
						"after global clear, only approved statements are visible"),
				() -> assertEquals(1L, snapshot.size(null, null, null, CTX_A),
						"context filter A should see 1 approved statement in A"),
				() -> assertEquals(1L, snapshot.size(null, null, null, CTX_B),
						"context filter B should see 1 approved statement in B")
		);
	}

	@Test
	public void size_afterContextClear_excludesClearedContextData() {
		// backing has 2 in A and 3 in B
		SailDataset backing = new ListBackedDataset(java.util.List.of(
				st("1", "1", CTX_A), st("2", "2", CTX_A),
				st("3", "3", CTX_B), st("4", "4", CTX_B), st("5", "5", CTX_B)
		));

		Changeset changes = new Changeset() {
			@Override
			public void flush() throws SailException {
			}

			@Override
			public org.eclipse.rdf4j.model.Model createEmptyModel() {
				return new org.eclipse.rdf4j.model.impl.LinkedHashModel();
			}
		};

		// clear only context A
		changes.clear(CTX_A);

		SailDataset snapshot = new SailDatasetImpl(backing, changes);

		assertAll(
				() -> assertEquals(3L, snapshot.size(null, null, null),
						"global view should exclude cleared context A (only B remains)"),
				() -> assertEquals(0L, snapshot.size(null, null, null, CTX_A),
						"cleared context A should be empty"),
				() -> assertEquals(3L, snapshot.size(null, null, null, CTX_B),
						"uncleared context B remains visible")
		);
	}

	@Test
	public void size_afterContextClear_withApprovedInClearedContext() {
		// backing has 1 in A and 1 in B
		SailDataset backing = new ListBackedDataset(java.util.List.of(
				st("1", "1", CTX_A),
				st("2", "2", CTX_B)
		));

		Changeset changes = new Changeset() {
			@Override
			public void flush() throws SailException {
			}

			@Override
			public org.eclipse.rdf4j.model.Model createEmptyModel() {
				return new org.eclipse.rdf4j.model.impl.LinkedHashModel();
			}
		};

		// clear A, then approve a new statement in A and another in B
		changes.clear(CTX_A);
		changes.approve(st("a", "a", CTX_A));
		changes.approve(st("b", "b", CTX_B));

		SailDataset snapshot = new SailDatasetImpl(backing, changes);

		// Global: backing B (1) + approved A (1) + approved B (1) = 3
		// Context A: only approved in A (1)
		// Context B: backing B (1) + approved B (1) = 2
		assertAll(
				() -> assertEquals(3L, snapshot.size(null, null, null), "global view reflects clear+approvals"),
				() -> assertEquals(1L, snapshot.size(null, null, null, CTX_A), "A has only approved statements"),
				() -> assertEquals(2L, snapshot.size(null, null, null, CTX_B), "B has both backing and approved")
		);
	}

	@Test
	public void size_noChanges_delegatesToDerivedFrom() {
		// With no approved/deprecated and not cleared, must delegate to backing.size
		SailDataset backing = new FixedSizeDataset(7);
		Changeset changes = new Changeset() {
			@Override
			public void flush() throws SailException {
			}

			@Override
			public org.eclipse.rdf4j.model.Model createEmptyModel() {
				return new org.eclipse.rdf4j.model.impl.LinkedHashModel();
			}
		};
		SailDataset snapshot = new SailDatasetImpl(backing, changes);
		assertEquals(7L, snapshot.size(null, null, null));
		assertEquals(7L, snapshot.size(null, null, null, CTX_A));
	}

	@Test
	public void size_withDeprecatedStatements_excludesDeprecatedOnes() {
		Statement a1 = st("1", "1", CTX_A);
		Statement b1 = st("2", "2", CTX_B);
		SailDataset backing = new ListBackedDataset(java.util.List.of(a1, b1));

		Changeset changes = new Changeset() {
			@Override
			public void flush() throws SailException {
			}

			@Override
			public org.eclipse.rdf4j.model.Model createEmptyModel() {
				return new org.eclipse.rdf4j.model.impl.LinkedHashModel();
			}
		};
		// deprecate one existing statement
		changes.deprecate(a1);

		SailDataset snapshot = new SailDatasetImpl(backing, changes);
		assertAll(
				() -> assertEquals(1L, snapshot.size(null, null, null), "one deprecated removed from global view"),
				() -> assertEquals(0L, snapshot.size(null, null, null, CTX_A), "deprecated in A excluded"),
				() -> assertEquals(1L, snapshot.size(null, null, null, CTX_B), "B remains visible")
		);
	}

	@Test
	public void size_withApprovedDuplicates_doesNotDoubleCount() {
		Statement b1 = st("2", "2", CTX_B);
		SailDataset backing = new ListBackedDataset(java.util.List.of(b1));

		Changeset changes = new Changeset() {
			@Override
			public void flush() throws SailException {
			}

			@Override
			public org.eclipse.rdf4j.model.Model createEmptyModel() {
				return new org.eclipse.rdf4j.model.impl.LinkedHashModel();
			}
		};
		// approve same statement as in backing
		changes.approve(b1);

		SailDataset snapshot = new SailDatasetImpl(backing, changes);
		assertEquals(1L, snapshot.size(null, null, null), "approved duplicate must not be double-counted");
	}
}
