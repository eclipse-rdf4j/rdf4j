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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.AbstractTupleQueryResultHandler;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.query.parser.sparql.manifest.SPARQL11ManifestTest;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A set of compliance tests on SPARQL query functionality which can not be
 * easily executed using the {@link SPARQL11ManifestTest} format. This includes
 * tests on queries with non-deterministic output (e.g. GROUP_CONCAT).
 * 
 * @author Jeen Broekstra
 */
public abstract class ComplexSPARQLQueryTest {

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	private Repository rep;

	protected RepositoryConnection conn;

	protected ValueFactory f;

	protected static final String EX_NS = "http://example.org/";

	private IRI bob;

	private IRI alice;

	private IRI mary;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp()
		throws Exception
	{
		logger.debug("setting up test");
		this.rep = newRepository();
		rep.initialize();

		f = rep.getValueFactory();
		conn = rep.getConnection();

		conn.clear(); // clear existing data from repo

		bob = f.createIRI(EX_NS, "bob");
		alice = f.createIRI(EX_NS, "alice");
		mary = f.createIRI(EX_NS, "mary");

		logger.debug("test setup complete.");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown()
		throws Exception
	{
		logger.debug("tearing down...");
		conn.close();
		conn = null;

		rep.shutDown();
		rep = null;

		logger.debug("tearDown complete.");
	}

	@Test
	public void testNullContext1()
		throws Exception
	{
		loadTestData("/testdata-query/dataset-query.trig");
		StringBuilder query = new StringBuilder();
		query.append(" SELECT * ");
		query.append(" FROM DEFAULT ");
		query.append(" WHERE { ?s ?p ?o } ");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try {
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);

			while (result.hasNext()) {
				BindingSet bs = result.next();
				assertNotNull(bs);

				Resource s = (Resource)bs.getValue("s");

				assertNotNull(s);
				assertFalse(bob.equals(s)); // should not be present in default
														// graph
				assertFalse(alice.equals(s)); // should not be present in default
														// graph
			}
			result.close();
		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testNullContext2()
		throws Exception
	{
		loadTestData("/testdata-query/dataset-query.trig");
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append(" SELECT * ");
		query.append(" FROM sesame:nil ");
		query.append(" WHERE { ?s ?p ?o } ");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try {
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);

			while (result.hasNext()) {
				BindingSet bs = result.next();
				assertNotNull(bs);

				Resource s = (Resource)bs.getValue("s");

				assertNotNull(s);
				assertFalse(bob.equals(s)); // should not be present in default
														// graph
				assertFalse(alice.equals(s)); // should not be present in default
														// graph
			}
			result.close();
		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testDescribeA()
		throws Exception
	{
		loadTestData("/testdata-query/dataset-describe.trig");
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("DESCRIBE ex:a");

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query.toString());

		ValueFactory f = conn.getValueFactory();
		IRI a = f.createIRI("http://example.org/a");
		IRI p = f.createIRI("http://example.org/p");
		Model result = QueryResults.asModel(gq.evaluate());
		Set<Value> objects = result.filter(a, p, null).objects();
		assertNotNull(objects);
		for (Value object : objects) {
			if (object instanceof BNode) {
				assertTrue(result.contains((Resource)object, null, null));
				assertEquals(2, result.filter((Resource)object, null, null).size());
			}
		}
	}

	@Test
	public void testDescribeAWhere()
		throws Exception
	{
		loadTestData("/testdata-query/dataset-describe.trig");
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("DESCRIBE ?x WHERE {?x rdfs:label \"a\". } ");

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query.toString());

		ValueFactory f = conn.getValueFactory();
		IRI a = f.createIRI("http://example.org/a");
		IRI p = f.createIRI("http://example.org/p");
		Model result = QueryResults.asModel(gq.evaluate());
		Set<Value> objects = result.filter(a, p, null).objects();
		assertNotNull(objects);
		for (Value object : objects) {
			if (object instanceof BNode) {
				assertTrue(result.contains((Resource)object, null, null));
				assertEquals(2, result.filter((Resource)object, null, null).size());
			}
		}
	}

	@Test
	public void testDescribeWhere()
		throws Exception
	{
		loadTestData("/testdata-query/dataset-describe.trig");
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("DESCRIBE ?x WHERE {?x rdfs:label ?y . } ");

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query.toString());

		ValueFactory vf = conn.getValueFactory();
		IRI a = vf.createIRI("http://example.org/a");
		IRI b = vf.createIRI("http://example.org/b");
		IRI c = vf.createIRI("http://example.org/c");
		IRI e = vf.createIRI("http://example.org/e");
		IRI f = vf.createIRI("http://example.org/f");
		IRI p = vf.createIRI("http://example.org/p");

		Model result = QueryResults.asModel(gq.evaluate());
		assertTrue(result.contains(a, p, null));
		assertTrue(result.contains(b, RDFS.LABEL, null));
		assertTrue(result.contains(c, RDFS.LABEL, null));
		assertTrue(result.contains(null, p, b));
		assertTrue(result.contains(e, RDFS.LABEL, null));
		assertTrue(result.contains(null, p, e));
		assertFalse(result.contains(f, null, null));
		Set<Value> objects = result.filter(a, p, null).objects();
		assertNotNull(objects);
		for (Value object : objects) {
			if (object instanceof BNode) {
				assertTrue(result.contains((Resource)object, null, null));
				assertEquals(2, result.filter((Resource)object, null, null).size());
			}
		}
	}

	@Test
	public void testDescribeB()
		throws Exception
	{
		loadTestData("/testdata-query/dataset-describe.trig");
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("DESCRIBE ex:b");

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query.toString());

		ValueFactory f = conn.getValueFactory();
		IRI b = f.createIRI("http://example.org/b");
		IRI p = f.createIRI("http://example.org/p");
		Model result = QueryResults.asModel(gq.evaluate());
		Set<Resource> subjects = result.filter(null, p, b).subjects();
		assertNotNull(subjects);
		for (Value subject : subjects) {
			if (subject instanceof BNode) {
				assertTrue(result.contains(null, null, subject));
			}
		}
	}

	@Test
	public void testDescribeD()
		throws Exception
	{
		loadTestData("/testdata-query/dataset-describe.trig");
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("DESCRIBE ex:d");

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query.toString());

		ValueFactory f = conn.getValueFactory();
		IRI d = f.createIRI("http://example.org/d");
		IRI p = f.createIRI("http://example.org/p");
		IRI e = f.createIRI("http://example.org/e");
		Model result = QueryResults.asModel(gq.evaluate());

		assertNotNull(result);
		assertTrue(result.contains(null, p, e));
		assertFalse(result.contains(e, null, null));
		Set<Value> objects = result.filter(d, p, null).objects();
		assertNotNull(objects);
		for (Value object : objects) {
			if (object instanceof BNode) {
				Set<Value> childObjects = result.filter((BNode)object, null, null).objects();
				assertNotNull(childObjects);
				for (Value childObject : childObjects) {
					if (childObject instanceof BNode) {
						assertTrue(result.contains((BNode)childObject, null, null));
					}
				}
			}
		}
	}

	@Test
	public void testDescribeF()
		throws Exception
	{
		loadTestData("/testdata-query/dataset-describe.trig");
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("DESCRIBE ex:f");

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query.toString());

		ValueFactory vf = conn.getValueFactory();
		IRI f = vf.createIRI("http://example.org/f");
		IRI p = vf.createIRI("http://example.org/p");
		Model result = QueryResults.asModel(gq.evaluate());

		assertNotNull(result);
		assertEquals(4, result.size());
		Set<Value> objects = result.filter(f, p, null).objects();
		assertNotNull(objects);
		for (Value object : objects) {
			if (object instanceof BNode) {
				Set<Value> childObjects = result.filter((BNode)object, null, null).objects();
				assertNotNull(childObjects);
				for (Value childObject : childObjects) {
					if (childObject instanceof BNode) {
						assertTrue(result.contains((BNode)childObject, null, null));
					}
				}
			}
		}
	}

	@Test
	public void testDescribeMultipleA()
		throws Exception
	{
		String update = "insert data { <urn:1> <urn:p1> <urn:v> . [] <urn:blank> <urn:1> . <urn:2> <urn:p2> <urn:3> . } ";
		conn.prepareUpdate(QueryLanguage.SPARQL, update).execute();

		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("DESCRIBE <urn:1> <urn:2> ");

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query.toString());

		ValueFactory vf = conn.getValueFactory();
		IRI urn1 = vf.createIRI("urn:1");
		IRI p1 = vf.createIRI("urn:p1");
		IRI p2 = vf.createIRI("urn:p2");
		IRI urn2 = vf.createIRI("urn:2");
		IRI blank = vf.createIRI("urn:blank");

		Model result = QueryResults.asModel(gq.evaluate());
		assertTrue(result.contains(urn1, p1, null));
		assertTrue(result.contains(null, blank, urn1));
		assertTrue(result.contains(urn2, p2, null));
	}

	@Test
	public void testDescribeMultipleB()
		throws Exception
	{
		String update = "insert data { <urn:1> <urn:p1> <urn:v> . <urn:1> <urn:blank> [] . <urn:2> <urn:p2> <urn:3> . } ";
		conn.prepareUpdate(QueryLanguage.SPARQL, update).execute();

		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("DESCRIBE <urn:1> <urn:2> ");

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query.toString());

		ValueFactory vf = conn.getValueFactory();
		IRI urn1 = vf.createIRI("urn:1");
		IRI p1 = vf.createIRI("urn:p1");
		IRI p2 = vf.createIRI("urn:p2");
		IRI urn2 = vf.createIRI("urn:2");
		IRI blank = vf.createIRI("urn:blank");
		Model result = QueryResults.asModel(gq.evaluate());

		assertTrue(result.contains(urn1, p1, null));
		assertTrue(result.contains(urn1, blank, null));
		assertTrue(result.contains(urn2, p2, null));
	}

	@Test
	public void testDescribeMultipleC()
		throws Exception
	{
		String update = "insert data { <urn:1> <urn:p1> <urn:v> . [] <urn:blank> <urn:1>. <urn:1> <urn:blank> [] . <urn:2> <urn:p2> <urn:3> . } ";
		conn.prepareUpdate(QueryLanguage.SPARQL, update).execute();

		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("DESCRIBE <urn:1> <urn:2> ");

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query.toString());

		ValueFactory vf = conn.getValueFactory();
		IRI urn1 = vf.createIRI("urn:1");
		IRI p1 = vf.createIRI("urn:p1");
		IRI p2 = vf.createIRI("urn:p2");
		IRI urn2 = vf.createIRI("urn:2");
		IRI blank = vf.createIRI("urn:blank");
		Model result = QueryResults.asModel(gq.evaluate());

		assertTrue(result.contains(urn1, p1, null));
		assertTrue(result.contains(urn1, blank, null));
		assertTrue(result.contains(null, blank, urn1));
		assertTrue(result.contains(urn2, p2, null));
	}

	@Test
	public void testDescribeMultipleD()
		throws Exception
	{
		String update = "insert data { <urn:1> <urn:p1> <urn:v> . [] <urn:blank> <urn:1>. <urn:2> <urn:p2> <urn:3> . [] <urn:blank> <urn:2> . <urn:4> <urn:p2> <urn:3> . <urn:4> <urn:blank> [] .} ";
		conn.prepareUpdate(QueryLanguage.SPARQL, update).execute();

		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("DESCRIBE <urn:1> <urn:2> <urn:4> ");

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query.toString());

		ValueFactory vf = conn.getValueFactory();
		IRI urn1 = vf.createIRI("urn:1");
		IRI p1 = vf.createIRI("urn:p1");
		IRI p2 = vf.createIRI("urn:p2");
		IRI urn2 = vf.createIRI("urn:2");
		IRI urn4 = vf.createIRI("urn:4");
		IRI blank = vf.createIRI("urn:blank");
		Model result = QueryResults.asModel(gq.evaluate());

		assertTrue(result.contains(urn1, p1, null));
		assertTrue(result.contains(null, blank, urn1));
		assertTrue(result.contains(urn2, p2, null));
		assertTrue(result.contains(urn4, p2, null));
		assertTrue(result.contains(urn4, blank, null));
	}

	@Test
	public void testGroupConcatDistinct()
		throws Exception
	{
		loadTestData("/testdata-query/dataset-query.trig");

		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("SELECT (GROUP_CONCAT(DISTINCT ?l) AS ?concat)");
		query.append("WHERE { ex:groupconcat-test ?p ?l . }");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try {
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);

			while (result.hasNext()) {
				BindingSet bs = result.next();
				assertNotNull(bs);

				Value concat = bs.getValue("concat");

				assertTrue(concat instanceof Literal);

				String lexValue = ((Literal)concat).getLabel();

				int occ = countCharOccurrences(lexValue, 'a');
				assertEquals(1, occ);
				occ = countCharOccurrences(lexValue, 'b');
				assertEquals(1, occ);
				occ = countCharOccurrences(lexValue, 'c');
				assertEquals(1, occ);
				occ = countCharOccurrences(lexValue, 'd');
				assertEquals(1, occ);
			}
			result.close();
		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testSameTermRepeatInOptional()
		throws Exception
	{
		loadTestData("/testdata-query/dataset-query.trig");
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append(" SELECT ?l ?opt1 ?opt2 ");
		query.append(" FROM ex:optional-sameterm-graph ");
		query.append(" WHERE { ");
		query.append("          ?s ex:p ex:A ; ");
		query.append("          { ");
		query.append("              { ");
		query.append("                 ?s ?p ?l .");
		query.append("                 FILTER(?p = rdfs:label) ");
		query.append("              } ");
		query.append("              OPTIONAL { ");
		query.append("                 ?s ?p ?opt1 . ");
		query.append("                 FILTER (?p = ex:prop1) ");
		query.append("              } ");
		query.append("              OPTIONAL { ");
		query.append("                 ?s ?p ?opt2 . ");
		query.append("                 FILTER (?p = ex:prop2) ");
		query.append("              } ");
		query.append("          }");
		query.append(" } ");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try {
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				BindingSet bs = result.next();
				count++;
				assertNotNull(bs);

				System.out.println(bs);

				Value l = bs.getValue("l");
				assertTrue(l instanceof Literal);
				assertEquals("label", ((Literal)l).getLabel());

				Value opt1 = bs.getValue("opt1");
				assertNull(opt1);

				Value opt2 = bs.getValue("opt2");
				assertNull(opt2);
			}
			result.close();

			assertEquals(1, count);
		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testSES1121VarNamesInOptionals()
		throws Exception
	{
		// Verifying that variable names have no influence on order of optionals
		// in query. See SES-1121.

		loadTestData("/testdata-query/dataset-ses1121.trig");

		StringBuilder query1 = new StringBuilder();
		query1.append(getNamespaceDeclarations());
		query1.append(" SELECT DISTINCT *\n");
		query1.append(" WHERE { GRAPH ?g { \n");
		query1.append("          OPTIONAL { ?var35 ex:p ?b . } \n ");
		query1.append("          OPTIONAL { ?b ex:q ?c . } \n ");
		query1.append("       } \n");
		query1.append(" } \n");

		StringBuilder query2 = new StringBuilder();
		query2.append(getNamespaceDeclarations());
		query2.append(" SELECT DISTINCT *\n");
		query2.append(" WHERE { GRAPH ?g { \n");
		query2.append("          OPTIONAL { ?var35 ex:p ?b . } \n ");
		query2.append("          OPTIONAL { ?b ex:q ?var2 . } \n ");
		query2.append("       } \n");
		query2.append(" } \n");

		TupleQuery tq1 = conn.prepareTupleQuery(QueryLanguage.SPARQL, query1.toString());
		TupleQuery tq2 = conn.prepareTupleQuery(QueryLanguage.SPARQL, query2.toString());

		try {
			TupleQueryResult result1 = tq1.evaluate();
			assertNotNull(result1);

			TupleQueryResult result2 = tq2.evaluate();
			assertNotNull(result2);

			List<BindingSet> qr1 = QueryResults.asList(result1);
			List<BindingSet> qr2 = QueryResults.asList(result2);

			// System.out.println(qr1);
			// System.out.println(qr2);

			// if optionals are not kept in same order, query results will be
			// different size.
			assertEquals(qr1.size(), qr2.size());

		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testSES1081SameTermWithValues()
		throws Exception
	{
		loadTestData("/testdata-query/dataset-ses1081.trig");
		StringBuilder query = new StringBuilder();
		query.append("PREFIX ex: <http://example.org/>\n");
		query.append(" SELECT * \n");
		query.append(" WHERE { \n ");
		query.append("          ?s ex:p ?a . \n");
		query.append("          FILTER sameTerm(?a, ?e) \n ");
		query.append("          VALUES ?e { ex:b } \n ");
		query.append(" } ");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try {
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				BindingSet bs = result.next();
				count++;
				assertNotNull(bs);

				Value s = bs.getValue("s");
				Value a = bs.getValue("a");

				assertNotNull(s);
				assertNotNull(a);
				assertEquals(f.createIRI("http://example.org/a"), s);
				assertEquals(f.createIRI("http://example.org/b"), a);
			}
			result.close();

			assertEquals(1, count);
		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testSES1898LeftJoinSemantics1()
		throws Exception
	{
		loadTestData("/testdata-query/dataset-ses1898.trig");
		StringBuilder query = new StringBuilder();
		query.append("  PREFIX : <http://example.org/> ");
		query.append("  SELECT * WHERE { ");
		query.append("    ?s :p1 ?v1 . ");
		query.append("    OPTIONAL {?s :p2 ?v2 } .");
		query.append("     ?s :p3 ?v2 . ");
		query.append("  } ");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try {
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				result.next();
				count++;
			}
			assertEquals(0, count);
		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testSES1073InverseSymmetricPattern()
		throws Exception
	{
		IRI a = f.createIRI("http://example.org/a");
		IRI b1 = f.createIRI("http://example.org/b1");
		IRI b2 = f.createIRI("http://example.org/b2");
		IRI c1 = f.createIRI("http://example.org/c1");
		IRI c2 = f.createIRI("http://example.org/c2");
		IRI a2b = f.createIRI("http://example.org/a2b");
		IRI b2c = f.createIRI("http://example.org/b2c");
		conn.add(a, a2b, b1);
		conn.add(a, a2b, b2);
		conn.add(b1, b2c, c1);
		conn.add(b2, b2c, c2);
		String query = "select * ";
		query += "where{ ";
		query += "?c1 ^<http://example.org/b2c>/^<http://example.org/a2b>/<http://example.org/a2b>/<http://example.org/b2c> ?c2 . ";
		query += " } ";
		TupleQueryResult qRes = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate();
		try {
			assertTrue(qRes.hasNext());
			int count = 0;
			while (qRes.hasNext()) {
				BindingSet r = qRes.next();
				System.out.println(r);
				count++;
			}
			assertEquals(4, count);
		}
		finally {
			qRes.close();
		}
	}

	@Test
	public void testSES1970CountDistinctWildcard()
		throws Exception
	{
		loadTestData("/testdata-query/dataset-ses1970.trig");

		String query = "SELECT (COUNT(DISTINCT *) AS ?c) {?s ?p ?o }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try {
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);

			assertTrue(result.hasNext());
			BindingSet s = result.next();
			Literal count = (Literal)s.getValue("c");
			assertNotNull(count);

			assertEquals(3, count.intValue());

			result.close();
		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testSES1685propPathSameVar()
		throws Exception
	{
		final String queryStr = "PREFIX : <urn:> SELECT ?x WHERE {?x :p+ ?x}";

		conn.add(new StringReader("@prefix : <urn:> . :a :p :b . :b :p :a ."), "", RDFFormat.TURTLE);

		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryStr);
		TupleQueryResult result = query.evaluate();

		assertNotNull(result);

		int count = 0;
		while (result.hasNext()) {
			result.next();
			count++;
		}
		// result should be both a and b.
		assertEquals(2, count);
	}

	@Test
	public void testSES2104ConstructBGPSameURI()
		throws Exception
	{
		final String queryStr = "PREFIX : <urn:> CONSTRUCT {:x :p :x } WHERE {} ";

		conn.add(new StringReader("@prefix : <urn:> . :a :p :b . "), "", RDFFormat.TURTLE);

		final IRI x = conn.getValueFactory().createIRI("urn:x");
		final IRI p = conn.getValueFactory().createIRI("urn:p");

		GraphQuery query = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryStr);
		Model result = QueryResults.asModel(query.evaluate());

		assertNotNull(result);
		assertFalse(result.isEmpty());
		assertTrue(result.contains(x, p, x));

	}

	@Test
	public void testSES1898LeftJoinSemantics2()
		throws Exception
	{
		loadTestData("/testdata-query/dataset-ses1898.trig");
		StringBuilder query = new StringBuilder();
		query.append("  PREFIX : <http://example.org/> ");
		query.append("  SELECT * WHERE { ");
		query.append("    ?s :p1 ?v1 . ");
		query.append("    ?s :p3 ?v2 . ");
		query.append("    OPTIONAL {?s :p2 ?v2 } .");
		query.append("  } ");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try {
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				result.next();
				count++;
			}
			assertEquals(1, count);
		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testIdenticalVariablesInStatementPattern()
		throws Exception
	{
		conn.add(alice, f.createIRI("http://purl.org/dc/elements/1.1/publisher"), bob);

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append("SELECT ?publisher ");
		queryBuilder.append("{ ?publisher <http://purl.org/dc/elements/1.1/publisher> ?publisher }");

		conn.prepareTupleQuery(QueryLanguage.SPARQL, queryBuilder.toString()).evaluate(
				new AbstractTupleQueryResultHandler() {

					public void handleSolution(BindingSet bindingSet) {
						fail("nobody is self published");
					}
				});
	}

	@Test
	public void testInComparison1()
		throws Exception
	{
		loadTestData("/testdata-query/dataset-ses1913.trig");
		StringBuilder query = new StringBuilder();
		query.append(" PREFIX : <http://example.org/>\n");
		query.append(" SELECT ?y WHERE { :a :p ?y. FILTER(?y in (:c, :d, 1/0 , 1)) } ");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		TupleQueryResult result = tq.evaluate();
		assertNotNull(result);
		assertTrue(result.hasNext());

		BindingSet bs = result.next();
		Value y = bs.getValue("y");
		assertNotNull(y);
		assertTrue(y instanceof Literal);
		assertEquals(f.createLiteral("1", XMLSchema.INTEGER), y);

	}

	@Test
	public void testInComparison2()
		throws Exception
	{
		loadTestData("/testdata-query/dataset-ses1913.trig");
		StringBuilder query = new StringBuilder();
		query.append(" PREFIX : <http://example.org/>\n");
		query.append(" SELECT ?y WHERE { :a :p ?y. FILTER(?y in (:c, :d, 1/0)) } ");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		TupleQueryResult result = tq.evaluate();
		assertNotNull(result);
		assertFalse(result.hasNext());

	}

	@Test
	public void testInComparison3()
		throws Exception
	{
		loadTestData("/testdata-query/dataset-ses1913.trig");
		StringBuilder query = new StringBuilder();
		query.append(" PREFIX : <http://example.org/>\n");
		query.append(" SELECT ?y WHERE { :a :p ?y. FILTER(?y in (:c, :d, 1, 1/0)) } ");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		TupleQueryResult result = tq.evaluate();
		assertNotNull(result);
		assertTrue(result.hasNext());

		BindingSet bs = result.next();
		Value y = bs.getValue("y");
		assertNotNull(y);
		assertTrue(y instanceof Literal);
		assertEquals(f.createLiteral("1", XMLSchema.INTEGER), y);
	}

	@Test
	public void testSES2121URIFunction()
		throws Exception
	{
		String query = "SELECT (URI(\"foo bar\") as ?uri) WHERE {}";
		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
		TupleQueryResult result = tq.evaluate();
		assertNotNull(result);
		assertTrue(result.hasNext());
		BindingSet bs = result.next();
		IRI uri = (IRI)bs.getValue("uri");
		assertTrue("uri result for invalid URI should be unbound", uri == null);

		query = "BASE <http://example.org/> SELECT (URI(\"foo bar\") as ?uri) WHERE {}";
		tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
		result = tq.evaluate();
		assertNotNull(result);
		assertTrue(result.hasNext());
		bs = result.next();
		uri = (IRI)bs.getValue("uri");
		assertTrue("uri result for valid URI reference should be bound", uri != null);
	}

	@Test
	public void testSES869ValueOfNow()
		throws Exception
	{
		StringBuilder query = new StringBuilder();
		query.append("SELECT ?p ( NOW() as ?n ) { BIND (NOW() as ?p ) }");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		TupleQueryResult result = tq.evaluate();
		assertNotNull(result);
		assertTrue(result.hasNext());

		BindingSet bs = result.next();
		Value p = bs.getValue("p");
		Value n = bs.getValue("n");

		assertNotNull(p);
		assertNotNull(n);
		assertEquals(p, n);

	}

	@Test
	public void testSES2136()
		throws Exception
	{
		loadTestData("/testcases-sparql-1.1-w3c/bindings/data02.ttl");
		StringBuilder query = new StringBuilder();
		query.append("PREFIX : <http://example.org/>\n");
		query.append("SELECT ?s ?o { \n");
		query.append(" { SELECT * WHERE { ?s ?p ?o . } }\n");
		query.append("	VALUES (?o) { (:b) }\n");
		query.append("}\n");

		ValueFactory vf = conn.getValueFactory();
		final IRI a = vf.createIRI("http://example.org/a");
		final IRI b = vf.createIRI("http://example.org/b");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		TupleQueryResult result = tq.evaluate();
		assertNotNull(result);
		assertTrue(result.hasNext());
		BindingSet bs = result.next();
		assertFalse("only one result expected", result.hasNext());
		assertEquals(a, bs.getValue("s"));
		assertEquals(b, bs.getValue("o"));
	}

	@Test
	public void testValuesInOptional()
		throws Exception
	{
		loadTestData("/testdata-query/dataset-ses1692.trig");
		StringBuilder query = new StringBuilder();
		query.append(" PREFIX : <http://example.org/>\n");
		query.append(" SELECT DISTINCT ?a ?name ?isX WHERE { ?b :p1 ?a . ?a :name ?name. OPTIONAL { ?a a :X . VALUES(?isX) { (:X) } } } ");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		TupleQueryResult result = tq.evaluate();
		assertNotNull(result);
		assertTrue(result.hasNext());

		int count = 0;
		while (result.hasNext()) {
			count++;
			BindingSet bs = result.next();
			System.out.println(bs);
			IRI a = (IRI)bs.getValue("a");
			assertNotNull(a);
			Value isX = bs.getValue("isX");
			Literal name = (Literal)bs.getValue("name");
			assertNotNull(name);
			if (a.stringValue().endsWith("a1")) {
				assertNotNull(isX);
			}
			else if (a.stringValue().endsWith(("a2"))) {
				assertNull(isX);
			}
		}
		assertEquals(2, count);
	}

	@Test
	public void testSES2052If1()
		throws Exception
	{
		loadTestData("/testdata-query/dataset-query.trig");
		StringBuilder query = new StringBuilder();
		query.append("SELECT ?p \n");
		query.append("WHERE { \n");
		query.append("         ?s ?p ?o . \n");
		query.append("        FILTER(IF(BOUND(?p), ?p = <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>, false)) \n");
		query.append("}");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
		try {
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);
			while (result.hasNext()) {
				BindingSet bs = result.next();

				IRI p = (IRI)bs.getValue("p");
				assertNotNull(p);
				assertEquals(RDF.TYPE, p);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testSES2052If2()
		throws Exception
	{
		loadTestData("/testdata-query/dataset-query.trig");
		StringBuilder query = new StringBuilder();
		query.append("SELECT ?p \n");
		query.append("WHERE { \n");
		query.append("         ?s ?p ?o . \n");
		query.append("        FILTER(IF(!BOUND(?p), false , ?p = <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>)) \n");
		query.append("}");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
		try {
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);
			while (result.hasNext()) {
				BindingSet bs = result.next();

				IRI p = (IRI)bs.getValue("p");
				assertNotNull(p);
				assertEquals(RDF.TYPE, p);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testSameTermRepeatInUnion()
		throws Exception
	{
		loadTestData("/testdata-query/dataset-query.trig");
		StringBuilder query = new StringBuilder();
		query.append("PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n");
		query.append("SELECT * {\n");
		query.append("    {\n");
		query.append("        ?sameTerm foaf:mbox ?mbox\n");
		query.append("        FILTER sameTerm(?sameTerm,$william)\n");
		query.append("    } UNION {\n");
		query.append("        ?x foaf:knows ?sameTerm\n");
		query.append("        FILTER sameTerm(?sameTerm,$william)\n");
		query.append("    }\n");
		query.append("}");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
		tq.setBinding("william", conn.getValueFactory().createIRI("http://example.org/william"));

		try {
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				BindingSet bs = result.next();
				count++;
				assertNotNull(bs);

				System.out.println(bs);

				Value mbox = bs.getValue("mbox");
				Value x = bs.getValue("x");

				assertTrue(mbox instanceof Literal || x instanceof IRI);
			}
			result.close();

			assertEquals(3, count);
		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testSameTermRepeatInUnionAndOptional()
		throws Exception
	{
		loadTestData("/testdata-query/dataset-query.trig");

		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("SELECT * {\n");
		query.append("    {\n");
		query.append("        ex:a ?p ?prop1\n");
		query.append("        FILTER (?p = ex:prop1)\n");
		query.append("    } UNION {\n");
		query.append("          ?s ex:p ex:A ; ");
		query.append("          { ");
		query.append("              { ");
		query.append("                 ?s ?p ?l .");
		query.append("                 FILTER(?p = rdfs:label) ");
		query.append("              } ");
		query.append("              OPTIONAL { ");
		query.append("                 ?s ?p ?opt1 . ");
		query.append("                 FILTER (?p = ex:prop1) ");
		query.append("              } ");
		query.append("              OPTIONAL { ");
		query.append("                 ?s ?p ?opt2 . ");
		query.append("                 FILTER (?p = ex:prop2) ");
		query.append("              } ");
		query.append("          }");
		query.append("    }\n");
		query.append("}");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try {
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				BindingSet bs = result.next();
				count++;
				assertNotNull(bs);

				System.out.println(bs);

				Value prop1 = bs.getValue("prop1");
				Value l = bs.getValue("l");

				assertTrue(prop1 instanceof Literal || l instanceof Literal);
				if (l instanceof Literal) {
					Value opt1 = bs.getValue("opt1");
					assertNull(opt1);

					Value opt2 = bs.getValue("opt2");
					assertNull(opt2);
				}
			}
			result.close();

			assertEquals(2, count);
		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testPropertyPathInTree()
		throws Exception
	{
		loadTestData("/testdata-query/dataset-query.trig");

		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append(" SELECT ?node ?name ");
		query.append(" FROM ex:tree-graph ");
		query.append(" WHERE { ?node ex:hasParent+ ex:b . ?node ex:name ?name . }");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try {
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);

			while (result.hasNext()) {
				BindingSet bs = result.next();
				assertNotNull(bs);

				System.out.println(bs);

			}
			result.close();
		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testFilterRegexBoolean()
		throws Exception
	{
		loadTestData("/testdata-query/dataset-query.trig");

		// test case for issue SES-1050
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append(" SELECT *");
		query.append(" WHERE { ");
		query.append("       ?x foaf:name ?name ; ");
		query.append("          foaf:mbox ?mbox . ");
		query.append("       FILTER(EXISTS { ");
		query.append("            FILTER(REGEX(?name, \"Bo\") && REGEX(?mbox, \"bob\")) ");
		// query.append("            FILTER(REGEX(?mbox, \"bob\")) ");
		query.append("            } )");
		query.append(" } ");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try {
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);

			assertTrue(result.hasNext());
			int count = 0;
			while (result.hasNext()) {
				BindingSet bs = result.next();
				count++;
				System.out.println(bs);
			}
			assertEquals(1, count);

			result.close();
		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testGroupConcatNonDistinct()
		throws Exception
	{
		loadTestData("/testdata-query/dataset-query.trig");
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("SELECT (GROUP_CONCAT(?l) AS ?concat)");
		query.append("WHERE { ex:groupconcat-test ?p ?l . }");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try {
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);

			while (result.hasNext()) {
				BindingSet bs = result.next();
				assertNotNull(bs);

				Value concat = bs.getValue("concat");

				assertTrue(concat instanceof Literal);

				String lexValue = ((Literal)concat).getLabel();

				int occ = countCharOccurrences(lexValue, 'a');
				assertEquals(1, occ);
				occ = countCharOccurrences(lexValue, 'b');
				assertEquals(2, occ);
				occ = countCharOccurrences(lexValue, 'c');
				assertEquals(2, occ);
				occ = countCharOccurrences(lexValue, 'd');
				assertEquals(1, occ);
			}
			result.close();
		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	/**
	 * @see http://www.openrdf.org/issues/browse/SES-1091
	 * @throws Exception
	 */
	public void testArbitraryLengthPathWithBinding1()
		throws Exception
	{
		loadTestData("/testdata-query/alp-testdata.ttl");
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("SELECT ?parent ?child ");
		query.append("WHERE { ?child a owl:Class . ?child rdfs:subClassOf+ ?parent . }");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try {
			// first execute without binding
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();
				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(7, count);

			// execute again, but this time setting a binding
			tq.setBinding("parent", OWL.THING);

			result = tq.evaluate();
			assertNotNull(result);

			count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();
				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(4, count);
		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	/**
	 * @see http://www.openrdf.org/issues/browse/SES-1091
	 * @throws Exception
	 */
	public void testArbitraryLengthPathWithBinding2()
		throws Exception
	{
		loadTestData("/testdata-query/alp-testdata.ttl");

		// query without initializing ?child first.
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("SELECT ?parent ?child ");
		query.append("WHERE { ?child rdfs:subClassOf+ ?parent . }");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try {
			// first execute without binding
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();
				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(7, count);

			// execute again, but this time setting a binding
			tq.setBinding("parent", OWL.THING);

			result = tq.evaluate();
			assertNotNull(result);

			count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();
				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(4, count);
		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	/**
	 * @see http://www.openrdf.org/issues/browse/SES-1091
	 * @throws Exception
	 */
	public void testArbitraryLengthPathWithBinding3()
		throws Exception
	{
		loadTestData("/testdata-query/alp-testdata.ttl");

		// binding on child instead of parent.
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("SELECT ?parent ?child ");
		query.append("WHERE { ?child rdfs:subClassOf+ ?parent . }");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try {
			// first execute without binding
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();
				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(7, count);

			// execute again, but this time setting a binding
			tq.setBinding("child", f.createIRI(EX_NS, "C"));

			result = tq.evaluate();
			assertNotNull(result);

			count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();
				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(2, count);
		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	/**
	 * @see http://www.openrdf.org/issues/browse/SES-1091
	 * @throws Exception
	 */
	public void testArbitraryLengthPathWithBinding4()
		throws Exception
	{
		loadTestData("/testdata-query/alp-testdata.ttl", this.alice);

		// binding on child instead of parent.
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("SELECT ?parent ?child ");
		query.append("WHERE { ?child rdfs:subClassOf+ ?parent . }");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try {
			// first execute without binding
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();
				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(7, count);

			// execute again, but this time setting a binding
			tq.setBinding("child", f.createIRI(EX_NS, "C"));

			result = tq.evaluate();
			assertNotNull(result);

			count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();
				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(2, count);
		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	/**
	 * @see http://www.openrdf.org/issues/browse/SES-1091
	 * @throws Exception
	 */
	public void testArbitraryLengthPathWithBinding5()
		throws Exception
	{
		loadTestData("/testdata-query/alp-testdata.ttl", this.alice, this.bob);

		// binding on child instead of parent.
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("SELECT ?parent ?child ");
		query.append("WHERE { ?child rdfs:subClassOf+ ?parent . }");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try {
			// first execute without binding
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);

			System.out.println("--- testArbitraryLengthPathWithBinding5 ---");

			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();

				System.out.println(bs);

				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(7, count);

			// execute again, but this time setting a binding
			tq.setBinding("child", f.createIRI(EX_NS, "C"));

			result = tq.evaluate();
			assertNotNull(result);

			count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();
				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(2, count);
		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	/**
	 * @see http://www.openrdf.org/issues/browse/SES-1091
	 * @throws Exception
	 */
	public void testArbitraryLengthPathWithBinding6()
		throws Exception
	{
		loadTestData("/testdata-query/alp-testdata.ttl", this.alice, this.bob, this.mary);

		// binding on child instead of parent.
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("SELECT ?parent ?child ");
		query.append("WHERE { ?child rdfs:subClassOf+ ?parent . }");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try {
			// first execute without binding
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);

			System.out.println("--- testArbitraryLengthPathWithBinding6 ---");

			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();

				System.out.println(bs);

				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(7, count);

			// execute again, but this time setting a binding
			tq.setBinding("child", f.createIRI(EX_NS, "C"));

			result = tq.evaluate();
			assertNotNull(result);

			count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();
				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(2, count);
		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	/**
	 * @see http://www.openrdf.org/issues/browse/SES-1091
	 * @throws Exception
	 */
	public void testArbitraryLengthPathWithBinding7()
		throws Exception
	{
		loadTestData("/testdata-query/alp-testdata.ttl", this.alice, this.bob, this.mary);

		// binding on child instead of parent.
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("SELECT ?parent ?child ");
		query.append("WHERE { ?child rdfs:subClassOf+ ?parent . }");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
		SimpleDataset dt = new SimpleDataset();
		dt.addDefaultGraph(this.alice);
		tq.setDataset(dt);

		try {
			// first execute without binding
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);

			System.out.println("--- testArbitraryLengthPathWithBinding7 ---");

			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();

				System.out.println(bs);

				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(7, count);

			// execute again, but this time setting a binding
			tq.setBinding("child", f.createIRI(EX_NS, "C"));

			result = tq.evaluate();
			assertNotNull(result);

			count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();
				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(2, count);
		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	/**
	 * @see http://www.openrdf.org/issues/browse/SES-1091
	 * @throws Exception
	 */
	public void testArbitraryLengthPathWithBinding8()
		throws Exception
	{
		loadTestData("/testdata-query/alp-testdata.ttl", this.alice, this.bob, this.mary);

		// binding on child instead of parent.
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("SELECT ?parent ?child ");
		query.append("WHERE { ?child rdfs:subClassOf+ ?parent . }");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
		SimpleDataset dt = new SimpleDataset();
		dt.addDefaultGraph(this.alice);
		dt.addDefaultGraph(this.bob);
		tq.setDataset(dt);

		try {
			// first execute without binding
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);
			System.out.println("--- testArbitraryLengthPathWithBinding8 ---");
			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();

				System.out.println(bs);

				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(7, count);

			// execute again, but this time setting a binding
			tq.setBinding("child", f.createIRI(EX_NS, "C"));

			result = tq.evaluate();
			assertNotNull(result);

			count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();
				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(2, count);
		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	/**
	 * @see http://www.openrdf.org/issues/browse/SES-1091
	 * @throws Exception
	 */
	public void testArbitraryLengthPathWithFilter1()
		throws Exception
	{
		loadTestData("/testdata-query/alp-testdata.ttl");
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("SELECT ?parent ?child ");
		query.append("WHERE { ?child a owl:Class . ?child rdfs:subClassOf+ ?parent . FILTER (?parent = owl:Thing) }");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try {
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();
				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(4, count);
		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	/**
	 * @see http://www.openrdf.org/issues/browse/SES-1091
	 * @throws Exception
	 */
	public void testArbitraryLengthPathWithFilter2()
		throws Exception
	{
		loadTestData("/testdata-query/alp-testdata.ttl");
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("SELECT ?parent ?child ");
		query.append("WHERE { ?child rdfs:subClassOf+ ?parent . FILTER (?parent = owl:Thing) }");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try {
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();
				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(4, count);
		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	/**
	 * @see http://www.openrdf.org/issues/browse/SES-1091
	 * @throws Exception
	 */
	public void testArbitraryLengthPathWithFilter3()
		throws Exception
	{
		loadTestData("/testdata-query/alp-testdata.ttl");
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("SELECT ?parent ?child ");
		query.append("WHERE { ?child rdfs:subClassOf+ ?parent . FILTER (?child = <http://example.org/C>) }");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try {
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();
				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(2, count);
		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testSES2147PropertyPathsWithIdenticalSubsPreds()
		throws Exception
	{

		StringBuilder data = new StringBuilder();
		data.append("<urn:s1> <urn:p> <urn:s2> .\n");
		data.append("<urn:s2> <urn:p> <urn:s3> .\n");
		data.append("<urn:s3> <urn:q> <urn:s4> .\n");
		data.append("<urn:s1> <urn:p> <urn:s5> .\n");
		data.append("<urn:s5> <urn:q> <urn:s6> .\n");

		conn.begin();
		conn.add(new StringReader(data.toString()), "", RDFFormat.NTRIPLES);
		conn.commit();

		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("SELECT ?x \n");
		query.append("WHERE { ?x <urn:p>*/<urn:q> <urn:s4> . \n");
		query.append("        ?x <urn:p>*/<urn:q> <urn:s6> . \n");
		query.append("} \n");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try {
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);
			assertTrue(result.hasNext());

			Value x = result.next().getValue("x");
			assertNotNull(x);
			assertTrue(x instanceof IRI);
			assertEquals("urn:s1", x.stringValue());
		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testSES1991UUIDEvaluation()
		throws Exception
	{
		loadTestData("/testdata-query/defaultgraph.ttl");
		String query = "SELECT ?uid WHERE {?s ?p ?o . BIND(UUID() as ?uid) } LIMIT 2";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try {
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);

			IRI uuid1 = (IRI)result.next().getValue("uid");
			IRI uuid2 = (IRI)result.next().getValue("uid");

			assertNotNull(uuid1);
			assertNotNull(uuid2);
			assertFalse(uuid1.equals(uuid2));

			result.close();
		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testSES1991STRUUIDEvaluation()
		throws Exception
	{
		loadTestData("/testdata-query/defaultgraph.ttl");
		String query = "SELECT ?uid WHERE {?s ?p ?o . BIND(STRUUID() as ?uid) } LIMIT 2";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try {
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);

			Literal uid1 = (Literal)result.next().getValue("uid");
			Literal uid2 = (Literal)result.next().getValue("uid");

			assertNotNull(uid1);
			assertFalse(uid1.equals(uid2));

			result.close();
		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testSES1991RANDEvaluation()
		throws Exception
	{
		loadTestData("/testdata-query/defaultgraph.ttl");
		String query = "SELECT ?r WHERE {?s ?p ?o . BIND(RAND() as ?r) } LIMIT 3";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try {
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);

			Literal r1 = (Literal)result.next().getValue("r");
			Literal r2 = (Literal)result.next().getValue("r");
			Literal r3 = (Literal)result.next().getValue("r");

			assertNotNull(r1);

			// there is a small chance that two successive calls to the random
			// number generator will generate the exact same value, so we check for
			// three successive calls (still theoretically possible to be
			// identical, but phenomenally unlikely).
			assertFalse(r1.equals(r2) && r1.equals(r3));

			result.close();
		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testSES1991NOWEvaluation()
		throws Exception
	{
		loadTestData("/testdata-query/defaultgraph.ttl");
		String query = "SELECT ?d WHERE {?s ?p ?o . BIND(NOW() as ?d) } LIMIT 2";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try {
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);

			Literal d1 = (Literal)result.next().getValue("d");
			Literal d2 = (Literal)result.next().getValue("d");

			assertNotNull(d1);
			assertEquals(d1, d2);

			result.close();
		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testSES2024PropertyPathAnonVarSharing()
		throws Exception
	{
		loadTestData("/testdata-query/dataset-ses2024.trig");
		String query = "PREFIX : <http://example.org/> SELECT * WHERE { ?x1 :p/:lit ?l1 . ?x1 :diff ?x2 . ?x2 :p/:lit ?l2 . }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try {
			TupleQueryResult result = tq.evaluate();
			assertNotNull(result);

			BindingSet bs = result.next();
			Literal l1 = (Literal)bs.getValue("l1");
			Literal l2 = (Literal)bs.getValue("l2");

			assertNotNull(l1);
			assertFalse(l1.equals(l2));

			result.close();
		}
		catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	/* private / protected methods */

	private int countCharOccurrences(String string, char ch) {
		int count = 0;
		for (int i = 0; i < string.length(); i++) {
			if (string.charAt(i) == ch) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Get a set of useful namespace prefix declarations.
	 * 
	 * @return namespace prefix declarations for dc, foaf and ex.
	 */
	protected String getNamespaceDeclarations() {
		StringBuilder declarations = new StringBuilder();
		declarations.append("PREFIX dc: <" + DCTERMS.NAMESPACE + "> \n");
		declarations.append("PREFIX foaf: <" + FOAF.NAMESPACE + "> \n");
		declarations.append("PREFIX sesame: <" + SESAME.NAMESPACE + "> \n");
		declarations.append("PREFIX ex: <" + EX_NS + "> \n");
		declarations.append("\n");

		return declarations.toString();
	}

	protected abstract Repository newRepository()
		throws Exception;

	protected void loadTestData(String dataFile, Resource... contexts)
		throws RDFParseException, RepositoryException, IOException
	{
		logger.debug("loading dataset {}", dataFile);
		InputStream dataset = ComplexSPARQLQueryTest.class.getResourceAsStream(dataFile);
		try {
			conn.add(dataset, "",
					Rio.getParserFormatForFileName(dataFile).orElseThrow(Rio.unsupportedFormat(dataFile)),
					contexts);
		}
		finally {
			dataset.close();
		}
		logger.debug("dataset loaded.");
	}
}
