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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
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

@SuppressWarnings("ConstantConditions")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SketchBasedJoinEstimatorAdvancedTest {

	/* ------------------------------------------------------------- */
	/* Test infrastructure */
	/* ------------------------------------------------------------- */

	private static final ValueFactory VF = SimpleValueFactory.getInstance();
	private StubSailStore sailStore;
	private SketchBasedJoinEstimator est;

	private static final int K = 128;
	private static final long THROTTLE_EVERY = 10;
	private static final long THROTTLE_MS = 20;

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

	private void rebuild() {
		est.rebuildOnceSlow();
	}

	private static void approx(double exp, double act) {
		double eps = Math.max(1.0, exp * 0.05);
		assertEquals(exp, act, eps);
	}

	/* ------------------------------------------------------------- */
	/* A1 – toggleDoubleBuffering */
	/* ------------------------------------------------------------- */

	@Test
	void toggleDoubleBuffering() {
		sailStore.add(stmt(s1, p1, o1));
		rebuild();
		approx(1.0, est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue()));

		// second generation of data
		sailStore.add(stmt(s1, p2, o1));
		rebuild();

		approx(1.0, est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue()));
		approx(1.0, est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p2.stringValue()));
	}

	/* ------------------------------------------------------------- */
	/* A2 – throttleHonoured */
	/* ------------------------------------------------------------- */

	@Test
	void throttleHonoured() {
		for (int i = 0; i < 200; i++) {
			sailStore.add(stmt(VF.createIRI("urn:s" + i), p1, o1));
		}
		long t0 = System.nanoTime();
		rebuild();
		long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);

		long expectedMin = (200 / THROTTLE_EVERY) * THROTTLE_MS;
		assertTrue(elapsedMs >= expectedMin * 0.8, "Rebuild finished too quickly – throttle ignored?");
	}

	/* ------------------------------------------------------------- */
	/* A3 – backgroundRefreshIdempotent */
	/* ------------------------------------------------------------- */

	@Test
	void backgroundRefreshIdempotent() throws Exception {
		est.startBackgroundRefresh(3);
		est.startBackgroundRefresh(3); // no second thread
		Thread.sleep(20);
		est.stop();
		est.stop(); // idempotent

		/* Give thread system a moment to settle and assert */
		Thread.sleep(10);
		Thread.getAllStackTraces()
				.keySet()
				.stream()
				.filter(t -> t.getName().startsWith("RdfJoinEstimator-Refresh"))
				.forEach(t -> assertFalse(t.isAlive(), "Refresh thread still alive"));
	}

	/* ------------------------------------------------------------- */
	/* A4 – joinChainThreeWay */
	/* ------------------------------------------------------------- */

	@Test
	void joinChainThreeWay() {
		sailStore.addAll(List.of(stmt(s1, p1, o1), stmt(s1, p2, o1), stmt(s1, p2, o2)));
		rebuild();

		double size = est.estimate(SketchBasedJoinEstimator.Component.S, null, p1.stringValue(), o1.stringValue(), null)
				.join(SketchBasedJoinEstimator.Component.S, null, p2.stringValue(), o1.stringValue(), null)
				.join(SketchBasedJoinEstimator.Component.S, null, p2.stringValue(), o2.stringValue(), null)
				.estimate();

		approx(1.0, size); // only {?s = s1}
	}

	/* ------------------------------------------------------------- */
	/* A5 – estimateJoinOnMixedPairFallback */
	/* ------------------------------------------------------------- */

	@Test
	void estimateJoinOnMixedPairFallback() {
		sailStore.add(stmt(s1, p1, o1));
		rebuild();

		// (S,O) is not one of the six predefined pairs
		double card = est.estimateCount(SketchBasedJoinEstimator.Component.P, s1.stringValue(), null, o1.stringValue(),
				null);

		approx(1.0, card);
	}

	/* ------------------------------------------------------------- */
	/* A6 – tombstoneAcrossRebuilds */
	/* ------------------------------------------------------------- */

	@Test
	void tombstoneAcrossRebuilds() {
		/* 1st generation – add */
		est.addStatement(stmt(s1, p1, o1));
		rebuild();
		approx(1.0, est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue()));

		/* 2nd – delete */
		est.deleteStatement(stmt(s1, p1, o1));
		rebuild();
		approx(0.0, est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue()));

		/* 3rd – re‑add */
		est.addStatement(stmt(s1, p1, o1));
		rebuild();
		approx(1.0, est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue()));
	}

	/* ------------------------------------------------------------- */
	/* A7 – cardinalitySingleUnknownValue */
	/* ------------------------------------------------------------- */

	@Test
	void cardinalitySingleUnknownValue() {
		rebuild();
		double v = est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, "urn:does-not-exist");
		assertEquals(0.0, v);
	}
}
