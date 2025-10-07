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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("ConstantConditions")
class SketchBasedJoinEstimatorConfigTest {

	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	private StubSailStore store;
	private Resource s1;
	private IRI p1;
	private Value o1;

	@BeforeEach
	void setUp() {
		store = new StubSailStore();
		s1 = VF.createIRI("urn:s1");
		p1 = VF.createIRI("urn:p1");
		o1 = VF.createIRI("urn:o1");
	}

	private Statement st(Resource s, IRI p, Value o) {
		return VF.createStatement(s, p, o);
	}

	private void rebuild(SketchBasedJoinEstimator est) {
		est.rebuildOnceSlow();
	}

	@Test
	void customDefaultContextValue() {
		// Given a custom default context label configured via constructor
		SketchBasedJoinEstimator.Config cfg = SketchBasedJoinEstimator.Config.defaults()
				.withNominalEntries(128)
				.withThrottleEveryN(1)
				.withThrottleMillis(0)
				.withDefaultContext("urn:mine");

		SketchBasedJoinEstimator est = new SketchBasedJoinEstimator(store, cfg);

		// One triple with null context
		store.add(st(s1, p1, o1));
		rebuild(est);

		// The custom label must be used to represent the default context in sketches
		double cardMine = est.cardinalitySingle(SketchBasedJoinEstimator.Component.C, "urn:mine");
		double cardDefault = est.cardinalitySingle(SketchBasedJoinEstimator.Component.C, "urn:default-context");

		assertEquals(1.0, cardMine, 0.0001);
		assertEquals(0.0, cardDefault, 0.0001);
	}

	@Test
	void stalenessAgeSlaInfluencesScore() throws Exception {
		SketchBasedJoinEstimator.Config cfg = SketchBasedJoinEstimator.Config.defaults()
				.withNominalEntries(64)
				.withThrottleEveryN(1)
				.withThrottleMillis(0)
				.withStalenessAgeSlaMillis(1); // extremely small SLA to quickly ramp age score

		SketchBasedJoinEstimator est = new SketchBasedJoinEstimator(store, cfg);

		// Load one statement and publish snapshot
		store.addAll(List.of(st(s1, p1, o1)));
		rebuild(est);

		// Wait a tiny bit so ageMillis > SLA
		Thread.sleep(5);

		// With SLA=1ms and default weights, age contribution alone should push score above 0.1
		assertTrue(est.isStale(0.1));
	}
}
