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
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParser;
import org.junit.jupiter.api.Test;

class ReflexiveStarPathExtensionTest {

	@Test
	void standardPipelineHonorsCustomPathPreparation() {
		class Strategy extends DefaultEvaluationStrategy {
			int preparedPaths;

			Strategy() {
				super(new EmptyTripleSource(), null);
			}

			@Override
			protected QueryEvaluationStep prepare(ArbitraryLengthPath path, QueryEvaluationContext context) {
				preparedPaths++;
				return super.prepare(path, context);
			}
		}
		var strategy = new Strategy();
		var parsed = new SPARQLParser().parseQuery("SELECT ?s WHERE { ?s <urn:p>* ?s }", null);
		var optimized = strategy.optimize(parsed.getTupleExpr(), new EvaluationStatistics(),
				EmptyBindingSet.getInstance());
		strategy.precompile(optimized);
		assertThat(strategy.preparedPaths).isEqualTo(1);
	}
}
