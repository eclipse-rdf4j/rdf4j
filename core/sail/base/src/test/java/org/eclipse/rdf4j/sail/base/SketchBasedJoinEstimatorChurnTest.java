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

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SketchBasedJoinEstimatorChurnTest {

	private static final SimpleValueFactory VF = SimpleValueFactory.getInstance();

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

	private Statement st() {
		return VF.createStatement(s1, p1, o1);
	}

	@Test
	void removalRatioTriggersRebuild() {
		SketchBasedJoinEstimator est = new SketchBasedJoinEstimator(store,
				SketchBasedJoinEstimator.Config.defaults()
						.withNominalEntries(64)
						.withThrottleEveryN(1)
						.withThrottleMillis(0)
						.withChurnSampleMin(1)
						.withChurnSamplePercent(1.0)
						.withChurnSampleMax(0)
						.withChurnReaddThreshold(1.0) // disable re-add trigger for this case
						.withChurnRemovalRatioThreshold(0.5));

		est.rebuildOnceSlow();

		Statement st = st();
		est.addStatement(st);
		est.deleteStatement(st);

		assertTrue(est.isStale(10.0),
				"Removal ratio above threshold should mark estimator stale even with high staleness threshold");
	}

	@Test
	void readdPercentageTriggersRebuild() {
		SketchBasedJoinEstimator est = new SketchBasedJoinEstimator(store,
				SketchBasedJoinEstimator.Config.defaults()
						.withNominalEntries(64)
						.withThrottleEveryN(1)
						.withThrottleMillis(0)
						.withChurnSampleMin(1)
						.withChurnSamplePercent(1.0)
						.withChurnSampleMax(0)
						.withChurnReaddThreshold(0.5)
						.withChurnRemovalRatioThreshold(1.1)); // avoid removal ratio trigger

		est.rebuildOnceSlow();

		Statement st = st();
		est.addStatement(st);
		est.deleteStatement(st);
		est.addStatement(st);

		assertTrue(est.isStale(10.0),
				"Re-add churn above threshold should mark estimator stale even with high staleness threshold");
	}
}
