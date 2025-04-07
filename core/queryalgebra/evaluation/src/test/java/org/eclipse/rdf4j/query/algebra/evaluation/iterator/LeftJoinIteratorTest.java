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
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.*;
import org.eclipse.rdf4j.query.algebra.evaluation.*;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.DefaultEvaluationStrategy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LeftJoinIteratorTest {

	private static final QueryBindingSet RIGHT_BINDINGS = new QueryBindingSet(1);
	private static final ValueFactory VALUE_FACTORY = SimpleValueFactory.getInstance();
	private static final EvaluationStrategy EVALUATOR = new DefaultEvaluationStrategy(new TripleSource() {

		@Override
		public ValueFactory getValueFactory() {
			return SimpleValueFactory.getInstance();
		}

		@Override
		public CloseableIteration<? extends Statement> getStatements(Resource subj, IRI pred,
				Value obj, Resource... contexts) throws QueryEvaluationException {
			return null;
		}
	}, null);

	private QueryBindingSet bindingSet;
	private BindingSetAssignment left;
	private QueryEvaluationStep leftHandSide;
	private RightHandSideQueryEvaluationStep rightHandSide;

	@BeforeAll
	static void beforeAll() {
		RIGHT_BINDINGS.addBinding("right", VALUE_FACTORY.createLiteral(42));
	}

	@BeforeEach
	void setUp() {
		bindingSet = new QueryBindingSet(1);
		bindingSet.addBinding("left", VALUE_FACTORY.createLiteral(42));

		left = new BindingSetAssignment();
		left.setBindingSets(List.of(bindingSet));

		leftHandSide = EVALUATOR.precompile(left);
		rightHandSide = new RightHandSideQueryEvaluationStep();
	}

	@Test
	@DisplayName("when condition can be evaluated only with left hand side and condition evaluates to false, then don't evaluate right hand side")
	void skipsRightHandSideEvaluation() {
		QueryValueEvaluationStep condition = bindings -> BooleanLiteral.valueOf(false);
		Compare compare = new Compare(new Var("left"), new ValueConstant(VALUE_FACTORY.createLiteral(1337)));

		runLeftOnce(condition, leftJoin(compare));

		assertFalse(rightHandSide.isEvaluated);
	}

	@Test
	@DisplayName("when condition can be evaluated only with left hand side and condition evaluates to false, then return left bindings")
	void onlyReturnLeftHandSideBindings() {
		QueryValueEvaluationStep condition = bindings -> BooleanLiteral.valueOf(false);
		Compare compare = new Compare(new Var("left"), new ValueConstant(VALUE_FACTORY.createLiteral(1337)));

		var result = runLeftOnce(condition, leftJoin(compare));

		assertIterableEquals(left.getBindingSets(), Set.of(result));
	}

	@Test
	@DisplayName("when condition can be evaluated only with left hand side and condition evaluates to true, then evaluate right hand side")
	void evaluatesRightHandSideWithTrueCondition() {
		QueryValueEvaluationStep condition = bindings -> BooleanLiteral.valueOf(true);
		Compare compare = new Compare(new Var("left"), new ValueConstant(VALUE_FACTORY.createLiteral(42)));

		runLeftOnce(condition, leftJoin(compare));

		assertTrue(rightHandSide.isEvaluated);
	}

	@Test
	@DisplayName("when condition can be evaluated only with left hand side and condition evaluates to true, then return joined bindings")
	void returnsRightBindings() {
		QueryValueEvaluationStep condition = bindings -> BooleanLiteral.valueOf(true);
		Compare compare = new Compare(new Var("left"), new ValueConstant(VALUE_FACTORY.createLiteral(42)));

		var result = runLeftOnce(condition, leftJoin(compare));

		var expected = new QueryBindingSet(2);
		bindingSet.forEach(expected::addBinding);
		RIGHT_BINDINGS.forEach(expected::addBinding);
		assertIterableEquals(expected, result);
	}

	@Test
	@DisplayName("when condition can be evaluated only with left hand side and condition evaluates to true, then only evaluate condition once")
	void onlyEvaluatesConditionOnce() {
		var evaluations = new AtomicInteger(0);
		QueryValueEvaluationStep condition = bindings -> {
			evaluations.incrementAndGet();
			return BooleanLiteral.valueOf(true);
		};
		Compare compare = new Compare(new Var("left"), new ValueConstant(VALUE_FACTORY.createLiteral(42)));

		runLeftOnce(condition, leftJoin(compare));

		assertEquals(1, evaluations.get());
	}

	@Test
	@DisplayName("when condition cannot be evaluated only with left hand side, then evaluate right hand side")
	void evaluatesRightHandSideEvaluation() {
		QueryValueEvaluationStep condition = bindings -> BooleanLiteral.valueOf(true);
		Compare compare = new Compare(new Var("right"), new ValueConstant(VALUE_FACTORY.createLiteral(42)));

		runLeftOnce(condition, leftJoin(compare));

		assertTrue(rightHandSide.isEvaluated);
	}

	@Test
	@DisplayName("when no condition, then evaluate right hand side")
	void noCondition() {
		Compare compare = new Compare(new Var("right"), new ValueConstant(VALUE_FACTORY.createLiteral(42)));

		runLeftOnce(null, leftJoin(compare));

		assertTrue(rightHandSide.isEvaluated);
	}

	private LeftJoin leftJoin(Compare compare) {
		return new LeftJoin(left, new EmptySet(), compare);
	}

	private BindingSet runLeftOnce(QueryValueEvaluationStep condition, LeftJoin leftJoin) {
		try (LeftJoinIterator iterator = new LeftJoinIterator(
				leftHandSide,
				rightHandSide,
				condition,
				bindingSet,
				Set.of("left", "right"),
				leftJoin)) {
			return iterator.getNextElement();
		}
	}

	private static class RightHandSideQueryEvaluationStep implements QueryEvaluationStep {

		private boolean isEvaluated = false;

		@Override
		public CloseableIteration<BindingSet> evaluate(BindingSet bindings) {
			isEvaluated = true;
			var bothBindings = new QueryBindingSet(2);
			bindings.forEach(bothBindings::addBinding);
			RIGHT_BINDINGS.forEach(bothBindings::addBinding);
			var rightIterator = List.of(bothBindings).iterator();

			return new CloseableIteration<>() {
				@Override
				public void close() {
					// Nothing to close
				}

				@Override
				public boolean hasNext() {
					return rightIterator.hasNext();
				}

				@Override
				public BindingSet next() {
					return rightIterator.next();
				}
			};
		}
	}
}
