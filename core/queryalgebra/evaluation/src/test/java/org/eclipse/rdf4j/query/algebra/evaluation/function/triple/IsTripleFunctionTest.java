/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.triple;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test rdf:isTriple(a) function
 *
 * @author damyan.ognyanov
 */
public class IsTripleFunctionTest {

	private IsTripleFunction function;

	private final ValueFactory f = SimpleValueFactory.getInstance();

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		function = new IsTripleFunction();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testEvaluateWithTriple() {
		IRI subj = f.createIRI("urn:a");
		IRI pred = f.createIRI("urn:b");
		IRI obj = f.createIRI("urn:c");
		Triple testValue = f.createTriple(subj, pred, obj);

		Value value = function.evaluate(f, testValue);
		assertNotNull(value);
		assertTrue("expect Literal", value instanceof Literal);
		assertTrue("expect positive result", ((Literal) value).booleanValue());
		value = function.evaluate(f, subj);
		assertNotNull(value);
		assertTrue("expect Literal", value instanceof Literal);
		assertTrue("expect negative result", !((Literal) value).booleanValue());
	}

	@Test(expected = ValueExprEvaluationException.class)
	public void testNegativeWrongArguments() {
		IRI subj = f.createIRI("urn:a");
		IRI pred = f.createIRI("urn:b");
		IRI obj = f.createIRI("urn:c");
		Triple testValue = f.createTriple(subj, pred, obj);

		function.evaluate(f, testValue, subj);
		fail("expect ValueExprEvaluationException");
	}

	@Test(expected = ValueExprEvaluationException.class)
	public void testNegativeNoArguments() {
		function.evaluate(f);
		fail("expect ValueExprEvaluationException");
	}
}
