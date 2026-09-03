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
package org.eclipse.rdf4j.query.algebra.evaluation.optimizer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EmptyTripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategy;
import org.junit.jupiter.api.Test;

class StandardQueryOptimizerPipelineTest {

	// REINFORCE: join ordering (and the iterative-evaluation rewrite) run before any filter localization pass and
	// before the IN->VALUES rewrite, and ORDER/LIMIT hoisting is the last optimizer
	@Test
	void joinOrderingPrecedesFilterLocalization() {
		StandardQueryOptimizerPipeline pipeline = new StandardQueryOptimizerPipeline(
				new StrictEvaluationStrategy(new EmptyTripleSource(), null), new EmptyTripleSource(),
				new EvaluationStatistics());
		List<QueryOptimizer> optimizers = new ArrayList<>();
		pipeline.getOptimizers().forEach(optimizers::add);

		int joinIndex = firstIndexOfType(optimizers, QueryJoinOptimizer.class);
		int iterativeIndex = optimizers.indexOf(StandardQueryOptimizerPipeline.ITERATIVE_EVALUATION_OPTIMIZER);
		int firstFilterIndex = firstIndexOfType(optimizers, FilterOptimizer.class);
		int lastFilterIndex = lastIndexOfType(optimizers, FilterOptimizer.class);
		int filterInValuesIndex = optimizers.indexOf(StandardQueryOptimizerPipeline.FILTER_IN_VALUES_OPTIMIZER);
		int orderLimitIndex = optimizers.indexOf(StandardQueryOptimizerPipeline.ORDER_LIMIT_OPTIMIZER);

		assertThat(joinIndex).isNotNegative();
		assertThat(iterativeIndex).isGreaterThan(joinIndex);
		assertThat(firstFilterIndex).isGreaterThan(iterativeIndex);
		assertThat(filterInValuesIndex).isGreaterThan(firstFilterIndex);
		assertThat(lastFilterIndex).isGreaterThan(filterInValuesIndex);
		assertThat(optimizers.stream().filter(FilterOptimizer.class::isInstance).count()).isEqualTo(2L);
		assertThat(orderLimitIndex).isGreaterThan(lastFilterIndex);
		for (int index = orderLimitIndex + 1; index < optimizers.size(); index++) {
			assertThat(optimizers.get(index)).isInstanceOf(ParentReferenceChecker.class);
		}
	}

	private static int firstIndexOfType(List<QueryOptimizer> optimizers, Class<?> type) {
		for (int index = 0; index < optimizers.size(); index++) {
			if (type.isInstance(optimizers.get(index))) {
				return index;
			}
		}
		return -1;
	}

	private static int lastIndexOfType(List<QueryOptimizer> optimizers, Class<?> type) {
		for (int index = optimizers.size() - 1; index >= 0; index--) {
			if (type.isInstance(optimizers.get(index))) {
				return index;
			}
		}
		return -1;
	}
}
