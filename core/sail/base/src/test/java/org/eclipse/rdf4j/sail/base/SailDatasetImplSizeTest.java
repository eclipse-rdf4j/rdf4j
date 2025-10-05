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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
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
			public Model createEmptyModel() {
				return new LinkedHashModel();
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
		private final List<Statement> data;

		private ListBackedDataset(List<Statement> data) {
			this.data = List.copyOf(data);
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
			Stream<Statement> stream = data.stream();
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
				Set<Resource> ctxs = new HashSet<>(Arrays.asList(contexts));
				stream = stream.filter(st -> ctxs.contains(st.getContext()));
			}
			Iterator<Statement> it = stream.iterator();
			return new CloseableIteratorIteration<>(it);
		}
	}

	private static final ValueFactory VF = SimpleValueFactory.getInstance();
	private static final Resource CTX_A = VF.createIRI("urn:ctx:A");
	private static final Resource CTX_B = VF.createIRI("urn:ctx:B");
	private static final Resource CTX_C = VF.createIRI("urn:ctx:C");
	private static final IRI P = VF.createIRI("urn:p");
	private static final IRI Q = VF.createIRI("urn:q");

	private static Statement st(String s, String o, Resource ctx) {
		return VF.createStatement(VF.createIRI("urn:s:" + s), P, VF.createIRI("urn:o:" + o), ctx);
	}

	@Test
	public void size_afterMultiContextClear_andMixedContextQueries() {
		SailDataset backing = new ListBackedDataset(List.of(
				st("1", "1", CTX_A),
				st("2", "2", CTX_B),
				st("3", "3", CTX_C)
		));

		Changeset changes = new Changeset() {
			@Override
			public void flush() throws SailException {
			}

			@Override
			public Model createEmptyModel() {
				return new LinkedHashModel();
			}
		};

		// clear two contexts at once (A and B)
		changes.clear(CTX_A, CTX_B);

		SailDataset snapshot = new SailDatasetImpl(backing, changes);

		// Global should only include C; mixed queries should reflect filters properly
		assertAll(
				() -> assertEquals(1L, snapshot.size(null, null, null), "only C remains globally"),
				() -> assertEquals(0L, snapshot.size(null, null, null, CTX_A), "A cleared => empty"),
				() -> assertEquals(0L, snapshot.size(null, null, null, CTX_B), "B cleared => empty"),
				() -> assertEquals(1L, snapshot.size(null, null, null, CTX_C), "C remains"),
				() -> assertEquals(1L, snapshot.size(null, null, null, CTX_B, CTX_C), "B cleared, C remains => 1")
		);
	}

	@Test
	public void size_withSubjPredObjFilters_afterContextClear() {
		var s1 = VF.createIRI("urn:s:1");
		var o1 = VF.createIRI("urn:o:1");
		var o2 = VF.createIRI("urn:o:2");

		SailDataset backing = new ListBackedDataset(List.of(
				VF.createStatement(s1, P, o1, CTX_A),
				VF.createStatement(s1, P, o1, CTX_B),
				VF.createStatement(VF.createIRI("urn:s:2"), P, o2, CTX_B)
		));

		Changeset changes = new Changeset() {
			@Override
			public void flush() throws SailException {
			}

			@Override
			public Model createEmptyModel() {
				return new LinkedHashModel();
			}
		};

		changes.clear(CTX_A);
		SailDataset snapshot = new SailDatasetImpl(backing, changes);

		assertAll(
				() -> assertEquals(1L, snapshot.size(s1, P, null), "s1@A removed, only s1@B remains"),
				() -> assertEquals(1L, snapshot.size(null, P, o2), "o2 only in B remains"),
				() -> assertEquals(0L, snapshot.size(s1, P, o1, CTX_A), "A cleared => empty for filter"),
				() -> assertEquals(1L, snapshot.size(s1, P, o1, CTX_B), "B unaffected => 1 for filter")
		);
	}

	@Test
	public void size_withFilters_afterGlobalClear_withApprovals() {
		var s1 = VF.createIRI("urn:s:1");
		var o1 = VF.createIRI("urn:o:1");
		var o2 = VF.createIRI("urn:o:2");

		SailDataset backing = new ListBackedDataset(List.of(
				VF.createStatement(VF.createIRI("urn:s:x"), P, VF.createIRI("urn:o:x"), CTX_A)
		));

		Changeset changes = new Changeset() {
			@Override
			public void flush() throws SailException {
			}

			@Override
			public Model createEmptyModel() {
				return new LinkedHashModel();
			}
		};

		changes.clear();
		changes.approve(VF.createStatement(s1, P, o1, CTX_A));
		changes.approve(VF.createStatement(s1, P, o2, CTX_B));

		SailDataset snapshot = new SailDatasetImpl(backing, changes);

		assertAll(
				() -> assertEquals(2L, snapshot.size(s1, P, null), "two approved for s1 after global clear"),
				() -> assertEquals(1L, snapshot.size(null, P, o1), "only approved o1 remains"),
				() -> assertEquals(1L, snapshot.size(null, P, o2), "only approved o2 remains")
		);
	}

	@Test
	public void size_tripleTerms_afterClear_andApprovals() {
		// create a triple value, then use it as subject and object in statements
		var ts = VF.createTriple(VF.createIRI("urn:ts:s"), P, VF.createIRI("urn:ts:o"));
		var to = VF.createTriple(VF.createIRI("urn:to:s"), P, VF.createIRI("urn:to:o"));

		Statement subjTripleInA = VF.createStatement((Resource) ts, P, VF.createIRI("urn:o:X"), CTX_A);
		Statement objTripleInB = VF.createStatement(VF.createIRI("urn:s:Y"), P, to, CTX_B);

		SailDataset backing = new ListBackedDataset(List.of(subjTripleInA, objTripleInB));

		Changeset changes = new Changeset() {
			@Override
			public void flush() throws SailException {
			}

			@Override
			public Model createEmptyModel() {
				return new LinkedHashModel();
			}
		};

		// clear A, then approve a triple-subject statement in C
		changes.clear(CTX_A);
		Statement approvedInC = VF.createStatement((Resource) ts, P, VF.createIRI("urn:o:Z"), CTX_C);
		changes.approve(approvedInC);

		SailDataset snapshot = new SailDatasetImpl(backing, changes);

		// Expected: removed subjTripleInA (cleared); kept objTripleInB; added approvedInC
		assertAll(
				() -> assertEquals(2L, snapshot.size(null, null, null), "B + approved C"),
				() -> assertEquals(0L, snapshot.size(null, null, null, CTX_A), "A cleared"),
				() -> assertEquals(1L, snapshot.size(null, null, null, CTX_B), "B remains"),
				() -> assertEquals(1L, snapshot.size(null, null, null, CTX_C), "approved in C")
		);
	}

	@Test
	public void size_deprecatedThenApprovedDuplicate_acrossContexts() {
		// Same SPO in two contexts in backing
		Statement a = st("x", "y", CTX_A);
		Statement b = st("x", "y", CTX_B);
		SailDataset backing = new ListBackedDataset(List.of(a, b));

		Changeset changes = new Changeset() {
			@Override
			public void flush() throws SailException {
			}

			@Override
			public Model createEmptyModel() {
				return new LinkedHashModel();
			}
		};

		// Deprecate B from backing, then approve B again
		changes.deprecate(b);
		changes.approve(b);

		SailDataset snapshot = new SailDatasetImpl(backing, changes);

		// Expect both contexts visible, and no double count
		assertAll(
				() -> assertEquals(2L, snapshot.size(null, null, null), "A and B should both be visible"),
				() -> assertEquals(1L, snapshot.size(null, null, null, CTX_A), "A visible"),
				() -> assertEquals(1L, snapshot.size(null, null, null, CTX_B), "B re-approved and visible")
		);
	}

	@Test
	public void size_contextArrayWithDuplicatesAndNulls() {
		// Backing has one in default (null) context and one in A
		Statement def = VF.createStatement(VF.createIRI("urn:s:def"), P, VF.createIRI("urn:o:def"));
		Statement inA = st("a", "a", CTX_A);
		SailDataset backing = new ListBackedDataset(List.of(def, inA));

		Changeset changes = new Changeset() {
			@Override
			public void flush() throws SailException {
			}

			@Override
			public Model createEmptyModel() {
				return new LinkedHashModel();
			}
		};

		// Clear default graph only
		changes.clear((Resource) null);

		SailDataset snapshot = new SailDatasetImpl(backing, changes);

		// Ask size with duplicate and null contexts in the query
		assertAll(
				() -> assertEquals(1L, snapshot.size(null, null, null), "Only A remains globally"),
				() -> assertEquals(0L, snapshot.size(null, null, null, (Resource) null), "default graph cleared"),
				() -> assertEquals(1L, snapshot.size(null, null, null, CTX_A, (Resource) null, CTX_A),
						"duplicates ignored; default graph cleared; A remains")
		);
	}

	@Test
	public void size_additionalFilterCombinations_predOnly_objOnly_mixed() {
		var s1 = VF.createIRI("urn:s:1");
		var s2 = VF.createIRI("urn:s:2");
		var o1 = VF.createIRI("urn:o:1");
		var o2 = VF.createIRI("urn:o:2");

		SailDataset backing = new ListBackedDataset(List.of(
				VF.createStatement(s1, P, o1, CTX_A),
				VF.createStatement(s2, P, o2, CTX_B),
				VF.createStatement(s2, Q, o2, CTX_A)
		));

		Changeset changes = new Changeset() {
			@Override
			public void flush() throws SailException {
			}

			@Override
			public Model createEmptyModel() {
				return new LinkedHashModel();
			}
		};

		// Clear B, then approve a new P/o2 in A for s2
		changes.clear(CTX_B);
		changes.approve(VF.createStatement(s2, P, o2, CTX_A));

		SailDataset snapshot = new SailDatasetImpl(backing, changes);

		assertAll(
				// pred-only across all contexts: s1@A (P/o1), s2@A (P/o2 approved) => 2
				() -> assertEquals(2L, snapshot.size(null, P, null), "two P statements after clear+approve"),
				// obj-only: o2 now appears only in A (approved); B was cleared
				() -> assertEquals(2L, snapshot.size(null, null, o2), "Q@A + approved P@A have o2"),
				// mixed filter: s2,P,null => only approved one in A
				() -> assertEquals(1L, snapshot.size(s2, P, null), "s2@P only approved in A remains"),
				// context filter combos
				() -> assertEquals(2L, snapshot.size(null, null, o2, CTX_A), "both P/Q@A with o2 => 2"),
				() -> assertEquals(0L, snapshot.size(null, null, o2, CTX_B), "B cleared")
		);
	}

	@Test
	public void size_afterGlobalClear_countsApprovedOnly() {
		SailDataset backing = new ListBackedDataset(List.of(
				st("1", "1", CTX_A),
				st("2", "2", CTX_B)
		));

		Changeset changes = new Changeset() {
			@Override
			public void flush() throws SailException {
			}

			@Override
			public Model createEmptyModel() {
				return new LinkedHashModel();
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
		SailDataset backing = new ListBackedDataset(List.of(
				st("1", "1", CTX_A), st("2", "2", CTX_A),
				st("3", "3", CTX_B), st("4", "4", CTX_B), st("5", "5", CTX_B)
		));

		Changeset changes = new Changeset() {
			@Override
			public void flush() throws SailException {
			}

			@Override
			public Model createEmptyModel() {
				return new LinkedHashModel();
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
		SailDataset backing = new ListBackedDataset(List.of(
				st("1", "1", CTX_A),
				st("2", "2", CTX_B)
		));

		Changeset changes = new Changeset() {
			@Override
			public void flush() throws SailException {
			}

			@Override
			public Model createEmptyModel() {
				return new LinkedHashModel();
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
			public Model createEmptyModel() {
				return new LinkedHashModel();
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
		SailDataset backing = new ListBackedDataset(List.of(a1, b1));

		Changeset changes = new Changeset() {
			@Override
			public void flush() throws SailException {
			}

			@Override
			public Model createEmptyModel() {
				return new LinkedHashModel();
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
		SailDataset backing = new ListBackedDataset(List.of(b1));

		Changeset changes = new Changeset() {
			@Override
			public void flush() throws SailException {
			}

			@Override
			public Model createEmptyModel() {
				return new LinkedHashModel();
			}
		};
		// approve same statement as in backing
		changes.approve(b1);

		SailDataset snapshot = new SailDatasetImpl(backing, changes);
		assertEquals(1L, snapshot.size(null, null, null), "approved duplicate must not be double-counted");
	}
}
