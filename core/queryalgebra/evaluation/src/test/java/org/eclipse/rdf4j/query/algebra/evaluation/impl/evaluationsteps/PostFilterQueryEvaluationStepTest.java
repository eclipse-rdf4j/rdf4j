/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedList;
import java.util.Queue;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PostFilterQueryEvaluationStepTest {

	private static final ValueFactory VALUE_FACTORY = SimpleValueFactory.getInstance();

	private QueryBindingSet bindingSet;

	@BeforeEach
	void setUp() {
		bindingSet = new QueryBindingSet(1);
		bindingSet.addBinding("a", VALUE_FACTORY.createLiteral(42));
	}

	@Test
	@DisplayName("when wrapped returns empty iteration, then LeftJoinPostFilterQueryEvaluationStep returns empty iteration")
	void emptyIteration() {
		QueryEvaluationStep wrapped = (bindings) -> QueryEvaluationStep.EMPTY_ITERATION;
		QueryEvaluationStep postFilter = new PostFilterQueryEvaluationStep(
				wrapped,
				bindings -> BooleanLiteral.valueOf(true));

		var result = postFilter.evaluate(bindingSet);

		assertThat(result).isEqualTo(QueryEvaluationStep.EMPTY_ITERATION);
	}

	@Test
	@DisplayName("when wrapped returns non-empty iteration, then LeftJoinPostFilterQueryEvaluationStep returns filtered iteration")
	void filteredIteration() {
		QueryEvaluationStep wrapped = new TestQueryEvaluationStep();
		QueryValueEvaluationStep condition = bindings -> {
			var shouldAccept = bindings.getValue("b")
					.stringValue()
					.equals("abc");
			return BooleanLiteral.valueOf(shouldAccept);
		};
		QueryEvaluationStep postFilter = new PostFilterQueryEvaluationStep(wrapped, condition);

		var result = postFilter.evaluate(bindingSet);

		assertThat(result)
				.toIterable()
				.map(bindings -> bindings.getValue("b"))
				.containsExactly(VALUE_FACTORY.createLiteral("abc"));
	}

	private static class TestQueryEvaluationStep implements QueryEvaluationStep {

		private final Queue<String> values = new LinkedList<>();

		private TestQueryEvaluationStep() {
			values.add("abc");
			values.add("xyz");
		}

		@Override
		public CloseableIteration<BindingSet> evaluate(BindingSet bindings) {
			return new CloseableIteration<>() {
				@Override
				public void close() {
					// Nothing to close
				}

				@Override
				public boolean hasNext() {
					return !values.isEmpty();
				}

				@Override
				public BindingSet next() {
					var output = new QueryBindingSet(2);
					bindings.forEach(output::addBinding);
					output.addBinding("b", VALUE_FACTORY.createLiteral(values.poll()));
					return output;
				}
			};
		}
	}
}
