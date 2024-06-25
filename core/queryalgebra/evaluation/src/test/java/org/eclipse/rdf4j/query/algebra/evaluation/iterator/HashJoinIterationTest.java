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
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategy;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.junit.jupiter.api.Test;

/**
 * @author MJAHale
 */
public class HashJoinIterationTest {

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	private final EvaluationStrategy evaluator = new StrictEvaluationStrategy(new TripleSource() {

		@Override
		public ValueFactory getValueFactory() {
			return SimpleValueFactory.getInstance();
		}

		@Override
		public CloseableIteration<? extends Statement> getStatements(Resource subj, IRI pred,
				Value obj, Resource... contexts) throws QueryEvaluationException {
			// TODO Auto-generated method stub
			return null;
		}
	}, null);

	@Test
	public void testCartesianJoin() throws QueryEvaluationException {
		BindingSetAssignment left = new BindingSetAssignment();
		{
			QueryBindingSet leftb = new QueryBindingSet();
			leftb.addBinding("a", vf.createLiteral("1"));
			left.setBindingSets(List.<BindingSet>of(leftb));
		}

		BindingSetAssignment right = new BindingSetAssignment();
		{
			QueryBindingSet rightb = new QueryBindingSet();
			rightb.addBinding("b", vf.createLiteral("2"));
			right.setBindingSets(List.<BindingSet>of(rightb));
		}

		HashJoinIteration iter = new HashJoinIteration(evaluator.evaluate(left, EmptyBindingSet.getInstance()),
				left.getBindingNames(), evaluator.evaluate(right, EmptyBindingSet.getInstance()),
				right.getBindingNames(), false);
		BindingSet actual = iter.next();

		assertEquals("1", actual.getValue("a").stringValue());
		assertEquals("2", actual.getValue("b").stringValue());
	}

	@Test
	public void testInnerJoin() throws QueryEvaluationException {
		BindingSetAssignment left = new BindingSetAssignment();
		{
			QueryBindingSet leftb = new QueryBindingSet();
			leftb.addBinding("a", vf.createLiteral("1"));
			leftb.addBinding("i", vf.createLiteral("x"));
			left.setBindingSets(List.<BindingSet>of(leftb));
		}

		BindingSetAssignment right = new BindingSetAssignment();
		{
			QueryBindingSet rightb = new QueryBindingSet();
			rightb.addBinding("b", vf.createLiteral("2"));
			rightb.addBinding("i", vf.createLiteral("x"));
			right.setBindingSets(List.<BindingSet>of(rightb));
		}

		HashJoinIteration iter = new HashJoinIteration(evaluator.evaluate(left, EmptyBindingSet.getInstance()),
				left.getBindingNames(), evaluator.evaluate(right, EmptyBindingSet.getInstance()),
				right.getBindingNames(), false);
		BindingSet actual = iter.next();

		assertEquals("1", actual.getValue("a").stringValue());
		assertEquals("2", actual.getValue("b").stringValue());
		assertEquals("x", actual.getValue("i").stringValue());
	}

	@Test
	public void testLeftJoin() throws QueryEvaluationException {
		BindingSetAssignment left = new BindingSetAssignment();
		{
			QueryBindingSet leftb = new QueryBindingSet();
			leftb.addBinding("a", vf.createLiteral("1"));
			leftb.addBinding("i", vf.createLiteral("x"));
			left.setBindingSets(List.<BindingSet>of(leftb));
		}

		BindingSetAssignment right = new BindingSetAssignment();
		{
			QueryBindingSet rightb = new QueryBindingSet();
			rightb.addBinding("b", vf.createLiteral("2"));
			rightb.addBinding("i", vf.createLiteral("y"));
			right.setBindingSets(List.<BindingSet>of(rightb));
		}

		HashJoinIteration iter = new HashJoinIteration(evaluator.evaluate(left, EmptyBindingSet.getInstance()),
				left.getBindingNames(), evaluator.evaluate(right, EmptyBindingSet.getInstance()),
				right.getBindingNames(), true);
		BindingSet actual = iter.next();

		assertEquals("1", actual.getValue("a").stringValue());
		assertEquals("x", actual.getValue("i").stringValue());
		assertFalse(actual.hasBinding("b"));
	}
}
