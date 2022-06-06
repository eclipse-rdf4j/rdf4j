/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.string;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jeen
 */
public class LowerCaseTest {

	private LowerCase lcaseFunc;

	private final ValueFactory f = SimpleValueFactory.getInstance();

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		lcaseFunc = new LowerCase();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testEvaluate1() {

		Literal pattern = f.createLiteral("foobar");

		try {
			Literal result = lcaseFunc.evaluate(f, pattern);

			assertTrue(result.getLabel().equals("foobar"));
		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testEvaluate2() {

		Literal pattern = f.createLiteral("FooBar");

		try {
			Literal result = lcaseFunc.evaluate(f, pattern);

			assertTrue(result.getLabel().equals("foobar"));
		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testEvaluate3() {

		Literal pattern = f.createLiteral("FooBar");
		Literal startIndex = f.createLiteral(4);

		try {
			lcaseFunc.evaluate(f, pattern, startIndex);
			fail("illegal number of parameters");
		} catch (ValueExprEvaluationException e) {
			// do nothing, expected
		}
	}

}
