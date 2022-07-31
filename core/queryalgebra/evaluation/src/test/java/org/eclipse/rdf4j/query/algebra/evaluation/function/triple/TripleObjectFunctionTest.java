/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.triple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test rdf:subject(a) function
 *
 * @author damyan.ognyanov
 */
public class TripleObjectFunctionTest {

	private TripleObjectFunction function;

	private final ValueFactory f = SimpleValueFactory.getInstance();

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		function = new TripleObjectFunction();
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
		assertTrue("expect IRI", value instanceof IRI);
		assertEquals("expect same value", obj, value);
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
	public void testWrongArguments() {
		IRI subj = f.createIRI("urn:a");

		function.evaluate(f, subj);
		fail("expect ValueExprEvaluationException");
	}
}
