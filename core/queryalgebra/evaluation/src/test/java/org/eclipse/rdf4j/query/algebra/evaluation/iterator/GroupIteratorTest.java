/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Avg;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Count;
import org.eclipse.rdf4j.query.algebra.Group;
import org.eclipse.rdf4j.query.algebra.GroupElem;
import org.eclipse.rdf4j.query.algebra.Sum;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategy;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;

/**
 * @author Bart Hanssens
 */
public class GroupIteratorTest {
	private final static ValueFactory vf = SimpleValueFactory.getInstance();
	private final EvaluationStrategy evaluator = new StrictEvaluationStrategy(null, null);

	private final static BindingSetAssignment EMPTY_ASSIGNMENT;
	static {
		EMPTY_ASSIGNMENT = new BindingSetAssignment();
		EMPTY_ASSIGNMENT.setBindingSets(Collections.EMPTY_LIST);
	}

	private final static BindingSetAssignment NONEMPTY_ASSIGNMENT;
	static {
		NONEMPTY_ASSIGNMENT = new BindingSetAssignment();
		ArrayList list = new ArrayList();
		for (int i = 1; i < 10; i++) {
			QueryBindingSet bindings = new QueryBindingSet();
			bindings.addBinding("a", vf.createLiteral(i));
			list.add(bindings);
		}
		NONEMPTY_ASSIGNMENT.setBindingSets(list);
	}

	@Test
	public void testAvgZero() throws QueryEvaluationException {
		Group group = new Group(EMPTY_ASSIGNMENT);
		group.addGroupElement(new GroupElem("avg", new Avg(new Var("a"))));
		GroupIterator gi = new GroupIterator(evaluator, group, EmptyBindingSet.getInstance());

		assertEquals(gi.next().getBinding("avg").getValue(), vf.createLiteral("0", XMLSchema.INTEGER));
	}

	@Test
	public void testAvgNotZero() throws QueryEvaluationException {
		Group group = new Group(NONEMPTY_ASSIGNMENT);
		group.addGroupElement(new GroupElem("avg", new Avg(new Var("a"))));
		GroupIterator gi = new GroupIterator(evaluator, group, EmptyBindingSet.getInstance());

		assertEquals(gi.next().getBinding("avg").getValue(), vf.createLiteral("5", XMLSchema.DECIMAL));
	}

	@Test
	public void testCountNotZero() throws QueryEvaluationException {
		Group group = new Group(NONEMPTY_ASSIGNMENT);
		group.addGroupElement(new GroupElem("count", new Count(new Var("a"))));
		GroupIterator gi = new GroupIterator(evaluator, group, EmptyBindingSet.getInstance());

		assertEquals(gi.next().getBinding("count").getValue(), vf.createLiteral("9", XMLSchema.INTEGER));
	}

	@Test
	public void testSumNotZero() throws QueryEvaluationException {
		Group group = new Group(NONEMPTY_ASSIGNMENT);
		group.addGroupElement(new GroupElem("sum", new Sum(new Var("a"))));
		GroupIterator gi = new GroupIterator(evaluator, group, EmptyBindingSet.getInstance());

		assertEquals(gi.next().getBinding("sum").getValue(), vf.createLiteral("45", XMLSchema.INTEGER));
	}
}
