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

package org.eclipse.rdf4j.sail.memory;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TimedIterationExhaustionTest {
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"SELECT ?s WHERE { ?s <urn:base> ?v FILTER(?v < 0) } | 0",
			"SELECT ?s WHERE { ?s <urn:base> ?v FILTER NOT EXISTS { ?s <urn:inner> ?w FILTER(?w < 0) } } | 3",
			"SELECT ?s WHERE { ?s <urn:base> ?v MINUS { ?s <urn:inner> ?w FILTER(?w < 0) } } | 3"
	})
	void timedExecutionIncludesPullThatExhaustsEmptyFilter(String sparql, long expectedRows) {
		var repository = new SailRepository(new MemoryStore());
		try (var connection = repository.getConnection()) {
			var factory = connection.getValueFactory();
			connection.begin();
			for (int i = 1; i <= 3; i++) {
				var subject = factory.createIRI("urn:subject:" + i);
				var value = factory.createLiteral(i);
				connection.add(subject, factory.createIRI("urn:base"), value);
				connection.add(subject, factory.createIRI("urn:inner"), value);
			}
			connection.commit();

			var explanation = connection.prepareTupleQuery(sparql).explain(Explanation.Level.Timed);
			assertThat(explanation.toGenericPlanNode().getResultSizeActual()).isEqualTo(expectedRows);
			var exhaustedFilters = new ArrayList<Filter>();
			((TupleExpr) explanation.tupleExpr()).visit(new AbstractQueryModelVisitor<RuntimeException>() {
				@Override
				public void meet(Filter node) {
					if (node.getHasNextCallCountActual() > 0 && node.getHasNextTrueCountActual() == 0) {
						exhaustedFilters.add(node);
					}
					super.meet(node);
				}
			});
			assertThat(exhaustedFilters).hasSize(1);
			var exhausted = exhaustedFilters.getFirst();
			assertThat(exhausted.getNextCallCountActual()).isZero();
			assertThat(exhausted.getHasNextTimeNanosActual()).isPositive();
			assertThat(exhausted.getTotalTimeNanosActual()).isEqualTo(exhausted.getHasNextTimeNanosActual());
		} finally {
			repository.shutDown();
		}
	}
}
