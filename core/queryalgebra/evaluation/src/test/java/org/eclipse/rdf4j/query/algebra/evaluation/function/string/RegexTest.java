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
package org.eclipse.rdf4j.query.algebra.evaluation.function.string;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Regex;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EmptyTripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategy;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.repository.sparql.federation.SPARQLServiceResolver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author james
 */
public class RegexTest {

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	private SPARQLServiceResolver serviceResolver;

	@Before
	public void setUp() {
		serviceResolver = new SPARQLServiceResolver();
	}

	@After
	public void tearDown() {
		serviceResolver.shutDown();
	}

	@Test
	public void testEvaluate1() throws QueryEvaluationException {

		Literal expr = vf.createLiteral("foobar");
		Literal pattern = vf.createLiteral("foobar");

		try {
			Literal result = evaluate(expr, pattern);

			assertTrue(result.booleanValue());
		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testEvaluate2() throws QueryEvaluationException {

		Literal expr = vf.createLiteral("foobar");
		Literal pattern = vf.createLiteral("FooBar");
		Literal flags = vf.createLiteral("i");

		try {
			Literal result = evaluate(expr, pattern, flags);

			assertTrue(result.booleanValue());
		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testEvaluate3() throws QueryEvaluationException {

		Literal pattern = vf.createLiteral("FooBar");
		Literal startIndex = vf.createLiteral(4);

		try {
			evaluate(pattern, startIndex, startIndex, startIndex);
			fail("illegal number of parameters");
		} catch (ValueExprEvaluationException e) {
			// do nothing, expected
		}
	}

	@Test
	public void testEvaluate4() throws QueryEvaluationException {

		Literal expr = vf.createLiteral("foobar", "en");
		Literal pattern = vf.createLiteral("FooBar");
		Literal flags = vf.createLiteral("i");

		try {
			Literal result = evaluate(expr, pattern, flags);

			assertTrue(result.booleanValue());
		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testEvaluate5() throws QueryEvaluationException {

		Literal expr = vf.createLiteral("foobar", XSD.STRING);
		Literal pattern = vf.createLiteral("FooBar");
		Literal flags = vf.createLiteral("i");

		try {
			Literal result = evaluate(expr, pattern, flags);

			assertTrue(result.booleanValue());
		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testEvaluate6() throws QueryEvaluationException {

		Literal expr = vf.createLiteral("foobar", XSD.TOKEN);
		Literal pattern = vf.createLiteral("FooBar");
		Literal flags = vf.createLiteral("i");

		try {
			evaluate(expr, pattern, flags);
			fail("Regex should not process typed literals");
		} catch (ValueExprEvaluationException e) {
			// do nothing, expected
		}
	}

	private Literal evaluate(Value... args) throws ValueExprEvaluationException, QueryEvaluationException {
		StrictEvaluationStrategy strategy = new StrictEvaluationStrategy(new EmptyTripleSource(vf), serviceResolver);
		ValueExpr expr = new Var("expr", args[0]);
		ValueExpr pattern = new Var("pattern", args[1]);
		ValueExpr flags = null;
		if (args.length > 2) {
			flags = new Var("flags", args[2]);
		}
		return (Literal) strategy.evaluate(new Regex(expr, pattern, flags), new EmptyBindingSet());
	}

}
