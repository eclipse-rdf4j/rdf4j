/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.util;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.ValueFactoryImpl;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Order;
import org.eclipse.rdf4j.query.algebra.OrderElem;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedService;
import org.eclipse.rdf4j.query.algebra.evaluation.util.OrderComparator;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;
import org.junit.Before;
import org.junit.Test;

/**
 * @author james
 */
public class OrderComparatorTest {

	class EvaluationStrategyStub implements EvaluationStrategy {

		public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Service expr,
				String serviceUri, CloseableIteration<BindingSet, QueryEvaluationException> bindings)
					throws QueryEvaluationException
		{
			throw new UnsupportedOperationException();
		}

		public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(TupleExpr expr,
				BindingSet bindings)
					throws QueryEvaluationException
		{
			throw new UnsupportedOperationException();
		}

		public Value evaluate(ValueExpr expr, BindingSet bindings)
			throws ValueExprEvaluationException, QueryEvaluationException
		{
			return null;
		}

		public boolean isTrue(ValueExpr expr, BindingSet bindings)
			throws ValueExprEvaluationException, QueryEvaluationException
		{
			throw new UnsupportedOperationException();
		}

		public FederatedService getService(String serviceUrl)
			throws QueryEvaluationException
		{
			throw new UnsupportedOperationException();
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

	private EvaluationStrategyStub strategy = new EvaluationStrategyStub();

	private Order order = new Order();

	private OrderElem asc = new OrderElem();

	private OrderElem desc = new OrderElem();

	private ComparatorStub cmp = new ComparatorStub();

	private int ZERO = 0;

	private int POS = 378;

	private int NEG = -7349;

	@Test
	public void testEquals()
		throws Exception
	{
		order.addElement(asc);
		cmp.setIterator(Arrays.asList(ZERO).iterator());
		OrderComparator sud = new OrderComparator(strategy, order, cmp);
		assertTrue(sud.compare(null, null) == 0);
	}

	@Test
	public void testZero()
		throws Exception
	{
		order.addElement(asc);
		order.addElement(asc);
		cmp.setIterator(Arrays.asList(ZERO, POS).iterator());
		OrderComparator sud = new OrderComparator(strategy, order, cmp);
		assertTrue(sud.compare(null, null) > 0);
	}

	@Test
	public void testTerm()
		throws Exception
	{
		order.addElement(asc);
		order.addElement(asc);
		cmp.setIterator(Arrays.asList(POS, NEG).iterator());
		OrderComparator sud = new OrderComparator(strategy, order, cmp);
		assertTrue(sud.compare(null, null) > 0);
	}

	@Test
	public void testAscLessThan()
		throws Exception
	{
		order.addElement(asc);
		cmp.setIterator(Arrays.asList(NEG).iterator());
		OrderComparator sud = new OrderComparator(strategy, order, cmp);
		assertTrue(sud.compare(null, null) < 0);
	}

	@Test
	public void testAscGreaterThan()
		throws Exception
	{
		order.addElement(asc);
		cmp.setIterator(Arrays.asList(POS).iterator());
		OrderComparator sud = new OrderComparator(strategy, order, cmp);
		assertTrue(sud.compare(null, null) > 0);
	}

	@Test
	public void testDescLessThan()
		throws Exception
	{
		order.addElement(desc);
		cmp.setIterator(Arrays.asList(NEG).iterator());
		OrderComparator sud = new OrderComparator(strategy, order, cmp);
		assertTrue(sud.compare(null, null) > 0);
	}

	@Test
	public void testDescGreaterThan()
		throws Exception
	{
		order.addElement(desc);
		cmp.setIterator(Arrays.asList(POS).iterator());
		OrderComparator sud = new OrderComparator(strategy, order, cmp);
		assertTrue(sud.compare(null, null) < 0);
	}

	@Test
	public void testDisjunctBindingNames()
		throws Exception
	{
		OrderComparator sud = new OrderComparator(strategy, order, cmp);
		QueryBindingSet a = new QueryBindingSet();
		QueryBindingSet b = new QueryBindingSet();
		a.addBinding("a", ValueFactoryImpl.getInstance().createLiteral("a"));
		b.addBinding("b", ValueFactoryImpl.getInstance().createLiteral("b"));
		assertTrue(sud.compare(a, b) != 0);
		assertTrue(sud.compare(a, b) != sud.compare(b, a));
	}
	
	@Test 
	public void testEqualBindingNamesUnequalValues() {
		OrderComparator sud = new OrderComparator(strategy, order, new ValueComparator());
		QueryBindingSet a = new QueryBindingSet();
		QueryBindingSet b = new QueryBindingSet();
		a.addBinding("a", ValueFactoryImpl.getInstance().createLiteral("ab"));
		a.addBinding("b", ValueFactoryImpl.getInstance().createLiteral("b"));
		b.addBinding("b", ValueFactoryImpl.getInstance().createLiteral("b"));
		b.addBinding("a", ValueFactoryImpl.getInstance().createLiteral("ac"));
		assertTrue(sud.compare(a, b) < 0);
		assertTrue(sud.compare(a, b) != sud.compare(b, a));
	}

	@Before
	public void setUp()
		throws Exception
	{
		asc.setAscending(true);
		desc.setAscending(false);
	}
}
