/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.eclipse.rdf4j.query.parser.sparql.manifest.SPARQL11ManifestTest;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.After;
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
 * @author Jeen Broekstra
 */
public abstract class ComplexSPARQLQueryTest {

	@BeforeClass
	public static void setUpClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
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
		StringBuilder query = new StringBuilder();
		query.append(" SELECT * ");
		query.append(" FROM DEFAULT ");
		query.append(" WHERE { ?s ?p ?o } ");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try (TupleQueryResult result = tq.evaluate();) {
			assertNotNull(result);

			while (result.hasNext()) {
				BindingSet bs = result.next();
				assertNotNull(bs);

				Resource s = (Resource) bs.getValue("s");

				assertNotNull(s);
				assertFalse(bob.equals(s)); // should not be present in default
				// graph
				assertFalse(alice.equals(s)); // should not be present in
				// default
				// graph
			}
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testSES2373SubselectOptional() throws Exception {
		conn.prepareUpdate(QueryLanguage.SPARQL,
				"insert data {" + "<u:1> <u:r> <u:subject> ." + "<u:1> <u:v> 1 ." + "<u:1> <u:x> <u:x1> ."
						+ "<u:2> <u:r> <u:subject> ." + "<u:2> <u:v> 2 ." + "<u:2> <u:x> <u:x2> ."
						+ "<u:3> <u:r> <u:subject> ." + "<u:3> <u:v> 3 ." + "<u:3> <u:x> <u:x3> ."
						+ "<u:4> <u:r> <u:subject> ." + "<u:4> <u:v> 4 ." + "<u:4> <u:x> <u:x4> ."
						+ "<u:5> <u:r> <u:subject> ." + "<u:5> <u:v> 5 ." + "<u:5> <u:x> <u:x5> ." + "}")
				.execute();

		StringBuilder qb = new StringBuilder();
		qb.append("select ?x { \n");
		qb.append(" { select ?v { ?v <u:r> <u:subject> filter (?v = <u:1>) } }.\n");
		qb.append("  optional {  select ?val { ?v <u:v> ?val .} }\n");
		qb.append("  ?v <u:x> ?x \n");
		qb.append("}\n");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, qb.toString());
		try (TupleQueryResult result = tq.evaluate();) {
			assertTrue("The query should return a result", result.hasNext());
			BindingSet b = result.next();
			assertTrue("?x is from the mandatory part of the query and should be bound", b.hasBinding("x"));
		}
	}

	@Test
	public void testSES2154SubselectOptional() throws Exception {
		StringBuilder ub = new StringBuilder();
		ub.append("insert data { \n");
		ub.append(" <urn:s1> a <urn:C> .  \n");
		ub.append(" <urn:s2> a <urn:C> .  \n");
		ub.append(" <urn:s3> a <urn:C> .  \n");
		ub.append(" <urn:s4> a <urn:C> .  \n");
		ub.append(" <urn:s5> a <urn:C> .  \n");
		ub.append(" <urn:s6> a <urn:C> .  \n");
		ub.append(" <urn:s7> a <urn:C> .  \n");
		ub.append(" <urn:s8> a <urn:C> .  \n");
		ub.append(" <urn:s9> a <urn:C> .  \n");
		ub.append(" <urn:s10> a <urn:C> .  \n");
		ub.append(" <urn:s11> a <urn:C> .  \n");
		ub.append(" <urn:s12> a <urn:C> .  \n");

		ub.append(" <urn:s1> <urn:p> \"01\" .  \n");
		ub.append(" <urn:s2> <urn:p> \"02\" .  \n");
		ub.append(" <urn:s3> <urn:p> \"03\" .  \n");
		ub.append(" <urn:s4> <urn:p> \"04\" .  \n");
		ub.append(" <urn:s5> <urn:p> \"05\" .  \n");
		ub.append(" <urn:s6> <urn:p> \"06\" .  \n");
		ub.append(" <urn:s7> <urn:p> \"07\" .  \n");
		ub.append(" <urn:s8> <urn:p> \"08\" .  \n");
		ub.append(" <urn:s9> <urn:p> \"09\" .  \n");
		ub.append(" <urn:s10> <urn:p> \"10\" .  \n");
		ub.append(" <urn:s11> <urn:p> \"11\" .  \n");
		ub.append(" <urn:s12> <urn:p> \"12\" .  \n");
		ub.append("} \n");

		conn.prepareUpdate(QueryLanguage.SPARQL, ub.toString()).execute();

		StringBuilder qb = new StringBuilder();
		qb.append("SELECT ?s ?label\n");
		qb.append("WHERE { \n");
		qb.append(" 	  ?s a <urn:C> \n .\n");
		qb.append(" 	  OPTIONAL  { {SELECT ?label  WHERE { \n");
		qb.append("                     ?s <urn:p> ?label . \n");
		qb.append("   	      } ORDER BY ?label LIMIT 2 \n");
		qb.append("		    }\n");
		qb.append("       }\n");
		qb.append("}\n");
		qb.append("ORDER BY ?s\n");
		qb.append("LIMIT 10 \n");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, qb.toString());
		try (TupleQueryResult evaluate = tq.evaluate();) {
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
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append(" SELECT * ");
		query.append(" FROM sesame:nil ");
		query.append(" WHERE { ?s ?p ?o } ");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try (TupleQueryResult result = tq.evaluate();) {
			assertNotNull(result);

			while (result.hasNext()) {
				BindingSet bs = result.next();
				assertNotNull(bs);

				Resource s = (Resource) bs.getValue("s");

				assertNotNull(s);
				assertFalse(bob.equals(s)); // should not be present in default
				// graph
				assertFalse(alice.equals(s)); // should not be present in
				// default
				// graph
			}
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testDescribeA() throws Exception {
		loadTestData("/testdata-query/dataset-describe.trig");
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("DESCRIBE ex:a");

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query.toString());

		ValueFactory f = conn.getValueFactory();
		IRI a = f.createIRI("http://example.org/a");
		IRI p = f.createIRI("http://example.org/p");
		try (GraphQueryResult evaluate = gq.evaluate();) {
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
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("DESCRIBE ?x WHERE {?x rdfs:label \"a\". } ");

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query.toString());

		ValueFactory f = conn.getValueFactory();
		IRI a = f.createIRI("http://example.org/a");
		IRI p = f.createIRI("http://example.org/p");
		try (GraphQueryResult evaluate = gq.evaluate();) {
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

		try (GraphQueryResult evaluate = gq.evaluate();) {
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
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("DESCRIBE ex:b");

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query.toString());

		ValueFactory f = conn.getValueFactory();
		IRI b = f.createIRI("http://example.org/b");
		IRI p = f.createIRI("http://example.org/p");
		try (GraphQueryResult evaluate = gq.evaluate();) {
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
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("DESCRIBE ex:d");

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query.toString());

		ValueFactory f = conn.getValueFactory();
		IRI d = f.createIRI("http://example.org/d");
		IRI p = f.createIRI("http://example.org/p");
		IRI e = f.createIRI("http://example.org/e");
		try (GraphQueryResult evaluate = gq.evaluate();) {
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
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("DESCRIBE ex:f");

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query.toString());

		ValueFactory vf = conn.getValueFactory();
		IRI f = vf.createIRI("http://example.org/f");
		IRI p = vf.createIRI("http://example.org/p");
		try (GraphQueryResult evaluate = gq.evaluate();) {
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
	public void testDescribeMultipleA() throws Exception {
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

		try (GraphQueryResult evaluate = gq.evaluate();) {
			Model result = QueryResults.asModel(evaluate);
			assertTrue(result.contains(urn1, p1, null));
			assertTrue(result.contains(null, blank, urn1));
			assertTrue(result.contains(urn2, p2, null));
		}
	}

	@Test
	public void testDescribeMultipleB() throws Exception {
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
		try (GraphQueryResult evaluate = gq.evaluate();) {
			Model result = QueryResults.asModel(evaluate);

			assertTrue(result.contains(urn1, p1, null));
			assertTrue(result.contains(urn1, blank, null));
			assertTrue(result.contains(urn2, p2, null));
		}
	}

	@Test
	public void testDescribeMultipleC() throws Exception {
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
		try (GraphQueryResult evaluate = gq.evaluate();) {
			Model result = QueryResults.asModel(evaluate);

			assertTrue(result.contains(urn1, p1, null));
			assertTrue(result.contains(urn1, blank, null));
			assertTrue(result.contains(null, blank, urn1));
			assertTrue(result.contains(urn2, p2, null));
		}
	}

	@Test
	public void testDescribeMultipleD() throws Exception {
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
		try (GraphQueryResult evaluate = gq.evaluate();) {
			Model result = QueryResults.asModel(evaluate);

			assertTrue(result.contains(urn1, p1, null));
			assertTrue(result.contains(null, blank, urn1));
			assertTrue(result.contains(urn2, p2, null));
			assertTrue(result.contains(urn4, p2, null));
			assertTrue(result.contains(urn4, blank, null));
		}
	}

	@Test
	public void testGroupByEmpty() throws Exception {
		// see issue https://github.com/eclipse/rdf4j/issues/573
		String query = "select ?x where {?x ?p ?o} group by ?x";

		TupleQuery tq = conn.prepareTupleQuery(query);
		try (TupleQueryResult result = tq.evaluate();) {
			assertNotNull(result);
			assertFalse(result.hasNext());
		}
	}

	@Test
	public void testGroupConcatDistinct() throws Exception {
		loadTestData("/testdata-query/dataset-query.trig");

		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("SELECT (GROUP_CONCAT(DISTINCT ?l) AS ?concat)");
		query.append("WHERE { ex:groupconcat-test ?p ?l . }");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try (TupleQueryResult result = tq.evaluate();) {
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

		try (TupleQueryResult result = tq.evaluate();) {
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

		try (TupleQueryResult result1 = tq1.evaluate(); TupleQueryResult result2 = tq2.evaluate();) {
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
		StringBuilder query = new StringBuilder();
		query.append("PREFIX ex: <http://example.org/>\n");
		query.append(" SELECT * \n");
		query.append(" WHERE { \n ");
		query.append("          ?s ex:p ?a . \n");
		query.append("          FILTER sameTerm(?a, ?e) \n ");
		query.append("          VALUES ?e { ex:b } \n ");
		query.append(" } ");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try (TupleQueryResult result = tq.evaluate();) {
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
		StringBuilder query = new StringBuilder();
		query.append("  PREFIX : <http://example.org/> ");
		query.append("  SELECT * WHERE { ");
		query.append("    ?s :p1 ?v1 . ");
		query.append("    OPTIONAL {?s :p2 ?v2 } .");
		query.append("     ?s :p3 ?v2 . ");
		query.append("  } ");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
		try (TupleQueryResult result = tq.evaluate();) {
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				result.next();
				count++;
			}
			assertEquals(0, count);
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testSES1073InverseSymmetricPattern() throws Exception {
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
		try (TupleQueryResult result = tq.evaluate();) {
			assertTrue(result.hasNext());
			int count = 0;
			while (result.hasNext()) {
				BindingSet r = result.next();
				// System.out.println(r);
				count++;
			}
			assertEquals(4, count);
		}
	}

	@Test
	public void testSES1970CountDistinctWildcard() throws Exception {
		loadTestData("/testdata-query/dataset-ses1970.trig");

		String query = "SELECT (COUNT(DISTINCT *) AS ?c) {?s ?p ?o }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate();) {
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
		try (TupleQueryResult result = query.evaluate();) {
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				result.next();
				count++;
			}
			// result should be both a and b.
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
		try (GraphQueryResult evaluate = query.evaluate();) {
			Model result = QueryResults.asModel(evaluate);

			assertNotNull(result);
			assertFalse(result.isEmpty());
			assertTrue(result.contains(x, p, x));
		}
	}

	@Test
	public void testSES1898LeftJoinSemantics2() throws Exception {
		loadTestData("/testdata-query/dataset-ses1898.trig");
		StringBuilder query = new StringBuilder();
		query.append("  PREFIX : <http://example.org/> ");
		query.append("  SELECT * WHERE { ");
		query.append("    ?s :p1 ?v1 . ");
		query.append("    ?s :p3 ?v2 . ");
		query.append("    OPTIONAL {?s :p2 ?v2 } .");
		query.append("  } ");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try (TupleQueryResult result = tq.evaluate();) {
			assertNotNull(result);

			int count = 0;
			while (result.hasNext()) {
				result.next();
				count++;
			}
			assertEquals(1, count);
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testIdenticalVariablesInStatementPattern() throws Exception {
		conn.add(alice, f.createIRI("http://purl.org/dc/elements/1.1/publisher"), bob);

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append("SELECT ?publisher ");
		queryBuilder.append("{ ?publisher <http://purl.org/dc/elements/1.1/publisher> ?publisher }");

		conn.prepareTupleQuery(QueryLanguage.SPARQL, queryBuilder.toString())
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
		StringBuilder query = new StringBuilder();
		query.append(" PREFIX : <http://example.org/>\n");
		query.append(" SELECT ?y WHERE { :a :p ?y. FILTER(?y in (:c, :d, 1/0 , 1)) } ");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try (TupleQueryResult result = tq.evaluate();) {
			assertNotNull(result);
			assertTrue(result.hasNext());

			BindingSet bs = result.next();
			Value y = bs.getValue("y");
			assertNotNull(y);
			assertTrue(y instanceof Literal);
			assertEquals(f.createLiteral("1", XMLSchema.INTEGER), y);
		}
	}

	@Test
	public void testInComparison2() throws Exception {
		loadTestData("/testdata-query/dataset-ses1913.trig");
		StringBuilder query = new StringBuilder();
		query.append(" PREFIX : <http://example.org/>\n");
		query.append(" SELECT ?y WHERE { :a :p ?y. FILTER(?y in (:c, :d, 1/0)) } ");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try (TupleQueryResult result = tq.evaluate();) {
			assertNotNull(result);
			assertFalse(result.hasNext());
		}
	}

	@Test
	public void testInComparison3() throws Exception {
		loadTestData("/testdata-query/dataset-ses1913.trig");
		StringBuilder query = new StringBuilder();
		query.append(" PREFIX : <http://example.org/>\n");
		query.append(" SELECT ?y WHERE { :a :p ?y. FILTER(?y in (:c, :d, 1, 1/0)) } ");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try (TupleQueryResult result = tq.evaluate();) {
			assertNotNull(result);
			assertTrue(result.hasNext());

			BindingSet bs = result.next();
			Value y = bs.getValue("y");
			assertNotNull(y);
			assertTrue(y instanceof Literal);
			assertEquals(f.createLiteral("1", XMLSchema.INTEGER), y);
		}
	}

	@Test
	public void testSES2121URIFunction() throws Exception {
		String query = "SELECT (URI(\"foo bar\") as ?uri) WHERE {}";
		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
		try (TupleQueryResult result = tq.evaluate();) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			BindingSet bs = result.next();
			IRI uri = (IRI) bs.getValue("uri");
			assertTrue("uri result for invalid URI should be unbound", uri == null);
		}

		query = "BASE <http://example.org/> SELECT (URI(\"foo bar\") as ?uri) WHERE {}";
		tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
		try (TupleQueryResult result = tq.evaluate();) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			BindingSet bs = result.next();
			IRI uri = (IRI) bs.getValue("uri");
			assertTrue("uri result for valid URI reference should be bound", uri != null);
		}
	}

	@Test
	public void test27NormalizeIRIFunction() throws Exception {
		String query = "SELECT (IRI(\"../bar\") as ?Iri) WHERE {}";
		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query, "http://example.com/foo/");
		try (TupleQueryResult result = tq.evaluate();) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			BindingSet bs = result.next();
			IRI actual = (IRI) bs.getValue("Iri");
			IRI expected = f.createIRI("http://example.com/bar");
			assertEquals("IRI result for relative IRI should be normalized", expected, actual);
		}
	}

	@Test
	public void testSES869ValueOfNow() throws Exception {
		StringBuilder query = new StringBuilder();
		query.append("SELECT ?p ( NOW() as ?n ) { BIND (NOW() as ?p ) }");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try (TupleQueryResult result = tq.evaluate();) {
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

		try (TupleQueryResult result = tq.evaluate();) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			BindingSet bs = result.next();
			assertFalse("only one result expected", result.hasNext());
			assertEquals(a, bs.getValue("s"));
			assertEquals(b, bs.getValue("o"));
		}
	}

	@Test
	public void testRegexCaseNonAscii() throws Exception {
		String query = "ask {filter (regex(\"Валовой\", \"валовой\", \"i\")) }";

		assertTrue("case-insensitive match on Cyrillic should succeed", conn.prepareBooleanQuery(query).evaluate());

		query = "ask {filter (regex(\"Валовой\", \"валовой\")) }";

		assertFalse("case-sensitive match on Cyrillic should fail", conn.prepareBooleanQuery(query).evaluate());

	}

	@Test
	public void testValuesInOptional() throws Exception {
		loadTestData("/testdata-query/dataset-ses1692.trig");
		StringBuilder query = new StringBuilder();
		query.append(" PREFIX : <http://example.org/>\n");
		query.append(
				" SELECT DISTINCT ?a ?name ?isX WHERE { ?b :p1 ?a . ?a :name ?name. OPTIONAL { ?a a :X . VALUES(?isX) { (:X) } } } ");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try (TupleQueryResult result = tq.evaluate();) {
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
	 * See https://github.com/eclipse/rdf4j/issues/1978
	 */
	@Test
	public void testMaxAggregateWithGroupEmptyResult() throws Exception {
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
	public void testMaxAggregateWithoutGroupEmptySolution() throws Exception {
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
	public void testMinAggregateWithGroupEmptyResult() throws Exception {
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
	public void testMinAggregateWithoutGroupEmptySolution() throws Exception {
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
	public void testSampleAggregateWithGroupEmptyResult() throws Exception {
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
	public void testSampleAggregateWithoutGroupEmptySolution() throws Exception {
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
		StringBuilder query = new StringBuilder();
		query.append("SELECT ?p \n");
		query.append("WHERE { \n");
		query.append("         ?s ?p ?o . \n");
		query.append("        FILTER(IF(BOUND(?p), ?p = <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>, false)) \n");
		query.append("}");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
		try (TupleQueryResult result = tq.evaluate();) {
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
		StringBuilder query = new StringBuilder();
		query.append("SELECT ?p \n");
		query.append("WHERE { \n");
		query.append("         ?s ?p ?o . \n");
		query.append(
				"        FILTER(IF(!BOUND(?p), false , ?p = <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>)) \n");
		query.append("}");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
		try (TupleQueryResult result = tq.evaluate();) {
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

		try (TupleQueryResult result = tq.evaluate();) {
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

		try (TupleQueryResult result = tq.evaluate();) {
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

		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append(" SELECT ?node ?name ");
		query.append(" FROM ex:tree-graph ");
		query.append(" WHERE { ?node ex:hasParent+ ex:b . ?node ex:name ?name . }");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try (TupleQueryResult result = tq.evaluate();) {
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
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append(" SELECT *");
		query.append(" WHERE { ");
		query.append("       ?x foaf:name ?name ; ");
		query.append("          foaf:mbox ?mbox . ");
		query.append("       FILTER(EXISTS { ");
		query.append("            FILTER(REGEX(?name, \"Bo\") && REGEX(?mbox, \"bob\")) ");
		// query.append(" FILTER(REGEX(?mbox, \"bob\")) ");
		query.append("            } )");
		query.append(" } ");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try (TupleQueryResult result = tq.evaluate();) {
			assertNotNull(result);

			assertTrue(result.hasNext());
			int count = 0;
			while (result.hasNext()) {
				BindingSet bs = result.next();
				count++;
				// System.out.println(bs);
			}
			assertEquals(1, count);
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testGroupConcatNonDistinct() throws Exception {
		loadTestData("/testdata-query/dataset-query.trig");
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("SELECT (GROUP_CONCAT(?l) AS ?concat)");
		query.append("WHERE { ex:groupconcat-test ?p ?l . }");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try (TupleQueryResult result = tq.evaluate();) {
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

	@Test
	/**
	 * @see http://www.openrdf.org/issues/browse/SES-1091
	 * @throws Exception
	 */
	public void testArbitraryLengthPathWithBinding1() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl");
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("SELECT ?parent ?child ");
		query.append("WHERE { ?child a owl:Class . ?child rdfs:subClassOf+ ?parent . }");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try (TupleQueryResult result = tq.evaluate();) {
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

			try (TupleQueryResult result2 = tq.evaluate();) {
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

	@Test
	/**
	 * @see http://www.openrdf.org/issues/browse/SES-1091
	 * @throws Exception
	 */
	public void testArbitraryLengthPathWithBinding2() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl");

		// query without initializing ?child first.
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("SELECT ?parent ?child ");
		query.append("WHERE { ?child rdfs:subClassOf+ ?parent . }");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try (TupleQueryResult result = tq.evaluate();) {
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

			try (TupleQueryResult result2 = tq.evaluate();) {
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

	@Test
	/**
	 * @see http://www.openrdf.org/issues/browse/SES-1091
	 * @throws Exception
	 */
	public void testArbitraryLengthPathWithBinding3() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl");

		// binding on child instead of parent.
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("SELECT ?parent ?child ");
		query.append("WHERE { ?child rdfs:subClassOf+ ?parent . }");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try (TupleQueryResult result = tq.evaluate();) {
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

			try (TupleQueryResult result2 = tq.evaluate();) {
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

	@Test
	/**
	 * @see http://www.openrdf.org/issues/browse/SES-1091
	 * @throws Exception
	 */
	public void testArbitraryLengthPathWithBinding4() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl", this.alice);

		// binding on child instead of parent.
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("SELECT ?parent ?child ");
		query.append("WHERE { ?child rdfs:subClassOf+ ?parent . }");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try (TupleQueryResult result = tq.evaluate();) {
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

			try (TupleQueryResult result2 = tq.evaluate();) {
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

	@Test
	/**
	 * @see http://www.openrdf.org/issues/browse/SES-1091
	 * @throws Exception
	 */
	public void testArbitraryLengthPathWithBinding5() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl", this.alice, this.bob);

		// binding on child instead of parent.
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("SELECT ?parent ?child ");
		query.append("WHERE { ?child rdfs:subClassOf+ ?parent . }");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try (TupleQueryResult result = tq.evaluate();) {
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

			try (TupleQueryResult result2 = tq.evaluate();) {
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

	@Test
	/**
	 * @see http://www.openrdf.org/issues/browse/SES-1091
	 * @throws Exception
	 */
	public void testArbitraryLengthPathWithBinding6() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl", this.alice, this.bob, this.mary);

		// binding on child instead of parent.
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("SELECT ?parent ?child ");
		query.append("WHERE { ?child rdfs:subClassOf+ ?parent . }");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try (TupleQueryResult result = tq.evaluate();) {
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

			try (TupleQueryResult result2 = tq.evaluate();) {
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

	@Test
	/**
	 * @see http://www.openrdf.org/issues/browse/SES-1091
	 * @throws Exception
	 */
	public void testArbitraryLengthPathWithBinding7() throws Exception {
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

		try (TupleQueryResult result = tq.evaluate();) {
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

			try (TupleQueryResult result2 = tq.evaluate();) {
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

	@Test
	/**
	 * @see http://www.openrdf.org/issues/browse/SES-1091
	 * @throws Exception
	 */
	public void testArbitraryLengthPathWithBinding8() throws Exception {
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

		try (TupleQueryResult result = tq.evaluate();) {
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

			try (TupleQueryResult result2 = tq.evaluate();) {
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

	@Test
	/**
	 * @see http://www.openrdf.org/issues/browse/SES-1091
	 * @throws Exception
	 */
	public void testArbitraryLengthPathWithFilter1() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl");
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("SELECT ?parent ?child ");
		query.append("WHERE { ?child a owl:Class . ?child rdfs:subClassOf+ ?parent . FILTER (?parent = owl:Thing) }");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try (TupleQueryResult result = tq.evaluate();) {
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

	@Test
	/**
	 * @see http://www.openrdf.org/issues/browse/SES-1091
	 * @throws Exception
	 */
	public void testArbitraryLengthPathWithFilter2() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl");
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("SELECT ?parent ?child ");
		query.append("WHERE { ?child rdfs:subClassOf+ ?parent . FILTER (?parent = owl:Thing) }");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try (TupleQueryResult result = tq.evaluate();) {
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

	@Test
	/**
	 * @see http://www.openrdf.org/issues/browse/SES-1091
	 * @throws Exception
	 */
	public void testArbitraryLengthPathWithFilter3() throws Exception {
		loadTestData("/testdata-query/alp-testdata.ttl");
		StringBuilder query = new StringBuilder();
		query.append(getNamespaceDeclarations());
		query.append("SELECT ?parent ?child ");
		query.append("WHERE { ?child rdfs:subClassOf+ ?parent . FILTER (?child = <http://example.org/C>) }");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

		try (TupleQueryResult result = tq.evaluate();) {
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

	@Test
	public void testSES2147PropertyPathsWithIdenticalSubsPreds() throws Exception {

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

		try (TupleQueryResult result = tq.evaluate();) {

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

		try (TupleQueryResult result = tq.evaluate();) {
			assertNotNull(result);

			IRI uuid1 = (IRI) result.next().getValue("uid");
			IRI uuid2 = (IRI) result.next().getValue("uid");

			assertNotNull(uuid1);
			assertNotNull(uuid2);
			assertFalse(uuid1.equals(uuid2));
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

		try (TupleQueryResult result = tq.evaluate();) {
			assertNotNull(result);

			Literal uid1 = (Literal) result.next().getValue("uid");
			Literal uid2 = (Literal) result.next().getValue("uid");

			assertNotNull(uid1);
			assertFalse(uid1.equals(uid2));
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

		try (TupleQueryResult result = tq.evaluate();) {
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

		try (TupleQueryResult result = tq.evaluate();) {
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

		try (TupleQueryResult result = tq.evaluate();) {
			assertNotNull(result);

			BindingSet bs = result.next();
			Literal l1 = (Literal) bs.getValue("l1");
			Literal l2 = (Literal) bs.getValue("l2");

			assertNotNull(l1);
			assertFalse(l1.equals(l2));
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testSES2361UndefMin() throws Exception {
		String query = "SELECT (MIN(?v) as ?min) WHERE { VALUES ?v { 1 2 undef 3 4 }}";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate();) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			assertEquals("1", result.next().getValue("min").stringValue());
			assertFalse(result.hasNext());
		}
	}

	@Test
	public void testSES2361UndefMax() throws Exception {
		String query = "SELECT (MAX(?v) as ?max) WHERE { VALUES ?v { 1 2 7 undef 3 4 }}";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate();) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			assertEquals("7", result.next().getValue("max").stringValue());
			assertFalse(result.hasNext());
		}
	}

	@Test
	public void testSES2361UndefCount() throws Exception {
		String query = "SELECT (COUNT(?v) as ?c) WHERE { VALUES ?v { 1 2 undef 3 4 }}";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate();) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			assertEquals("4", result.next().getValue("c").stringValue());
			assertFalse(result.hasNext());
		}
	}

	@Test
	public void testSES2361UndefCountWildcard() throws Exception {
		String query = "SELECT (COUNT(*) as ?c) WHERE { VALUES ?v { 1 2 undef 3 4 }}";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate();) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			assertEquals("4", result.next().getValue("c").stringValue());
			assertFalse(result.hasNext());
		}
	}

	@Test
	public void testSES2361UndefSum() throws Exception {
		String query = "SELECT (SUM(?v) as ?s) WHERE { VALUES ?v { 1 2 undef 3 4 }}";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate();) {
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

		try (TupleQueryResult evaluate = tq.evaluate();) {
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

		try (TupleQueryResult evaluate = tq.evaluate();) {
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
	public void testBindError() throws Exception {
		StringBuilder ub = new StringBuilder();
		ub.append("insert data { <urn:test:subj> <urn:test:pred> _:blank }");

		conn.prepareUpdate(QueryLanguage.SPARQL, ub.toString()).execute();

		StringBuilder qb = new StringBuilder();

		qb.append("SELECT * \n");
		qb.append("WHERE { \n");
		qb.append("  VALUES (?NAValue) { (<http://null>) } \n ");
		qb.append("  BIND(IF(?NAValue != <http://null>, ?NAValue, ?notBoundVar) as ?ValidNAValue) \n ");
		qb.append("  { ?disjClass (owl:disjointWith|^owl:disjointWith)? ?disjClass2 . }\n");
		qb.append("}\n");

		List<BindingSet> result = QueryResults.asList(conn.prepareTupleQuery(qb.toString()).evaluate());

		assertEquals("query should return 2 solutions", 2, result.size());
	}

	@Test
	/**
	 * See https://github.com/eclipse/rdf4j/issues/1405
	 */
	public void testBindScope() throws Exception {
		String query = "SELECT * {\n" +
				"  { BIND (\"a\" AS ?a) }\n" +
				"  { BIND (?a AS ?b) } \n" +
				"}";

		List<BindingSet> result = QueryResults.asList(conn.prepareTupleQuery(query).evaluate());

		assertEquals(1, result.size());

		assertEquals(conn.getValueFactory().createLiteral("a"), result.get(0).getValue("a"));
		assertNull(result.get(0).getValue("b"));
	}

	@Test
	/**
	 * See https://github.com/eclipse/rdf4j/issues/1642
	 */
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

		List<BindingSet> result = QueryResults.asList(conn.prepareTupleQuery(query).evaluate());

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
	public void testSES2250BindErrors() throws Exception {
		StringBuilder ub = new StringBuilder();
		ub.append("insert data { <urn:test:subj> <urn:test:pred> _:blank }");

		conn.prepareUpdate(QueryLanguage.SPARQL, ub.toString()).execute();

		StringBuilder qb = new StringBuilder();
		qb.append("SELECT * {\n" + "    ?s1 ?p1 ?blank . " + "    FILTER(isBlank(?blank))"
				+ "    BIND (iri(?blank) as ?biri)" + "    ?biri ?p2 ?o2 ." + "}");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, qb.toString());
		try (TupleQueryResult evaluate = tq.evaluate();) {
			assertFalse("The query should not return a result", evaluate.hasNext());
		}
	}

	@Test
	public void testSES2250BindErrorsInPath() throws Exception {
		StringBuilder ub = new StringBuilder();
		ub.append("insert data { <urn:test:subj> <urn:test:pred> _:blank }");

		conn.prepareUpdate(QueryLanguage.SPARQL, ub.toString()).execute();

		StringBuilder qb = new StringBuilder();
		qb.append("SELECT * {\n" + "    ?s1 ?p1 ?blank . " + "    FILTER(isBlank(?blank))"
				+ "    BIND (iri(?blank) as ?biri)" + "    ?biri <urn:test:pred>* ?o2 ." + "}");

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, qb.toString());
		try (TupleQueryResult evaluate = tq.evaluate();) {
			assertFalse("The query should not return a result", evaluate.hasNext());
		}
	}

	@Test
	public void testEmptyUnion() throws Exception {
		String query = "PREFIX : <http://example.org/> " + "SELECT ?visibility WHERE {"
				+ "OPTIONAL { SELECT ?var WHERE { :s a :MyType . BIND (:s as ?var ) .} } ."
				+ "BIND (IF(BOUND(?var), 'VISIBLE', 'HIDDEN') as ?visibility)" + "}";
		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate();) {
			assertNotNull(result);
			assertFalse(result.hasNext());
		}
	}

	/**
	 * https://github.com/eclipse/rdf4j/issues/1026
	 */
	@Test
	public void testFilterExistsExternalValuesClause() throws Exception {
		StringBuilder ub = new StringBuilder();
		ub.append("insert data {\n");
		ub.append("  <http://subj1> a <http://type> .\n");
		ub.append("  <http://subj2> a <http://type> .\n");
		ub.append("  <http://subj1> <http://predicate> <http://obj1> .\n");
		ub.append("  <http://subj2> <http://predicate> <http://obj2> .\n");
		ub.append("}");
		conn.prepareUpdate(QueryLanguage.SPARQL, ub.toString()).execute();

		String query = "select ?s  {\n" + "    ?s a* <http://type> .\n"
				+ "    FILTER EXISTS {?s <http://predicate> ?o}\n"
				+ "} limit 100 values ?o {<http://obj1>}";

		TupleQuery tq = conn.prepareTupleQuery(query);

		List<BindingSet> result = QueryResults.asList(tq.evaluate());
		assertEquals("single result expected", 1, result.size());
		assertEquals("http://subj1", result.get(0).getValue("s").stringValue());
	}

	/**
	 * See https://github.com/eclipse/rdf4j/issues/1267
	 */
	@Test
	public void testSeconds() throws Exception {
		String qry = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
				+ "SELECT (SECONDS(\"2011-01-10T14:45:13\"^^xsd:dateTime) AS ?sec) { }";

		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, qry).evaluate();) {
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
	public void testSecondsMilliseconds() throws Exception {
		String qry = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
				+ "SELECT (SECONDS(\"2011-01-10T14:45:13.815-05:00\"^^xsd:dateTime) AS ?sec) { }";

		try (TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, qry).evaluate();) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			assertEquals("13.815", result.next().getValue("sec").stringValue());
			assertFalse(result.hasNext());
		}
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
		StringBuilder declarations = new StringBuilder();
		declarations.append("PREFIX dc: <" + DCTERMS.NAMESPACE + "> \n");
		declarations.append("PREFIX foaf: <" + FOAF.NAMESPACE + "> \n");
		declarations.append("PREFIX sesame: <" + SESAME.NAMESPACE + "> \n");
		declarations.append("PREFIX ex: <" + EX_NS + "> \n");
		declarations.append("\n");

		return declarations.toString();
	}

	protected abstract Repository newRepository() throws Exception;

	protected void loadTestData(String dataFile, Resource... contexts)
			throws RDFParseException, RepositoryException, IOException {
		logger.debug("loading dataset {}", dataFile);
		try (InputStream dataset = ComplexSPARQLQueryTest.class.getResourceAsStream(dataFile);) {
			conn.add(dataset, "", Rio.getParserFormatForFileName(dataFile).orElseThrow(Rio.unsupportedFormat(dataFile)),
					contexts);
		}
		logger.debug("dataset loaded.");
	}
}
