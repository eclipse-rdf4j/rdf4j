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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
	@BeforeEach
	public void setUp() throws Exception {
		function = new IsTripleFunction();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
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
		assertTrue(value instanceof Literal, "expect Literal");
		assertTrue(((Literal) value).booleanValue(), "expect positive result");
		value = function.evaluate(f, subj);
		assertNotNull(value);
		assertTrue(value instanceof Literal, "expect Literal");
		assertTrue(!((Literal) value).booleanValue(), "expect negative result");
	}

	@Test
	public void testNegativeWrongArguments() {
		IRI subj = f.createIRI("urn:a");
		IRI pred = f.createIRI("urn:b");
		IRI obj = f.createIRI("urn:c");
		Triple testValue = f.createTriple(subj, pred, obj);

		assertThrows(ValueExprEvaluationException.class, () -> function.evaluate(f, testValue, subj));
	}

	@Test
	public void testNegativeNoArguments() {
		assertThrows(ValueExprEvaluationException.class, () -> function.evaluate(f));
	}
}
