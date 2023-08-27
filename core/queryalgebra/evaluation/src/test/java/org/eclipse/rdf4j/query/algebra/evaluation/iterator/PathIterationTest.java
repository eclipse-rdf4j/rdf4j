/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.StatementPattern.Scope;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.DefaultEvaluationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PathIterationTest {

	private DefaultEvaluationStrategy evaluator;
	private static final ValueFactory vf = SimpleValueFactory.getInstance();
	private static final Resource one = vf.createIRI("https://example.org/", "one");
	private static final Resource two = vf.createIRI("https://example.org/", "two");
	private static final Resource three = vf.createIRI("https://example.org/", "three");

	@BeforeEach
	public void setUp() {
		Model m = new LinkedHashModel();
		m.add(one, RDFS.SUBCLASSOF, two);
		m.add(two, RDFS.SUBCLASSOF, three);

		TripleSource ts = new TripleSource() {

			@Override
			public CloseableIteration<? extends Statement> getStatements(Resource subj,
					IRI pred, Value obj, Resource... contexts) throws QueryEvaluationException {
				return new CloseableIteratorIteration<>(m.getStatements(subj, pred, obj, contexts).iterator());
			}

			@Override
			public ValueFactory getValueFactory() {
				return vf;
			}
		};
		evaluator = new DefaultEvaluationStrategy(ts, null);
	}

	@Test
	public void zeroHop() {
		// SELECT * WHERE { ?subClass rdfs:subClassOf+ ?superClass }

		Var startVar = new Var("subClass");
		Var endVar = new Var("superClass");
		TupleExpr pathExpression = new StatementPattern(startVar, new Var("lala", RDFS.SUBCLASSOF, true, true), endVar);
		Var contextVar = null;
		long minLength = 0;
		BindingSet bindings = new QueryBindingSet();
		// Expect but not necessary in this order
		// one | one
		// one | two
		// one | three
		// two | three
		// two | two
		// three | three

		try (PathIteration zlp = new PathIteration(evaluator, Scope.DEFAULT_CONTEXTS, startVar,
				pathExpression, endVar, contextVar, minLength, bindings)) {
			assertExpected(zlp.getNextElement(), one, one);

			assertExpected(zlp.getNextElement(), two, two);

			assertExpected(zlp.getNextElement(), three, three);

			assertExpected(zlp.getNextElement(), one, two);

			assertExpected(zlp.getNextElement(), two, three);

			assertExpected(zlp.getNextElement(), one, three);

			assertNull(zlp.getNextElement());
		}
	}

	void assertExpected(BindingSet result, Value subClass, Value superClass) {
		assertNotNull(result);
		assertTrue(result.hasBinding("subClass"), "path zlp evaluation should binding for subClass var");
		assertEquals(subClass, result.getBinding("subClass").getValue());
		assertTrue(result.hasBinding("superClass"), "path zlp evaluation should binding for superClass var");
		assertEquals(superClass, result.getBinding("superClass").getValue());
		assertEquals(2, result.size());
	}

	@Test
	public void oneHop() {
		// SELECT * WHERE { ?subClass rdfs:subClassOf+ ?superClass }

		Var startVar = new Var("subClass");
		Var endVar = new Var("superClass");
		TupleExpr pathExpression = new StatementPattern(startVar, new Var("lala", RDFS.SUBCLASSOF, true, true), endVar);
		Var contextVar = null;
		long minLength = 1;
		// Expected
		// one two
		// one three
		// two three
		BindingSet bindings = new QueryBindingSet();
		try (PathIteration zlp = new PathIteration(evaluator, Scope.DEFAULT_CONTEXTS, startVar,
				pathExpression, endVar, contextVar, minLength, bindings)) {
			assertExpected(zlp.getNextElement(), one, two);
			assertExpected(zlp.getNextElement(), two, three);
			assertExpected(zlp.getNextElement(), one, three);
			assertNull(zlp.getNextElement());
		}
	}

	@Test
	public void oneHopStartConstant() {
		// SELECT * WHERE { ?subClass rdfs:subClassOf+ ?superClass }

		Var startVar = new Var("subClass", one, true, true);
		Var endVar = new Var("superClass");
		TupleExpr pathExpression = new StatementPattern(startVar, new Var("lala", RDFS.SUBCLASSOF, true, true), endVar);
		Var contextVar = null;
		long minLength = 1;
		BindingSet bindings = new QueryBindingSet();
		try (PathIteration zlp = new PathIteration(evaluator, Scope.DEFAULT_CONTEXTS, startVar,
				pathExpression, endVar, contextVar, minLength, bindings)) {
			assertExpected(zlp.getNextElement(), one, two);
			assertExpected(zlp.getNextElement(), one, three);
			assertNull(zlp.getNextElement());
		}
	}
}
