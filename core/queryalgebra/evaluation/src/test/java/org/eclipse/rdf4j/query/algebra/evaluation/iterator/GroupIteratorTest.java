/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.*;
import org.eclipse.rdf4j.query.algebra.evaluation.*;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.util.MathUtil;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.parser.sparql.aggregate.AggregateCollector;
import org.eclipse.rdf4j.query.parser.sparql.aggregate.AggregateFunction;
import org.eclipse.rdf4j.query.parser.sparql.aggregate.AggregateFunctionFactory;
import org.eclipse.rdf4j.query.parser.sparql.aggregate.CustomAggregateFunctionRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Bart Hanssens
 */
public class GroupIteratorTest {
	private final static ValueFactory vf = SimpleValueFactory.getInstance();
	private static final EvaluationStrategy evaluator = new StrictEvaluationStrategy(null, null);
	private static final QueryEvaluationContext context = new QueryEvaluationContext.Minimal(
			vf.createLiteral(Date.from(Instant.now())), null, null);
	private static BindingSetAssignment EMPTY_ASSIGNMENT;
	private static BindingSetAssignment NONEMPTY_ASSIGNMENT;
	private static AggregateFunctionFactory aggregateFunctionFactory;

	@BeforeAll
	public static void init() {
		EMPTY_ASSIGNMENT = new BindingSetAssignment();
		EMPTY_ASSIGNMENT.setBindingSets(Collections.emptyList());
		NONEMPTY_ASSIGNMENT = new BindingSetAssignment();
		var list = new ArrayList<BindingSet>();
		for (int i = 1; i < 10; i++) {
			var bindings = new QueryBindingSet();
			bindings.addBinding("a", vf.createLiteral(i));
			list.add(bindings);
		}
		NONEMPTY_ASSIGNMENT.setBindingSets(Collections.unmodifiableList(list));
		aggregateFunctionFactory = new AggregateFunctionFactory() {
			@Override
			public String getIri() {
				return "https://www.rdf4j.org/aggregate#x";
			}

			@Override
			public AggregateFunction<SumCollector, Value> buildFunction(Function<BindingSet, Value> evaluationStep) {
				return new AggregateFunction<>(evaluationStep) {

					private ValueExprEvaluationException typeError = null;

					@Override
					public void processAggregate(BindingSet s, Predicate<Value> distinctValue, SumCollector sum)
							throws QueryEvaluationException {
						if (typeError != null) {
							// halt further processing if a type error has been raised
							return;
						}
						Value v = evaluate(s);
						if (v instanceof Literal) {
							if (distinctValue.test(v)) {
								Literal nextLiteral = (Literal) v;
								if (nextLiteral.getDatatype() != null
										&& XMLDatatypeUtil.isNumericDatatype(nextLiteral.getDatatype())) {
									sum.value = MathUtil.compute(sum.value, nextLiteral, MathExpr.MathOp.PLUS);
								} else {
									typeError = new ValueExprEvaluationException("not a number: " + v);
								}
							} else {
								typeError = new ValueExprEvaluationException("not a number: " + v);
							}
						}
					}
				};
			}

			@Override
			public SumCollector getCollector() {
				return new SumCollector();
			}
		};
		CustomAggregateFunctionRegistry.getInstance().add(aggregateFunctionFactory);
	}

	@AfterAll
	public static void cleanUp() {
		CustomAggregateFunctionRegistry.getInstance().remove(aggregateFunctionFactory);
	}

	@Test
	public void testAvgEmptySet() throws QueryEvaluationException {
		Group group = new Group(EMPTY_ASSIGNMENT);
		group.addGroupElement(new GroupElem("avg", new Avg(new Var("a"))));
		try (GroupIterator gi = new GroupIterator(evaluator, group, EmptyBindingSet.getInstance(), context)) {

			assertThat(gi.next().getBinding("avg").getValue())
					.describedAs("AVG on empty set should result in 0")
					.isEqualTo(vf.createLiteral("0", XSD.INTEGER));
		}
	}

	@Test
	public void testMaxEmptySet_DefaultGroup() throws QueryEvaluationException {
		Group group = new Group(EMPTY_ASSIGNMENT);
		group.addGroupElement(new GroupElem("max", new Max(new Var("a"))));
		try (GroupIterator gi = new GroupIterator(evaluator, group, EmptyBindingSet.getInstance(), context)) {

			assertThat(gi.hasNext()).isTrue();
			assertThat(gi.next().size()).isEqualTo(0);
		}
	}

	@Test
	public void testMaxEmptySet_Grouped() throws QueryEvaluationException {
		Group group = new Group(EMPTY_ASSIGNMENT);
		group.addGroupElement(new GroupElem("max", new Max(new Var("a"))));
		group.addGroupBindingName("x"); // we are grouping by variable x

		try (GroupIterator gi = new GroupIterator(evaluator, group, EmptyBindingSet.getInstance(), context)) {

			assertThat(gi.hasNext()).isFalse();
		}
	}

	@Test
	public void testMinEmptySet() throws QueryEvaluationException {
		Group group = new Group(EMPTY_ASSIGNMENT);
		group.addGroupElement(new GroupElem("min", new Min(new Var("a"))));
		try (GroupIterator gi = new GroupIterator(evaluator, group, EmptyBindingSet.getInstance(), context)) {

			assertThat(gi.hasNext()).isTrue();
			assertThat(gi.next().size()).isEqualTo(0);
		}
	}

	@Test
	public void testSampleEmptySet() throws QueryEvaluationException {
		Group group = new Group(EMPTY_ASSIGNMENT);
		group.addGroupElement(new GroupElem("sample", new Sample(new Var("a"))));
		try (GroupIterator gi = new GroupIterator(evaluator, group, EmptyBindingSet.getInstance(), context)) {

			assertThat(gi.hasNext()).isTrue();
			assertThat(gi.next().size()).isEqualTo(0);
		}
	}

	@Test
	public void testGroupConcatEmptySet() throws QueryEvaluationException {
		Group group = new Group(EMPTY_ASSIGNMENT);
		group.addGroupElement(new GroupElem("groupconcat", new GroupConcat(new Var("a"))));
		try (GroupIterator gi = new GroupIterator(evaluator, group, EmptyBindingSet.getInstance(), context)) {

			assertThat(gi.next().getBinding("groupconcat").getValue())
					.describedAs("GROUP_CONCAT on empty set should result in empty string")
					.isEqualTo(vf.createLiteral(""));
		}
	}

	@Test
	public void testAvgNotZero() throws QueryEvaluationException {
		Group group = new Group(NONEMPTY_ASSIGNMENT);
		group.addGroupElement(new GroupElem("avg", new Avg(new Var("a"))));
		try (GroupIterator gi = new GroupIterator(evaluator, group, EmptyBindingSet.getInstance(), context)) {

			assertThat(gi.next().getBinding("avg").getValue()).isEqualTo(vf.createLiteral("5", XSD.DECIMAL));
		}
	}

	@Test
	public void testCountNotZero() throws QueryEvaluationException {
		Group group = new Group(NONEMPTY_ASSIGNMENT);
		group.addGroupElement(new GroupElem("count", new Count(new Var("a"))));
		try (GroupIterator gi = new GroupIterator(evaluator, group, EmptyBindingSet.getInstance(), context)) {

			assertThat(gi.next().getBinding("count").getValue()).isEqualTo(vf.createLiteral("9", XSD.INTEGER));
		}
	}

	@Test
	public void testSumNotZero() throws QueryEvaluationException {
		Group group = new Group(NONEMPTY_ASSIGNMENT);
		group.addGroupElement(new GroupElem("sum", new Sum(new Var("a"))));
		try (GroupIterator gi = new GroupIterator(evaluator, group, EmptyBindingSet.getInstance(), context)) {

			assertThat(gi.next().getBinding("sum").getValue()).isEqualTo(vf.createLiteral("45", XSD.INTEGER));
		}
	}

	@Test
	public void testCustomAggregateFunction_Nonempty() throws QueryEvaluationException {
		Group group = new Group(NONEMPTY_ASSIGNMENT);
		group.addGroupElement(new GroupElem("customSum",
				new AggregateFunctionCall(new Var("a"), aggregateFunctionFactory.getIri(), false)));
		try (GroupIterator gi = new GroupIterator(evaluator, group, EmptyBindingSet.getInstance(), context)) {
			assertThat(gi.next().getBinding("customSum").getValue()).isEqualTo(vf.createLiteral("45", XSD.INTEGER));
		}
	}

	@Test
	public void testCustomAggregateFunction_Empty() throws QueryEvaluationException {
		Group group = new Group(EMPTY_ASSIGNMENT);
		group.addGroupElement(new GroupElem("customSum",
				new AggregateFunctionCall(new Var("a"), aggregateFunctionFactory.getIri(), false)));
		try (GroupIterator gi = new GroupIterator(evaluator, group, EmptyBindingSet.getInstance(), context)) {
			assertThat(gi.next().getBinding("customSum").getValue()).isEqualTo(vf.createLiteral("0", XSD.INTEGER));
		}
	}

	@Test
	public void testCustomAggregateFunction_WrongIri() throws QueryEvaluationException {
		Group group = new Group(EMPTY_ASSIGNMENT);
		group.addGroupElement(new GroupElem("customSum", new AggregateFunctionCall(new Var("a"), "urn:i", false)));
		try (GroupIterator gi = new GroupIterator(evaluator, group, EmptyBindingSet.getInstance(), context)) {
			assertThatExceptionOfType(QueryEvaluationException.class)
					.isThrownBy(() -> gi.next().getBinding("customSum").getValue());
		}
	}

	@Test
	public void testGroupIteratorClose() throws QueryEvaluationException, InterruptedException {
		// Lock which is already locked to block the thread driving the iteration
		Lock lock = new ReentrantLock();
		lock.lock();
		// Latch to rendezvous on with the iterating thread
		CountDownLatch iterating = new CountDownLatch(1);
		// Latch to record whether the iteration under GroupIterator was closed
		CountDownLatch closed = new CountDownLatch(1);

		EvaluationStrategy evaluator = new StrictEvaluationStrategy(null, null) {
			@Override
			protected QueryEvaluationStep prepare(EmptySet emptySet, QueryEvaluationContext context)
					throws QueryEvaluationException {
				return bindings -> new LookAheadIteration<>() {
					@Override
					protected BindingSet getNextElement() {
						iterating.countDown(); // signal to test thread iteration started
						lock.lock(); // block iterating thread
						return null;
					}

					@Override
					protected void handleClose() {
						closed.countDown();
					}
				};
			}
		};

		Group group = new Group(new EmptySet());
		GroupIterator groupIterator = new GroupIterator(evaluator, group, EmptyBindingSet.getInstance(), context);

		Thread iteratorThread = new Thread(groupIterator::next, "GroupIteratorTest#testGroupIteratorClose");
		try {
			iteratorThread.start();
			assertThat(iterating.await(5, TimeUnit.SECONDS)).isTrue();
			groupIterator.close();
			assertThat(closed.await(5, TimeUnit.SECONDS)).isTrue();
		} finally {
			lock.unlock();
			iteratorThread.join(Duration.ofSeconds(5).toMillis());
			assertThat(iteratorThread.isAlive()).isFalse();
		}
	}

	/**
	 * Dummy collector to verify custom aggregate functions
	 */
	private static class SumCollector implements AggregateCollector {
		protected Literal value = vf.createLiteral("0", CoreDatatype.XSD.INTEGER);

		@Override
		public Value getFinalValue() {
			return value;
		}
	}
}
