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

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.*;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.*;
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

	private static final int K = 128; // small k for deterministic tests
	private static final long THROTTLE_EVERY = 1; // disable throttling
	private static final long THROTTLE_MS = 0;

	private Resource s1 = VF.createIRI("urn:s1");
	private Resource s2 = VF.createIRI("urn:s2");
	private IRI p1 = VF.createIRI("urn:p1");
	private IRI p2 = VF.createIRI("urn:p2");
	private Value o1 = VF.createIRI("urn:o1");
	private Value o2 = VF.createIRI("urn:o2");
	private Resource c1 = VF.createIRI("urn:c1");

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
		double eps = Math.max(1.0, expected * 0.05); // 5 % or ±1
		assertEquals(expected, actual, eps);
	}

	/* ------------------------------------------------------------- */
	/* 1. Functional “happy path” tests */
	/* ------------------------------------------------------------- */

	@Test
	void singleCardinalityAfterFullRebuild() {
		sailStore.addAll(List.of(
				stmt(s1, p1, o1),
				stmt(s2, p1, o1)
		));
		fullRebuild();

		double cardP1 = est.cardinalitySingle(
				SketchBasedJoinEstimator.Component.P, p1.stringValue());

		assertApprox(2.0, cardP1);
	}

	@Test
	void pairCardinality() {
		sailStore.addAll(List.of(
				stmt(s1, p1, o1),
				stmt(s1, p1, o2)
		));
		fullRebuild();

		double cardSP = est.cardinalityPair(
				SketchBasedJoinEstimator.Pair.SP,
				s1.stringValue(), p1.stringValue());

		assertApprox(2.0, cardSP);
	}

	@Test
	void basicJoinEstimate() {
		// s1 p1 o1
		// s1 p2 o1
		sailStore.addAll(List.of(
				stmt(s1, p1, o1),
				stmt(s1, p2, o1)
		));
		fullRebuild();

		double size = est.estimate(
				SketchBasedJoinEstimator.Component.S,
				null, p1.stringValue(), o1.stringValue(), null)
				.join(SketchBasedJoinEstimator.Component.S,
						null, p2.stringValue(), o1.stringValue(), null)
				.estimate();

		assertApprox(1.0, size); // only { ?s = s1 } satisfies both
	}

	@Test
	void incrementalAddVisibleAfterRebuild() {
		fullRebuild(); // initial empty snapshot
		assertApprox(0.0, est.cardinalitySingle(
				SketchBasedJoinEstimator.Component.P, p1.stringValue()));

		est.addStatement(stmt(s1, p1, o1));
		fullRebuild(); // force compaction

		assertApprox(1.0, est.cardinalitySingle(
				SketchBasedJoinEstimator.Component.P, p1.stringValue()));
	}

	@Test
	void incrementalDeleteVisibleAfterRebuild() {
		sailStore.add(stmt(s1, p1, o1));
		fullRebuild();
		assertApprox(1.0, est.cardinalitySingle(
				SketchBasedJoinEstimator.Component.P, p1.stringValue()));

		est.deleteStatement(stmt(s1, p1, o1));
		fullRebuild();

		assertApprox(0.0, est.cardinalitySingle(
				SketchBasedJoinEstimator.Component.P, p1.stringValue()));
	}

	/* ------------------------------------------------------------- */
	/* 2. Edge‑case tests */
	/* ------------------------------------------------------------- */

	@Test
	void noConstantPatternReturnsZero() {
		fullRebuild();
		double size = est.estimate(
				SketchBasedJoinEstimator.Component.S,
				null, null, null, null).estimate();

		assertEquals(0.0, size);
	}

	@Test
	void unknownPairFallsBackToMinSingle() {
		sailStore.addAll(List.of(
				stmt(s1, p1, o1),
				stmt(s1, p2, o1)
		));
		fullRebuild();

		// Pair (S,S) is “unknown” but min{|S=s1|, |S=s1|} = 2
		double card = est.estimateCount(
				SketchBasedJoinEstimator.Component.P,
				s1.stringValue(), null, null, null);

		assertApprox(2.0, card);
	}

	@Test
	void nullContextHandledCorrectly() {
		sailStore.add(stmt(s1, p1, o1)); // null context
		fullRebuild();

		double cardC = est.cardinalitySingle(
				SketchBasedJoinEstimator.Component.C,
				"urn:default-context");

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

		double total = est.cardinalitySingle(
				SketchBasedJoinEstimator.Component.P, p1.stringValue()); // p1 is just one of 1000

		assertTrue(total <= 1000.0); // never over-estimates
	}

	@Test
	void addThenDeleteBeforeRebuild() {
		fullRebuild();
		est.addStatement(stmt(s1, p1, o1));
		est.deleteStatement(stmt(s1, p1, o1));
		fullRebuild();
		assertApprox(0.0, est.cardinalitySingle(
				SketchBasedJoinEstimator.Component.P, p1.stringValue()));
	}

	@Test
	void deleteThenAddBeforeRebuild() {
		sailStore.add(stmt(s1, p1, o1));
		fullRebuild();

		est.deleteStatement(stmt(s1, p1, o1));
		est.addStatement(stmt(s1, p1, o1));
		fullRebuild();

		assertApprox(1.0, est.cardinalitySingle(
				SketchBasedJoinEstimator.Component.P, p1.stringValue()));
	}

	@Test
	void interleavedWritesDuringRebuild() throws Exception {
		// prime with one statement so rebuild takes some time
		for (int i = 0; i < 10000; i++) {
			sailStore.add(stmt(
					VF.createIRI("urn:s" + i),
					p1, o1));
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
		double card = est.cardinalitySingle(
				SketchBasedJoinEstimator.Component.P, p1.stringValue());
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
				Statement st = stmt(
						VF.createIRI("urn:s" + ThreadLocalRandom.current().nextInt(10000)),
						p1, o1);
				if (i % 2 == 0) {
					est.addStatement(st);
				} else {
					est.deleteStatement(st);
				}
			}
		};
		Runnable reader = () -> {
			for (int i = 0; i < opsPerThread; i++) {
				est.cardinalitySingle(
						SketchBasedJoinEstimator.Component.P, p1.stringValue());
			}
		};

		for (int t = 0; t < nThreads / 2; t++) {
			exec.submit(writer);
			exec.submit(reader);
		}

		exec.shutdown();
		assertTrue(exec.awaitTermination(5, TimeUnit.SECONDS),
				"concurrent run did not finish in time");

		// Ensure no explosion in estimate (safety property)
		fullRebuild();
		double card = est.cardinalitySingle(
				SketchBasedJoinEstimator.Component.P, p1.stringValue());
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
				double v = est.cardinalitySingle(
						SketchBasedJoinEstimator.Component.P, p1.stringValue());
				assertTrue(v >= 0.0); // never crashes, never negative
			}
		});

		assertDoesNotThrow((Executable) fut::get);
		est.stop();
		exec.shutdownNow();
	}
}
