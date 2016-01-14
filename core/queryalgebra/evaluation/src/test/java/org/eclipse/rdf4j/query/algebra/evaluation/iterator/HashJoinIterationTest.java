/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.Arrays;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.ValueFactoryImpl;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.SimpleEvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.HashJoinIteration;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author MJAHale
 */
public class HashJoinIterationTest {
	private final ValueFactory vf = ValueFactoryImpl.getInstance();
	private final EvaluationStrategy evaluator = new SimpleEvaluationStrategy(null, null);

	@Test
	public void testCartesianJoin() throws QueryEvaluationException {
		BindingSetAssignment left = new BindingSetAssignment();
		{
			QueryBindingSet leftb = new QueryBindingSet();
			leftb.addBinding("a", vf.createLiteral("1"));
			left.setBindingSets(Arrays.<BindingSet>asList(leftb));
		}

		BindingSetAssignment right = new BindingSetAssignment();
		{
			QueryBindingSet rightb = new QueryBindingSet();
			rightb.addBinding("b", vf.createLiteral("2"));
			right.setBindingSets(Arrays.<BindingSet>asList(rightb));
		}

		HashJoinIteration iter = new HashJoinIteration(evaluator, left, right, EmptyBindingSet.getInstance(), false);
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
			left.setBindingSets(Arrays.<BindingSet>asList(leftb));
		}

		BindingSetAssignment right = new BindingSetAssignment();
		{
			QueryBindingSet rightb = new QueryBindingSet();
			rightb.addBinding("b", vf.createLiteral("2"));
			rightb.addBinding("i", vf.createLiteral("x"));
			right.setBindingSets(Arrays.<BindingSet>asList(rightb));
		}

		HashJoinIteration iter = new HashJoinIteration(evaluator, left, right, EmptyBindingSet.getInstance(), false);
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
			left.setBindingSets(Arrays.<BindingSet>asList(leftb));
		}

		BindingSetAssignment right = new BindingSetAssignment();
		{
			QueryBindingSet rightb = new QueryBindingSet();
			rightb.addBinding("b", vf.createLiteral("2"));
			rightb.addBinding("i", vf.createLiteral("y"));
			right.setBindingSets(Arrays.<BindingSet>asList(rightb));
		}

		HashJoinIteration iter = new HashJoinIteration(evaluator, left, right, EmptyBindingSet.getInstance(), true);
		BindingSet actual = iter.next();
		
		assertEquals("1", actual.getValue("a").stringValue());
		assertEquals("x", actual.getValue("i").stringValue());
		assertFalse(actual.hasBinding("b"));
	}
}
