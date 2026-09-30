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
package org.eclipse.rdf4j.query.explanation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class TelemetryMetricNamesTest {

	// REINFORCE: lifecycle coherence markers carry the optimizer prefix but are excluded from ordinary explain output
	@Test
	void optimizerExplainMetricsExcludeLifecycleMarkers() {
		List<String> lifecycleMarkers = List.of(
				TelemetryMetricNames.CANCELLED_COUNT_ACTUAL,
				TelemetryMetricNames.ABORTED_COUNT_ACTUAL,
				TelemetryMetricNames.EXHAUSTED_CLOSE_COUNT_ACTUAL);

		for (String marker : lifecycleMarkers) {
			assertTrue(TelemetryMetricNames.isOptimizerMetric(marker), marker);
			assertFalse(TelemetryMetricNames.isOptimizerExplainMetric(marker), marker);
		}

		assertTrue(TelemetryMetricNames.isOptimizerExplainMetric("optimizer.candidateCount"));
		assertTrue(TelemetryMetricNames.isOptimizerExplainMetric(TelemetryMetricNames.OPTIMIZER_PREFIX + "score"));
		assertFalse(TelemetryMetricNames.isOptimizerExplainMetric("candidateCount"));
		assertFalse(TelemetryMetricNames.isOptimizerExplainMetric("queryCancelled"));
		assertFalse(TelemetryMetricNames.isOptimizerExplainMetric(""));
		assertFalse(TelemetryMetricNames.isOptimizerExplainMetric(null));
	}

	// REINFORCE: every optimizer explain metric is an optimizer metric (the explain set is a strict subset)
	@Test
	void optimizerExplainMetricImpliesOptimizerMetric() {
		List<String> candidates = List.of(
				"optimizer.candidateCount",
				TelemetryMetricNames.OPTIMIZER_PREFIX,
				TelemetryMetricNames.CANCELLED_COUNT_ACTUAL,
				TelemetryMetricNames.PLANNED_ESTIMATE_USAGE,
				"optimizer",
				"x");

		for (String candidate : candidates) {
			if (TelemetryMetricNames.isOptimizerExplainMetric(candidate)) {
				assertTrue(TelemetryMetricNames.isOptimizerMetric(candidate), candidate);
			}
		}
		assertFalse(TelemetryMetricNames.isOptimizerMetric("optimizer"));
		assertTrue(TelemetryMetricNames.isOptimizerMetric(TelemetryMetricNames.OPTIMIZER_PREFIX));
		assertTrue(TelemetryMetricNames.isOptimizerExplainMetric(TelemetryMetricNames.OPTIMIZER_PREFIX));
	}
}
