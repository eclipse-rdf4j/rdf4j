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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.JoinFactorCostModel;
import org.eclipse.rdf4j.sail.lmdb.estimation.QuadSnapshotIdentity;
import org.junit.jupiter.api.Test;

/**
 * Mid-planning calibration updates (a live filter sample, a concurrent query's feedback) must not let plan alternatives
 * priced under old calibration mix with post-update computations.
 */
class LmdbPlanningCalibrationCoherenceTest {

	@Test
	void factorCostCacheKeysRotateWhenLearnedCalibrationChanges() {
		StatementPattern pattern = new StatementPattern(new Var("s"), new Var("p"), new Var("o"));
		QuadSnapshotIdentity identity = new QuadSnapshotIdentity(1L, 2L, 3L);
		JoinFactorCostModel.CostContext costContext = JoinFactorCostModel.CostContext.of(
				java.util.Set.of(), 1.0d, 1.0d, false);

		EstimateContext beforeUpdate = EstimateContext.root(pattern, identity, 4L, 100L);
		EstimateContext afterUpdate = EstimateContext.root(pattern, identity, 4L, 101L);

		assertNotEquals(ScopedFactorCostCacheKey.of(pattern, costContext, beforeUpdate),
				ScopedFactorCostCacheKey.of(pattern, costContext, afterUpdate),
				"A cached factor cost bakes in learned calibration — entries from before a mid-planning"
						+ " calibration bump must not be served afterwards");
		assertEquals(ScopedFactorCostCacheKey.of(pattern, costContext, beforeUpdate),
				ScopedFactorCostCacheKey.of(pattern, costContext, beforeUpdate),
				"Identical calibration still shares the cache entry");
	}

	@Test
	void pinnedPosteriorReproducesTheLiveEstimate() {
		FrontierLearningModel model = new FrontierLearningModel();
		FrontierLearningKey key = FrontierLearningKey.of(
				"join", "JOIN@pin", 91L, 7L, "spoc-prefix", "spoc", "prefix");
		for (long epoch = 1; epoch <= 4; epoch++) {
			model.observe(key, FrontierCostDimension.OUTPUT_ROWS, 100.0d, 400.0d, epoch);
		}

		FrontierLearningModel.PosteriorSnapshot pinned = model.posterior(key, FrontierCostDimension.OUTPUT_ROWS);
		assertNotNull(pinned);
		FrontierLearningModel.DimensionEstimate live = model.estimate(key, FrontierCostDimension.OUTPUT_ROWS, 100.0d);
		FrontierLearningModel.DimensionEstimate replayed = FrontierLearningModel.estimate(pinned, 100.0d);
		assertNotNull(live);
		assertNotNull(replayed);
		assertEquals(live.correctedValue(), replayed.correctedValue(), 1e-12,
				"A pinned posterior must reproduce the live estimate exactly for the same predicted value");

		// New observations move the live model but not the pinned snapshot — the coherence guarantee.
		for (long epoch = 5; epoch <= 12; epoch++) {
			model.observe(key, FrontierCostDimension.OUTPUT_ROWS, 100.0d, 6_400.0d, epoch);
		}
		FrontierLearningModel.DimensionEstimate moved = model.estimate(key, FrontierCostDimension.OUTPUT_ROWS, 100.0d);
		FrontierLearningModel.DimensionEstimate stillPinned = FrontierLearningModel.estimate(pinned, 100.0d);
		assertNotNull(moved);
		assertNotNull(stillPinned);
		assertNotEquals(moved.correctedValue(), stillPinned.correctedValue(),
				"sanity: the live model moved");
		assertEquals(replayed.correctedValue(), stillPinned.correctedValue(), 1e-12,
				"Alternatives priced against a pinned posterior stay on identical calibration even when"
						+ " observations land concurrently");
	}
}
