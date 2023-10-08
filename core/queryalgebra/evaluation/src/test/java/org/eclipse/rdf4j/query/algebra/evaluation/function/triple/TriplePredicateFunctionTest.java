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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test rdf:predicate(a) function
 *
 * @author damyan.ognyanov
 */
public class TriplePredicateFunctionTest {

	private TriplePredicateFunction function;

	private final ValueFactory f = SimpleValueFactory.getInstance();

	/**
	 */
	@BeforeEach
	public void setUp() {
		function = new TriplePredicateFunction();
	}

	/**
	 */
	@AfterEach
	public void tearDown() {
	}

	@Test
	public void testEvaluateWithTriple() {
		IRI subj = f.createIRI("urn:a");
		IRI pred = f.createIRI("urn:b");
		IRI obj = f.createIRI("urn:c");
		Triple testValue = f.createTriple(subj, pred, obj);

		Value value = function.evaluate(f, testValue);
		assertNotNull(value);
		assertTrue(value instanceof IRI, "expect IRI");
		assertEquals(pred, value, "expect same value");
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
	public void testWrongArguments() {
		IRI subj = f.createIRI("urn:a");

		assertThrows(ValueExprEvaluationException.class, () -> function.evaluate(f, subj));
	}
}
