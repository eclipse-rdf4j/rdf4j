/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.triple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.rdf4j.model.BNode;
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
 * Test rdf:Statement(a, b, c) function cases covered: subject one of (IRI | BNode | Triple | Literal) predicate one of
 * (IRI | Literal) object one of (IRI | BNode | Triple | Literal)
 *
 * @author damyan.ognyanov
 */
public class StatementFunctionTest {

	private StatementFunction function;

	private final ValueFactory f = SimpleValueFactory.getInstance();

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		function = new StatementFunction();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testEvaluateIRIAndIRI() {
		IRI subj = f.createIRI("urn:a");
		IRI pred = f.createIRI("urn:b");
		IRI obj = f.createIRI("urn:c");

		Value value = function.evaluate(f, subj, pred, obj);
		assertNotNull(value);
		assertTrue("expect Triple", value instanceof Triple);
		Triple other = f.createTriple(subj, pred, obj);
		assertEquals("expect to be the same", value, other);
	}

	@Test
	public void testEvaluateIRIAndBNode() {
		IRI subj = f.createIRI("urn:a");
		IRI pred = f.createIRI("urn:b");
		BNode obj = f.createBNode();

		Value value = function.evaluate(f, subj, pred, obj);
		assertNotNull(value);
		assertTrue("expect Triple", value instanceof Triple);
		Triple other = f.createTriple(subj, pred, obj);
		assertEquals("expect to be the same", value, other);
	}

	@Test
	public void testEvaluateIRIAndLiteral() {
		IRI subj = f.createIRI("urn:a");
		IRI pred = f.createIRI("urn:b");
		Literal obj = f.createLiteral(1);

		Value value = function.evaluate(f, subj, pred, obj);
		assertNotNull(value);
		assertTrue("expect Triple", value instanceof Triple);
		Triple other = f.createTriple(subj, pred, obj);
		assertEquals("expect to be the same", value, other);
	}

	@Test
	public void testEvaluateIRIAndTriple() {
		IRI subj = f.createIRI("urn:a");
		IRI pred = f.createIRI("urn:b");
		Triple obj = f.createTriple(subj, subj, subj);

		Value value = function.evaluate(f, subj, pred, obj);
		assertNotNull(value);
		assertTrue("expect Triple", value instanceof Triple);
		Triple other = f.createTriple(subj, pred, obj);
		assertEquals("expect to be the same", value, other);
	}

	@Test
	public void testEvaluateBNodeAndIRI() {
		BNode subj = f.createBNode();
		IRI pred = f.createIRI("urn:b");
		IRI obj = f.createIRI("urn:c");

		Value value = function.evaluate(f, subj, pred, obj);
		assertNotNull(value);
		assertTrue("expect Triple", value instanceof Triple);
		Triple other = f.createTriple(subj, pred, obj);
		assertEquals("expect to be the same", value, other);
	}

	@Test
	public void testEvaluateBNodeAndBNode() {
		BNode subj = f.createBNode();
		IRI pred = f.createIRI("urn:b");
		BNode obj = f.createBNode();

		Value value = function.evaluate(f, subj, pred, obj);
		assertNotNull(value);
		assertTrue("expect Triple", value instanceof Triple);
		Triple other = f.createTriple(subj, pred, obj);
		assertEquals("expect to be the same", value, other);
	}

	@Test
	public void testEvaluateBNodeAndLiteral() {
		BNode subj = f.createBNode();
		IRI pred = f.createIRI("urn:b");
		Literal obj = f.createLiteral(1);

		Value value = function.evaluate(f, subj, pred, obj);
		assertNotNull(value);
		assertTrue("expect Triple", value instanceof Triple);
		Triple other = f.createTriple(subj, pred, obj);
		assertEquals("expect to be the same", value, other);
	}

	@Test
	public void testEvaluateBNodeAndTriple() {
		BNode subj = f.createBNode();
		IRI pred = f.createIRI("urn:b");
		Triple obj = f.createTriple(pred, pred, pred);

		Value value = function.evaluate(f, subj, pred, obj);
		assertNotNull(value);
		assertTrue("expect Triple", value instanceof Triple);
		Triple other = f.createTriple(subj, pred, obj);
		assertEquals("expect to be the same", value, other);
	}

	@Test
	public void testEvaluateTripleAndIRI() {
		IRI pred = f.createIRI("urn:b");
		IRI obj = f.createIRI("urn:c");
		Triple subj = f.createTriple(pred, pred, pred);

		Value value = function.evaluate(f, subj, pred, obj);
		assertNotNull(value);
		assertTrue("expect Triple", value instanceof Triple);
		Triple other = f.createTriple(subj, pred, obj);
		assertEquals("expect to be the same", value, other);
	}

	@Test
	public void testEvaluateTripleAndBNode() {
		IRI pred = f.createIRI("urn:b");
		BNode obj = f.createBNode();
		Triple subj = f.createTriple(pred, pred, pred);

		Value value = function.evaluate(f, subj, pred, obj);
		assertNotNull(value);
		assertTrue("expect Triple", value instanceof Triple);
		Triple other = f.createTriple(subj, pred, obj);
		assertEquals("expect to be the same", value, other);
	}

	@Test
	public void testEvaluateTripleAndLiteral() {
		IRI pred = f.createIRI("urn:b");
		Literal obj = f.createLiteral(1);
		Triple subj = f.createTriple(pred, pred, pred);

		Value value = function.evaluate(f, subj, pred, obj);
		assertNotNull(value);
		assertTrue("expect Triple", value instanceof Triple);
		Triple other = f.createTriple(subj, pred, obj);
		assertEquals("expect to be the same", value, other);
	}

	@Test
	public void testEvaluateTripleAndTriple() {
		IRI pred = f.createIRI("urn:b");
		Triple obj = f.createTriple(pred, pred, pred);
		Triple subj = f.createTriple(pred, pred, pred);

		Value value = function.evaluate(f, subj, pred, obj);
		assertNotNull(value);
		assertTrue("expect Triple", value instanceof Triple);
		Triple other = f.createTriple(subj, pred, obj);
		assertEquals("expect to be the same", value, other);
	}

	@Test(expected = ValueExprEvaluationException.class)
	public void testNegariveWrongNumberOfArguments() {
		IRI subj = f.createIRI("urn:a");
		IRI pred = f.createIRI("urn:b");
		IRI obj = f.createIRI("urn:c");

		function.evaluate(f, subj, pred, obj, null);
		fail("should throw ValueExprEvaluationException");
	}

	@Test(expected = ValueExprEvaluationException.class)
	public void testNegativeLiteralAsSubject() {
		Literal subj = f.createLiteral(1);
		IRI pred = f.createIRI("urn:b");
		IRI obj = f.createIRI("urn:c");

		function.evaluate(f, subj, pred, obj);
		fail("should not accept Literal as subject");
	}

	@Test(expected = ValueExprEvaluationException.class)
	public void testNegativeLiteralAsPredicate() {
		Literal subj = f.createLiteral(1);
		IRI pred = f.createIRI("urn:b");
		IRI obj = f.createIRI("urn:c");

		function.evaluate(f, subj, pred, obj);
		fail("should not accept Literal as predicate");
	}
}
