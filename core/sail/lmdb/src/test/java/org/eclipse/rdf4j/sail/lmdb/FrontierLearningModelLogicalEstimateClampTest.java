/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * A logical posterior's safety envelope [rawLog - cap, rawLog + cap] is intersected with the global range [0,
 * MAX_ABS_LOG_ERROR]. When the raw planner estimate is inflated far beyond the global cap (multiplied-out cardinalities
 * through path expansions can reach 1e21+ rows) the two intervals are disjoint; estimateLogical must pin to the cap
 * instead of handing Math.clamp an inverted window (which throws IllegalArgumentException mid-planning).
 */
class FrontierLearningModelLogicalEstimateClampTest {

	private static final double MAX_ABS_LOG_ERROR = Math.log1p(1.0e12d);

	@Test
	void inflatedRawEstimateAboveGlobalCapPinsInsteadOfThrowing() {
		// Modest evidence: cap = ln(4) * weight, mean well inside the global range — as produced by
		// exactPosterior after one completed execution.
		FrontierLearningModel.PosteriorSnapshot snapshot = new FrontierLearningModel.PosteriorSnapshot(
				5.0d, 0.1d, 1.0d, Math.log(4.0d), 1L, 0L, 0L);

		// rawLog = log1p(1e21) ≈ 48.4 → lowerLog ≈ 47.0 > MAX_ABS_LOG_ERROR ≈ 27.6: disjoint envelope.
		double inflatedPredicted = 1.0e21d;
		FrontierLearningModel.DimensionEstimate estimate = FrontierLearningModel.estimateLogical(snapshot,
				FrontierCostDimension.OUTPUT_ROWS, inflatedPredicted);

		assertNotNull(estimate, "A disjoint envelope must degrade to the global cap, not fail the estimate");
		assertEquals(Math.expm1(MAX_ABS_LOG_ERROR), estimate.correctedValue(), 1.0e-3d,
				"When the evidence envelope lies entirely above the global log cap the estimate pins to the cap");
	}

	@Test
	void ordinaryEstimateStillHonoursTheEvidenceEnvelope() {
		double cap = Math.log(4.0d);
		FrontierLearningModel.PosteriorSnapshot snapshot = new FrontierLearningModel.PosteriorSnapshot(
				5.0d, 0.1d, 1.0d, cap, 1L, 0L, 0L);

		double predicted = 100.0d;
		FrontierLearningModel.DimensionEstimate estimate = FrontierLearningModel.estimateLogical(snapshot,
				FrontierCostDimension.OUTPUT_ROWS, predicted);

		assertNotNull(estimate);
		double rawLog = Math.log1p(predicted);
		double expected = Math.expm1(Math.clamp(snapshot.mean(), rawLog - cap, rawLog + cap));
		assertEquals(expected, estimate.correctedValue(), 1.0e-9d,
				"Estimates inside the global range keep the ±cap envelope around the raw estimate");
		assertTrue(estimate.correctedValue() > predicted,
				"sanity: the posterior mean above rawLog pulls the estimate up within the envelope");
	}
}
