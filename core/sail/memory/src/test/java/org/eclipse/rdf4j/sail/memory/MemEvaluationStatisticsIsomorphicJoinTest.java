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
package org.eclipse.rdf4j.sail.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailSource;
import org.eclipse.rdf4j.sail.base.SailStore;
import org.eclipse.rdf4j.sail.base.SketchBasedJoinEstimator;
import org.eclipse.rdf4j.sail.memory.model.MemStatementList;
import org.eclipse.rdf4j.sail.memory.model.MemValueFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MemEvaluationStatisticsIsomorphicJoinTest {

	@BeforeEach
	void clearCache() {
		MemEvaluationStatistics.cache.invalidateAll();
	}

	@Test
	void isomorphicJoinsShareCacheEntry() {
		AtomicInteger calls = new AtomicInteger();
		SketchBasedJoinEstimator estimator = new CountingEstimator(calls);
		MemEvaluationStatistics stats = new MemEvaluationStatistics(new MemValueFactory(), new MemStatementList(),
				estimator);

		ValueFactory vf = SimpleValueFactory.getInstance();

		StatementPattern left1 = new StatementPattern(
				Var.of("s", vf.createBNode("a")),
				Var.of("p", vf.createIRI("urn:p")),
				Var.of("o"));
		StatementPattern right1 = new StatementPattern(
				Var.of("o"),
				Var.of("q", vf.createIRI("urn:q")),
				Var.of("x"));
		Join join1 = new Join(left1, right1);

		StatementPattern left2 = new StatementPattern(
				Var.of("subj", vf.createBNode("b")),
				Var.of("pred", vf.createIRI("urn:p")),
				Var.of("joinVar"));
		StatementPattern right2 = new StatementPattern(
				Var.of("joinVar"),
				Var.of("pred2", vf.createIRI("urn:q")),
				Var.of("obj2"));
		Join join2 = new Join(left2, right2);

		stats.getCardinality(join1);
		stats.getCardinality(join2);

		assertEquals(1, calls.get(), "Isomorphic joins should share cached estimate");
	}

	@Test
	void isomorphicJoinEqualityIgnoresVariableNamesAndConstantIdentity() {
		ValueFactory vf1 = SimpleValueFactory.getInstance();
		ValueFactory vf2 = SimpleValueFactory.getInstance();

		StatementPattern left1 = new StatementPattern(
				Var.of("s", vf1.createIRI("urn:subj")),
				Var.of("p", vf1.createIRI("urn:p")),
				Var.of("o"));
		StatementPattern right1 = new StatementPattern(
				Var.of("o"),
				Var.of("q", vf1.createIRI("urn:q")),
				Var.of("x", vf1.createLiteral("1")));
		Join join1 = new Join(left1, right1);

		StatementPattern left2 = new StatementPattern(
				Var.of("subject", vf2.createIRI("urn:subj")),
				Var.of("predicate", vf2.createIRI("urn:p")),
				Var.of("joinVar"));
		StatementPattern right2 = new StatementPattern(
				Var.of("joinVar"),
				Var.of("predicate2", vf2.createIRI("urn:q")),
				Var.of("object2", vf2.createLiteral("1")));
		Join join2 = new Join(left2, right2);

		MemEvaluationStatistics.IsomorphicJoin key1 = new MemEvaluationStatistics.IsomorphicJoin(join1);
		MemEvaluationStatistics.IsomorphicJoin key2 = new MemEvaluationStatistics.IsomorphicJoin(join2);

		assertEquals(key1, key2);
		assertEquals(key1.hashCode(), key2.hashCode());
	}

	@Test
	void nonIsomorphicJoinsNotEqualWhenVariableSharingDiffers() {
		ValueFactory vf = SimpleValueFactory.getInstance();

		StatementPattern left1 = new StatementPattern(
				Var.of("s"),
				Var.of("p", vf.createIRI("urn:p")),
				Var.of("o"));
		StatementPattern right1 = new StatementPattern(
				Var.of("o"),
				Var.of("q", vf.createIRI("urn:q")),
				Var.of("x"));
		Join join1 = new Join(left1, right1);

		StatementPattern left2 = new StatementPattern(
				Var.of("s"),
				Var.of("p", vf.createIRI("urn:p")),
				Var.of("o"));
		StatementPattern right2 = new StatementPattern(
				Var.of("s"),
				Var.of("q", vf.createIRI("urn:q")),
				Var.of("x"));
		Join join2 = new Join(left2, right2);

		MemEvaluationStatistics.IsomorphicJoin key1 = new MemEvaluationStatistics.IsomorphicJoin(join1);
		MemEvaluationStatistics.IsomorphicJoin key2 = new MemEvaluationStatistics.IsomorphicJoin(join2);

		assertFalse(key1.equals(key2), "Different literal datatypes should not be isomorphic");
	}

	@Test
	void blankNodeIdsIgnoredButReuseStructurePreserved() {
		ValueFactory vf = SimpleValueFactory.getInstance();

		// Same blank node reused across both patterns
		Var bn1Left = Var.of("s", vf.createBNode("a"));
		Var bn1Right = Var.of("s", vf.createBNode("a"));
		Join join1 = new Join(
				new StatementPattern(bn1Left, Var.of("p", vf.createIRI("urn:p")), Var.of("o")),
				new StatementPattern(bn1Right, Var.of("q", vf.createIRI("urn:q")), Var.of("x")));

		// Different blank node labels but same reuse structure
		Var bn2Left = Var.of("subj", vf.createBNode("x"));
		Var bn2Right = Var.of("subj", vf.createBNode("x"));
		Join join2 = new Join(
				new StatementPattern(bn2Left, Var.of("pred", vf.createIRI("urn:p")), Var.of("joinVar")),
				new StatementPattern(bn2Right, Var.of("pred2", vf.createIRI("urn:q")), Var.of("obj2")));

		// Two distinct blank nodes (no reuse)
		Join join3 = new Join(
				new StatementPattern(Var.of("s", vf.createBNode("x")), Var.of("p", vf.createIRI("urn:p")), Var.of("o")),
				new StatementPattern(Var.of("s", vf.createBNode("y")), Var.of("q", vf.createIRI("urn:q")),
						Var.of("x")));

		MemEvaluationStatistics.IsomorphicJoin key1 = new MemEvaluationStatistics.IsomorphicJoin(join1);
		MemEvaluationStatistics.IsomorphicJoin key2 = new MemEvaluationStatistics.IsomorphicJoin(join2);
		MemEvaluationStatistics.IsomorphicJoin key3 = new MemEvaluationStatistics.IsomorphicJoin(join3);

		assertEquals(key1, key2);
		assertNotEquals(key1, key3);
	}

	@Test
	void differentLiteralDatatypesMakeJoinsNonIsomorphic() {
		ValueFactory vf = SimpleValueFactory.getInstance();

		StatementPattern left1 = new StatementPattern(
				Var.of("s"),
				Var.of("p", vf.createIRI("urn:p")),
				Var.of("o", vf.createLiteral("1")));
		StatementPattern right1 = new StatementPattern(
				Var.of("o"),
				Var.of("q", vf.createIRI("urn:q")),
				Var.of("x"));
		Join join1 = new Join(left1, right1);

		StatementPattern left2 = new StatementPattern(
				Var.of("s2"),
				Var.of("p2", vf.createIRI("urn:p")),
				Var.of("o2", vf.createLiteral(1)));
		StatementPattern right2 = new StatementPattern(
				Var.of("o2"),
				Var.of("q2", vf.createIRI("urn:q")),
				Var.of("x2"));
		Join join2 = new Join(left2, right2);

		MemEvaluationStatistics.IsomorphicJoin key1 = new MemEvaluationStatistics.IsomorphicJoin(join1);
		MemEvaluationStatistics.IsomorphicJoin key2 = new MemEvaluationStatistics.IsomorphicJoin(join2);

		assertFalse(key1.equals(key2), "Different literal datatypes should not be isomorphic");
	}

	@Test
	void sharedVariablesRecognizedEvenWhenDifferentInstances() {
		ValueFactory vf = SimpleValueFactory.getInstance();

		// Join variable "x" is shared by name across patterns (different Var instances).
		Join joinWithSharing = new Join(
				new StatementPattern(Var.of("s"), Var.of("p", vf.createIRI("urn:p")), Var.of("x")),
				new StatementPattern(Var.of("x"), Var.of("q", vf.createIRI("urn:q")), Var.of("o")));

		// Same positions, but no shared variable (names differ).
		Join joinWithoutSharing = new Join(
				new StatementPattern(Var.of("s2"), Var.of("p2", vf.createIRI("urn:p")), Var.of("x1")),
				new StatementPattern(Var.of("x2"), Var.of("q2", vf.createIRI("urn:q")), Var.of("o2")));

		MemEvaluationStatistics.IsomorphicJoin keySharing = new MemEvaluationStatistics.IsomorphicJoin(joinWithSharing);
		MemEvaluationStatistics.IsomorphicJoin keyNoSharing = new MemEvaluationStatistics.IsomorphicJoin(
				joinWithoutSharing);

		assertNotEquals(keySharing, keyNoSharing, "Sharing vs. non-sharing should not be isomorphic");
	}

	@Test
	void allVariableOverlapCombinationsProduceUniqueIsomorphicSignatures() {
		List<int[]> partitions = generateCanonicalPartitions(8);
		Set<MemEvaluationStatistics.IsomorphicJoin> seen = new HashSet<>(partitions.size());

		for (int[] partition : partitions) {
			Join joinA = buildJoin(partition, "a");
			Join joinB = buildJoin(partition, "b");

			MemEvaluationStatistics.IsomorphicJoin keyA = new MemEvaluationStatistics.IsomorphicJoin(joinA);
			MemEvaluationStatistics.IsomorphicJoin keyB = new MemEvaluationStatistics.IsomorphicJoin(joinB);

			assertEquals(keyA, keyB,
					() -> "Expected isomorphic under renaming for partition " + Arrays.toString(partition));
			assertEquals(keyA.hashCode(), keyB.hashCode(),
					() -> "Hash codes should match for isomorphic partition " + Arrays.toString(partition));

			if (!seen.add(keyA)) {
				fail("Different variable overlap configurations produced the same signature: "
						+ Arrays.toString(partition));
			}
		}

		assertEquals(partitions.size(), seen.size(),
				"Each distinct overlap configuration should yield a distinct IsomorphicJoin signature");
	}

	private static Join buildJoin(int[] partition, String prefix) {
		Var[] vars = new Var[partition.length];
		for (int i = 0; i < partition.length; i++) {
			vars[i] = Var.of(prefix + partition[i]);
		}

		StatementPattern left = new StatementPattern(vars[0], vars[1], vars[2], vars[3]);
		StatementPattern right = new StatementPattern(vars[4], vars[5], vars[6], vars[7]);

		return new Join(left, right);
	}

	private static List<int[]> generateCanonicalPartitions(int size) {
		List<int[]> out = new ArrayList<>();
		int[] assignment = new int[size];
		assignment[0] = 0;
		generatePartitions(1, 0, assignment, out);
		return out;
	}

	private static void generatePartitions(int index, int maxClass, int[] assignment, List<int[]> out) {
		if (index == assignment.length) {
			out.add(assignment.clone());
			return;
		}

		for (int cls = 0; cls <= maxClass + 1; cls++) {
			assignment[index] = cls;
			generatePartitions(index + 1, Math.max(maxClass, cls), assignment, out);
		}
	}

	private static final class CountingEstimator extends SketchBasedJoinEstimator {
		private final AtomicInteger calls;

		CountingEstimator(AtomicInteger calls) {
			super(new StubSailStore(), 16, 1, 0);
			this.calls = calls;
		}

		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public double cardinality(Join node) {
			calls.incrementAndGet();
			return 42.0;
		}
	}

	private static final class StubSailStore implements SailStore {
		private static final ValueFactory VF = SimpleValueFactory.getInstance();

		@Override
		public ValueFactory getValueFactory() {
			return VF;
		}

		@Override
		public EvaluationStatistics getEvaluationStatistics() {
			throw new UnsupportedOperationException();
		}

		@Override
		public SailSource getExplicitSailSource() {
			throw new UnsupportedOperationException();
		}

		@Override
		public SailSource getInferredSailSource() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void close() throws SailException {
			// no-op
		}
	}
}
