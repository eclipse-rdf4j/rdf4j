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
import org.eclipse.rdf4j.query.algebra.evaluation.function.string.Substring;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jeen
 */
public class SubstringTest {

	private Substring substrFunc;

	private ValueFactory f = SimpleValueFactory.getInstance();

	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp()
		throws Exception
	{
		substrFunc = new Substring();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown()
		throws Exception
	{
	}

	@Test
	public void testEvaluate1() {
		
		Literal pattern = f.createLiteral("foobar");
		Literal startIndex = f.createLiteral(4);
		
		try {
			Literal result = substrFunc.evaluate(f, pattern, startIndex);
			
			assertTrue(result.getLabel().equals("bar"));
		}
		catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testEvaluate2() {
		
		Literal pattern = f.createLiteral("foobar");
		Literal startIndex = f.createLiteral(4);
		Literal length = f.createLiteral(2);
		
		try {
			Literal result = substrFunc.evaluate(f, pattern, startIndex, length);
			
			assertTrue(result.getLabel().equals("ba"));
		}
		catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testEvaluate3() {
		
		Literal pattern = f.createLiteral("foobar");
		Literal startIndex = f.createLiteral(4);
		Literal length = f.createLiteral(5);
		
		try {
			substrFunc.evaluate(f, pattern, startIndex, length);
			fail("illegal length spec should have resulted in error");
		}
		catch (ValueExprEvaluationException e) {
			// do nothing, expected
		}
	}

	
	@Test
	public void testEvaluate4() {
		
		Literal pattern = f.createLiteral("foobar");
		
		try {
			substrFunc.evaluate(f, pattern);
			fail("illegal number of args hould have resulted in error");
		}
		catch (ValueExprEvaluationException e) {
			// do nothing, expected
		}
	}
}
