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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Extra coverage for public API facets that were not exercised in {@link SketchBasedJoinEstimatorTest}.
 */
@SuppressWarnings("ConstantConditions")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SketchBasedJoinEstimatorExtraTest {

	/* ------------------------------------------------------------- */
	/* Test infrastructure */
	/* ------------------------------------------------------------- */

	private static final ValueFactory VF = SimpleValueFactory.getInstance();
	private StubSailStore sailStore;
	private SketchBasedJoinEstimator est;

	private static final int K = 128;
	private static final long THROTTLE_EVERY = 1;
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

	private static void assertApprox(double expected, double actual) {
		double eps = Math.max(1.0, expected * 0.05); // 5 % or ±1
		assertEquals(expected, actual, eps);
	}

	/* ------------------------------------------------------------- */
	/* 1. Basic public helpers */
	/* ------------------------------------------------------------- */

	@Test
	void readyFlagAfterInitialRebuild() {
		assertFalse(est.isReady(), "Estimator should not be ready before data‑load");

		sailStore.add(stmt(s1, p1, o1));
		fullRebuild();

		assertTrue(est.isReady(), "Estimator did not report readiness after rebuild");
	}

	@Test
	void suggestNominalEntriesReturnsPowerOfTwo() {
		int k = SketchBasedJoinEstimator.suggestNominalEntries();

		assertTrue(k >= 4, "k must be at least 4");
		assertEquals(0, k & (k - 1), "k must be a power‑of‑two");
	}

	/* ------------------------------------------------------------- */
	/* 2. Legacy join helpers */
	/* ------------------------------------------------------------- */

	@Test
	void estimateJoinOnSingles() {
		// Only one triple ⟨s1 p1 o1⟩ so |join| = 1
		sailStore.add(stmt(s1, p1, o1));
		fullRebuild();

		double joinSize = est.estimateJoinOn(
				SketchBasedJoinEstimator.Component.S, // join on ?s
				SketchBasedJoinEstimator.Component.P, p1.stringValue(),
				SketchBasedJoinEstimator.Component.O, o1.stringValue());

		assertApprox(1.0, joinSize);
	}

	@Test
	void estimateJoinOnPairs() {
		/*
		 * Data ───────────────────────────────────────────── s1 p1 o1 c1 s1 p1 o2 c1
		 */
		sailStore.addAll(List.of(
				stmt(s1, p1, o1, c1),
				stmt(s1, p1, o2, c1)
		));
		fullRebuild();

		double joinSize = est.estimateJoinOn(
				SketchBasedJoinEstimator.Component.C, // join on ?c
				SketchBasedJoinEstimator.Pair.SP,
				s1.stringValue(), p1.stringValue(),
				SketchBasedJoinEstimator.Pair.PO,
				p1.stringValue(), o1.stringValue());

		assertApprox(1.0, joinSize);
	}

	/* ------------------------------------------------------------- */
	/* 3. Optimiser‑facing Join helper */
	/* ------------------------------------------------------------- */

	@Test
	void cardinalityJoinNodeHappyPath() {
		/*
		 * Data: s1 p1 o1 s1 p2 o1
		 */
		sailStore.addAll(List.of(
				stmt(s1, p1, o1),
				stmt(s1, p2, o1)
		));
		fullRebuild();

		StatementPattern left = new StatementPattern(
				new Var("s"),
				new Var("p1", p1),
				new Var("o1", o1));

		StatementPattern right = new StatementPattern(
				new Var("s"),
				new Var("p2", p2),
				new Var("o1", o1));

		double card = est.cardinality(new Join(left, right));

		assertApprox(1.0, card);
	}

	@Test
	void cardinalityJoinNodeNoCommonVariable() {
		/* left & right bind DIFFERENT subject variables */
		sailStore.add(stmt(s1, p1, o1));
		fullRebuild();

		StatementPattern left = new StatementPattern(new Var("s1"), new Var("p1", p1), new Var("o1", o1));
		StatementPattern right = new StatementPattern(new Var("s2"), new Var("p1", p1), new Var("o1", o1));

		double card = est.cardinality(new Join(left, right));

		assertEquals(Double.MAX_VALUE, card, "Estimator should return sentinel when no common var exists");
	}
}
