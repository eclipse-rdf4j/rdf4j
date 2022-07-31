/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.util;

import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Order;
import org.eclipse.rdf4j.query.algebra.OrderElem;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizerPipeline;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedService;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.junit.Before;
import org.junit.Test;

/**
 * @author james
 */
public class OrderComparatorTest {
	private static final ValueFactory vf = SimpleValueFactory.getInstance();
	private final QueryEvaluationContext context = new QueryEvaluationContext.Minimal(
			vf.createLiteral(Date.from(Instant.now())), null);

	class EvaluationStrategyStub implements EvaluationStrategy {

		@Override
		public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Service expr, String serviceUri,
				CloseableIteration<BindingSet, QueryEvaluationException> bindings) throws QueryEvaluationException {
			throw new UnsupportedOperationException();
		}

		@Override
		public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(TupleExpr expr, BindingSet bindings)
				throws QueryEvaluationException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Value evaluate(ValueExpr expr, BindingSet bindings)
				throws ValueExprEvaluationException, QueryEvaluationException {
			return null;
		}

		@Override
		public boolean isTrue(ValueExpr expr, BindingSet bindings)
				throws ValueExprEvaluationException, QueryEvaluationException {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isTrue(QueryValueEvaluationStep expr, BindingSet bindings)
				throws ValueExprEvaluationException, QueryEvaluationException {
			throw new UnsupportedOperationException();
		}

		@Override
		public FederatedService getService(String serviceUrl) throws QueryEvaluationException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setOptimizerPipeline(QueryOptimizerPipeline pipeline) {
			// TODO Auto-generated method stub

		}

		@Override
		public TupleExpr optimize(TupleExpr expr, EvaluationStatistics evaluationStatistics, BindingSet bindings) {
			// TODO Auto-generated method stub
			return null;
		}
	}

	class ComparatorStub extends ValueComparator {

		Iterator<Integer> iter;

		public void setIterator(Iterator<Integer> iter) {
			this.iter = iter;
		}

		@Override
		public int compare(Value o1, Value o2) {
			return iter.next();
		}
	}

	private final EvaluationStrategyStub strategy = new EvaluationStrategyStub();

	private final Order order = new Order();

	private final OrderElem asc = new OrderElem();

	private final OrderElem desc = new OrderElem();

	private final ComparatorStub cmp = new ComparatorStub();

	private final int ZERO = 0;

	private final int POS = 378;

	private final int NEG = -7349;

	@Test
	public void testEquals() throws Exception {
		order.addElement(asc);
		cmp.setIterator(List.of(ZERO).iterator());
		OrderComparator sud = new OrderComparator(strategy, order, cmp, context);
		assertTrue(sud.compare(null, null) == 0);
	}

	@Test
	public void testZero() throws Exception {
		order.addElement(asc);
		order.addElement(asc);
		cmp.setIterator(Arrays.asList(ZERO, POS).iterator());
		OrderComparator sud = new OrderComparator(strategy, order, cmp, context);
		assertTrue(sud.compare(null, null) > 0);
	}

	@Test
	public void testTerm() throws Exception {
		order.addElement(asc);
		order.addElement(asc);
		cmp.setIterator(Arrays.asList(POS, NEG).iterator());
		OrderComparator sud = new OrderComparator(strategy, order, cmp, context);
		assertTrue(sud.compare(null, null) > 0);
	}

	@Test
	public void testAscLessThan() throws Exception {
		order.addElement(asc);
		cmp.setIterator(List.of(NEG).iterator());
		OrderComparator sud = new OrderComparator(strategy, order, cmp, context);
		assertTrue(sud.compare(null, null) < 0);
	}

	@Test
	public void testAscGreaterThan() throws Exception {
		order.addElement(asc);
		cmp.setIterator(List.of(POS).iterator());
		OrderComparator sud = new OrderComparator(strategy, order, cmp, context);
		assertTrue(sud.compare(null, null) > 0);
	}

	@Test
	public void testDescLessThan() throws Exception {
		order.addElement(desc);
		cmp.setIterator(List.of(NEG).iterator());
		OrderComparator sud = new OrderComparator(strategy, order, cmp, context);
		assertTrue(sud.compare(null, null) > 0);
	}

	@Test
	public void testDescGreaterThan() throws Exception {
		order.addElement(desc);
		cmp.setIterator(List.of(POS).iterator());
		OrderComparator sud = new OrderComparator(strategy, order, cmp, context);
		assertTrue(sud.compare(null, null) < 0);
	}

	@Test
	public void testDisjunctBindingNames() throws Exception {
		OrderComparator sud = new OrderComparator(strategy, order, cmp, context);
		QueryBindingSet a = new QueryBindingSet();
		QueryBindingSet b = new QueryBindingSet();
		a.addBinding("a", SimpleValueFactory.getInstance().createLiteral("a"));
		b.addBinding("b", SimpleValueFactory.getInstance().createLiteral("b"));
		assertTrue(sud.compare(a, b) != 0);
		assertTrue(sud.compare(a, b) != sud.compare(b, a));
	}

	@Test
	public void testEqualBindingNamesUnequalValues() {
		OrderComparator sud = new OrderComparator(strategy, order, new ValueComparator(), context);
		QueryBindingSet a = new QueryBindingSet();
		QueryBindingSet b = new QueryBindingSet();
		a.addBinding("a", SimpleValueFactory.getInstance().createLiteral("ab"));
		a.addBinding("b", SimpleValueFactory.getInstance().createLiteral("b"));
		b.addBinding("b", SimpleValueFactory.getInstance().createLiteral("b"));
		b.addBinding("a", SimpleValueFactory.getInstance().createLiteral("ac"));
		assertTrue(sud.compare(a, b) < 0);
		assertTrue(sud.compare(a, b) != sud.compare(b, a));
	}

	@Before
	public void setUp() throws Exception {
		asc.setAscending(true);
		desc.setAscending(false);
	}
}
