/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved.
 * SPDX‑License‑Identifier: BSD‑3‑Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.base;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.function.Executable;

@SuppressWarnings("ConstantConditions")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SketchBasedJoinEstimatorTest {

	/* ------------------------------------------------------------- */
	/* Test infrastructure */
	/* ------------------------------------------------------------- */

	private static final ValueFactory VF = SimpleValueFactory.getInstance();
	private StubSailStore sailStore;
	private SketchBasedJoinEstimator est;

	private static final int K = 128; // default k
	private static final long THROTTLE_EVERY = 1;
	private static final long THROTTLE_MS = 0;

	private final Resource s1 = VF.createIRI("urn:s1");
	private final Resource s2 = VF.createIRI("urn:s2");
	private final Resource s3 = VF.createIRI("urn:s3");

	private final IRI p1 = VF.createIRI("urn:p1");
	private final IRI p2 = VF.createIRI("urn:p2");
	private final IRI p3 = VF.createIRI("urn:p3");

	private final Value o1 = VF.createIRI("urn:o1");
	private final Value o2 = VF.createIRI("urn:o2");
	private final Value o3 = VF.createIRI("urn:o3");

	private final Resource c1 = VF.createIRI("urn:c1");

	@BeforeEach
	void setUp() {
		sailStore = new StubSailStore();
		est = new SketchBasedJoinEstimator(sailStore, K, THROTTLE_EVERY, THROTTLE_MS);
	}

	/* Helpers ----------------------------------------------------- */

	private Statement stmt(Resource s, IRI p, Value o, Resource c) {
		return VF.createStatement(s, p, o, c);
	}

	private Statement stmt(Resource s, IRI p, Value o) {
		return VF.createStatement(s, p, o);
	}

	private void fullRebuild() {
		est.rebuildOnceSlow();
	}

	private void assertApprox(double expected, double actual) {
		double eps = Math.max(1.0, expected * 0.05);
		assertEquals(expected, actual, eps);
	}

	/* ============================================================== */
	/* 1. Functional “happy path” tests (existing) */
	/* ============================================================== */

	@Test
	void singleCardinalityAfterFullRebuild() {
		sailStore.addAll(List.of(stmt(s1, p1, o1), stmt(s2, p1, o1)));
		fullRebuild();
		assertApprox(2.0, est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue()));
	}

	@Test
	void pairCardinality() {
		sailStore.addAll(List.of(stmt(s1, p1, o1), stmt(s1, p1, o2)));
		fullRebuild();
		assertApprox(2.0,
				est.cardinalityPair(SketchBasedJoinEstimator.Pair.SP, s1.stringValue(), p1.stringValue()));
	}

	@Test
	void basicJoinEstimate() {
		sailStore.addAll(List.of(stmt(s1, p1, o1), stmt(s1, p2, o1)));
		fullRebuild();
		double size = est.estimate(SketchBasedJoinEstimator.Component.S, null, p1.stringValue(), o1.stringValue(), null)
				.join(SketchBasedJoinEstimator.Component.S, null, p2.stringValue(), o1.stringValue(), null)
				.estimate();
		assertApprox(1.0, size);
	}

	/* incremental add/delete covered in original code … --------------------------------------- */
	/* ============================================================= */
	/* 2. New functional coverage */
	/* ============================================================= */

	@Test
	void threeWayJoinEstimate() {
		// Data: s1 p1 o1 ; s1 p2 o1 ; s1 p2 o2
		sailStore.addAll(List.of(
				stmt(s1, p1, o1),
				stmt(s1, p2, o1),
				stmt(s1, p2, o2)
		));
		fullRebuild();

		double result = est.estimate(SketchBasedJoinEstimator.Component.S,
				null, p1.stringValue(), o1.stringValue(), null) // binds ?s = s1
				.join(SketchBasedJoinEstimator.Component.S,
						null, p2.stringValue(), o1.stringValue(), null) // still ?s = s1
				.join(SketchBasedJoinEstimator.Component.S,
						null, p2.stringValue(), o2.stringValue(), null) // still ?s = s1
				.estimate();

		assertApprox(1.0, result);
	}

	@Test
	void switchJoinVariableMidChain() {
		/*
		 * (?s p1 o1) ⋈_{?s} (?s p2 ?o) ⋈_{?o} (?s2 p3 ?o) Should yield 1 result: { ?s=s1, ?o=o1 }
		 */
		sailStore.addAll(List.of(
				stmt(s1, p1, o1), // left
				stmt(s1, p2, o1), // mid
				stmt(s2, p3, o1) // right shares ?o
		));
		fullRebuild();

		double size = est.estimate(SketchBasedJoinEstimator.Component.S,
				null, p1.stringValue(), o1.stringValue(), null)
				.join(SketchBasedJoinEstimator.Component.S,
						null, p2.stringValue(), null, null) // ?o free, ?s join
				.join(SketchBasedJoinEstimator.Component.O,
						s2.stringValue(), p3.stringValue(), null, null) // now join on ?o
				.estimate();

		assertApprox(1.0, size);
	}

	@Test
	void threeConstantsUsesMinSingle() {
		sailStore.add(stmt(s1, p1, o1, c1));
		fullRebuild();
		double card = est.estimateCount(SketchBasedJoinEstimator.Component.S,
				s1.stringValue(), p1.stringValue(), o1.stringValue(), null);
		assertApprox(1.0, card);
	}

	@Test
	void pairCardinalityAfterDelete() {
		sailStore.addAll(List.of(stmt(s1, p1, o1), stmt(s1, p1, o2)));
		fullRebuild();
		assertApprox(2.0,
				est.cardinalityPair(SketchBasedJoinEstimator.Pair.SP, s1.stringValue(), p1.stringValue()));

		est.deleteStatement(stmt(s1, p1, o1));
		fullRebuild();
		assertApprox(1.0,
				est.cardinalityPair(SketchBasedJoinEstimator.Pair.SP, s1.stringValue(), p1.stringValue()));
	}

	@Test
	void joinAfterDelete() {
		sailStore.addAll(List.of(
				stmt(s1, p1, o1), stmt(s1, p2, o1), // initially gives join size 1
				stmt(s2, p1, o2), stmt(s2, p2, o2) // second candidate
		));
		fullRebuild();
		double initial = est.estimate(SketchBasedJoinEstimator.Component.S,
				null, p1.stringValue(), null, null)
				.join(SketchBasedJoinEstimator.Component.S,
						null, p2.stringValue(), null, null)
				.estimate();
		assertApprox(2.0, initial); // {s1,s2}

		est.deleteStatement(stmt(s2, p1, o2));
		est.deleteStatement(stmt(s2, p2, o2));
		fullRebuild();

		double after = est.estimate(SketchBasedJoinEstimator.Component.S,
				null, p1.stringValue(), null, null)
				.join(SketchBasedJoinEstimator.Component.S,
						null, p2.stringValue(), null, null)
				.estimate();
		assertApprox(1.0, after);
	}

	@Test
	void idempotentAddSameStatement() {
		for (int i = 0; i < 100; i++) {
			est.addStatement(stmt(s1, p1, o1));
		}
		fullRebuild();
		assertApprox(1.0,
				est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue()));
	}

	@Test
	void pairWithDefaultContext() {
		sailStore.add(stmt(s1, p1, o1)); // (null context)
		fullRebuild();
		double card = est.cardinalityPair(
				SketchBasedJoinEstimator.Pair.SP,
				s1.stringValue(), p1.stringValue());
		assertApprox(1.0, card);
	}

	@Test
	void suggestNominalEntriesWithinBudget() {
		int k = SketchBasedJoinEstimator.suggestNominalEntries();
		assertTrue(k >= 16 && (k & (k - 1)) == 0); // power‑of‑two
	}

	/* ============================================================== */
	/* 3. Additional edge‑case tests */
	/* ============================================================== */

	@Test
	void emptyEstimatorReturnsZero() {
		// no data, no rebuild
		assertEquals(0.0, est.cardinalitySingle(SketchBasedJoinEstimator.Component.S, s1.stringValue()));
	}

	@Test
	void pairHashCollisionSafety() {
		SketchBasedJoinEstimator small = new SketchBasedJoinEstimator(sailStore, 16, 1, 0);
		sailStore.add(stmt(s1, p1, o1));
		sailStore.add(stmt(s2, p2, o2));
		small.rebuildOnceSlow();
		double card = small.cardinalityPair(SketchBasedJoinEstimator.Pair.SP, s1.stringValue(), p1.stringValue());
		assertTrue(card <= 1.0);
	}

	@Test
	void duplicateAddThenDelete() {
		est.addStatement(stmt(s1, p1, o1));
		est.addStatement(stmt(s1, p1, o1));
		est.deleteStatement(stmt(s1, p1, o1));
		fullRebuild();
		assertApprox(0.0,
				est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue()));
	}

	@Test
	void joinWithZeroDistinctOnOneSide() {
		/*
		 * Left pattern binds ?s = s1 . Right pattern binds ?s = s1 as a constant (=> no free join variable,
		 * distinct=0). Implementation should treat intersectionDistinct==0 and return 0 safely.
		 */
		sailStore.add(stmt(s1, p1, o1));
		fullRebuild();
		double size = est.estimate(SketchBasedJoinEstimator.Component.S,
				null, p1.stringValue(), null, null)
				.join(SketchBasedJoinEstimator.Component.S,
						s1.stringValue(), p2.stringValue(), null, null)
				.estimate();
		assertEquals(0.0, size);
	}

	@Test
	void smallKStability() {
		SketchBasedJoinEstimator tiny = new SketchBasedJoinEstimator(sailStore, 16, 1, 0);
		for (int i = 0; i < 5000; i++) {
			sailStore.add(stmt(VF.createIRI("urn:s" + i), p1, o1));
		}
		tiny.rebuildOnceSlow();
		double card = tiny.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue());
		assertTrue(card > 4000 && card < 6000); // tolerate 20 % error
	}

	@Test
	void pairKeyOverflowDoesNotCollide() throws Exception {
		Method pk = SketchBasedJoinEstimator.class.getDeclaredMethod("pairKey", int.class, int.class);
		pk.setAccessible(true);
		long k1 = (long) pk.invoke(null, 0x80000000, 42);
		long k2 = (long) pk.invoke(null, 0x7fffffff, 42);
		assertNotEquals(k1, k2);
	}

	/* ============================================================== */
	/* 4. Concurrency / race‑condition additions */
	/* ============================================================== */

	@Test
	void writeDuringSnapshotSwap() throws Exception {
		sailStore.add(stmt(s1, p1, o1));
		fullRebuild();
		est.startBackgroundRefresh(1); // aggressive
		ExecutorService ex = Executors.newFixedThreadPool(2);

		Future<?> fut = ex.submit(() -> {
			for (int i = 0; i < 1000; i++) {
				est.addStatement(stmt(
						VF.createIRI("urn:dyn" + i), p1, o1));
			}
		});

		Thread.sleep(50); // allow some swaps
		est.stop();
		fut.get(1, TimeUnit.SECONDS);
		ex.shutdown();

		fullRebuild();
		double card = est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue());
		assertTrue(card >= 1000);
	}

	@Test
	void interruptDuringRebuild() throws Exception {
		for (int i = 0; i < 10000; i++) {
			sailStore.add(stmt(
					VF.createIRI("urn:s" + i), p1, o1));
		}
		est.startBackgroundRefresh(50);
		Thread.sleep(20); // almost certainly in rebuild
		est.stop(); // should terminate thread
		Thread.sleep(20);
		assertFalse(est.isReady() && Thread.getAllStackTraces()
				.keySet()
				.stream()
				.anyMatch(t -> t.getName().startsWith("RdfJoinEstimator-Refresh")));
	}

	@Test
	void rapidBackToBackRebuilds() throws Exception {
		est.startBackgroundRefresh(1);
		ExecutorService exec = Executors.newSingleThreadExecutor();
		Future<?> writer = exec.submit(() -> {
			for (int i = 0; i < 500; i++) {
				est.addStatement(stmt(VF.createIRI("urn:s" + i), p1, o1));
				est.deleteStatement(stmt(VF.createIRI("urn:s" + (i / 2)), p1, o1));
			}
		});
		writer.get();
		exec.shutdown();
		est.stop();
		fullRebuild();
		double card = est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue());
		assertTrue(card >= 0);
	}

	@Test
	void concurrentSuggestNominalEntries() throws Exception {
		ExecutorService exec = Executors.newFixedThreadPool(8);
		List<Future<Integer>> list = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			list.add(exec.submit(SketchBasedJoinEstimator::suggestNominalEntries));
		}
		for (Future<Integer> f : list) {
			int k = f.get();
			assertTrue(k >= 16 && (k & (k - 1)) == 0);
		}
		exec.shutdown();
	}

	/* ============================================================== */
	/* Retain existing concurrency tests from the original suite */
	/* ============================================================== */

	@Test
	void concurrentReadersAndWriters() throws Exception {
		sailStore.add(stmt(s1, p1, o1));
		fullRebuild();

		int nThreads = 8, ops = 500;
		ExecutorService ex = Executors.newFixedThreadPool(nThreads);

		Runnable writer = () -> {
			for (int i = 0; i < ops; i++) {
				Statement st = stmt(VF.createIRI("urn:s" + ThreadLocalRandom.current().nextInt(10000)), p1, o1);
				if (i % 2 == 0) {
					est.addStatement(st);
				} else {
					est.deleteStatement(st);
				}
			}
		};
		Runnable reader = () -> {
			for (int i = 0; i < ops; i++) {
				est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue());
			}
		};

		for (int t = 0; t < nThreads / 2; t++) {
			ex.submit(writer);
			ex.submit(reader);
		}
		ex.shutdown();
		assertTrue(ex.awaitTermination(5, TimeUnit.SECONDS));
		fullRebuild();
		double card = est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue());
		assertTrue(card >= 0 && card < 15000);
	}

	@Test
	void snapshotIsolationDuringSwap() {
		sailStore.add(stmt(s1, p1, o1));
		fullRebuild();
		est.startBackgroundRefresh(5);

		ExecutorService ex = Executors.newSingleThreadExecutor();
		Future<?> fut = ex.submit(() -> {
			for (int i = 0; i < 1000; i++) {
				assertTrue(est.cardinalitySingle(
						SketchBasedJoinEstimator.Component.P, p1.stringValue()) >= 0.0);
			}
		});
		assertDoesNotThrow((Executable) fut::get);
		est.stop();
		ex.shutdownNow();
	}
}
