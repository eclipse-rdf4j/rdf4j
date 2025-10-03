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
class SketchBasedJoinEstimatorGapTest {

	/* ------------------------------------------------------------- */
	/* Infrastructure */
	/* ------------------------------------------------------------- */

	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	private StubSailStore store;
	private SketchBasedJoinEstimator est;

	private static final int K = 128;
	private static final long THR_EVERY = 10;
	private static final long THR_MS_DISABLED = 0;

	private final Resource s1 = VF.createIRI("urn:s1");
	private final IRI p1 = VF.createIRI("urn:p1");
	private final IRI p2 = VF.createIRI("urn:p2");
	private final Value o1 = VF.createIRI("urn:o1");
	private final Value o2 = VF.createIRI("urn:o2");
	private final Resource c1 = VF.createIRI("urn:c1");

	@BeforeEach
	void init() {
		store = new StubSailStore();
		est = new SketchBasedJoinEstimator(store, K, THR_EVERY, THR_MS_DISABLED);
	}

	private Statement triple(Resource s, IRI p, Value o, Resource c) {
		return VF.createStatement(s, p, o, c);
	}

	private Statement triple(Resource s, IRI p, Value o) {
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
	/* B1 – pair‑complement fast‑path */
	/* ------------------------------------------------------------- */

	@Test
	void pairComplementFastPath() {
		store.addAll(List.of(
				triple(s1, p1, o1),
				triple(s1, p1, o2)
		));
		rebuild();

		double distinctO = est.estimateCount(
				SketchBasedJoinEstimator.Component.O,
				s1.stringValue(), p1.stringValue(), null, null);

		approx(2.0, distinctO); // {o1,o2}
	}

	/* ------------------------------------------------------------- */
	/* B2 – generic fallback with 3 constants */
	/* ------------------------------------------------------------- */

	@Test
	void genericFallbackThreeConstants() {
		store.add(triple(s1, p1, o1, c1));
		rebuild();

		double cardC = est.estimateCount(
				SketchBasedJoinEstimator.Component.C,
				s1.stringValue(), p1.stringValue(), o1.stringValue(), null);

		approx(1.0, cardC);
	}

	/* ------------------------------------------------------------- */
	/* B3 – background thread publishes data */
	/* ------------------------------------------------------------- */

	@Test
	void backgroundRefreshPublishes() throws Exception {
		rebuild(); // empty snapshot baseline
		assertApproxZero();

		est.startBackgroundRefresh(3); // ms
		store.add(triple(s1, p1, o1)); // triggers rebuild request
		est.addStatement(triple(s1, p1, o1));

		Thread.sleep(120); // > a few refresh periods
		double card = est.cardinalitySingle(
				SketchBasedJoinEstimator.Component.P, p1.stringValue());

		est.stop();
		approx(1.0, card);
	}

	private void assertApproxZero() {
		double v = est.cardinalitySingle(SketchBasedJoinEstimator.Component.P, p1.stringValue());
		assertEquals(0.0, v, 0.0001);
	}

	/* ------------------------------------------------------------- */
	/* B4 – join early‑out on empty intersection */
	/* ------------------------------------------------------------- */

	@Test
	void joinEarlyOutZero() {
		store.add(triple(s1, p1, o1));
		rebuild();

		double sz = est.estimate(
				SketchBasedJoinEstimator.Component.S,
				null, p1.stringValue(), o1.stringValue(), null)
				.join(SketchBasedJoinEstimator.Component.S,
						null, p2.stringValue(), o2.stringValue(), null) // absent
				.estimate();

		assertEquals(0.0, sz, 0.0001);
	}

	private long timed(Runnable r) {
		long t0 = System.nanoTime();
		r.run();
		return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
	}
}
