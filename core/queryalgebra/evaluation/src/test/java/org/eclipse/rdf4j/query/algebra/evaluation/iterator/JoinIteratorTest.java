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

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategy;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.junit.Test;

/**
 * @author MJAHale
 */
public class JoinIteratorTest {

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	private final EvaluationStrategy evaluator = new StrictEvaluationStrategy(new TripleSource() {

		@Override
		public ValueFactory getValueFactory() {
			return SimpleValueFactory.getInstance();
		}

		@Override
		public CloseableIteration<? extends Statement, QueryEvaluationException> getStatements(Resource subj, IRI pred,
				Value obj, Resource... contexts) throws QueryEvaluationException {
			// TODO Auto-generated method stub
			return null;
		}
	}, null);
	private final QueryEvaluationContext context = new QueryEvaluationContext.Minimal(
			vf.createLiteral(Date.from(Instant.now())), null);

	/**
	 * Tests joins between two different BindingSetAssignments with the same BindingSets but ordered differently.
	 */
	@Test
	public void testBindingSetAssignmentJoin() throws QueryEvaluationException {
		testBindingSetAssignmentJoin(5, 5, EmptyBindingSet.getInstance());

		{
			QueryBindingSet b = new QueryBindingSet();
			b.addBinding("a", vf.createLiteral(2));
			testBindingSetAssignmentJoin(1, 5, b);
		}

		{
			QueryBindingSet b = new QueryBindingSet();
			b.addBinding("x", vf.createLiteral("foo"));
			testBindingSetAssignmentJoin(5, 5, b);
		}
	}

	private void testBindingSetAssignmentJoin(int expectedSize, int n, BindingSet bindings)
			throws QueryEvaluationException {
		BindingSetAssignment left = new BindingSetAssignment();
		{
			List<BindingSet> leftb = new ArrayList<>();
			for (int i = 0; i < n; i++) {
				QueryBindingSet b = new QueryBindingSet();
				b.addBinding("a", vf.createLiteral(i));
				leftb.add(b);
			}
			left.setBindingSets(leftb);
		}

		BindingSetAssignment right = new BindingSetAssignment();
		{
			List<BindingSet> rightb = new ArrayList<>();
			for (int i = n; i >= 0; i--) {
				QueryBindingSet b = new QueryBindingSet();
				b.addBinding("a", vf.createLiteral(i));
				rightb.add(b);
			}
			right.setBindingSets(rightb);
		}

		JoinIterator lrIter = new JoinIterator(evaluator, new Join(left, right), bindings, context);
		Set<BindingSet> lr = Iterations.asSet(lrIter);
		assertEquals(expectedSize, lr.size());

		JoinIterator rlIter = new JoinIterator(evaluator, new Join(right, left), bindings, context);
		Set<BindingSet> rl = Iterations.asSet(rlIter);
		assertEquals(expectedSize, rl.size());

		assertEquals(lr, rl);

		// check bindings
		for (BindingSet b : lr) {
			for (String name : bindings.getBindingNames()) {
				assertEquals(bindings.getValue(name), b.getValue(name));
			}
		}
	}
}
