/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.QueryJoinOptimizer;

/**
 * Tests to monitor QueryJoinOptimizer behaviour when cardinalities are below 1.
 *
 */
public class QueryJoinOptimizerEmptyStatisticsTest extends QueryJoinOptimizerTest {

	@Override
	public QueryJoinOptimizer getOptimizer() {
		return new QueryJoinOptimizer(new EvaluationStatistics() {
			@Override
			protected CardinalityCalculator createCardinalityCalculator() {
				return cardinalityCalculator;
			}

			final CardinalityCalculator cardinalityCalculator = new CardinalityCalculator() {
				@Override
				protected double getCardinality(StatementPattern sp) {
					double value = 0.1;
					if (sp.getSubjectVar() == null || !sp.getSubjectVar().hasValue()) {
						value += 0.1;
					}
					if (sp.getPredicateVar() == null || !sp.getPredicateVar().hasValue()) {
						value += 0.1;
					}
					if (sp.getObjectVar() == null || !sp.getObjectVar().hasValue()) {
						value += 0.1;
					}
					if (sp.getContextVar() == null || !sp.getContextVar().hasValue()) {
						value += 0.1;
					}
					return value;
				}
			};
		});
	}

}
