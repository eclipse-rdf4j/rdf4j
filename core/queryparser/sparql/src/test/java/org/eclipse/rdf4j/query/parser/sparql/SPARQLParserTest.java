/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Order;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.Slice;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.parser.ParsedBooleanQuery;
import org.eclipse.rdf4j.query.parser.ParsedGraphQuery;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.ParsedUpdate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jeen
 */
public class SPARQLParserTest {

	private SPARQLParser parser;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp()
		throws Exception
	{
		parser = new SPARQLParser();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown()
		throws Exception
	{
		parser = null;
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.query.parser.sparql.SPARQLParser#parseQuery(java.lang.String, java.lang.String)}
	 * .
	 */
	@Test
	public void testSourceStringAssignment()
		throws Exception
	{
		String simpleSparqlQuery = "SELECT * WHERE {?X ?P ?Y }";

		ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

		assertNotNull(q);
		assertEquals(simpleSparqlQuery, q.getSourceString());
	}

	@Test
	public void testInsertDataLineNumberReporting()
		throws Exception
	{
		String insertDataString = "INSERT DATA {\n incorrect reference }";

		try {
			ParsedUpdate u = parser.parseUpdate(insertDataString, null);
			fail("should have resulted in parse exception");
		}
		catch (MalformedQueryException e) {
			assertTrue(e.getMessage().contains("line 2,"));
		}

	}

	@Test
	public void testDeleteDataLineNumberReporting()
		throws Exception
	{
		String deleteDataString = "DELETE DATA {\n incorrect reference }";

		try {
			ParsedUpdate u = parser.parseUpdate(deleteDataString, null);
			fail("should have resulted in parse exception");
		}
		catch (MalformedQueryException e) {
			assertTrue(e.getMessage().contains("line 2,"));
		}
	}

	@Test
	public void testSES1922PathSequenceWithValueConstant()
		throws Exception
	{

		StringBuilder qb = new StringBuilder();
		qb.append("ASK {?A (<foo:bar>)/<foo:foo> <foo:objValue>} ");

		ParsedQuery q = parser.parseQuery(qb.toString(), null);
		TupleExpr te = q.getTupleExpr();

		assertNotNull(te);

		assertTrue(te instanceof Slice);
		Slice s = (Slice)te;
		assertTrue(s.getArg() instanceof Join);
		Join j = (Join)s.getArg();

		assertTrue(j.getLeftArg() instanceof StatementPattern);
		assertTrue(j.getRightArg() instanceof StatementPattern);
		StatementPattern leftArg = (StatementPattern)j.getLeftArg();
		StatementPattern rightArg = (StatementPattern)j.getRightArg();

		assertTrue(leftArg.getObjectVar().equals(rightArg.getSubjectVar()));
		assertEquals(leftArg.getObjectVar().getName(), rightArg.getSubjectVar().getName());
	}

	@Test
	public void testParsedBooleanQueryRootNode()
		throws Exception
	{
		StringBuilder qb = new StringBuilder();
		qb.append("ASK {?a <foo:bar> \"test\"}");

		ParsedBooleanQuery q = (ParsedBooleanQuery)parser.parseQuery(qb.toString(), null);
		TupleExpr te = q.getTupleExpr();

		assertNotNull(te);
		assertTrue(te instanceof Slice);
		assertNull(te.getParentNode());
	}

	@Test
	public void testParseIntegerObjectValue()
		throws Exception
	{
		// test that the parser correctly parses the object value as an integer, instead of as a decimal. 
		String query = "select ?Concept where { ?Concept a 1. ?Concept2 a 1. } ";
		ParsedTupleQuery q = (ParsedTupleQuery)parser.parseQuery(query, null);

		// all we're verifying is that the query is parsed without error. If it doesn't parse as integer but as a decimal, the
		// parser will fail, because the statement pattern doesn't end with a full-stop.
		assertNotNull(q);
	}

	@Test
	public void testParsedTupleQueryRootNode()
		throws Exception
	{
		StringBuilder qb = new StringBuilder();
		qb.append("SELECT *  {?a <foo:bar> \"test\"}");

		ParsedTupleQuery q = (ParsedTupleQuery)parser.parseQuery(qb.toString(), null);
		TupleExpr te = q.getTupleExpr();

		assertNotNull(te);
		assertTrue(te instanceof Projection);
		assertNull(te.getParentNode());
	}

	@Test
	public void testParsedGraphQueryRootNode()
		throws Exception
	{
		StringBuilder qb = new StringBuilder();
		qb.append("CONSTRUCT WHERE {?a <foo:bar> \"test\"}");

		ParsedGraphQuery q = (ParsedGraphQuery)parser.parseQuery(qb.toString(), null);
		TupleExpr te = q.getTupleExpr();

		assertNotNull(te);
		assertTrue(te instanceof Projection);
		assertNull(te.getParentNode());
	}

	@Test
	public void testOrderByWithAliases1()
		throws Exception
	{
		String queryString = " SELECT ?x (SUM(?v1)/COUNT(?v1) as ?r) WHERE { ?x <urn:foo> ?v1 } GROUP BY ?x ORDER BY ?r";

		ParsedQuery query = parser.parseQuery(queryString, null);

		assertNotNull(query);
		TupleExpr te = query.getTupleExpr();

		assertTrue(te instanceof Projection);

		te = ((Projection)te).getArg();

		assertTrue(te instanceof Order);

		te = ((Order)te).getArg();

		assertTrue(te instanceof Extension);
	}

	@Test
	public void testSES1927UnequalLiteralValueConstants1()
		throws Exception
	{

		StringBuilder qb = new StringBuilder();
		qb.append("ASK {?a <foo:bar> \"test\". ?a <foo:foo> \"test\"@en .} ");

		ParsedQuery q = parser.parseQuery(qb.toString(), null);
		TupleExpr te = q.getTupleExpr();

		assertNotNull(te);

		assertTrue(te instanceof Slice);
		Slice s = (Slice)te;
		assertTrue(s.getArg() instanceof Join);
		Join j = (Join)s.getArg();

		assertTrue(j.getLeftArg() instanceof StatementPattern);
		assertTrue(j.getRightArg() instanceof StatementPattern);
		StatementPattern leftArg = (StatementPattern)j.getLeftArg();
		StatementPattern rightArg = (StatementPattern)j.getRightArg();

		assertFalse(leftArg.getObjectVar().equals(rightArg.getObjectVar()));
		assertNotEquals(leftArg.getObjectVar().getName(), rightArg.getObjectVar().getName());
	}

	@Test
	public void testSES1927UnequalLiteralValueConstants2()
		throws Exception
	{

		StringBuilder qb = new StringBuilder();
		qb.append("ASK {?a <foo:bar> \"test\". ?a <foo:foo> \"test\"^^<foo:bar> .} ");

		ParsedQuery q = parser.parseQuery(qb.toString(), null);
		TupleExpr te = q.getTupleExpr();

		assertNotNull(te);

		assertTrue(te instanceof Slice);
		Slice s = (Slice)te;
		assertTrue(s.getArg() instanceof Join);
		Join j = (Join)s.getArg();

		assertTrue(j.getLeftArg() instanceof StatementPattern);
		assertTrue(j.getRightArg() instanceof StatementPattern);
		StatementPattern leftArg = (StatementPattern)j.getLeftArg();
		StatementPattern rightArg = (StatementPattern)j.getRightArg();

		assertFalse(leftArg.getObjectVar().equals(rightArg.getObjectVar()));
		assertNotEquals(leftArg.getObjectVar().getName(), rightArg.getObjectVar().getName());
	}

}
