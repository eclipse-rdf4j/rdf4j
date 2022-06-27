/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.query.parser.sparql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.model.util.Statements.statement;
import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.AbstractTupleQueryResultHandler;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.impl.SimpleBinding;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.testsuite.query.parser.sparql.manifest.SPARQL11ManifestTest;
import org.eclipse.rdf4j.testsuite.sparql.RepositorySPARQLComplianceTestSuite;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A set of compliance tests on SPARQL query functionality which can not be easily executed using the
 * {@link SPARQL11ManifestTest} format. This includes tests on queries with non-deterministic output (e.g.
 * GROUP_CONCAT).
 *
 * @deprecated use {@link RepositorySPARQLComplianceTestSuite} instead.
 * @author Jeen Broekstra
 */
@Deprecated(since = "4.0.2", forRemoval = true)
public abstract class ComplexSPARQLQueryTest {

	@BeforeClass
	public static void setUpClass() {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	@AfterClass
	public static void afterClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
	}

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
	public void setUp() throws Exception {
		logger.debug("setting up test");
		this.rep = newRepository();

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
	public void tearDown() throws Exception {
		try {
			if (conn != null) {
				conn.close();
			}
		} finally {
			if (rep != null) {
				rep.shutDown();
			}
		}
	}

	@Test
	public void testNullContext1() throws Exception {
		loadTestData("/testdata-query/dataset-query.trig");
		String query = " SELECT * " +
				" FROM DEFAULT " +
				" WHERE { ?s ?p ?o } ";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			while (result.hasNext()) {
				BindingSet bs = result.next();
				assertNotNull(bs);

				Resource s = (Resource) bs.getValue("s");

				assertNotNull(s);
				assertNotEquals(bob, s); // should not be present in default
				// graph
				assertNotEquals(alice, s); // should not be present in
				// default
				// graph
			}
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testSES2373SubselectOptional() {
		conn.prepareUpdate(QueryLanguage.SPARQL,
				"insert data {" + "<u:1> <u:r> <u:subject> ." + "<u:1> <u:v> 1 ." + "<u:1> <u:x> <u:x1> ."
						+ "<u:2> <u:r> <u:subject> ." + "<u:2> <u:v> 2 ." + "<u:2> <u:x> <u:x2> ."
						+ "<u:3> <u:r> <u:subject> ." + "<u:3> <u:v> 3 ." + "<u:3> <u:x> <u:x3> ."
						+ "<u:4> <u:r> <u:subject> ." + "<u:4> <u:v> 4 ." + "<u:4> <u:x> <u:x4> ."
						+ "<u:5> <u:r> <u:subject> ." + "<u:5> <u:v> 5 ." + "<u:5> <u:x> <u:x5> ." + "}")
				.execute();

		String qb = "select ?x { \n" +
				" { select ?v { ?v <u:r> <u:subject> filter (?v = <u:1>) } }.\n" +
				"  optional {  select ?val { ?v <u:v> ?val .} }\n" +
				"  ?v <u:x> ?x \n" +
				"}\n";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, qb);
		try (TupleQueryResult result = tq.evaluate()) {
			assertTrue("The query should return a result", result.hasNext());
			BindingSet b = result.next();
			assertTrue("?x is from the mandatory part of the query and should be bound", b.hasBinding("x"));
		}
	}

	@Test
	public void testSES2154SubselectOptional() {

		String ub = "insert data { \n" +
				" <urn:s1> a <urn:C> .  \n" +
				" <urn:s2> a <urn:C> .  \n" +
				" <urn:s3> a <urn:C> .  \n" +
				" <urn:s4> a <urn:C> .  \n" +
				" <urn:s5> a <urn:C> .  \n" +
				" <urn:s6> a <urn:C> .  \n" +
				" <urn:s7> a <urn:C> .  \n" +
				" <urn:s8> a <urn:C> .  \n" +
				" <urn:s9> a <urn:C> .  \n" +
				" <urn:s10> a <urn:C> .  \n" +
				" <urn:s11> a <urn:C> .  \n" +
				" <urn:s12> a <urn:C> .  \n" +
				" <urn:s1> <urn:p> \"01\" .  \n" +
				" <urn:s2> <urn:p> \"02\" .  \n" +
				" <urn:s3> <urn:p> \"03\" .  \n" +
				" <urn:s4> <urn:p> \"04\" .  \n" +
				" <urn:s5> <urn:p> \"05\" .  \n" +
				" <urn:s6> <urn:p> \"06\" .  \n" +
				" <urn:s7> <urn:p> \"07\" .  \n" +
				" <urn:s8> <urn:p> \"08\" .  \n" +
				" <urn:s9> <urn:p> \"09\" .  \n" +
				" <urn:s10> <urn:p> \"10\" .  \n" +
				" <urn:s11> <urn:p> \"11\" .  \n" +
				" <urn:s12> <urn:p> \"12\" .  \n" +
				"} \n";

		conn.prepareUpdate(QueryLanguage.SPARQL, ub).execute();

		String qb = "SELECT ?s ?label\n" +
				"WHERE { \n" +
				" 	  ?s a <urn:C> \n .\n" +
				" 	  OPTIONAL  { {SELECT ?label  WHERE { \n" +
				"                     ?s <urn:p> ?label . \n" +
				"   	      } ORDER BY ?label LIMIT 2 \n" +
				"		    }\n" +
				"       }\n" +
				"}\n" +
				"ORDER BY ?s\n" +
				"LIMIT 10 \n";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, qb);
		try (TupleQueryResult evaluate = tq.evaluate()) {
			assertTrue("The query should return a result", evaluate.hasNext());

			List<BindingSet> result = QueryResults.asList(evaluate);
			assertEquals(10, result.size());
			for (BindingSet bs : result) {
				Literal label = (Literal) bs.getValue("label");
				assertTrue("wrong label value (expected '01' or '02', but got '" + label.stringValue() + "')",
						label.stringValue().equals("01") || label.stringValue().equals("02"));
			}
		}
	}

	@Test
	public void testNullContext2() throws Exception {
		loadTestData("/testdata-query/dataset-query.trig");
		String query = getNamespaceDeclarations() +
				" SELECT * " +
				" FROM rdf4j:nil " +
				" WHERE { ?s ?p ?o } ";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			while (result.hasNext()) {
				BindingSet bs = result.next();
				assertNotNull(bs);

				Resource s = (Resource) bs.getValue("s");

				assertNotNull(s);
				assertNotEquals(bob, s); // should not be present in default
				// graph
				assertNotEquals(alice, s); // should not be present in
				// default
				// graph
			}
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testSesameNilAsGraph() throws Exception {
		loadTestData("/testdata-query/dataset-query.trig");
		String query = getNamespaceDeclarations() +
				" SELECT * " +
				" WHERE { GRAPH rdf4j:nil { ?s ?p ?o } } ";
//		query.append(" WHERE { ?s ?p ?o } ");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try {
			List<BindingSet> result = QueryResults.asList(tq.evaluate());

			// nil graph should not be empty
			assertThat(result.size()).isGreaterThan(1);

			for (BindingSet bs : result) {
				Resource s = (Resource) bs.getValue("s");

				assertNotNull(s);
				assertThat(s).withFailMessage("%s should not be present in nil graph", bob).isNotEqualTo(bob);
				assertThat(s).withFailMessage("%s should not be present in nil graph", alice).isNotEqualTo(alice);
			}
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testDescribeA() throws Exception {
		loadTestData("/testdata-query/dataset-describe.trig");
		String query = getNamespaceDeclarations() +
				"DESCRIBE ex:a";

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query);

		ValueFactory f = conn.getValueFactory();
		IRI a = f.createIRI("http://example.org/a");
		IRI p = f.createIRI("http://example.org/p");
		try (GraphQueryResult evaluate = gq.evaluate()) {
			Model result = QueryResults.asModel(evaluate);
			Set<Value> objects = result.filter(a, p, null).objects();
			assertNotNull(objects);
			for (Value object : objects) {
				if (object instanceof BNode) {
					assertTrue(result.contains((Resource) object, null, null));
					assertEquals(2, result.filter((Resource) object, null, null).size());
				}
			}
		}
	}

	@Test
	public void testDescribeAWhere() throws Exception {
		loadTestData("/testdata-query/dataset-describe.trig");
		String query = getNamespaceDeclarations() +
				"DESCRIBE ?x WHERE {?x rdfs:label \"a\". } ";

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query);

		ValueFactory f = conn.getValueFactory();
		IRI a = f.createIRI("http://example.org/a");
		IRI p = f.createIRI("http://example.org/p");
		try (GraphQueryResult evaluate = gq.evaluate()) {
			Model result = QueryResults.asModel(evaluate);
			Set<Value> objects = result.filter(a, p, null).objects();
			assertNotNull(objects);
			for (Value object : objects) {
				if (object instanceof BNode) {
					assertTrue(result.contains((Resource) object, null, null));
					assertEquals(2, result.filter((Resource) object, null, null).size());
				}
			}
		}
	}

	@Test
	public void testDescribeWhere() throws Exception {
		loadTestData("/testdata-query/dataset-describe.trig");
		String query = getNamespaceDeclarations() +
				"DESCRIBE ?x WHERE {?x rdfs:label ?y . } ";

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query);

		ValueFactory vf = conn.getValueFactory();
		IRI a = vf.createIRI("http://example.org/a");
		IRI b = vf.createIRI("http://example.org/b");
		IRI c = vf.createIRI("http://example.org/c");
		IRI e = vf.createIRI("http://example.org/e");
		IRI f = vf.createIRI("http://example.org/f");
		IRI p = vf.createIRI("http://example.org/p");

		try (GraphQueryResult evaluate = gq.evaluate()) {
			Model result = QueryResults.asModel(evaluate);
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
					assertTrue(result.contains((Resource) object, null, null));
					assertEquals(2, result.filter((Resource) object, null, null).size());
				}
			}
		}
	}

	@Test
	public void testDescribeB() throws Exception {
		loadTestData("/testdata-query/dataset-describe.trig");
		String query = getNamespaceDeclarations() +
				"DESCRIBE ex:b";

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query);

		ValueFactory f = conn.getValueFactory();
		IRI b = f.createIRI("http://example.org/b");
		IRI p = f.createIRI("http://example.org/p");
		try (GraphQueryResult evaluate = gq.evaluate()) {
			Model result = QueryResults.asModel(evaluate);
			Set<Resource> subjects = result.filter(null, p, b).subjects();
			assertNotNull(subjects);
			for (Value subject : subjects) {
				if (subject instanceof BNode) {
					assertTrue(result.contains(null, null, subject));
				}
			}
		}
	}

	@Test
	public void testDescribeD() throws Exception {
		loadTestData("/testdata-query/dataset-describe.trig");
		String query = getNamespaceDeclarations() +
				"DESCRIBE ex:d";

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query);

		ValueFactory f = conn.getValueFactory();
		IRI d = f.createIRI("http://example.org/d");
		IRI p = f.createIRI("http://example.org/p");
		IRI e = f.createIRI("http://example.org/e");
		try (GraphQueryResult evaluate = gq.evaluate()) {
			Model result = QueryResults.asModel(evaluate);

			assertNotNull(result);
			assertTrue(result.contains(null, p, e));
			assertFalse(result.contains(e, null, null));
			Set<Value> objects = result.filter(d, p, null).objects();
			assertNotNull(objects);
			for (Value object : objects) {
				if (object instanceof BNode) {
					Set<Value> childObjects = result.filter((BNode) object, null, null).objects();
					assertNotNull(childObjects);
					for (Value childObject : childObjects) {
						if (childObject instanceof BNode) {
							assertTrue(result.contains((BNode) childObject, null, null));
						}
					}
				}
			}
		}
	}

	@Test
	public void testDescribeF() throws Exception {
		loadTestData("/testdata-query/dataset-describe.trig");
		String query = getNamespaceDeclarations() +
				"DESCRIBE ex:f";

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query);

		ValueFactory vf = conn.getValueFactory();
		IRI f = vf.createIRI("http://example.org/f");
		IRI p = vf.createIRI("http://example.org/p");
		try (GraphQueryResult evaluate = gq.evaluate()) {
			Model result = QueryResults.asModel(evaluate);

			assertNotNull(result);
			assertEquals(4, result.size());
			Set<Value> objects = result.filter(f, p, null).objects();
			assertNotNull(objects);
			for (Value object : objects) {
				if (object instanceof BNode) {
					Set<Value> childObjects = result.filter((BNode) object, null, null).objects();
					assertNotNull(childObjects);
					for (Value childObject : childObjects) {
						if (childObject instanceof BNode) {
							assertTrue(result.contains((BNode) childObject, null, null));
						}
					}
				}
			}
		}
	}

	@Test
	public void testDescribeMultipleA() {
		String update = "insert data { <urn:1> <urn:p1> <urn:v> . [] <urn:blank> <urn:1> . <urn:2> <urn:p2> <urn:3> . } ";
		conn.prepareUpdate(QueryLanguage.SPARQL, update).execute();

		String query = getNamespaceDeclarations() +
				"DESCRIBE <urn:1> <urn:2> ";

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query);

		ValueFactory vf = conn.getValueFactory();
		IRI urn1 = vf.createIRI("urn:1");
		IRI p1 = vf.createIRI("urn:p1");
		IRI p2 = vf.createIRI("urn:p2");
		IRI urn2 = vf.createIRI("urn:2");
		IRI blank = vf.createIRI("urn:blank");

		try (GraphQueryResult evaluate = gq.evaluate()) {
			Model result = QueryResults.asModel(evaluate);
			assertTrue(result.contains(urn1, p1, null));
			assertTrue(result.contains(null, blank, urn1));
			assertTrue(result.contains(urn2, p2, null));
		}
	}

	@Test
	public void testDescribeMultipleB() {
		String update = "insert data { <urn:1> <urn:p1> <urn:v> . <urn:1> <urn:blank> [] . <urn:2> <urn:p2> <urn:3> . } ";
		conn.prepareUpdate(QueryLanguage.SPARQL, update).execute();

		String query = getNamespaceDeclarations() +
				"DESCRIBE <urn:1> <urn:2> ";

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query);

		ValueFactory vf = conn.getValueFactory();
		IRI urn1 = vf.createIRI("urn:1");
		IRI p1 = vf.createIRI("urn:p1");
		IRI p2 = vf.createIRI("urn:p2");
		IRI urn2 = vf.createIRI("urn:2");
		IRI blank = vf.createIRI("urn:blank");
		try (GraphQueryResult evaluate = gq.evaluate()) {
			Model result = QueryResults.asModel(evaluate);

			assertTrue(result.contains(urn1, p1, null));
			assertTrue(result.contains(urn1, blank, null));
			assertTrue(result.contains(urn2, p2, null));
		}
	}

	@Test
	public void testDescribeMultipleC() {
		String update = "insert data { <urn:1> <urn:p1> <urn:v> . [] <urn:blank> <urn:1>. <urn:1> <urn:blank> [] . <urn:2> <urn:p2> <urn:3> . } ";
		conn.prepareUpdate(QueryLanguage.SPARQL, update).execute();

		String query = getNamespaceDeclarations() +
				"DESCRIBE <urn:1> <urn:2> ";

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query);

		ValueFactory vf = conn.getValueFactory();
		IRI urn1 = vf.createIRI("urn:1");
		IRI p1 = vf.createIRI("urn:p1");
		IRI p2 = vf.createIRI("urn:p2");
		IRI urn2 = vf.createIRI("urn:2");
		IRI blank = vf.createIRI("urn:blank");
		try (GraphQueryResult evaluate = gq.evaluate()) {
			Model result = QueryResults.asModel(evaluate);

			assertTrue(result.contains(urn1, p1, null));
			assertTrue(result.contains(urn1, blank, null));
			assertTrue(result.contains(null, blank, urn1));
			assertTrue(result.contains(urn2, p2, null));
		}
	}

	@Test
	public void testDescribeMultipleD() {
		String update = "insert data { <urn:1> <urn:p1> <urn:v> . [] <urn:blank> <urn:1>. <urn:2> <urn:p2> <urn:3> . [] <urn:blank> <urn:2> . <urn:4> <urn:p2> <urn:3> . <urn:4> <urn:blank> [] .} ";
		conn.prepareUpdate(QueryLanguage.SPARQL, update).execute();

		String query = getNamespaceDeclarations() +
				"DESCRIBE <urn:1> <urn:2> <urn:4> ";

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query);

		ValueFactory vf = conn.getValueFactory();
		IRI urn1 = vf.createIRI("urn:1");
		IRI p1 = vf.createIRI("urn:p1");
		IRI p2 = vf.createIRI("urn:p2");
		IRI urn2 = vf.createIRI("urn:2");
		IRI urn4 = vf.createIRI("urn:4");
		IRI blank = vf.createIRI("urn:blank");
		try (GraphQueryResult evaluate = gq.evaluate()) {
			Model result = QueryResults.asModel(evaluate);

			assertTrue(result.contains(urn1, p1, null));
			assertTrue(result.contains(null, blank, urn1));
			assertTrue(result.contains(urn2, p2, null));
			assertTrue(result.contains(urn4, p2, null));
			assertTrue(result.contains(urn4, blank, null));
		}
	}

	@Test
	public void testGroupByEmpty() {
		// see issue https://github.com/eclipse/rdf4j/issues/573
		String query = "select ?x where {?x ?p ?o} group by ?x";

		TupleQuery tq = conn.prepareTupleQuery(query);
		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);
			assertFalse(result.hasNext());
		}
	}

	@Test
	public void testGroupConcatDistinct() throws Exception {
		loadTestData("/testdata-query/dataset-query.trig");

		String query = getNamespaceDeclarations() +
				"SELECT (GROUP_CONCAT(DISTINCT ?l) AS ?concat)" +
				"WHERE { ex:groupconcat-test ?p ?l . }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			while (result.hasNext()) {
				BindingSet bs = result.next();
				assertNotNull(bs);

				Value concat = bs.getValue("concat");

				assertTrue(concat instanceof Literal);

				String lexValue = ((Literal) concat).getLabel();

				int occ = countCharOccurrences(lexValue, 'a');
				assertEquals(1, occ);
				occ = countCharOccurrences(lexValue, 'b');
				assertEquals(1, occ);
				occ = countCharOccurrences(lexValue, 'c');
				assertEquals(1, occ);
				occ = countCharOccurrences(lexValue, 'd');
				assertEquals(1, occ);
			}
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testSameTermRepeatInOptional() throws Exception {
		loadTestData("/testdata-query/dataset-query.trig");
		String query = getNamespaceDeclarations() +
				" SELECT ?l ?opt1 ?opt2 " +
				" FROM ex:optional-sameterm-graph " +
				" WHERE { " +
				"          ?s ex:p ex:A ; " +
				"          { " +
				"              { " +
				"                 ?s ?p ?l ." +
				"                 FILTER(?p = rdfs:label) " +
				"              } " +
				"              OPTIONAL { " +
				"                 ?s ?p ?opt1 . " +
				"                 FILTER (?p = ex:prop1) " +
				"              } " +
				"              OPTIONAL { " +
				"                 ?s ?p ?opt2 . " +
				"                 FILTER (?p = ex:prop2) " +
				"              } " +
				"          }" +
				" } ";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				BindingSet bs = result.next();
				count++;
				assertNotNull(bs);

				// System.out.println(bs);

				Value l = bs.getValue("l");
				assertTrue(l instanceof Literal);
				assertEquals("label", ((Literal) l).getLabel());

				Value opt1 = bs.getValue("opt1");
				assertNull(opt1);

				Value opt2 = bs.getValue("opt2");
				assertNull(opt2);
			}
			assertEquals(1, count);
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testSES1121VarNamesInOptionals() throws Exception {
		// Verifying that variable names have no influence on order of optionals
		// in query. See SES-1121.

		loadTestData("/testdata-query/dataset-ses1121.trig");

		String query1 = getNamespaceDeclarations() +
				" SELECT DISTINCT *\n" +
				" WHERE { GRAPH ?g { \n" +
				"          OPTIONAL { ?var35 ex:p ?b . } \n " +
				"          OPTIONAL { ?b ex:q ?c . } \n " +
				"       } \n" +
				" } \n";

		String query2 = getNamespaceDeclarations() +
				" SELECT DISTINCT *\n" +
				" WHERE { GRAPH ?g { \n" +
				"          OPTIONAL { ?var35 ex:p ?b . } \n " +
				"          OPTIONAL { ?b ex:q ?var2 . } \n " +
				"       } \n" +
				" } \n";

		TupleQuery tq1 = conn.prepareTupleQuery(QueryLanguage.SPARQL, query1);
		TupleQuery tq2 = conn.prepareTupleQuery(QueryLanguage.SPARQL, query2);

		try (TupleQueryResult result1 = tq1.evaluate(); TupleQueryResult result2 = tq2.evaluate()) {
			assertNotNull(result1);
			assertNotNull(result2);

			List<BindingSet> qr1 = QueryResults.asList(result1);
			List<BindingSet> qr2 = QueryResults.asList(result2);

			// System.out.println(qr1);
			// System.out.println(qr2);

			// if optionals are not kept in same order, query results will be
			// different size.
			assertEquals(qr1.size(), qr2.size());

		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testSES1081SameTermWithValues() throws Exception {
		loadTestData("/testdata-query/dataset-ses1081.trig");
		String query = "PREFIX ex: <http://example.org/>\n" +
				" SELECT * \n" +
				" WHERE { \n " +
				"          ?s ex:p ?a . \n" +
				"          FILTER sameTerm(?a, ?e) \n " +
				"          VALUES ?e { ex:b } \n " +
				" } ";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
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
			assertEquals(1, count);
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testSES1898LeftJoinSemantics1() throws Exception {
		loadTestData("/testdata-query/dataset-ses1898.trig");
		String query = "  PREFIX : <http://example.org/> " +
				"  SELECT * WHERE { " +
				"    ?s :p1 ?v1 . " +
				"    OPTIONAL {?s :p2 ?v2 } ." +
				"     ?s :p3 ?v2 . " +
				"  } ";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
		try (Stream<BindingSet> result = tq.evaluate().stream()) {
			long count = result.count();
			assertEquals(0, count);
		}
	}

	@Test
	public void testSES1073InverseSymmetricPattern() {
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
		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (Stream<BindingSet> result = tq.evaluate().stream()) {
			long count = result.count();
			assertEquals(4, count);
		}
	}

	@Test
	public void testSES1970CountDistinctWildcard() throws Exception {
		loadTestData("/testdata-query/dataset-ses1970.trig");

		String query = "SELECT (COUNT(DISTINCT *) AS ?c) {?s ?p ?o }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			assertTrue(result.hasNext());
			BindingSet s = result.next();
			Literal count = (Literal) s.getValue("c");
			assertNotNull(count);

			assertEquals(3, count.intValue());
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testSES1685propPathSameVar() throws Exception {
		final String queryStr = "PREFIX : <urn:> SELECT ?x WHERE {?x :p+ ?x}";

		conn.add(new StringReader("@prefix : <urn:> . :a :p :b . :b :p :a ."), "", RDFFormat.TURTLE);

		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryStr);

		try (Stream<BindingSet> result = query.evaluate().stream()) {
			long count = result.count();
			assertEquals(2, count);
		}
	}

	@Test
	public void testSES2104ConstructBGPSameURI() throws Exception {
		final String queryStr = "PREFIX : <urn:> CONSTRUCT {:x :p :x } WHERE {} ";

		conn.add(new StringReader("@prefix : <urn:> . :a :p :b . "), "", RDFFormat.TURTLE);

		final IRI x = conn.getValueFactory().createIRI("urn:x");
		final IRI p = conn.getValueFactory().createIRI("urn:p");

		GraphQuery query = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryStr);
		try (GraphQueryResult evaluate = query.evaluate()) {
			Model result = QueryResults.asModel(evaluate);

			assertNotNull(result);
			assertFalse(result.isEmpty());
			assertTrue(result.contains(x, p, x));
		}
	}

	@Test
	public void testSES1898LeftJoinSemantics2() throws Exception {
		loadTestData("/testdata-query/dataset-ses1898.trig");
		String query = "  PREFIX : <http://example.org/> " +
				"  SELECT * WHERE { " +
				"    ?s :p1 ?v1 . " +
				"    ?s :p3 ?v2 . " +
				"    OPTIONAL {?s :p2 ?v2 } ." +
				"  } ";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (Stream<BindingSet> result = tq.evaluate().stream()) {
			long count = result.count();
			assertEquals(1, count);
		}
	}

	@Test
	public void testIdenticalVariablesInStatementPattern() {
		conn.add(alice, f.createIRI("http://purl.org/dc/elements/1.1/publisher"), bob);

		String queryBuilder = "SELECT ?publisher " +
				"{ ?publisher <http://purl.org/dc/elements/1.1/publisher> ?publisher }";

		conn.prepareTupleQuery(QueryLanguage.SPARQL, queryBuilder)
				.evaluate(new AbstractTupleQueryResultHandler() {

					@Override
					public void handleSolution(BindingSet bindingSet) {
						fail("nobody is self published");
					}
				});
	}

	@Test
	public void testInComparison1() throws Exception {
		loadTestData("/testdata-query/dataset-ses1913.trig");
		String query = " PREFIX : <http://example.org/>\n" +
				" SELECT ?y WHERE { :a :p ?y. FILTER(?y in (:c, :d, 1/0 , 1)) } ";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());

			BindingSet bs = result.next();
			Value y = bs.getValue("y");
			assertNotNull(y);
			assertTrue(y instanceof Literal);
			assertEquals(f.createLiteral("1", CoreDatatype.XSD.INTEGER), y);
		}
	}

	@Test
	public void testInComparison2() throws Exception {
		loadTestData("/testdata-query/dataset-ses1913.trig");
		String query = " PREFIX : <http://example.org/>\n" +
				" SELECT ?y WHERE { :a :p ?y. FILTER(?y in (:c, :d, 1/0)) } ";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);
			assertFalse(result.hasNext());
		}
	}

	@Test
	public void testInComparison3() throws Exception {
		loadTestData("/testdata-query/dataset-ses1913.trig");
		String query = " PREFIX : <http://example.org/>\n" +
				" SELECT ?y WHERE { :a :p ?y. FILTER(?y in (:c, :d, 1, 1/0)) } ";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());

			BindingSet bs = result.next();
			Value y = bs.getValue("y");
			assertNotNull(y);
			assertTrue(y instanceof Literal);
			assertEquals(f.createLiteral("1", XSD.INTEGER), y);
		}
	}

	@Test
	public void testSES2121URIFunction() {
		String query = "SELECT (URI(\"foo bar\") as ?uri) WHERE {}";
		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			BindingSet bs = result.next();
			IRI uri = (IRI) bs.getValue("uri");
			assertNull("uri result for invalid URI should be unbound", uri);
		}

		query = "BASE <http://example.org/> SELECT (URI(\"foo bar\") as ?uri) WHERE {}";
		tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			BindingSet bs = result.next();
			IRI uri = (IRI) bs.getValue("uri");
			assertNotNull("uri result for valid URI reference should be bound", uri);
		}
	}

	@Test
	public void test27NormalizeIRIFunction() {
		String query = "SELECT (IRI(\"../bar\") as ?Iri) WHERE {}";
		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query, "http://example.com/foo/");
		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			BindingSet bs = result.next();
			IRI actual = (IRI) bs.getValue("Iri");
			IRI expected = f.createIRI("http://example.com/bar");
			assertEquals("IRI result for relative IRI should be normalized", expected, actual);
		}
	}

	@Test
	public void testSES869ValueOfNow() {
		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL,
				"SELECT ?p ( NOW() as ?n ) { BIND (NOW() as ?p ) }");

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());

			BindingSet bs = result.next();
			Value p = bs.getValue("p");
			Value n = bs.getValue("n");

			assertNotNull(p);
			assertNotNull(n);
			assertEquals(p, n);
		}
	}

	@Test
	public void testSES2136() throws Exception {
		loadTestData("/testcases-sparql-1.1-w3c/bindings/data02.ttl");
		String query = "PREFIX : <http://example.org/>\n" +
				"SELECT ?s ?o { \n" +
				" { SELECT * WHERE { ?s ?p ?o . } }\n" +
				"	VALUES (?o) { (:b) }\n" +
				"}\n";

		ValueFactory vf = conn.getValueFactory();
		final IRI a = vf.createIRI("http://example.org/a");
		final IRI b = vf.createIRI("http://example.org/b");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			BindingSet bs = result.next();
			assertFalse("only one result expected", result.hasNext());
			assertEquals(a, bs.getValue("s"));
			assertEquals(b, bs.getValue("o"));
		}
	}

	@Test
	public void testRegexCaseNonAscii() {
		String query = "ask {filter (regex(\"Валовой\", \"валовой\", \"i\")) }";

		assertTrue("case-insensitive match on Cyrillic should succeed", conn.prepareBooleanQuery(query).evaluate());

		query = "ask {filter (regex(\"Валовой\", \"валовой\")) }";

		assertFalse("case-sensitive match on Cyrillic should fail", conn.prepareBooleanQuery(query).evaluate());

	}

	@Test
	public void testValuesInOptional() throws Exception {
		loadTestData("/testdata-query/dataset-ses1692.trig");
		String query = " PREFIX : <http://example.org/>\n" +
				" SELECT DISTINCT ?a ?name ?isX WHERE { ?b :p1 ?a . ?a :name ?name. OPTIONAL { ?a a :X . VALUES(?isX) { (:X) } } } ";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());

			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();
				// System.out.println(bs);
				IRI a = (IRI) bs.getValue("a");
				assertNotNull(a);
				Value isX = bs.getValue("isX");
				Literal name = (Literal) bs.getValue("name");
				assertNotNull(name);
				if (a.stringValue().endsWith("a1")) {
					assertNotNull(isX);
				} else if (a.stringValue().endsWith(("a2"))) {
					assertNull(isX);
				}
			}
			assertEquals(2, count);
		}
	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/3072
	 *
	 */
	@Test
	public void testValuesAfterOptional() throws Exception {
		String data = "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> . \n"
				+ "@prefix :     <urn:ex:> . \n"
				+ ":r1 a rdfs:Resource . \n"
				+ ":r2 a rdfs:Resource ; rdfs:label \"a label\" . \n";

		String query = ""
				+ "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"
				+ "prefix :     <urn:ex:> \n"
				+ "\n"
				+ "select ?resource ?label where { \n"
				+ "  ?resource a rdfs:Resource . \n"
				+ "  optional { ?resource rdfs:label ?label } \n"
				+ "  values ?label { undef } \n"
				+ "}";

		conn.add(new StringReader(data), RDFFormat.TURTLE);

		List<BindingSet> result = QueryResults.asList(conn.prepareTupleQuery(query).evaluate());
		assertThat(result).hasSize(2);
	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/1978
	 */
	@Test
	public void testMaxAggregateWithGroupEmptyResult() {
		String query = "select ?s (max(?o) as ?omax) {\n" +
				"   ?s ?p ?o .\n" +
				" }\n" +
				" group by ?s\n";

		try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
			assertThat(result.hasNext()).isFalse();
		}
	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/1978
	 */
	@Test
	public void testMaxAggregateWithoutGroupEmptySolution() {
		String query = "select (max(?o) as ?omax) {\n" +
				"   ?s ?p ?o .\n" +
				" }\n";

		try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
			assertThat(result.next()).isEmpty();
		}
	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/1978
	 */
	@Test
	public void testMinAggregateWithGroupEmptyResult() {
		String query = "select ?s (min(?o) as ?omin) {\n" +
				"   ?s ?p ?o .\n" +
				" }\n" +
				" group by ?s\n";

		try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
			assertThat(result.hasNext()).isFalse();
		}
	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/1978
	 */
	@Test
	public void testMinAggregateWithoutGroupEmptySolution() {
		String query = "select (min(?o) as ?omin) {\n" +
				"   ?s ?p ?o .\n" +
				" }\n";

		try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
			assertThat(result.next()).isEmpty();
		}
	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/1978
	 */
	@Test
	public void testSampleAggregateWithGroupEmptyResult() {
		String query = "select ?s (sample(?o) as ?osample) {\n" +
				"   ?s ?p ?o .\n" +
				" }\n" +
				" group by ?s\n";

		try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
			assertThat(result.hasNext()).isFalse();
		}
	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/1978
	 */
	@Test
	public void testSampleAggregateWithoutGroupEmptySolution() {
		String query = "select (sample(?o) as ?osample) {\n" +
				"   ?s ?p ?o .\n" +
				" }\n";

		try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
			assertThat(result.next()).isEmpty();
		}
	}

	@Test
	public void testSES2052If1() throws Exception {
		loadTestData("/testdata-query/dataset-query.trig");
		String query = "SELECT ?p \n" +
				"WHERE { \n" +
				"         ?s ?p ?o . \n" +
				"        FILTER(IF(BOUND(?p), ?p = <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>, false)) \n" +
				"}";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);
			while (result.hasNext()) {
				BindingSet bs = result.next();

				IRI p = (IRI) bs.getValue("p");
				assertNotNull(p);
				assertEquals(RDF.TYPE, p);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testSES2052If2() throws Exception {
		loadTestData("/testdata-query/dataset-query.trig");
		String query = "SELECT ?p \n" +
				"WHERE { \n" +
				"         ?s ?p ?o . \n" +
				"        FILTER(IF(!BOUND(?p), false , ?p = <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>)) \n" +
				"}";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);
			while (result.hasNext()) {
				BindingSet bs = result.next();

				IRI p = (IRI) bs.getValue("p");
				assertNotNull(p);
				assertEquals(RDF.TYPE, p);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testSameTermRepeatInUnion() throws Exception {
		loadTestData("/testdata-query/dataset-query.trig");
		String query = "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n" +
				"SELECT * {\n" +
				"    {\n" +
				"        ?sameTerm foaf:mbox ?mbox\n" +
				"        FILTER sameTerm(?sameTerm,$william)\n" +
				"    } UNION {\n" +
				"        ?x foaf:knows ?sameTerm\n" +
				"        FILTER sameTerm(?sameTerm,$william)\n" +
				"    }\n" +
				"}";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
		tq.setBinding("william", conn.getValueFactory().createIRI("http://example.org/william"));

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				BindingSet bs = result.next();
				count++;
				assertNotNull(bs);

				// System.out.println(bs);

				Value mbox = bs.getValue("mbox");
				Value x = bs.getValue("x");

				assertTrue(mbox instanceof Literal || x instanceof IRI);
			}
			assertEquals(3, count);
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testSameTermRepeatInUnionAndOptional() throws Exception {
		loadTestData("/testdata-query/dataset-query.trig");

		String query = getNamespaceDeclarations() +
				"SELECT * {\n" +
				"    {\n" +
				"        ex:a ?p ?prop1\n" +
				"        FILTER (?p = ex:prop1)\n" +
				"    } UNION {\n" +
				"          ?s ex:p ex:A ; " +
				"          { " +
				"              { " +
				"                 ?s ?p ?l ." +
				"                 FILTER(?p = rdfs:label) " +
				"              } " +
				"              OPTIONAL { " +
				"                 ?s ?p ?opt1 . " +
				"                 FILTER (?p = ex:prop1) " +
				"              } " +
				"              OPTIONAL { " +
				"                 ?s ?p ?opt2 . " +
				"                 FILTER (?p = ex:prop2) " +
				"              } " +
				"          }" +
				"    }\n" +
				"}";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				BindingSet bs = result.next();
				count++;
				assertNotNull(bs);

				// System.out.println(bs);

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
			assertEquals(2, count);
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testPropertyPathInTree() throws Exception {
		loadTestData("/testdata-query/dataset-query.trig");

		String query = getNamespaceDeclarations() +
				" SELECT ?node ?name " +
				" FROM ex:tree-graph " +
				" WHERE { ?node ex:hasParent+ ex:b . ?node ex:name ?name . }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			while (result.hasNext()) {
				BindingSet bs = result.next();
				assertNotNull(bs);

				// System.out.println(bs);
			}
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testFilterRegexBoolean() throws Exception {
		loadTestData("/testdata-query/dataset-query.trig");

		// test case for issue SES-1050
		String query = getNamespaceDeclarations() +
				" SELECT *" +
				" WHERE { " +
				"       ?x foaf:name ?name ; " +
				"          foaf:mbox ?mbox . " +
				"       FILTER(EXISTS { " +
				"            FILTER(REGEX(?name, \"Bo\") && REGEX(?mbox, \"bob\")) " +
				// query.append(" FILTER(REGEX(?mbox, \"bob\")) ");
				"            } )" +
				" } ";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (Stream<BindingSet> result = tq.evaluate().stream()) {
			long count = result.count();
			assertEquals(1, count);
		}
	}

	@Test
	public void testGroupConcatNonDistinct() throws Exception {
		loadTestData("/testdata-query/dataset-query.trig");
		String query = getNamespaceDeclarations() +
				"SELECT (GROUP_CONCAT(?l) AS ?concat)" +
				"WHERE { ex:groupconcat-test ?p ?l . }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			while (result.hasNext()) {
				BindingSet bs = result.next();
				assertNotNull(bs);

				Value concat = bs.getValue("concat");

				assertTrue(concat instanceof Literal);

				String lexValue = ((Literal) concat).getLabel();

				int occ = countCharOccurrences(lexValue, 'a');
				assertEquals(1, occ);
				occ = countCharOccurrences(lexValue, 'b');
				assertEquals(2, occ);
				occ = countCharOccurrences(lexValue, 'c');
				assertEquals(2, occ);
				occ = countCharOccurrences(lexValue, 'd');
				assertEquals(1, occ);
			}
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	/**
	 * @see <a href="http://www.openrdf.org/issues/browse/SES-1091">SES-1091</a>
	 * @throws Exception
	 */
	@Test
	public void testArbitraryLengthPathWithBinding1() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl");
		String query = getNamespaceDeclarations() +
				"SELECT ?parent ?child " +
				"WHERE { ?child a owl:Class . ?child rdfs:subClassOf+ ?parent . }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			// first execute without binding
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

			try (TupleQueryResult result2 = tq.evaluate()) {
				assertNotNull(result2);

				count = 0;
				while (result2.hasNext()) {
					count++;
					BindingSet bs = result2.next();
					assertTrue(bs.hasBinding("child"));
					assertTrue(bs.hasBinding("parent"));
				}
				assertEquals(4, count);
			}
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	/**
	 * @see <a href="http://www.openrdf.org/issues/browse/SES-1091">SES-1091</a>
	 * @throws Exception
	 */
	@Test
	public void testArbitraryLengthPathWithBinding2() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl");

		// query without initializing ?child first.
		String query = getNamespaceDeclarations() +
				"SELECT ?parent ?child " +
				"WHERE { ?child rdfs:subClassOf+ ?parent . }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			// first execute without binding
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

			try (TupleQueryResult result2 = tq.evaluate()) {
				assertNotNull(result2);

				count = 0;
				while (result2.hasNext()) {
					count++;
					BindingSet bs = result2.next();
					assertTrue(bs.hasBinding("child"));
					assertTrue(bs.hasBinding("parent"));
				}
				assertEquals(4, count);
			}
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	/**
	 * @see <a href="http://www.openrdf.org/issues/browse/SES-1091">SES-1091</a>
	 * @throws Exception
	 */
	@Test
	public void testArbitraryLengthPathWithBinding3() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl");

		// binding on child instead of parent.
		String query = getNamespaceDeclarations() +
				"SELECT ?parent ?child " +
				"WHERE { ?child rdfs:subClassOf+ ?parent . }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			// first execute without binding
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

			try (TupleQueryResult result2 = tq.evaluate()) {
				assertNotNull(result2);

				count = 0;
				while (result2.hasNext()) {
					count++;
					BindingSet bs = result2.next();
					assertTrue(bs.hasBinding("child"));
					assertTrue(bs.hasBinding("parent"));
				}
				assertEquals(2, count);
			}
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	/**
	 * @see <a href="http://www.openrdf.org/issues/browse/SES-1091">SES-1091</a>
	 * @throws Exception
	 */
	@Test
	public void testArbitraryLengthPathWithBinding4() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl", this.alice);

		// binding on child instead of parent.
		String query = getNamespaceDeclarations() +
				"SELECT ?parent ?child " +
				"WHERE { ?child rdfs:subClassOf+ ?parent . }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			// first execute without binding
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

			try (TupleQueryResult result2 = tq.evaluate()) {
				assertNotNull(result2);

				count = 0;
				while (result2.hasNext()) {
					count++;
					BindingSet bs = result2.next();
					assertTrue(bs.hasBinding("child"));
					assertTrue(bs.hasBinding("parent"));
				}
				assertEquals(2, count);
			}
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	/**
	 * @see <a href="http://www.openrdf.org/issues/browse/SES-1091">SES-1091</a>
	 * @throws Exception
	 */
	@Test
	public void testArbitraryLengthPathWithBinding5() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl", this.alice, this.bob);

		// binding on child instead of parent.
		String query = getNamespaceDeclarations() +
				"SELECT ?parent ?child " +
				"WHERE { ?child rdfs:subClassOf+ ?parent . }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			// first execute without binding
			assertNotNull(result);

			// System.out.println("--- testArbitraryLengthPathWithBinding5
			// ---");

			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();

				// System.out.println(bs);

				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(7, count);

			// execute again, but this time setting a binding
			tq.setBinding("child", f.createIRI(EX_NS, "C"));

			try (TupleQueryResult result2 = tq.evaluate()) {
				assertNotNull(result2);

				count = 0;
				while (result2.hasNext()) {
					count++;
					BindingSet bs = result2.next();
					assertTrue(bs.hasBinding("child"));
					assertTrue(bs.hasBinding("parent"));
				}
				assertEquals(2, count);
			}
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	/**
	 * @see <a href="http://www.openrdf.org/issues/browse/SES-1091">SES-1091</a>
	 * @throws Exception
	 */
	@Test
	public void testArbitraryLengthPathWithBinding6() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl", this.alice, this.bob, this.mary);

		// binding on child instead of parent.
		String query = getNamespaceDeclarations() +
				"SELECT ?parent ?child " +
				"WHERE { ?child rdfs:subClassOf+ ?parent . }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			// first execute without binding
			assertNotNull(result);

			// System.out.println("--- testArbitraryLengthPathWithBinding6
			// ---");

			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();

				// System.out.println(bs);

				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(7, count);

			// execute again, but this time setting a binding
			tq.setBinding("child", f.createIRI(EX_NS, "C"));

			try (TupleQueryResult result2 = tq.evaluate()) {
				assertNotNull(result2);

				count = 0;
				while (result2.hasNext()) {
					count++;
					BindingSet bs = result2.next();
					assertTrue(bs.hasBinding("child"));
					assertTrue(bs.hasBinding("parent"));
				}
				assertEquals(2, count);
			}
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	/**
	 * @see <a href="http://www.openrdf.org/issues/browse/SES-1091">SES-1091</a>
	 * @throws Exception
	 */
	@Test
	public void testArbitraryLengthPathWithBinding7() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl", this.alice, this.bob, this.mary);

		// binding on child instead of parent.
		String query = getNamespaceDeclarations() +
				"SELECT ?parent ?child " +
				"WHERE { ?child rdfs:subClassOf+ ?parent . }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
		SimpleDataset dt = new SimpleDataset();
		dt.addDefaultGraph(this.alice);
		tq.setDataset(dt);

		try (TupleQueryResult result = tq.evaluate()) {
			// first execute without binding
			assertNotNull(result);

			// System.out.println("--- testArbitraryLengthPathWithBinding7
			// ---");

			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();

				// System.out.println(bs);

				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(7, count);

			// execute again, but this time setting a binding
			tq.setBinding("child", f.createIRI(EX_NS, "C"));

			try (TupleQueryResult result2 = tq.evaluate()) {
				assertNotNull(result2);

				count = 0;
				while (result2.hasNext()) {
					count++;
					BindingSet bs = result2.next();
					assertTrue(bs.hasBinding("child"));
					assertTrue(bs.hasBinding("parent"));
				}
				assertEquals(2, count);
			}
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	/**
	 * @see <a href="http://www.openrdf.org/issues/browse/SES-1091">SES-1091</a>
	 * @throws Exception
	 */
	@Test
	public void testArbitraryLengthPathWithBinding8() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl", this.alice, this.bob, this.mary);

		// binding on child instead of parent.
		String query = getNamespaceDeclarations() +
				"SELECT ?parent ?child " +
				"WHERE { ?child rdfs:subClassOf+ ?parent . }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
		SimpleDataset dt = new SimpleDataset();
		dt.addDefaultGraph(this.alice);
		dt.addDefaultGraph(this.bob);
		tq.setDataset(dt);

		try (TupleQueryResult result = tq.evaluate()) {
			// first execute without binding
			assertNotNull(result);
			// System.out.println("--- testArbitraryLengthPathWithBinding8
			// ---");
			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();

				// System.out.println(bs);

				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(7, count);

			// execute again, but this time setting a binding
			tq.setBinding("child", f.createIRI(EX_NS, "C"));

			try (TupleQueryResult result2 = tq.evaluate()) {
				assertNotNull(result2);

				count = 0;
				while (result2.hasNext()) {
					count++;
					BindingSet bs = result2.next();
					assertTrue(bs.hasBinding("child"));
					assertTrue(bs.hasBinding("parent"));
				}
				assertEquals(2, count);
			}
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	/**
	 * @see <a href="http://www.openrdf.org/issues/browse/SES-1091">SES-1091</a>
	 * @throws Exception
	 */
	@Test
	public void testArbitraryLengthPathWithFilter1() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl");
		String query = getNamespaceDeclarations() +
				"SELECT ?parent ?child " +
				"WHERE { ?child a owl:Class . ?child rdfs:subClassOf+ ?parent . FILTER (?parent = owl:Thing) }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();
				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(4, count);
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	/**
	 * @see <a href="http://www.openrdf.org/issues/browse/SES-1091">SES-1091</a>
	 * @throws Exception
	 */
	@Test
	public void testArbitraryLengthPathWithFilter2() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl");
		String query = getNamespaceDeclarations() +
				"SELECT ?parent ?child " +
				"WHERE { ?child rdfs:subClassOf+ ?parent . FILTER (?parent = owl:Thing) }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();
				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(4, count);
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	/**
	 * @see <a href="http://www.openrdf.org/issues/browse/SES-1091">SES-1091</a>
	 * @throws Exception
	 */
	@Test
	public void testArbitraryLengthPathWithFilter3() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl");
		String query = getNamespaceDeclarations() +
				"SELECT ?parent ?child " +
				"WHERE { ?child rdfs:subClassOf+ ?parent . FILTER (?child = <http://example.org/C>) }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				count++;
				BindingSet bs = result.next();
				assertTrue(bs.hasBinding("child"));
				assertTrue(bs.hasBinding("parent"));
			}
			assertEquals(2, count);
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	/**
	 * @see <a href="https://github.com/eclipse/rdf4j/issues/2727">GH-2727</a>
	 */
	@Test
	public void testNestedInversePropertyPathWithZeroLength() {
		String insert = "insert data {\n"
				+ "    <urn:1> <urn:prop> <urn:object> .\n"
				+ "    <urn:2> <urn:prop> <urn:mid:1> .\n"
				+ "    <urn:mid:1> <urn:prop> <urn:object> .\n"
				+ "    <urn:3> <urn:prop> <urn:mid:2> .\n"
				+ "    <urn:mid:2> <urn:prop> <urn:mid:3> .\n"
				+ "    <urn:mid:3> <urn:prop> <urn:object> .\n"
				+ "}";

		String query = "select * where { \n"
				+ "    <urn:object> (^<urn:prop>)? ?o .\n"
				+ "}";

		conn.prepareUpdate(insert).execute();

		TupleQuery tq = conn.prepareTupleQuery(query);

		List<BindingSet> result = QueryResults.asList(tq.evaluate());
		assertThat(result).hasSize(4);
	}

	@Test
	public void testSES2147PropertyPathsWithIdenticalSubsPreds() throws Exception {

		String data = "<urn:s1> <urn:p> <urn:s2> .\n" +
				"<urn:s2> <urn:p> <urn:s3> .\n" +
				"<urn:s3> <urn:q> <urn:s4> .\n" +
				"<urn:s1> <urn:p> <urn:s5> .\n" +
				"<urn:s5> <urn:q> <urn:s6> .\n";

		conn.begin();
		conn.add(new StringReader(data), "", RDFFormat.NTRIPLES);
		conn.commit();

		String query = getNamespaceDeclarations() +
				"SELECT ?x \n" +
				"WHERE { ?x <urn:p>*/<urn:q> <urn:s4> . \n" +
				"        ?x <urn:p>*/<urn:q> <urn:s6> . \n" +
				"} \n";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {

			assertNotNull(result);
			assertTrue(result.hasNext());

			Value x = result.next().getValue("x");
			assertNotNull(x);
			assertTrue(x instanceof IRI);
			assertEquals("urn:s1", x.stringValue());
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testSES1991UUIDEvaluation() throws Exception {
		loadTestData("/testdata-query/defaultgraph.ttl");
		String query = "SELECT ?uid WHERE {?s ?p ?o . BIND(UUID() as ?uid) } LIMIT 2";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			IRI uuid1 = (IRI) result.next().getValue("uid");
			IRI uuid2 = (IRI) result.next().getValue("uid");

			assertNotNull(uuid1);
			assertNotNull(uuid2);
			assertNotEquals(uuid1, uuid2);
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testSES1991STRUUIDEvaluation() throws Exception {
		loadTestData("/testdata-query/defaultgraph.ttl");
		String query = "SELECT ?uid WHERE {?s ?p ?o . BIND(STRUUID() as ?uid) } LIMIT 2";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			Literal uid1 = (Literal) result.next().getValue("uid");
			Literal uid2 = (Literal) result.next().getValue("uid");

			assertNotNull(uid1);
			assertNotEquals(uid1, uid2);
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testSES1991RANDEvaluation() throws Exception {
		loadTestData("/testdata-query/defaultgraph.ttl");
		String query = "SELECT ?r WHERE {?s ?p ?o . BIND(RAND() as ?r) } LIMIT 3";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			Literal r1 = (Literal) result.next().getValue("r");
			Literal r2 = (Literal) result.next().getValue("r");
			Literal r3 = (Literal) result.next().getValue("r");

			assertNotNull(r1);

			// there is a small chance that two successive calls to the random
			// number generator will generate the exact same value, so we check
			// for
			// three successive calls (still theoretically possible to be
			// identical, but phenomenally unlikely).
			assertFalse(r1.equals(r2) && r1.equals(r3));
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testSES1991NOWEvaluation() throws Exception {
		loadTestData("/testdata-query/defaultgraph.ttl");
		String query = "SELECT ?d WHERE {?s ?p ?o . BIND(NOW() as ?d) } LIMIT 2";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			Literal d1 = (Literal) result.next().getValue("d");
			Literal d2 = (Literal) result.next().getValue("d");

			assertNotNull(d1);
			assertEquals(d1, d2);
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testSES2024PropertyPathAnonVarSharing() throws Exception {
		loadTestData("/testdata-query/dataset-ses2024.trig");
		String query = "PREFIX : <http://example.org/> SELECT * WHERE { ?x1 :p/:lit ?l1 . ?x1 :diff ?x2 . ?x2 :p/:lit ?l2 . }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			BindingSet bs = result.next();
			Literal l1 = (Literal) bs.getValue("l1");
			Literal l2 = (Literal) bs.getValue("l2");

			assertNotNull(l1);
			assertNotEquals(l1, l2);
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testPropertyPathNegationInversion() throws Exception {
		String data = "@prefix : <http://example.org/>.\n"
				+ ":Mary :parentOf :Jim.\n"
				+ ":Jim :knows :Jane.\n"
				+ ":Jane :worksFor :IBM.";

		conn.add(new StringReader(data), "", RDFFormat.TURTLE);
		String query1 = "prefix : <http://example.org/> ASK WHERE { :IBM ^(:|!:) :Jane } ";

		assertTrue(conn.prepareBooleanQuery(query1).evaluate());

		String query2 = "prefix : <http://example.org/> ASK WHERE { :IBM ^(:|!:) ?a } ";
		assertTrue(conn.prepareBooleanQuery(query2).evaluate());

		String query3 = "prefix : <http://example.org/> ASK WHERE { :IBM (^(:|!:))* :Mary } ";
		assertTrue(conn.prepareBooleanQuery(query3).evaluate());

	}

	@Test
	public void testSES2361UndefMin() {
		String query = "SELECT (MIN(?v) as ?min) WHERE { VALUES ?v { 1 2 undef 3 4 }}";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			assertEquals("1", result.next().getValue("min").stringValue());
			assertFalse(result.hasNext());
		}
	}

	@Test
	public void testSES2361UndefMax() {
		String query = "SELECT (MAX(?v) as ?max) WHERE { VALUES ?v { 1 2 7 undef 3 4 }}";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			assertEquals("7", result.next().getValue("max").stringValue());
			assertFalse(result.hasNext());
		}
	}

	@Test
	public void testSES2361UndefCount() {
		String query = "SELECT (COUNT(?v) as ?c) WHERE { VALUES ?v { 1 2 undef 3 4 }}";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			assertEquals("4", result.next().getValue("c").stringValue());
			assertFalse(result.hasNext());
		}
	}

	@Test
	public void testSES2361UndefCountWildcard() {
		String query = "SELECT (COUNT(*) as ?c) WHERE { VALUES ?v { 1 2 undef 3 4 }}";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			assertEquals("4", result.next().getValue("c").stringValue());
			assertFalse(result.hasNext());
		}
	}

	@Test
	public void testSES2361UndefSum() {
		String query = "SELECT (SUM(?v) as ?s) WHERE { VALUES ?v { 1 2 undef 3 4 }}";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			assertEquals("10", result.next().getValue("s").stringValue());
			assertFalse(result.hasNext());
		}
	}

	@Test
	public void testSES2336NegatedPropertyPathMod() throws Exception {
		loadTestData("/testdata-query/dataset-ses2336.trig");
		String query = "prefix : <http://example.org/> select * where { ?s a :Test ; !:p? ?o . }";

		ValueFactory vf = conn.getValueFactory();
		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult evaluate = tq.evaluate()) {
			List<BindingSet> result = QueryResults.asList(evaluate);
			assertNotNull(result);

			IRI a = vf.createIRI(EX_NS, "a");
			IRI b = vf.createIRI(EX_NS, "b");
			IRI c = vf.createIRI(EX_NS, "c");
			IRI d = vf.createIRI(EX_NS, "d");
			IRI e = vf.createIRI(EX_NS, "e");
			IRI test = vf.createIRI(EX_NS, "Test");

			assertTrue(containsSolution(result, new SimpleBinding("s", a), new SimpleBinding("o", a)));
			assertTrue(containsSolution(result, new SimpleBinding("s", a), new SimpleBinding("o", test)));
			assertTrue(containsSolution(result, new SimpleBinding("s", a), new SimpleBinding("o", c)));
			assertTrue(containsSolution(result, new SimpleBinding("s", d), new SimpleBinding("o", d)));
			assertTrue(containsSolution(result, new SimpleBinding("s", d), new SimpleBinding("o", e)));
			assertTrue(containsSolution(result, new SimpleBinding("s", d), new SimpleBinding("o", test)));

			assertFalse(containsSolution(result, new SimpleBinding("s", a), new SimpleBinding("o", b)));

		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testSES1979MinMaxInf() throws Exception {
		loadTestData("/testdata-query/dataset-ses1979.trig");
		String query = "prefix : <http://example.org/> select (min(?o) as ?min) (max(?o) as ?max) where { ?s :float ?o }";

		ValueFactory vf = conn.getValueFactory();
		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult evaluate = tq.evaluate()) {
			List<BindingSet> result = QueryResults.asList(evaluate);
			assertNotNull(result);
			assertEquals(1, result.size());

			assertEquals(vf.createLiteral(Float.NEGATIVE_INFINITY), result.get(0).getValue("min"));
			assertEquals(vf.createLiteral(Float.POSITIVE_INFINITY), result.get(0).getValue("max"));
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/1018
	 */
	@Test
	public void testBindError() {

		conn.prepareUpdate(QueryLanguage.SPARQL, "insert data { <urn:test:subj> <urn:test:pred> _:blank }").execute();

		String qb = "SELECT * \n" +
				"WHERE { \n" +
				"  VALUES (?NAValue) { (<http://null>) } \n " +
				"  BIND(IF(?NAValue != <http://null>, ?NAValue, ?notBoundVar) as ?ValidNAValue) \n " +
				"  { ?disjClass (owl:disjointWith|^owl:disjointWith)? ?disjClass2 . }\n" +
				"}\n";

		List<BindingSet> result = QueryResults.asList(conn.prepareTupleQuery(qb).evaluate());

		assertEquals("query should return 2 solutions", 2, result.size());
	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/1405
	 */
	@Test
	public void testBindScope() {
		String query = "SELECT * {\n" +
				"  { BIND (\"a\" AS ?a) }\n" +
				"  { BIND (?a AS ?b) } \n" +
				"}";

		TupleQuery q = conn.prepareTupleQuery(query);
		List<BindingSet> result = QueryResults.asList(q.evaluate());

		assertEquals(1, result.size());

		assertEquals(conn.getValueFactory().createLiteral("a"), result.get(0).getValue("a"));
		assertNull(result.get(0).getValue("b"));
	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/1642
	 */
	@Test
	public void testBindScopeUnion() {

		ValueFactory f = conn.getValueFactory();
		String query = "prefix ex: <http://example.org/> \n" +
				"select * {\n" +
				"  bind(ex:v1 as ?v)\n" +
				"  bind(strafter(str(?v),str(ex:)) as ?b)\n" +
				"  {\n" +
				"    bind(?b as ?b1)\n" +
				"  } union {\n" +
				"    bind(?b as ?b2)\n" +
				"  }\n" +
				"}";

		TupleQuery q = conn.prepareTupleQuery(query);
		List<BindingSet> result = QueryResults.asList(q.evaluate());

		assertEquals(2, result.size());

		IRI v1 = f.createIRI("http://example.org/v1");
		Literal b = f.createLiteral("v1");
		for (BindingSet bs : result) {
			assertThat(bs.getValue("v")).isEqualTo(v1);
			assertThat(bs.getValue("b1")).isNull();
			assertThat(bs.getValue("b2")).isNull();
		}

	}

	@Test
	public void testSES2250BindErrors() {

		conn.prepareUpdate(QueryLanguage.SPARQL, "insert data { <urn:test:subj> <urn:test:pred> _:blank }").execute();

		String qb = "SELECT * {\n" + "    ?s1 ?p1 ?blank . " + "    FILTER(isBlank(?blank))"
				+ "    BIND (iri(?blank) as ?biri)" + "    ?biri ?p2 ?o2 ." + "}";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, qb);
		try (TupleQueryResult evaluate = tq.evaluate()) {
			assertFalse("The query should not return a result", evaluate.hasNext());
		}
	}

	@Test
	public void testSES2250BindErrorsInPath() {

		conn.prepareUpdate(QueryLanguage.SPARQL, "insert data { <urn:test:subj> <urn:test:pred> _:blank }").execute();

		String qb = "SELECT * {\n" + "    ?s1 ?p1 ?blank . " + "    FILTER(isBlank(?blank))"
				+ "    BIND (iri(?blank) as ?biri)" + "    ?biri <urn:test:pred>* ?o2 ." + "}";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, qb);
		try (TupleQueryResult evaluate = tq.evaluate()) {
			assertFalse("The query should not return a result", evaluate.hasNext());
		}
	}

	@Test
	public void testEmptyUnion() {
		String query = "PREFIX : <http://example.org/> " + "SELECT ?visibility WHERE {"
				+ "OPTIONAL { SELECT ?var WHERE { :s a :MyType . BIND (:s as ?var ) .} } ."
				+ "BIND (IF(BOUND(?var), 'VISIBLE', 'HIDDEN') as ?visibility)" + "}";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			assertNotNull(result);
			assertFalse(result.hasNext());
		}
	}

	/**
	 * https://github.com/eclipse/rdf4j/issues/1026
	 */
	@Test
	public void testFilterExistsExternalValuesClause() {
		String ub = "insert data {\n" +
				"  <http://subj1> a <http://type> .\n" +
				"  <http://subj2> a <http://type> .\n" +
				"  <http://subj1> <http://predicate> <http://obj1> .\n" +
				"  <http://subj2> <http://predicate> <http://obj2> .\n" +
				"}";
		conn.prepareUpdate(QueryLanguage.SPARQL, ub).execute();

		String query = "select ?s  {\n" + "    ?s a* <http://type> .\n"
				+ "    FILTER EXISTS {?s <http://predicate> ?o}\n"
				+ "} limit 100 values ?o {<http://obj1>}";

		TupleQuery tq = conn.prepareTupleQuery(query);

		List<BindingSet> result = QueryResults.asList(tq.evaluate());
		assertEquals("single result expected", 1, result.size());
		assertEquals("http://subj1", result.get(0).getValue("s").stringValue());
	}

	@Test
	public void testValuesClauseNamedGraph() throws Exception {
		String ex = "http://example.org/";
		String data = "@prefix foaf: <" + FOAF.NAMESPACE + "> .\n"
				+ "@prefix ex: <" + ex + "> .\n"
				+ "ex:graph1 {\n" +
				"	ex:Person1 rdf:type foaf:Person ;\n" +
				"		foaf:name \"Person 1\" .	ex:Person2 rdf:type foaf:Person ;\n" +
				"		foaf:name \"Person 2\" .	ex:Person3 rdf:type foaf:Person ;\n" +
				"		foaf:name \"Person 3\" .\n" +
				"}";

		conn.add(new StringReader(data), "", RDFFormat.TRIG);

		String query = "SELECT  ?person ?name ?__index \n"
				+ "WHERE { "
				+ "        VALUES (?person ?name  ?__index) { \n"
				+ "                  (<http://example.org/Person1> UNDEF \"0\") \n"
				+ "                  (<http://example.org/Person3> UNDEF \"2\")  } \n"
				+ "        GRAPH <http://example.org/graph1> { ?person <http://xmlns.com/foaf/0.1/name> ?name .   } }";

		TupleQuery q = conn.prepareTupleQuery(query);

		List<BindingSet> result = QueryResults.asList(q.evaluate());
		assertThat(result).hasSize(2);
	}

	@Test
	public void testValuesCartesianProduct() {
		final String queryString = ""
				+ "select ?x ?y where { "
				+ "  values ?x { undef 67 } "
				+ "  values ?y { undef 42 } "
				+ "}";
		final TupleQuery tupleQuery = conn.prepareTupleQuery(queryString);

		List<BindingSet> bindingSets = QueryResults.asList(tupleQuery.evaluate());
		assertThat(bindingSets).hasSize(4);
	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/1267
	 */
	@Test
	public void testSeconds() {
		String qry = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
				+ "SELECT (SECONDS(\"2011-01-10T14:45:13\"^^xsd:dateTime) AS ?sec) { }";

		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, qry).evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			assertEquals("13", result.next().getValue("sec").stringValue());
			assertFalse(result.hasNext());
		}
	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/1267
	 */
	@Test
	public void testSecondsMilliseconds() {
		String qry = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
				+ "SELECT (SECONDS(\"2011-01-10T14:45:13.815-05:00\"^^xsd:dateTime) AS ?sec) { }";

		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, qry).evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			assertEquals("13.815", result.next().getValue("sec").stringValue());
			assertFalse(result.hasNext());
		}
	}

	@Test
	public void testConstructModifiers() throws Exception {
		loadTestData("/testdata-query/dataset-construct-modifiers.ttl");
		String qry = "PREFIX foaf: <http://xmlns.com/foaf/0.1/> \n"
				+ "PREFIX site: <http://example.org/stats#> \n"
				+ "CONSTRUCT { \n"
				+ "  ?iri foaf:name ?name . \n"
				+ "  ?iri foaf:nick ?nick . \n"
				+ "} \n"
				+ "WHERE { \n"
				+ "  ?iri foaf:name ?name ; \n"
				+ "    site:hits ?hits ; \n"
				+ "    foaf:nick ?nick . \n"
				+ "} \n"
				+ "ORDER BY desc(?hits) \n"
				+ "LIMIT 3";
		Statement[] correctResult = {
				statement(iri("urn:1"), iri("http://xmlns.com/foaf/0.1/name"), literal("Alice"), null),
				statement(iri("urn:1"), iri("http://xmlns.com/foaf/0.1/nick"), literal("Al"), null),

				statement(iri("urn:3"), iri("http://xmlns.com/foaf/0.1/name"), literal("Eve"), null),
				statement(iri("urn:3"), iri("http://xmlns.com/foaf/0.1/nick"), literal("Ev"), null),

				statement(iri("urn:2"), iri("http://xmlns.com/foaf/0.1/name"), literal("Bob"), null),
				statement(iri("urn:2"), iri("http://xmlns.com/foaf/0.1/nick"), literal("Bo"), null),
		};
		GraphQuery gq = conn.prepareGraphQuery(qry);
		try (GraphQueryResult result = gq.evaluate()) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			int resultNo = 0;
			while (result.hasNext()) {
				Statement st = result.next();
				assertThat(resultNo).isLessThan(correctResult.length);
				assertEquals(correctResult[resultNo], st);
				resultNo++;
			}
			assertEquals(correctResult.length, resultNo);
		}
	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/3011
	 */
	@Test
	public void testConstruct_CyclicPathWithJoin() {
		IRI test = iri("urn:test"), a = iri("urn:a"), b = iri("urn:b"), c = iri("urn:c");
		conn.add(test, RDF.TYPE, DCAT.CATALOG);

		String query = "PREFIX dcat: <http://www.w3.org/ns/dcat#>\n"
				+ "\n"
				+ "CONSTRUCT {\n"
				+ "<urn:a> <urn:b> ?x .\n"
				+ "  ?x <urn:c> ?x .\n"
				+ "}\n"
				+ "WHERE {\n"
				+ "  ?x a dcat:Catalog .\n"
				+ "}";

		Model result = QueryResults.asModel(conn.prepareGraphQuery(query).evaluate());

		assertThat(result.contains(a, b, test)).isTrue();
		assertThat(result.contains(test, c, test)).isTrue();
	}

	@Test
	public void testSelectBindOnly() {
		String query = "select ?b1 ?b2 ?b3\n"
				+ "where {\n"
				+ "  bind(1 as ?b1)\n"
				+ "}";

		List<BindingSet> result = QueryResults.asList(conn.prepareTupleQuery(query).evaluate());

		assertThat(result.size()).isEqualTo(1);
		BindingSet solution = result.get(0);

		assertThat(solution.getValue("b1")).isEqualTo(literal("1", CoreDatatype.XSD.INTEGER));
		assertThat(solution.getValue("b2")).isNull();
		assertThat(solution.getValue("b3")).isNull();

	}

	private boolean containsSolution(List<BindingSet> result, Binding... solution) {
		final MapBindingSet bs = new MapBindingSet();
		for (Binding b : solution) {
			bs.addBinding(b);
		}
		return result.contains(bs);
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
		return "PREFIX dc: <" + DCTERMS.NAMESPACE + "> \n" +
				"PREFIX foaf: <" + FOAF.NAMESPACE + "> \n" +
				"PREFIX sesame: <" + SESAME.NAMESPACE + "> \n" +
				"PREFIX ex: <" + EX_NS + "> \n" +
				"\n";
	}

	protected abstract Repository newRepository() throws Exception;

	protected void loadTestData(String dataFile, Resource... contexts)
			throws RDFParseException, RepositoryException, IOException {
		logger.debug("loading dataset {}", dataFile);
		try (InputStream dataset = ComplexSPARQLQueryTest.class.getResourceAsStream(dataFile)) {
			conn.add(dataset, "", Rio.getParserFormatForFileName(dataFile).orElseThrow(Rio.unsupportedFormat(dataFile)),
					contexts);
		}
		logger.debug("dataset loaded.");
	}
}
