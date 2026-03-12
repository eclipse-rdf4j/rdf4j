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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SketchBasedJoinEstimatorChurnSamplingTest {

	private static final SimpleValueFactory VF = SimpleValueFactory.getInstance();

	private StubSailStore store;
	private SketchBasedJoinEstimator est;
	private Resource s1;
	private Resource s2;
	private IRI p1;
	private Value o1;
	private Value o2;

	@BeforeEach
	void setUp() {
		store = new StubSailStore();
		s1 = VF.createIRI("urn:s1");
		s2 = VF.createIRI("urn:s2");
		p1 = VF.createIRI("urn:p1");
		o1 = VF.createIRI("urn:o1");
		o2 = VF.createIRI("urn:o2");
	}

	private SketchBasedJoinEstimator est(int min, double pct, int max) {
		SketchBasedJoinEstimator e = new SketchBasedJoinEstimator(store, SketchBasedJoinEstimator.Config.defaults()
				.withNominalEntries(64)
				.withThrottleEveryN(1)
				.withThrottleMillis(0)
				.withChurnSampleMin(min)
				.withChurnSamplePercent(pct)
				.withChurnSampleMax(max));
		est = e;
		return e;
	}

	@Test
	void samplePercentZeroStopsAfterFirst() {
		SketchBasedJoinEstimator est = est(/* min */0, /* pct */0.0, /* max */10);

		est.addStatement(VF.createStatement(s1, p1, o1));
		est.addStatement(VF.createStatement(s2, p1, o2));

		SketchBasedJoinEstimator.Staleness st = est.staleness();
		assertEquals(0, st.sampledAdds, "pct=0 should prevent sampling when min=0");
	}

	@Test
	void minOverridesZeroPercentUntilSatisfied() {
		SketchBasedJoinEstimator est = est(/* min */2, /* pct */0.0, /* max */0);

		est.addStatement(VF.createStatement(s1, p1, o1));
		est.addStatement(VF.createStatement(s2, p1, o2));
		est.addStatement(VF.createStatement(VF.createIRI("urn:s3"), p1, o2));
		est.addStatement(VF.createStatement(VF.createIRI("urn:s4"), p1, o2));

		SketchBasedJoinEstimator.Staleness st = est.staleness();
		assertEquals(2, st.sampledAdds, "min=2 with pct=0 should sample every 2 statements when no max cap is set");
	}

	@Test
	void percentDrivesIntervalWhenBetweenMinAndMax() throws Exception {
		// approx store size 50_000 → pct=1% -> interval=500; max=1000, min=0 => expected 1000
		SketchBasedJoinEstimator est = est(/* min */0, /* pct */0.01, /* max */1000);
		setApproxStoreSize(est, 50_000);

		for (int i = 0; i < 2000; i++) {
			est.addStatement(VF.createStatement(VF.createIRI("urn:s" + i), p1, o1));
		}

		SketchBasedJoinEstimator.Staleness st = est.staleness();
		assertEquals(2, st.sampledAdds, "With interval 1000, two samples expected after 2000 events");
	}

	@Test
	void maxIntervalCapsPercentage() throws Exception {
		// approx store size 50_000 → pct=1% -> interval=500; max=2000 dominates; expect interval=2000 -> ~1 sample per
		// 2000 events
		SketchBasedJoinEstimator est = est(/* min */0, /* pct */0.01, /* max */2000);
		setApproxStoreSize(est, 50_000);

		for (int i = 0; i < 4000; i++) {
			est.addStatement(VF.createStatement(VF.createIRI("urn:sx" + i), p1, o2));
		}

		SketchBasedJoinEstimator.Staleness st = est.staleness();
		assertEquals(2, st.sampledAdds, "Max interval 2000 should yield two samples across 4000 events");
	}

	@Test
	void minIntervalOverridesPercentage() throws Exception {
		// pct interval 500, min 1200 => expect interval 1200 -> one sample across 1200..2399 range, two across 2400
		SketchBasedJoinEstimator est = est(/* min */1200, /* pct */0.01, /* max */0);
		setApproxStoreSize(est, 50_000);

		for (int i = 0; i < 2400; i++) {
			est.addStatement(VF.createStatement(VF.createIRI("urn:sm" + i), p1, o2));
		}

		SketchBasedJoinEstimator.Staleness st = est.staleness();
		assertEquals(2, st.sampledAdds, "Min interval 1200 should yield two samples across 2400 events");
	}

	private static void setApproxStoreSize(SketchBasedJoinEstimator est, long value) throws Exception {
		var fSeen = SketchBasedJoinEstimator.class.getDeclaredField("seenTriples");
		fSeen.setAccessible(true);
		fSeen.setLong(est, value);

		var fApprox = SketchBasedJoinEstimator.class.getDeclaredField("approxStoreSize");
		fApprox.setAccessible(true);
		((java.util.concurrent.atomic.AtomicLong) fApprox.get(est)).set(value);
	}
}
