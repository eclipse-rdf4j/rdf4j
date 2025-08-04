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
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.function.Executable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("ConstantConditions")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SketchBasedJoinEstimatorTest {

	/* ------------------------------------------------------------- */
	/* Test infrastructure */
	/* ------------------------------------------------------------- */

	private static final ValueFactory VF = SimpleValueFactory.getInstance();
	private static final Logger log = LoggerFactory.getLogger(SketchBasedJoinEstimatorTest.class);
	private StubSailStore sailStore;
	private SketchBasedJoinEstimator est;

	private static final int K = 128; // small k for deterministic tests
	private static final long THROTTLE_EVERY = 1; // disable throttling
	private static final long THROTTLE_MS = 0;

	private final Resource s1 = VF.createIRI("urn:s1");
	private final Resource s2 = VF.createIRI("urn:s2");
	private final IRI p1 = VF.createIRI("urn:p1");
	private final IRI p2 = VF.createIRI("urn:p2");
	private final Value o1 = VF.createIRI("urn:o1");
	private final Value o2 = VF.createIRI("urn:o2");
	private final Resource c1 = VF.createIRI("urn:c1");

	@BeforeEach
	void setUp() {
		sailStore = new StubSailStore();
		est = new SketchBasedJoinEstimator(sailStore, K, THROTTLE_EVERY, THROTTLE_MS);
	}

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
		double eps = Math.max(1.0, expected * 0.05); // 5 % or ±1
		assertEquals(expected, actual, eps);
	}

	/* ------------------------------------------------------------- */
	/* 1. Functional “happy path” tests */
	/* ------------------------------------------------------------- */

	@Test
	void singleCardinalityAfterFullRebuild() {
		sailStore.addAll(List.of(stmt(s1, p1, o1), stmt(s2, p1, o1)));
		fullRebuild();

		double cardP1 = est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue());

		assertApprox(2.0, cardP1);
	}

	@Test
	void pairCardinality() {
		sailStore.addAll(List.of(stmt(s1, p1, o1), stmt(s1, p1, o2)));
		fullRebuild();

		double cardSP = est.cardinalityPair(SketchBasedJoinEstimator.Pair.SP, s1.stringValue(), p1.stringValue());

		assertApprox(2.0, cardSP);
	}

	@Test
	void basicJoinEstimate() {
		// s1 p1 o1
		// s1 p2 o1
		sailStore.addAll(List.of(stmt(s1, p1, o1), stmt(s1, p2, o1)));
		fullRebuild();

		double size = est.estimate(SketchBasedJoinEstimator.Component.S, null, p1.stringValue(), o1.stringValue(), null)
				.join(SketchBasedJoinEstimator.Component.S, null, p2.stringValue(), o1.stringValue(), null)
				.estimate();

		assertApprox(1.0, size); // only { ?s = s1 } satisfies both
	}

	@Test
	void incrementalAddVisibleAfterRebuild() {
		fullRebuild(); // initial empty snapshot
		assertApprox(0.0, est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue()));

		est.addStatement(stmt(s1, p1, o1));
		fullRebuild(); // force compaction

		assertApprox(1.0, est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue()));
	}

	@Test
	void incrementalDeleteVisibleAfterRebuild() {
		sailStore.add(stmt(s1, p1, o1));
		fullRebuild();
		assertApprox(1.0, est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue()));

		est.deleteStatement(stmt(s1, p1, o1));
		fullRebuild();

		assertApprox(0.0, est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue()));
	}

	/* ------------------------------------------------------------- */
	/* 2. Edge‑case tests */
	/* ------------------------------------------------------------- */

	@Test
	void noConstantPatternReturnsZero() {
		fullRebuild();
		double size = est.estimate(SketchBasedJoinEstimator.Component.S, null, null, null, null).estimate();

		assertEquals(0.0, size);
	}

	@Test
	void unknownPairFallsBackToMinSingle() {
		sailStore.addAll(List.of(stmt(s1, p1, o1), stmt(s1, p2, o1)));
		fullRebuild();

		// Pair (S,S) is “unknown” but min{|S=s1|, |S=s1|} = 2
		double card = est.estimateCount(SketchBasedJoinEstimator.Component.P, s1.stringValue(), null, null, null);

		assertApprox(2.0, card);
	}

	@Test
	void nullContextHandledCorrectly() {
		sailStore.add(stmt(s1, p1, o1)); // null context
		fullRebuild();

		double cardC = est.cardinalitySingle(SketchBasedJoinEstimator.Component.C, "urn:default-context");

		assertApprox(1.0, cardC);
	}

	@Test
	void hashCollisionsRemainSafe() {
		// Use many distinct predicates but tiny k to induce collisions
		for (int i = 0; i < 1000; i++) {
			IRI p = VF.createIRI("urn:px" + i);
			sailStore.add(stmt(s1, p, o1));
		}
		fullRebuild();

		double total = est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue()); // p1 is just one
		// of 1000

		assertTrue(total <= 1000.0); // never over‑estimates
	}

	@Test
	void addThenDeleteBeforeRebuild() {
		fullRebuild();
		est.addStatement(stmt(s1, p1, o1));
		est.deleteStatement(stmt(s1, p1, o1));
		fullRebuild();
		assertApprox(0.0, est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue()));
	}

	@Test
	void deleteThenAddBeforeRebuild() {
		sailStore.add(stmt(s1, p1, o1));
		fullRebuild();

		est.deleteStatement(stmt(s1, p1, o1));
		est.addStatement(stmt(s1, p1, o1));
		fullRebuild();

		assertApprox(1.0, est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue()));
	}

	@Test
	void interleavedWritesDuringRebuild() throws Exception {
		// prime with one statement so rebuild takes some time
		for (int i = 0; i < 10000; i++) {
			sailStore.add(stmt(VF.createIRI("urn:s" + i), p1, o1));
		}
		fullRebuild();

		// start background refresh
		est.startBackgroundRefresh(10); // 10 ms period
		// fire live writes while refresh thread is busy
		est.addStatement(stmt(s2, p1, o1));
		est.deleteStatement(stmt(s1, p1, o1));

		// wait until background thread certainly ran at least once
		Thread.sleep(200);
		est.stop();

		// force final rebuild for determinism
		fullRebuild();

		/* s1 was deleted, s2 was added: net count unchanged */
		double card = est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue());
		assertApprox(10000.0, card);
	}

	/* ------------------------------------------------------------- */
	/* 3. Concurrency / race‑condition tests */
	/* ------------------------------------------------------------- */

	@Test
	void concurrentReadersAndWriters() throws Exception {
		sailStore.add(stmt(s1, p1, o1));
		fullRebuild();

		int nThreads = 8;
		int opsPerThread = 500;
		ExecutorService exec = Executors.newFixedThreadPool(nThreads);

		Runnable writer = () -> {
			for (int i = 0; i < opsPerThread; i++) {
				Statement st = stmt(VF.createIRI("urn:s" + ThreadLocalRandom.current().nextInt(10000)), p1, o1);
				if (i % 2 == 0) {
					est.addStatement(st);
				} else {
					est.deleteStatement(st);
				}
			}
		};
		Runnable reader = () -> {
			for (int i = 0; i < opsPerThread; i++) {
				est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue());
			}
		};

		for (int t = 0; t < nThreads / 2; t++) {
			exec.submit(writer);
			exec.submit(reader);
		}

		exec.shutdown();
		assertTrue(exec.awaitTermination(5, TimeUnit.SECONDS), "concurrent run did not finish in time");

		// Ensure no explosion in estimate (safety property)
		fullRebuild();
		double card = est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue());
		assertTrue(card >= 0 && card < 15000);
	}

	@Test
	void snapshotIsolationDuringSwap() {
		sailStore.add(stmt(s1, p1, o1));
		fullRebuild();

		est.startBackgroundRefresh(5);

		/* Continuously read during many swaps */
		ExecutorService exec = Executors.newSingleThreadExecutor();
		Future<?> fut = exec.submit(() -> {
			for (int i = 0; i < 1000; i++) {
				double v = est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue());
				assertTrue(v >= 0.0); // never crashes, never negative
			}
		});

		assertDoesNotThrow((Executable) fut::get);
		est.stop();
		exec.shutdownNow();
	}

	/* ------------------------------------------------------------- */
	/* 4. NEW functional and edge‑case tests */
	/* ------------------------------------------------------------- */

	@Test
	void threeWayJoinEstimate() {
		sailStore.addAll(List.of(stmt(s1, p1, o1), stmt(s1, p2, o1), stmt(s1, p2, o2)));
		fullRebuild();

		double size = est.estimate(SketchBasedJoinEstimator.Component.S, null, p1.stringValue(), o1.stringValue(), null)
				.join(SketchBasedJoinEstimator.Component.S, null, p2.stringValue(), o1.stringValue(), null)
				.join(SketchBasedJoinEstimator.Component.S, null, p2.stringValue(), o2.stringValue(), null)
				.estimate();

		assertApprox(1.0, size);
	}

	@Test
	void switchJoinVariableMidChain() {
		sailStore.addAll(List.of(stmt(s1, p1, o1), stmt(s1, p2, o1), stmt(s2, p1, o1)));
		fullRebuild();

		double size = est.estimate(SketchBasedJoinEstimator.Component.S, null, p1.stringValue(), o1.stringValue(), null)
				.join(SketchBasedJoinEstimator.Component.S, null, p2.stringValue(), null, null)
				.join(SketchBasedJoinEstimator.Component.O, s2.stringValue(), p1.stringValue(), null, null)
				.estimate();

		assertApprox(1.0, size);
	}

	@Test
	void threeConstantsUsesMinSingle() {
		sailStore.add(stmt(s1, p1, o1, c1));
		fullRebuild();

		double card = est.estimateCount(SketchBasedJoinEstimator.Component.S, s1.stringValue(), p1.stringValue(),
				o1.stringValue(), null);

		assertApprox(1.0, card);
	}

	@Test
	void pairCardinalityAfterDelete() {
		sailStore.addAll(List.of(stmt(s1, p1, o1), stmt(s1, p1, o2)));
		fullRebuild();
		assertApprox(2.0, est.cardinalityPair(SketchBasedJoinEstimator.Pair.SP, s1.stringValue(), p1.stringValue()));

		est.deleteStatement(stmt(s1, p1, o1));
		fullRebuild();

		assertApprox(1.0, est.cardinalityPair(SketchBasedJoinEstimator.Pair.SP, s1.stringValue(), p1.stringValue()));
	}

	@Test
	void joinAfterDelete() {
		sailStore.addAll(List.of(stmt(s1, p1, o1), stmt(s1, p2, o1), stmt(s2, p1, o1), stmt(s2, p2, o1)));
		fullRebuild();

		double before = est.estimate(SketchBasedJoinEstimator.Component.S, null, p1.stringValue(), null, null)
				.join(SketchBasedJoinEstimator.Component.S, null, p2.stringValue(), null, null)
				.estimate();

		est.deleteStatement(stmt(s2, p1, o1));
		est.deleteStatement(stmt(s2, p2, o1));
		fullRebuild();

		double after = est.estimate(SketchBasedJoinEstimator.Component.S, null, p1.stringValue(), null, null)
				.join(SketchBasedJoinEstimator.Component.S, null, p2.stringValue(), null, null)
				.estimate();

		assertApprox(1.0, after);
	}

	@Test
	void idempotentAddSameStatement() {
		for (int i = 0; i < 100; i++) {
			est.addStatement(stmt(s1, p1, o1));
		}
		fullRebuild();

		assertApprox(1.0, est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue()));
	}

	@Test
	void pairWithDefaultContext() {
		sailStore.add(stmt(s1, p1, o1)); // default context
		fullRebuild();

		double card = est.cardinalityPair(SketchBasedJoinEstimator.Pair.SP, s1.stringValue(), p1.stringValue());

		assertApprox(1.0, card);
	}

	@Test
	void suggestNominalEntriesWithinBudget() {
		int kSuggested = SketchBasedJoinEstimator.suggestNominalEntries();
		assertTrue(kSuggested >= 16 && (kSuggested & (kSuggested - 1)) == 0);
	}

	@Test
	void emptyEstimatorReturnsZero() {
		assertEquals(0.0, est.cardinalitySingle(SketchBasedJoinEstimator.Component.S, s1.stringValue()));
	}

	@Test
	void pairHashCollisionSafety() {
		SketchBasedJoinEstimator smallEst = new SketchBasedJoinEstimator(sailStore, 16, 1, 0);
		sailStore.add(stmt(s1, p1, o1));
		sailStore.add(stmt(s2, p2, o2));
		smallEst.rebuildOnceSlow();

		double card = smallEst.cardinalityPair(SketchBasedJoinEstimator.Pair.SP, s1.stringValue(), p1.stringValue());

		assertTrue(card <= 1.0);
	}

	@Test
	void duplicateAddThenDelete() {
		est.addStatement(stmt(s1, p1, o1));
		est.addStatement(stmt(s1, p1, o1));
		est.deleteStatement(stmt(s1, p1, o1));
		fullRebuild();

		assertApprox(0.0, est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue()));
	}

	@Test
	void joinWithZeroDistinctOnOneSide() {
		sailStore.add(stmt(s1, p1, o1));
		fullRebuild();

		double size = est.estimate(SketchBasedJoinEstimator.Component.S, null, p1.stringValue(), null, null)
				.join(SketchBasedJoinEstimator.Component.S, s1.stringValue(), p2.stringValue(), null, null)
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

		assertTrue(card > 4000 && card < 6000); // allow 20 % error
	}

	@Test
	void pairKeyOverflowDoesNotCollide() throws Exception {
		Method pk = SketchBasedJoinEstimator.class.getDeclaredMethod("pairKey", int.class, int.class);
		pk.setAccessible(true);

		long k1 = (long) pk.invoke(null, 0x80000000, 123);
		long k2 = (long) pk.invoke(null, 0x7fffffff, 123);

		assertNotEquals(k1, k2);
	}

	/* ------------------------------------------------------------- */
	/* 5. NEW concurrency / race‑condition tests */
	/* ------------------------------------------------------------- */

	@Test
	void liveAdding() throws Exception {
		sailStore.add(stmt(s1, p1, o1));
		fullRebuild();

		ExecutorService exec = Executors.newFixedThreadPool(1);
		Future<?> writer = exec.submit(() -> {
			for (int i = 0; i < 1000; i++) {
				est.addStatement(stmt(VF.createIRI("urn:dyn" + i), p1, o1));
				double card = est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue());
				System.out.println("Cardinality after add: " + card);
			}
		});

		writer.get(); // wait for writes
		exec.shutdown();

		double card = est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue());

		log.info("Cardinality after write during swap: {}", card);
		assertTrue(card >= 1000); // all inserts visible
	}

	@Test
	void liveDeleting() throws Exception {
		for (int i = 0; i < 1000; i++) {
			sailStore.add(stmt(VF.createIRI("urn:dyn" + i), p1, o1));
		}
		fullRebuild();

		ExecutorService exec = Executors.newFixedThreadPool(1);
		Future<?> writer = exec.submit(() -> {
			for (int i = 0; i < 1000; i++) {
				est.deleteStatement(stmt(VF.createIRI("urn:dyn" + i), p1, o1));
				double card = est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue());
				System.out.println("Cardinality after add: " + card);
			}
		});

		writer.get(); // wait for writes
		exec.shutdown();

		double card = est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue());

		log.info("Cardinality after write during swap: {}", card);
		assertTrue(card < 10); // all inserts visible
	}

	@Test
	void interruptDuringRebuild() throws InterruptedException {
		for (int i = 0; i < 20000; i++) {
			sailStore.add(stmt(VF.createIRI("urn:s" + i), p1, o1));
		}
		est.startBackgroundRefresh(50);
		Thread.sleep(25); // likely rebuilding
		est.stop();
		Thread.sleep(50);

		boolean threadAlive = Thread.getAllStackTraces()
				.keySet()
				.stream()
				.anyMatch(t -> t.getName().startsWith("RdfJoinEstimator-Refresh"));
		assertFalse(threadAlive);
	}

	@RepeatedTest(1000)
	void rapidBackToBackRebuilds() throws Exception {
		est.startBackgroundRefresh(1);
		ExecutorService exec = Executors.newSingleThreadExecutor();
		exec.submit(() -> {
			for (int i = 0; i < 500; i++) {
				est.addStatement(stmt(VF.createIRI("urn:s" + i), p1, o1));
				est.deleteStatement(stmt(VF.createIRI("urn:s" + (i / 2)), p1, o1));
			}
		}).get();
		exec.shutdown();

		est.stop();
		fullRebuild();

		double card = est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue());
		assertTrue(card >= 0);
	}

	@Test
	void concurrentSuggestNominalEntries() throws Exception {
		ExecutorService exec = Executors.newFixedThreadPool(8);
		List<Future<Integer>> futures = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			futures.add(exec.submit(SketchBasedJoinEstimator::suggestNominalEntries));
		}

		for (Future<Integer> f : futures) {
			int kValue = f.get();
			assertTrue(kValue >= 16 && (kValue & (kValue - 1)) == 0);
		}
		exec.shutdown();
	}
}
