/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.query.parser.sparql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for SPARQL 1.1 Update functionality.
 *
 * @author Jeen Broekstra
 */
public abstract class SPARQLUpdateTest {

	protected static final Logger logger = LoggerFactory.getLogger(SPARQLUpdateTest.class);

	private Repository rep;

	protected RepositoryConnection con;

	protected ValueFactory f;

	protected IRI bob;

	protected IRI alice;

	protected IRI graph1;

	protected IRI graph2;

	protected static final String EX_NS = "http://example.org/";

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		logger.debug("setting up test");

		rep = createRepository();
		con = rep.getConnection();
		f = rep.getValueFactory();

		loadDataset("/testdata-update/dataset-update.trig");

		bob = f.createIRI(EX_NS, "bob");
		alice = f.createIRI(EX_NS, "alice");

		graph1 = f.createIRI(EX_NS, "graph1");
		graph2 = f.createIRI(EX_NS, "graph2");

		logger.debug("setup complete.");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		logger.debug("tearing down...");
		con.close();
		con = null;

		rep.shutDown();
		rep = null;

		logger.debug("tearDown complete.");
	}

	/* test methods */

	@Test
	public void testDeleteFromDefaultGraph() throws Exception {

		con.add(RDF.FIRST, RDF.FIRST, RDF.FIRST);
		con.add(RDF.FIRST, RDF.FIRST, RDF.FIRST, RDF.ALT);
		con.add(RDF.FIRST, RDF.FIRST, RDF.FIRST, RDF.BAG);

		String update = getNamespaceDeclarations() +
				"DELETE { GRAPH sesame:nil { ?s ?p ?o } } WHERE { GRAPH rdf:Alt { ?s ?p ?o } }";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);
		operation.execute();

		assertTrue(con.hasStatement(RDF.FIRST, RDF.FIRST, RDF.FIRST, true, RDF.ALT));
		assertTrue(con.hasStatement(RDF.FIRST, RDF.FIRST, RDF.FIRST, true, RDF.BAG));
		assertFalse(con.hasStatement(RDF.FIRST, RDF.FIRST, RDF.FIRST, true, RDF4J.NIL));
		assertFalse(con.hasStatement(RDF.FIRST, RDF.FIRST, RDF.FIRST, true, (Resource) null));
	}

	@Test
	public void testDeleteFromDefaultGraphUsingWith() throws Exception {

		con.add(RDF.FIRST, RDF.FIRST, RDF.FIRST);
		con.add(RDF.FIRST, RDF.FIRST, RDF.FIRST, RDF.ALT);
		con.add(RDF.FIRST, RDF.FIRST, RDF.FIRST, RDF.BAG);

		String update = getNamespaceDeclarations() +
				"WITH sesame:nil DELETE { ?s ?p ?o  } WHERE { ?s ?p ?o }";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);
		operation.execute();

		assertTrue(con.hasStatement(RDF.FIRST, RDF.FIRST, RDF.FIRST, true, RDF.ALT));
		assertTrue(con.hasStatement(RDF.FIRST, RDF.FIRST, RDF.FIRST, true, RDF.BAG));
		assertFalse(con.hasStatement(RDF.FIRST, RDF.FIRST, RDF.FIRST, true, RDF4J.NIL));
		assertFalse(con.hasStatement(RDF.FIRST, RDF.FIRST, RDF.FIRST, true, (Resource) null));
	}

	@Test
	public void testInsertWhereInvalidTriple() throws Exception {
		String update = getNamespaceDeclarations() +
				"INSERT {?name a foaf:Person. ?x a <urn:TestSubject>. } WHERE { ?x foaf:name ?name }";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		try {
			operation.execute();
		} catch (ClassCastException e) {
			fail("subject-literal triple pattern should be silently ignored");
		}

		assertTrue(
				con.hasStatement((Resource) null, RDF.TYPE, con.getValueFactory().createIRI("urn:TestSubject"), true));
	}

	@Test
	public void testDeleteWhereInvalidTriple() throws Exception {
		String update = getNamespaceDeclarations() +
				"DELETE {?name a foaf:Person. ?x foaf:name ?name } WHERE { ?x foaf:name ?name }";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		try {
			operation.execute();
		} catch (ClassCastException e) {
			fail("subject-literal triple pattern should be silently ignored");
		}
		assertFalse(con.hasStatement(null, FOAF.NAME, null, true));
	}

	@Test
	public void testDeleteInsertWhereInvalidTriple() throws Exception {
		String update = getNamespaceDeclarations() +
				"DELETE {?name a foaf:Person} INSERT {?name a foaf:Agent} WHERE { ?x foaf:name ?name }";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		try {
			operation.execute();
		} catch (ClassCastException e) {
			fail("subject-literal triple pattern should be silently ignored");
		}
	}

	@Test
	public void testInsertWhere() throws Exception {
		logger.debug("executing test InsertWhere");
		String update = getNamespaceDeclarations() +
				"INSERT {?x rdfs:label ?y . } WHERE {?x foaf:name ?y }";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		assertFalse(con.hasStatement(bob, RDFS.LABEL, f.createLiteral("Bob"), true));
		assertFalse(con.hasStatement(alice, RDFS.LABEL, f.createLiteral("Alice"), true));

		operation.execute();

		assertTrue(con.hasStatement(bob, RDFS.LABEL, f.createLiteral("Bob"), true));
		assertTrue(con.hasStatement(alice, RDFS.LABEL, f.createLiteral("Alice"), true));
	}

	@Test
	public void testInsertWhereWithBinding() throws Exception {
		logger.debug("executing test testInsertWhereWithBinding");
		String update = getNamespaceDeclarations() +
				"INSERT {?x rdfs:label ?y . } WHERE {?x foaf:name ?y }";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);
		operation.setBinding("x", bob);

		assertFalse(con.hasStatement(bob, RDFS.LABEL, f.createLiteral("Bob"), true));
		assertFalse(con.hasStatement(alice, RDFS.LABEL, f.createLiteral("Alice"), true));

		operation.execute();

		assertTrue(con.hasStatement(bob, RDFS.LABEL, f.createLiteral("Bob"), true));
		assertFalse(con.hasStatement(alice, RDFS.LABEL, f.createLiteral("Alice"), true));
	}

	@Test
	public void testInsertWhereWithBindings2() throws Exception {
		logger.debug("executing test testInsertWhereWithBindings2");
		String update = getNamespaceDeclarations() +
				"INSERT {?x rdfs:label ?z . } WHERE {?x foaf:name ?y }";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);
		operation.setBinding("z", f.createLiteral("Bobbie"));
		operation.setBinding("x", bob);

		assertFalse(con.hasStatement(bob, RDFS.LABEL, f.createLiteral("Bobbie"), true));
		assertFalse(con.hasStatement(alice, RDFS.LABEL, null, true));

		operation.execute();

		assertTrue(con.hasStatement(bob, RDFS.LABEL, f.createLiteral("Bobbie"), true));
		assertFalse(con.hasStatement(alice, RDFS.LABEL, f.createLiteral("Alice"), true));
	}

	@Test
	public void testInsertEmptyWhere() throws Exception {
		logger.debug("executing test testInsertEmptyWhere");
		String update = getNamespaceDeclarations() +
				"INSERT { <" + bob + "> rdfs:label \"Bob\" . } WHERE { }";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		assertFalse(con.hasStatement(bob, RDFS.LABEL, f.createLiteral("Bob"), true));

		operation.execute();

		assertTrue(con.hasStatement(bob, RDFS.LABEL, f.createLiteral("Bob"), true));
	}

	@Test
	public void testInsertEmptyWhereWithBinding() throws Exception {
		logger.debug("executing test testInsertEmptyWhereWithBinding");
		String update = getNamespaceDeclarations() +
				"INSERT {?x rdfs:label ?y . } WHERE { }";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);
		operation.setBinding("x", bob);
		operation.setBinding("y", f.createLiteral("Bob"));

		assertFalse(con.hasStatement(bob, RDFS.LABEL, f.createLiteral("Bob"), true));

		operation.execute();

		assertTrue(con.hasStatement(bob, RDFS.LABEL, f.createLiteral("Bob"), true));
	}

	@Test
	public void testInsertNonMatchingWhere() throws Exception {
		logger.debug("executing test testInsertNonMatchingWhere");
		String update = getNamespaceDeclarations() +
				"INSERT { ?x rdfs:label ?y . } WHERE { ?x rdfs:comment ?y }";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		assertFalse(con.hasStatement(bob, RDFS.LABEL, null, true));

		operation.execute();

		assertFalse(con.hasStatement(bob, RDFS.LABEL, null, true));
	}

	@Test
	public void testInsertNonMatchingWhereWithBindings() throws Exception {
		logger.debug("executing test testInsertNonMatchingWhereWithBindings");
		String update = getNamespaceDeclarations() +
				"INSERT { ?x rdfs:label ?y . } WHERE { ?x rdfs:comment ?y }";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);
		operation.setBinding("x", bob);
		operation.setBinding("y", f.createLiteral("Bob"));

		assertFalse(con.hasStatement(bob, RDFS.LABEL, null, true));

		operation.execute();

		assertFalse(con.hasStatement(bob, RDFS.LABEL, null, true));
	}

	@Test
	public void testInsertWhereWithBindings() throws Exception {
		logger.debug("executing test testInsertWhereWithBindings");
		String update = getNamespaceDeclarations() +
				"INSERT { ?x rdfs:comment ?z . } WHERE { ?x foaf:name ?y }";

		Literal comment = f.createLiteral("Bob has a comment");

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);
		operation.setBinding("x", bob);
		operation.setBinding("z", comment);

		assertFalse(con.hasStatement(null, RDFS.COMMENT, comment, true));

		operation.execute();

		assertTrue(con.hasStatement(bob, RDFS.COMMENT, comment, true));
		assertFalse(con.hasStatement(alice, RDFS.COMMENT, comment, true));

	}

	@Test
	public void testInsertWhereWithOptional() throws Exception {
		logger.debug("executing testInsertWhereWithOptional");

		String update = getNamespaceDeclarations() +
				" INSERT { ?s ex:age ?incAge } " +
				// update.append(" DELETE { ?s ex:age ?age } ");
				" WHERE { ?s foaf:name ?name . " +
				" OPTIONAL {?s ex:age ?age . BIND ((?age + 1) as ?incAge)  } " +
				" } ";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		IRI age = f.createIRI(EX_NS, "age");

		assertFalse(con.hasStatement(alice, age, null, true));
		assertTrue(con.hasStatement(bob, age, null, true));

		operation.execute();

		RepositoryResult<Statement> result = con.getStatements(bob, age, null, true);

		while (result.hasNext()) {
			System.out.println(result.next().toString());
		}

		assertTrue(con.hasStatement(bob, age, f.createLiteral("43", XSD.INTEGER), true));

		result = con.getStatements(alice, age, null, true);

		while (result.hasNext()) {
			System.out.println(result.next());
		}
		assertFalse(con.hasStatement(alice, age, null, true));
	}

	@Test
	public void testInsertWhereWithBlankNode() throws Exception {
		logger.debug("executing testInsertWhereWithBlankNode");

		String update = getNamespaceDeclarations() +
				" INSERT { ?s ex:complexAge [ rdf:value ?age; rdfs:label \"old\" ] . } " +
				" WHERE { ?s ex:age ?age . " +
				" } ";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		IRI age = f.createIRI(EX_NS, "age");
		IRI complexAge = f.createIRI(EX_NS, "complexAge");

		assertTrue(con.hasStatement(bob, age, null, true));

		operation.execute();

		RepositoryResult<Statement> sts = con.getStatements(bob, complexAge, null, true);

		assertTrue(sts.hasNext());

		Value v1 = sts.next().getObject();

		sts.close();

		sts = con.getStatements(null, RDF.VALUE, null, true);

		assertTrue(sts.hasNext());

		Value v2 = sts.next().getSubject();

		assertEquals(v1, v2);

		sts.close();

		String query = getNamespaceDeclarations()
				+ " SELECT ?bn ?age ?l WHERE { ex:bob ex:complexAge ?bn. ?bn rdf:value ?age. ?bn rdfs:label ?l .} ";

		TupleQueryResult result = con.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate();

		assertTrue(result.hasNext());

		BindingSet bs = result.next();

		assertFalse(result.hasNext());

	}

	@Test
	public void testDeleteInsertWhere() throws Exception {
		logger.debug("executing test DeleteInsertWhere");
		String update = getNamespaceDeclarations() +
				"DELETE { ?x foaf:name ?y } INSERT {?x rdfs:label ?y . } WHERE {?x foaf:name ?y }";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		assertFalse(con.hasStatement(bob, RDFS.LABEL, f.createLiteral("Bob"), true));
		assertFalse(con.hasStatement(alice, RDFS.LABEL, f.createLiteral("Alice"), true));

		operation.execute();

		assertTrue(con.hasStatement(bob, RDFS.LABEL, f.createLiteral("Bob"), true));
		assertTrue(con.hasStatement(alice, RDFS.LABEL, f.createLiteral("Alice"), true));

		assertFalse(con.hasStatement(bob, FOAF.NAME, f.createLiteral("Bob"), true));
		assertFalse(con.hasStatement(alice, FOAF.NAME, f.createLiteral("Alice"), true));

	}

	@Test
	public void testDeleteWhereOptional() throws Exception {
		logger.debug("executing test testDeleteWhereOptional");
		String update = getNamespaceDeclarations() +
				" DELETE { ?x foaf:name ?y; foaf:mbox ?mbox. } " +
				" WHERE {?x foaf:name ?y. " +
				" OPTIONAL { ?x foaf:mbox ?mbox. FILTER (str(?mbox) = \"bob@example.org\") } }";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		Literal mboxBob = f.createLiteral("bob@example.org");
		Literal mboxAlice = f.createLiteral("alice@example.org");
		assertTrue(con.hasStatement(bob, FOAF.MBOX, mboxBob, true));
		assertTrue(con.hasStatement(alice, FOAF.MBOX, mboxAlice, true));

		assertTrue(con.hasStatement(bob, FOAF.NAME, f.createLiteral("Bob"), true));
		assertTrue(con.hasStatement(alice, FOAF.NAME, f.createLiteral("Alice"), true));

		operation.execute();

		assertFalse(con.hasStatement(bob, FOAF.MBOX, mboxBob, true));
		assertTrue(con.hasStatement(alice, FOAF.MBOX, mboxAlice, true));

		assertFalse(con.hasStatement(bob, FOAF.NAME, f.createLiteral("Bob"), true));
		assertFalse(con.hasStatement(alice, FOAF.NAME, f.createLiteral("Alice"), true));

	}

	@Test
	public void testDeleteInsertWhereWithBindings() throws Exception {
		logger.debug("executing test testDeleteInsertWhereWithBindings");
		String update = getNamespaceDeclarations() +
				"DELETE { ?x foaf:name ?y } INSERT {?x rdfs:label ?y . } WHERE {?x foaf:name ?y }";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		operation.setBinding("x", bob);

		assertFalse(con.hasStatement(bob, RDFS.LABEL, f.createLiteral("Bob"), true));
		assertFalse(con.hasStatement(alice, RDFS.LABEL, f.createLiteral("Alice"), true));

		operation.execute();

		assertTrue(con.hasStatement(bob, RDFS.LABEL, f.createLiteral("Bob"), true));
		assertFalse(con.hasStatement(alice, RDFS.LABEL, f.createLiteral("Alice"), true));

		assertFalse(con.hasStatement(bob, FOAF.NAME, f.createLiteral("Bob"), true));
		assertTrue(con.hasStatement(alice, FOAF.NAME, f.createLiteral("Alice"), true));
	}

	@Test
	public void testDeleteInsertWhereWithBindings2() throws Exception {
		logger.debug("executing test testDeleteInsertWhereWithBindings2");
		String update = getNamespaceDeclarations() +
				"DELETE { ?x foaf:name ?y } INSERT {?x rdfs:label ?z . } WHERE {?x foaf:name ?y }";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		operation.setBinding("z", f.createLiteral("person"));

		assertFalse(con.hasStatement(bob, RDFS.LABEL, f.createLiteral("Bob"), true));
		assertFalse(con.hasStatement(alice, RDFS.LABEL, f.createLiteral("Alice"), true));

		operation.execute();

		assertTrue(con.hasStatement(bob, RDFS.LABEL, f.createLiteral("person"), true));
		assertTrue(con.hasStatement(alice, RDFS.LABEL, f.createLiteral("person"), true));

		assertFalse(con.hasStatement(bob, FOAF.NAME, f.createLiteral("Bob"), true));
		assertFalse(con.hasStatement(alice, FOAF.NAME, f.createLiteral("Alice"), true));
	}

	@Test
	public void testDeleteInsertWhereLoopingBehavior() throws Exception {
		logger.debug("executing test testDeleteInsertWhereLoopingBehavior");
		String update = getNamespaceDeclarations() +
				" DELETE { ?x ex:age ?y } INSERT {?x ex:age ?z }" +
				" WHERE { " +
				"   ?x ex:age ?y ." +
				"   BIND((?y + 1) as ?z) " +
				"   FILTER( ?y < 46 ) " +
				" } ";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		IRI age = f.createIRI(EX_NS, "age");
		Literal originalAgeValue = f.createLiteral("42", XSD.INTEGER);
		Literal correctAgeValue = f.createLiteral("43", CoreDatatype.XSD.INTEGER);
		Literal inCorrectAgeValue = f.createLiteral("46", XSD.INTEGER);

		assertTrue(con.hasStatement(bob, age, originalAgeValue, true));

		operation.execute();

		assertFalse(con.hasStatement(bob, age, originalAgeValue, true));
		assertTrue(con.hasStatement(bob, age, correctAgeValue, true));
		assertFalse(con.hasStatement(bob, age, inCorrectAgeValue, true));
	}

	@Test
	public void testAutoCommitHandling() throws Exception {
		logger.debug("executing test testAutoCommitHandling");

		StringBuilder update = new StringBuilder();
		update.append(getNamespaceDeclarations());
		update.append("DELETE { ?x foaf:name ?y } INSERT {?x rdfs:label ?y . } WHERE {?x foaf:name ?y }");

		try {
			con.begin();
			Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update.toString());

			assertFalse(con.hasStatement(bob, RDFS.LABEL, f.createLiteral("Bob"), true));
			assertFalse(con.hasStatement(alice, RDFS.LABEL, f.createLiteral("Alice"), true));

			operation.execute();

			// update should be visible to own connection.
			assertTrue(con.hasStatement(bob, RDFS.LABEL, f.createLiteral("Bob"), true));
			assertTrue(con.hasStatement(alice, RDFS.LABEL, f.createLiteral("Alice"), true));

			assertFalse(con.hasStatement(bob, FOAF.NAME, f.createLiteral("Bob"), true));
			assertFalse(con.hasStatement(alice, FOAF.NAME, f.createLiteral("Alice"), true));

			try (RepositoryConnection con2 = rep.getConnection()) {
				// update should not yet be visible to separate connection
				assertFalse(con2.hasStatement(bob, RDFS.LABEL, f.createLiteral("Bob"), true));
				assertFalse(con2.hasStatement(alice, RDFS.LABEL, f.createLiteral("Alice"), true));

				assertTrue(con2.hasStatement(bob, FOAF.NAME, f.createLiteral("Bob"), true));
				assertTrue(con2.hasStatement(alice, FOAF.NAME, f.createLiteral("Alice"), true));

				con.commit();

				// after commit, update should be visible to separate connection.
				assertTrue(con2.hasStatement(bob, RDFS.LABEL, f.createLiteral("Bob"), true));
				assertTrue(con2.hasStatement(alice, RDFS.LABEL, f.createLiteral("Alice"), true));

				assertFalse(con2.hasStatement(bob, FOAF.NAME, f.createLiteral("Bob"), true));
				assertFalse(con2.hasStatement(alice, FOAF.NAME, f.createLiteral("Alice"), true));
			}
		} catch (Exception e) {
			if (con.isActive()) {
				con.rollback();
			}
		} finally {
			con.close();
		}
	}

	@Test
	public void testConsecutiveUpdatesInSameTransaction() throws Exception {
		// this tests if consecutive updates in the same transaction behave
		// correctly. See issue SES-930
		logger.debug("executing test testConsecutiveUpdatesInSameTransaction");

		StringBuilder update1 = new StringBuilder();
		update1.append(getNamespaceDeclarations());
		update1.append("DELETE { ?x foaf:name ?y } WHERE {?x foaf:name ?y }");

		try {
			con.begin();
			Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update1.toString());

			assertFalse(con.hasStatement(bob, RDFS.LABEL, f.createLiteral("Bob"), true));
			assertFalse(con.hasStatement(alice, RDFS.LABEL, f.createLiteral("Alice"), true));

			operation.execute();

			// update should be visible to own connection.
			assertFalse(con.hasStatement(bob, FOAF.NAME, f.createLiteral("Bob"), true));
			assertFalse(con.hasStatement(alice, FOAF.NAME, f.createLiteral("Alice"), true));

			String update2 = getNamespaceDeclarations() +
					"INSERT { ?x rdfs:label ?y } WHERE {?x foaf:name ?y }";

			operation = con.prepareUpdate(QueryLanguage.SPARQL, update2);

			operation.execute();

			con.commit();

			// update should not have resulted in any inserts: where clause is
			// empty.
			assertFalse(con.hasStatement(bob, RDFS.LABEL, f.createLiteral("Bob"), true));
			assertFalse(con.hasStatement(alice, RDFS.LABEL, f.createLiteral("Alice"), true));
		} catch (Exception e) {
			if (con.isActive()) {
				con.rollback();
			}
		}
	}

	@Test
	public void testInsertTransformedWhere() throws Exception {
		logger.debug("executing test InsertTransformedWhere");

		String update = getNamespaceDeclarations() +
				"INSERT {?x rdfs:label [] . } WHERE {?y ex:containsPerson ?x.  }";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		assertFalse(con.hasStatement(bob, RDFS.LABEL, null, true));
		assertFalse(con.hasStatement(alice, RDFS.LABEL, null, true));

		operation.execute();

		assertTrue(con.hasStatement(bob, RDFS.LABEL, null, true));
		assertTrue(con.hasStatement(alice, RDFS.LABEL, null, true));
	}

	@Test
	public void testInsertWhereGraph() throws Exception {
		logger.debug("executing testInsertWhereGraph");
		String update = getNamespaceDeclarations() +
				"INSERT {GRAPH ?g {?x rdfs:label ?y . }} WHERE {GRAPH ?g {?x foaf:name ?y }}";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		operation.execute();

		String message = "labels should have been inserted in corresponding named graphs only.";
		assertTrue(message, con.hasStatement(bob, RDFS.LABEL, f.createLiteral("Bob"), true, graph1));
		assertFalse(message, con.hasStatement(bob, RDFS.LABEL, f.createLiteral("Bob"), true, graph2));
		assertTrue(message, con.hasStatement(alice, RDFS.LABEL, f.createLiteral("Alice"), true, graph2));
		assertFalse(message, con.hasStatement(alice, RDFS.LABEL, f.createLiteral("Alice"), true, graph1));
	}

	@Test
	public void testInsertWhereUsing() throws Exception {

		logger.debug("executing testInsertWhereUsing");
		String update = getNamespaceDeclarations() +
				"INSERT {?x rdfs:label ?y . } USING ex:graph1 WHERE {?x foaf:name ?y }";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		operation.execute();

		String message = "label should have been inserted in default graph, for ex:bob only";
		assertTrue(message, con.hasStatement(bob, RDFS.LABEL, f.createLiteral("Bob"), true));
		assertFalse(message, con.hasStatement(bob, RDFS.LABEL, f.createLiteral("Bob"), true, graph1));
		assertFalse(message, con.hasStatement(bob, RDFS.LABEL, f.createLiteral("Bob"), true, graph2));
		assertFalse(message, con.hasStatement(alice, RDFS.LABEL, f.createLiteral("Alice"), true));
	}

	@Test
	public void testInsertWhereUsingWith() throws Exception {

		logger.debug("executing testInsertWhereUsingWith");
		String update = getNamespaceDeclarations() +
				"WITH ex:graph2 INSERT {?x rdfs:label ?y . } USING ex:graph1 WHERE {?x foaf:name ?y }";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		operation.execute();

		String message = "label should have been inserted in graph2, for ex:bob only";
		assertTrue(message, con.hasStatement(bob, RDFS.LABEL, f.createLiteral("Bob"), true, graph2));
		assertFalse(message, con.hasStatement(bob, RDFS.LABEL, f.createLiteral("Bob"), true, graph1));
		assertFalse(message, con.hasStatement(alice, RDFS.LABEL, f.createLiteral("Alice"), true));
	}

	@Test
	public void testInsertWhereWith() throws Exception {
		logger.debug("executing testInsertWhereWith");

		String update = getNamespaceDeclarations() +
				"WITH ex:graph1 INSERT {?x rdfs:label ?y . } WHERE {?x foaf:name ?y }";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		operation.execute();

		String message = "label should have been inserted in graph1 only, for ex:bob only";
		assertTrue(message, con.hasStatement(bob, RDFS.LABEL, f.createLiteral("Bob"), true, graph1));
		assertFalse(message, con.hasStatement(bob, RDFS.LABEL, f.createLiteral("Bob"), true, graph2));
		assertFalse(message, con.hasStatement(alice, RDFS.LABEL, f.createLiteral("Alice"), true, graph2));
		assertFalse(message, con.hasStatement(alice, RDFS.LABEL, f.createLiteral("Alice"), true, graph1));
	}

	@Test
	public void testDeleteWhereShortcut() throws Exception {
		logger.debug("executing testDeleteWhereShortcut");

		String update = getNamespaceDeclarations() +
				"DELETE WHERE {?x foaf:name ?y }";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		assertTrue(con.hasStatement(bob, FOAF.NAME, f.createLiteral("Bob"), true));
		assertTrue(con.hasStatement(alice, FOAF.NAME, f.createLiteral("Alice"), true));

		operation.execute();

		String msg = "foaf:name properties should have been deleted";
		assertFalse(msg, con.hasStatement(bob, FOAF.NAME, f.createLiteral("Bob"), true));
		assertFalse(msg, con.hasStatement(alice, FOAF.NAME, f.createLiteral("Alice"), true));

		msg = "foaf:knows properties should not have been deleted";
		assertTrue(msg, con.hasStatement(bob, FOAF.KNOWS, null, true));
		assertTrue(msg, con.hasStatement(alice, FOAF.KNOWS, null, true));
	}

	@Test
	public void testDeleteWhere() throws Exception {
		logger.debug("executing testDeleteWhere");

		String update = getNamespaceDeclarations() +
				"DELETE {?x foaf:name ?y } WHERE {?x foaf:name ?y }";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		assertTrue(con.hasStatement(bob, FOAF.NAME, f.createLiteral("Bob"), true));
		assertTrue(con.hasStatement(alice, FOAF.NAME, f.createLiteral("Alice"), true));

		operation.execute();

		String msg = "foaf:name properties should have been deleted";
		assertFalse(msg, con.hasStatement(bob, FOAF.NAME, f.createLiteral("Bob"), true));
		assertFalse(msg, con.hasStatement(alice, FOAF.NAME, f.createLiteral("Alice"), true));

	}

	@Test
	public void testDeleteTransformedWhere() throws Exception {
		logger.debug("executing testDeleteTransformedWhere");

		String update = getNamespaceDeclarations() +
				"DELETE {?y foaf:name ?n } WHERE {?x ex:containsPerson ?y . ?y foaf:name ?n . }";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		assertTrue(con.hasStatement(bob, FOAF.NAME, f.createLiteral("Bob"), true));
		assertTrue(con.hasStatement(alice, FOAF.NAME, f.createLiteral("Alice"), true));

		operation.execute();

		String msg = "foaf:name properties should have been deleted";
		assertFalse(msg, con.hasStatement(bob, FOAF.NAME, f.createLiteral("Bob"), true));
		assertFalse(msg, con.hasStatement(alice, FOAF.NAME, f.createLiteral("Alice"), true));

		msg = "ex:containsPerson properties should not have been deleted";
		assertTrue(msg, con.hasStatement(graph1, f.createIRI(EX_NS, "containsPerson"), bob, true));
		assertTrue(msg, con.hasStatement(graph2, f.createIRI(EX_NS, "containsPerson"), alice, true));

	}

	@Test
	public void testInsertData() throws Exception {
		logger.debug("executing testInsertData");

		String update = getNamespaceDeclarations() +
				"INSERT DATA { ex:book1 dc:title \"book 1\" ; dc:creator \"Ringo\" . } ";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		IRI book1 = f.createIRI(EX_NS, "book1");

		assertFalse(con.hasStatement(book1, DC.TITLE, f.createLiteral("book 1"), true));
		assertFalse(con.hasStatement(book1, DC.CREATOR, f.createLiteral("Ringo"), true));

		operation.execute();

		String msg = "two new statements about ex:book1 should have been inserted";
		assertTrue(msg, con.hasStatement(book1, DC.TITLE, f.createLiteral("book 1"), true));
		assertTrue(msg, con.hasStatement(book1, DC.CREATOR, f.createLiteral("Ringo"), true));
	}

	@Test
	public void testInsertData2() throws Exception {
		logger.debug("executing testInsertData2");

		String update = getNamespaceDeclarations() +
				"INSERT DATA { ex:book1 dc:title \"the number four\"^^<http://www.w3.org/2001/XMLSchema#integer> . }; ";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		IRI book1 = f.createIRI(EX_NS, "book1");

		assertFalse(con.hasStatement(book1, DC.TITLE, f.createLiteral("the number four", XSD.INTEGER), true));

		operation.execute();

		String msg = "new statement about ex:book1 should have been inserted";

		assertTrue(msg,
				con.hasStatement(book1, DC.TITLE, f.createLiteral("the number four", CoreDatatype.XSD.INTEGER), true));
	}

	@Test
	public void testInsertDataLangTaggedLiteral() throws Exception {
		logger.debug("executing testInsertDataLangTaggedLiteral");

		String update = getNamespaceDeclarations() +
				"INSERT DATA { ex:book1 dc:title \"book 1\"@en . } ";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		IRI book1 = f.createIRI(EX_NS, "book1");

		assertFalse(con.hasStatement(book1, DC.TITLE, f.createLiteral("book 1", "en"), true));

		operation.execute();

		String msg = "new statement about ex:book1 should have been inserted";
		assertTrue(msg, con.hasStatement(book1, DC.TITLE, f.createLiteral("book 1", "en"), true));
	}

	@Test
	public void testInsertDataGraph1() throws Exception {
		logger.debug("executing testInsertDataGraph1");

		String update = "INSERT DATA { \n" +
				"GRAPH <urn:g1> { <urn:s1> <urn:p1> <urn:o1> . } \n" +
				"<urn:s1> a <urn:C1> . \n" +
				"}";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);
		assertFalse(con.hasStatement(f.createIRI("urn:s1"), RDF.TYPE, null, true, (Resource) null));
		assertFalse(con.hasStatement(f.createIRI("urn:s1"), f.createIRI("urn:p1"), f.createIRI("urn:o1"), true,
				f.createIRI("urn:g1")));
		operation.execute();
		assertTrue(con.hasStatement(f.createIRI("urn:s1"), RDF.TYPE, null, true, (Resource) null));
		assertTrue(con.hasStatement(f.createIRI("urn:s1"), f.createIRI("urn:p1"), f.createIRI("urn:o1"), true,
				f.createIRI("urn:g1")));
	}

	@Test
	public void testInsertDataGraph2() throws Exception {
		logger.debug("executing testInsertDataGraph2");

		String update = "INSERT DATA { \n" +
				"<urn:s1> a <urn:C1> . \n" +
				"GRAPH <urn:g1> { <urn:s1> <urn:p1> <urn:o1> . } \n" +
				"}";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);
		assertFalse(con.hasStatement(f.createIRI("urn:s1"), RDF.TYPE, null, true, (Resource) null));
		assertFalse(con.hasStatement(f.createIRI("urn:s1"), f.createIRI("urn:p1"), f.createIRI("urn:o1"), true,
				f.createIRI("urn:g1")));
		operation.execute();
		assertTrue(con.hasStatement(f.createIRI("urn:s1"), RDF.TYPE, null, true, (Resource) null));
		assertTrue(con.hasStatement(f.createIRI("urn:s1"), f.createIRI("urn:p1"), f.createIRI("urn:o1"), true,
				f.createIRI("urn:g1")));
	}

	@Test
	public void testInsertDataGraph3() throws Exception {
		logger.debug("executing testInsertDataGraph3");

		String update = "INSERT DATA { \n" +
				"<urn:s1> a <urn:C1> . \n" +
				"GRAPH <urn:g1>{ <urn:s1> <urn:p1> <urn:o1> . <urn:s2> <urn:p2> <urn:o2> } \n" +
				"<urn:s2> a <urn:C2> \n" +
				"}";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);
		assertFalse(con.hasStatement(f.createIRI("urn:s1"), RDF.TYPE, null, true, (Resource) null));
		assertFalse(con.hasStatement(f.createIRI("urn:s1"), f.createIRI("urn:p1"), f.createIRI("urn:o1"), true,
				f.createIRI("urn:g1")));

		assertFalse(con.hasStatement(f.createIRI("urn:s2"), f.createIRI("urn:p2"), f.createIRI("urn:o2"), true,
				f.createIRI("urn:g1")));
		operation.execute();
		assertTrue(con.hasStatement(f.createIRI("urn:s1"), RDF.TYPE, null, true, (Resource) null));
		assertTrue(con.hasStatement(f.createIRI("urn:s2"), RDF.TYPE, null, true, (Resource) null));
		assertTrue(con.hasStatement(f.createIRI("urn:s1"), f.createIRI("urn:p1"), f.createIRI("urn:o1"), true,
				f.createIRI("urn:g1")));
		assertTrue(con.hasStatement(f.createIRI("urn:s2"), f.createIRI("urn:p2"), f.createIRI("urn:o2"), true,
				f.createIRI("urn:g1")));
	}

	@Test
	public void testInsertDataBlankNode() throws Exception {
		logger.debug("executing testInsertDataBlankNode");

		String update = getNamespaceDeclarations() +
				"INSERT DATA { _:foo dc:title \"book 1\" ; dc:creator \"Ringo\" . } ";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		assertFalse(con.hasStatement(null, DC.TITLE, f.createLiteral("book 1"), true));
		assertFalse(con.hasStatement(null, DC.CREATOR, f.createLiteral("Ringo"), true));

		operation.execute();

		RepositoryResult<Statement> titleStatements = con.getStatements(null, DC.TITLE, f.createLiteral("book 1"),
				true);
		assertNotNull(titleStatements);

		RepositoryResult<Statement> creatorStatements = con.getStatements(null, DC.CREATOR, f.createLiteral("Ringo"),
				true);
		assertNotNull(creatorStatements);

		BNode bookNode = null;
		if (titleStatements.hasNext()) {
			Statement ts = titleStatements.next();
			assertFalse(titleStatements.hasNext());

			Resource subject = ts.getSubject();
			assertTrue(subject instanceof BNode);
			bookNode = (BNode) subject;
		}
		titleStatements.close();
		assertNotNull(bookNode);
		assertFalse("_:foo".equals(bookNode.getID()));

		if (creatorStatements.hasNext()) {
			Statement cs = creatorStatements.next();
			assertFalse(creatorStatements.hasNext());

			Resource subject = cs.getSubject();
			assertTrue(subject instanceof BNode);
			assertEquals(bookNode, subject);
		} else {
			fail("at least one creator statement expected");
		}
		creatorStatements.close();
	}

	@Test
	public void testInsertDataMultiplePatterns() throws Exception {
		logger.debug("executing testInsertData");

		String update = getNamespaceDeclarations() +
				"INSERT DATA { ex:book1 dc:title \"book 1\". ex:book1 dc:creator \"Ringo\" . ex:book2 dc:creator \"George\". } ";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		IRI book1 = f.createIRI(EX_NS, "book1");
		IRI book2 = f.createIRI(EX_NS, "book2");

		assertFalse(con.hasStatement(book1, DC.TITLE, f.createLiteral("book 1"), true));
		assertFalse(con.hasStatement(book1, DC.CREATOR, f.createLiteral("Ringo"), true));
		assertFalse(con.hasStatement(book2, DC.CREATOR, f.createLiteral("George"), true));

		operation.execute();

		String msg = "newly inserted statement missing";
		assertTrue(msg, con.hasStatement(book1, DC.TITLE, f.createLiteral("book 1"), true));
		assertTrue(msg, con.hasStatement(book1, DC.CREATOR, f.createLiteral("Ringo"), true));
		assertTrue(msg, con.hasStatement(book2, DC.CREATOR, f.createLiteral("George"), true));
	}

	@Test
	public void testInsertDataInGraph() throws Exception {
		logger.debug("executing testInsertDataInGraph");

		String update = getNamespaceDeclarations() +
				"INSERT DATA { GRAPH ex:graph1 { ex:book1 dc:title \"book 1\" ; dc:creator \"Ringo\" . } } ";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		IRI book1 = f.createIRI(EX_NS, "book1");

		assertFalse(con.hasStatement(book1, DC.TITLE, f.createLiteral("book 1"), true, graph1));
		assertFalse(con.hasStatement(book1, DC.CREATOR, f.createLiteral("Ringo"), true, graph1));

		operation.execute();

		String msg = "two new statements about ex:book1 should have been inserted in graph1";
		assertTrue(msg, con.hasStatement(book1, DC.TITLE, f.createLiteral("book 1"), true, graph1));
		assertTrue(msg, con.hasStatement(book1, DC.CREATOR, f.createLiteral("Ringo"), true, graph1));
	}

	@Test
	public void testInsertDataInGraph2() throws Exception {
		logger.debug("executing testInsertDataInGraph2");

		String update = getNamespaceDeclarations() +
				"INSERT DATA { GRAPH ex:graph1 { ex:Human rdfs:subClassOf ex:Mammal. ex:Mammal rdfs:subClassOf ex:Animal. ex:george a ex:Human. ex:ringo a ex:Human. } } ";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		IRI human = f.createIRI(EX_NS, "Human");
		IRI mammal = f.createIRI(EX_NS, "Mammal");
		IRI george = f.createIRI(EX_NS, "george");

		operation.execute();

		assertTrue(con.hasStatement(human, RDFS.SUBCLASSOF, mammal, true, graph1));
		assertTrue(con.hasStatement(mammal, RDFS.SUBCLASSOF, null, true, graph1));
		assertTrue(con.hasStatement(george, RDF.TYPE, human, true, graph1));
	}

	@Test
	public void testDeleteData() throws Exception {
		logger.debug("executing testDeleteData");
		String update = getNamespaceDeclarations() +
				"DELETE DATA { ex:alice foaf:knows ex:bob. } ";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		assertTrue(con.hasStatement(alice, FOAF.KNOWS, bob, true));
		operation.execute();

		String msg = "statement should have been deleted.";
		assertFalse(msg, con.hasStatement(alice, FOAF.KNOWS, bob, true));
	}

	@Test
	public void testDeleteDataUnicode() throws Exception {
		IRI i18n = con.getValueFactory().createIRI(EX_NS, "東京");

		con.add(i18n, FOAF.KNOWS, bob);

		logger.debug("executing testDeleteData");
		String update = getNamespaceDeclarations() +
				"DELETE DATA { ex:東京 foaf:knows ex:bob. } ";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		assertTrue(con.hasStatement(i18n, FOAF.KNOWS, bob, true));
		operation.execute();

		String msg = "statement should have been deleted.";
		assertFalse(msg, con.hasStatement(i18n, FOAF.KNOWS, bob, true));
	}

	@Test
	public void testDeleteDataMultiplePatterns() throws Exception {
		logger.debug("executing testDeleteData");
		String update = getNamespaceDeclarations() +
				"DELETE DATA { ex:alice foaf:knows ex:bob. ex:alice foaf:mbox \"alice@example.org\" .} ";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		assertTrue(con.hasStatement(alice, FOAF.KNOWS, bob, true));
		assertTrue(con.hasStatement(alice, FOAF.MBOX, f.createLiteral("alice@example.org"), true));
		operation.execute();

		String msg = "statement should have been deleted.";
		assertFalse(msg, con.hasStatement(alice, FOAF.KNOWS, bob, true));
		assertFalse(msg, con.hasStatement(alice, FOAF.MBOX, f.createLiteral("alice@example.org"), true));
	}

	@Test
	public void testDeleteDataFromGraph() throws Exception {
		logger.debug("executing testDeleteDataFromGraph");

		String update = getNamespaceDeclarations() +
				"DELETE DATA { GRAPH ex:graph1 {ex:alice foaf:knows ex:bob. } } ";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		assertTrue(con.hasStatement(alice, FOAF.KNOWS, bob, true, graph1));
		operation.execute();

		String msg = "statement should have been deleted from graph1";
		assertFalse(msg, con.hasStatement(alice, FOAF.KNOWS, bob, true, graph1));
	}

	@Test
	public void testDeleteDataFromWrongGraph() throws Exception {
		logger.debug("executing testDeleteDataFromWrongGraph");

		String update = getNamespaceDeclarations() +

		// statement does not exist in graph2.
				"DELETE DATA { GRAPH ex:graph2 {ex:alice foaf:knows ex:bob. } } ";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		assertTrue(con.hasStatement(alice, FOAF.KNOWS, bob, true, graph1));
		assertFalse(con.hasStatement(alice, FOAF.KNOWS, bob, true, graph2));
		operation.execute();

		String msg = "statement should have not have been deleted from graph1";
		assertTrue(msg, con.hasStatement(alice, FOAF.KNOWS, bob, true, graph1));
	}

	@Test
	public void testCreateNewGraph() throws Exception {
		logger.debug("executing testCreateNewGraph");

		StringBuilder update = new StringBuilder();
		update.append(getNamespaceDeclarations());

		IRI newGraph = f.createIRI(EX_NS, "new-graph");

		update.append("CREATE GRAPH <" + newGraph + "> ");

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update.toString());

		operation.execute();
		assertTrue(con.hasStatement(null, null, null, false, graph1));
		assertTrue(con.hasStatement(null, null, null, false, graph2));
		assertFalse(con.hasStatement(null, null, null, false, newGraph));
		assertTrue(con.hasStatement(null, null, null, false));
	}

	@Test
	public void testCreateExistingGraph() throws Exception {
		logger.debug("executing testCreateExistingGraph");

		String update = getNamespaceDeclarations() +
				"CREATE GRAPH <" + graph1 + "> ";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		try {
			operation.execute();

			fail("creation of existing graph should have resulted in error.");
		} catch (UpdateExecutionException e) {
			// expected behavior
			if (con.isActive()) {
				con.rollback();
			}
		}
	}

	@Test
	public void testCopyToDefault() throws Exception {
		logger.debug("executing testCopyToDefault");
		String update = getNamespaceDeclarations() +
				"COPY GRAPH <" + graph1.stringValue() + "> TO DEFAULT";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		assertTrue(con.hasStatement(graph1, DC.PUBLISHER, null, false, (Resource) null));
		assertTrue(con.hasStatement(graph2, DC.PUBLISHER, null, false, (Resource) null));
		operation.execute();
		assertFalse(con.hasStatement(graph1, DC.PUBLISHER, null, false, (Resource) null));
		assertFalse(con.hasStatement(graph2, DC.PUBLISHER, null, false, (Resource) null));
		assertTrue(con.hasStatement(bob, FOAF.NAME, null, false, (Resource) null));
		assertTrue(con.hasStatement(bob, FOAF.NAME, null, false, graph1));
	}

	@Test
	public void testCopyToExistingNamed() throws Exception {
		logger.debug("executing testCopyToExistingNamed");

		String update = getNamespaceDeclarations() +
				"COPY GRAPH ex:graph1 TO ex:graph2";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		assertTrue(con.hasStatement(alice, FOAF.NAME, null, false, graph2));
		operation.execute();
		assertTrue(con.hasStatement(bob, FOAF.NAME, null, false, graph2));
		assertFalse(con.hasStatement(alice, FOAF.NAME, null, false, graph2));
		assertTrue(con.hasStatement(bob, FOAF.NAME, null, false, graph1));
	}

	@Test
	public void testCopyToNewNamed() throws Exception {
		logger.debug("executing testCopyToNewNamed");

		String update = getNamespaceDeclarations() +
				"COPY GRAPH ex:graph1 TO ex:graph3";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		operation.execute();
		assertTrue(con.hasStatement(bob, FOAF.NAME, null, false, f.createIRI(EX_NS, "graph3")));
		assertTrue(con.hasStatement(bob, FOAF.NAME, null, false, graph1));
	}

	@Test
	public void testCopyFromDefault() throws Exception {
		logger.debug("executing testCopyFromDefault");

		String update = getNamespaceDeclarations() +
				"COPY DEFAULT TO ex:graph3";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		assertTrue(con.hasStatement(graph1, DC.PUBLISHER, null, false, (Resource) null));
		assertTrue(con.hasStatement(graph2, DC.PUBLISHER, null, false, (Resource) null));
		operation.execute();
		assertTrue(con.hasStatement(graph1, DC.PUBLISHER, null, false, (Resource) null));
		assertTrue(con.hasStatement(graph2, DC.PUBLISHER, null, false, (Resource) null));
		assertTrue(con.hasStatement(graph1, DC.PUBLISHER, null, false, f.createIRI(EX_NS, "graph3")));
		assertTrue(con.hasStatement(graph2, DC.PUBLISHER, null, false, f.createIRI(EX_NS, "graph3")));

	}

	@Test
	public void testCopyFromDefaultToDefault() throws Exception {
		logger.debug("executing testCopyFromDefaultToDefault");

		String update = getNamespaceDeclarations() +
				"COPY DEFAULT TO DEFAULT";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		assertTrue(con.hasStatement(graph1, DC.PUBLISHER, null, false, (Resource) null));
		assertTrue(con.hasStatement(graph2, DC.PUBLISHER, null, false, (Resource) null));
		operation.execute();
		assertTrue(con.hasStatement(graph1, DC.PUBLISHER, null, false, (Resource) null));
		assertTrue(con.hasStatement(graph2, DC.PUBLISHER, null, false, (Resource) null));
	}

	@Test
	public void testAddToDefault() throws Exception {
		logger.debug("executing testAddToDefault");

		String update = getNamespaceDeclarations() +
				"ADD GRAPH <" + graph1.stringValue() + "> TO DEFAULT";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		assertTrue(con.hasStatement(graph1, DC.PUBLISHER, null, false, (Resource) null));
		assertTrue(con.hasStatement(graph2, DC.PUBLISHER, null, false, (Resource) null));
		operation.execute();
		assertTrue(con.hasStatement(graph1, DC.PUBLISHER, null, false, (Resource) null));
		assertTrue(con.hasStatement(graph2, DC.PUBLISHER, null, false, (Resource) null));
		assertTrue(con.hasStatement(bob, FOAF.NAME, null, false, (Resource) null));
		assertTrue(con.hasStatement(bob, FOAF.NAME, null, false, graph1));
	}

	@Test
	public void testAddToExistingNamed() throws Exception {
		logger.debug("executing testAddToExistingNamed");

		String update = getNamespaceDeclarations() +
				"ADD GRAPH ex:graph1 TO ex:graph2";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		operation.execute();
		assertTrue(con.hasStatement(bob, FOAF.NAME, null, false, graph2));
		assertTrue(con.hasStatement(alice, FOAF.NAME, null, false, graph2));
		assertTrue(con.hasStatement(bob, FOAF.NAME, null, false, graph1));
	}

	@Test
	public void testAddToNewNamed() throws Exception {
		logger.debug("executing testAddToNewNamed");

		String update = getNamespaceDeclarations() +
				"ADD GRAPH ex:graph1 TO ex:graph3";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		operation.execute();
		assertTrue(con.hasStatement(bob, FOAF.NAME, null, false, f.createIRI(EX_NS, "graph3")));
		assertTrue(con.hasStatement(bob, FOAF.NAME, null, false, graph1));
	}

	@Test
	public void testAddFromDefault() throws Exception {
		logger.debug("executing testAddFromDefault");

		String update = getNamespaceDeclarations() +
				"ADD DEFAULT TO ex:graph3";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		IRI graph3 = f.createIRI(EX_NS, "graph3");
		assertTrue(con.hasStatement(graph1, DC.PUBLISHER, null, false, (Resource) null));
		assertTrue(con.hasStatement(graph2, DC.PUBLISHER, null, false, (Resource) null));
		assertTrue(con.hasStatement(alice, FOAF.KNOWS, bob, false, graph1));
		operation.execute();
		assertTrue(con.hasStatement(graph1, DC.PUBLISHER, null, false, (Resource) null));
		assertTrue(con.hasStatement(graph2, DC.PUBLISHER, null, false, (Resource) null));
		assertTrue(con.hasStatement(graph1, DC.PUBLISHER, null, false, graph3));
		assertTrue(con.hasStatement(graph2, DC.PUBLISHER, null, false, graph3));
		assertTrue(con.hasStatement(alice, FOAF.KNOWS, bob, false, graph1));
		assertFalse(con.hasStatement(alice, FOAF.KNOWS, bob, false, graph3));
	}

	@Test
	public void testAddFromDefaultToDefault() throws Exception {
		logger.debug("executing testAddFromDefaultToDefault");

		String update = getNamespaceDeclarations() +
				"ADD DEFAULT TO DEFAULT";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		assertTrue(con.hasStatement(graph1, DC.PUBLISHER, null, false, (Resource) null));
		assertTrue(con.hasStatement(graph2, DC.PUBLISHER, null, false, (Resource) null));
		operation.execute();
		assertTrue(con.hasStatement(graph1, DC.PUBLISHER, null, false, (Resource) null));
		assertTrue(con.hasStatement(graph2, DC.PUBLISHER, null, false, (Resource) null));
	}

	@Test
	public void testMoveToDefault() throws Exception {
		logger.debug("executing testMoveToDefault");

		String update = getNamespaceDeclarations() +
				"MOVE GRAPH <" + graph1.stringValue() + "> TO DEFAULT";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		assertTrue(con.hasStatement(graph1, DC.PUBLISHER, null, false, (Resource) null));
		assertTrue(con.hasStatement(graph2, DC.PUBLISHER, null, false, (Resource) null));
		operation.execute();
		assertFalse(con.hasStatement(graph1, DC.PUBLISHER, null, false, (Resource) null));
		assertFalse(con.hasStatement(graph2, DC.PUBLISHER, null, false, (Resource) null));
		assertTrue(con.hasStatement(bob, FOAF.NAME, null, false, (Resource) null));
		assertFalse(con.hasStatement(null, null, null, false, graph1));
	}

	@Test
	public void testMoveToNewNamed() throws Exception {
		logger.debug("executing testMoveToNewNamed");
		String update = getNamespaceDeclarations() +
				"MOVE GRAPH ex:graph1 TO ex:graph3";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		operation.execute();
		assertTrue(con.hasStatement(bob, FOAF.NAME, null, false, f.createIRI(EX_NS, "graph3")));
		assertFalse(con.hasStatement(null, null, null, false, graph1));
	}

	@Test
	public void testMoveFromDefault() throws Exception {
		logger.debug("executing testMoveFromDefault");
		String update = getNamespaceDeclarations() +
				"MOVE DEFAULT TO ex:graph3";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		assertTrue(con.hasStatement(graph1, DC.PUBLISHER, null, false, (Resource) null));
		assertTrue(con.hasStatement(graph2, DC.PUBLISHER, null, false, (Resource) null));
		operation.execute();
		assertFalse(con.hasStatement(graph1, DC.PUBLISHER, null, false, (Resource) null));
		assertFalse(con.hasStatement(graph2, DC.PUBLISHER, null, false, (Resource) null));
		assertTrue(con.hasStatement(graph1, DC.PUBLISHER, null, false, f.createIRI(EX_NS, "graph3")));
		assertTrue(con.hasStatement(graph2, DC.PUBLISHER, null, false, f.createIRI(EX_NS, "graph3")));

	}

	@Test
	public void testMoveFromDefaultToDefault() throws Exception {
		logger.debug("executing testMoveFromDefaultToDefault");
		String update = getNamespaceDeclarations() +
				"MOVE DEFAULT TO DEFAULT";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		assertTrue(con.hasStatement(graph1, DC.PUBLISHER, null, false, (Resource) null));
		assertTrue(con.hasStatement(graph2, DC.PUBLISHER, null, false, (Resource) null));
		operation.execute();
		assertTrue(con.hasStatement(graph1, DC.PUBLISHER, null, false, (Resource) null));
		assertTrue(con.hasStatement(graph2, DC.PUBLISHER, null, false, (Resource) null));
	}

	@Test
	public void testClearAll() throws Exception {
		logger.debug("executing testClearAll");
		String update = "CLEAR ALL";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		operation.execute();
		assertFalse(con.hasStatement(null, null, null, false));

	}

	@Test
	public void testClearDefault() throws Exception {
		logger.debug("executing testClearDefault");

		String update = "CLEAR DEFAULT";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		// Verify statements exist in the named graphs.
		assertTrue(con.hasStatement(null, null, null, false, graph1));
		assertTrue(con.hasStatement(null, null, null, false, graph2));
		// Verify statements exist in the default graph.
		assertTrue(con.hasStatement(null, null, null, false, new Resource[] { null }));

		operation.execute();
		assertTrue(con.hasStatement(null, null, null, false, graph1));
		assertTrue(con.hasStatement(null, null, null, false, graph2));
		// Verify that no statements remain in the 'default' graph.
		assertFalse(con.hasStatement(null, null, null, false, new Resource[] { null }));
	}

	@Test
	public void testClearGraph() throws Exception {
		logger.debug("executing testClearGraph");
		String update = getNamespaceDeclarations() +
				"CLEAR GRAPH <" + graph1.stringValue() + "> ";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		operation.execute();
		assertFalse(con.hasStatement(null, null, null, false, graph1));
		assertTrue(con.hasStatement(null, null, null, false, graph2));
		assertTrue(con.hasStatement(null, null, null, false));
	}

	@Test
	public void testClearNamed() throws Exception {
		logger.debug("executing testClearNamed");
		String update = "CLEAR NAMED";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		operation.execute();
		assertFalse(con.hasStatement(null, null, null, false, graph1));
		assertFalse(con.hasStatement(null, null, null, false, graph2));
		assertTrue(con.hasStatement(null, null, null, false));

	}

	@Test
	public void testDropAll() throws Exception {
		logger.debug("executing testDropAll");
		String update = "DROP ALL";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		operation.execute();
		assertFalse(con.hasStatement(null, null, null, false));

	}

	@Test
	public void testDropDefault() throws Exception {
		logger.debug("executing testDropDefault");

		String update = "DROP DEFAULT";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		// Verify statements exist in the named graphs.
		assertTrue(con.hasStatement(null, null, null, false, graph1));
		assertTrue(con.hasStatement(null, null, null, false, graph2));
		// Verify statements exist in the default graph.
		assertTrue(con.hasStatement(null, null, null, false, new Resource[] { null }));

		operation.execute();
		assertTrue(con.hasStatement(null, null, null, false, graph1));
		assertTrue(con.hasStatement(null, null, null, false, graph2));
		// Verify that no statements remain in the 'default' graph.
		assertFalse(con.hasStatement(null, null, null, false, new Resource[] { null }));

	}

	@Test
	public void testDropGraph() throws Exception {
		logger.debug("executing testDropGraph");
		String update = getNamespaceDeclarations() +
				"DROP GRAPH <" + graph1.stringValue() + "> ";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		operation.execute();
		assertFalse(con.hasStatement(null, null, null, false, graph1));
		assertTrue(con.hasStatement(null, null, null, false, graph2));
		assertTrue(con.hasStatement(null, null, null, false));
	}

	@Test
	public void testDropNamed() throws Exception {
		logger.debug("executing testDropNamed");

		String update = "DROP NAMED";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		operation.execute();
		assertFalse(con.hasStatement(null, null, null, false, graph1));
		assertFalse(con.hasStatement(null, null, null, false, graph2));
		assertTrue(con.hasStatement(null, null, null, false));
	}

	@Test
	public void testUpdateSequenceDeleteInsert() throws Exception {
		logger.debug("executing testUpdateSequenceDeleteInsert");

		String update = getNamespaceDeclarations() +
				"DELETE {?y foaf:name ?n } WHERE {?x ex:containsPerson ?y. ?y foaf:name ?n . }; " +
				getNamespaceDeclarations() +
				"INSERT {?x foaf:name \"foo\" } WHERE {?y ex:containsPerson ?x} ";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		assertTrue(con.hasStatement(bob, FOAF.NAME, f.createLiteral("Bob"), true));
		assertTrue(con.hasStatement(alice, FOAF.NAME, f.createLiteral("Alice"), true));

		operation.execute();

		String msg = "foaf:name properties should have been deleted";
		assertFalse(msg, con.hasStatement(bob, FOAF.NAME, f.createLiteral("Bob"), true));
		assertFalse(msg, con.hasStatement(alice, FOAF.NAME, f.createLiteral("Alice"), true));

		msg = "foaf:name properties with value 'foo' should have been added";
		assertTrue(msg, con.hasStatement(bob, FOAF.NAME, f.createLiteral("foo"), true));
		assertTrue(msg, con.hasStatement(alice, FOAF.NAME, f.createLiteral("foo"), true));
	}

	@Test
	public void testUpdateSequenceInsertDelete() throws Exception {
		logger.debug("executing testUpdateSequenceInsertDelete");

		String update = getNamespaceDeclarations() +
				"INSERT {?x foaf:name \"foo\" } WHERE {?y ex:containsPerson ?x}; " +
				getNamespaceDeclarations() +
				"DELETE {?y foaf:name ?n } WHERE {?x ex:containsPerson ?y. ?y foaf:name ?n . } ";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		assertTrue(con.hasStatement(bob, FOAF.NAME, f.createLiteral("Bob"), true));
		assertTrue(con.hasStatement(alice, FOAF.NAME, f.createLiteral("Alice"), true));

		operation.execute();

		String msg = "foaf:name properties should have been deleted";
		assertFalse(msg, con.hasStatement(bob, FOAF.NAME, f.createLiteral("Bob"), true));
		assertFalse(msg, con.hasStatement(alice, FOAF.NAME, f.createLiteral("Alice"), true));

		msg = "foaf:name properties with value 'foo' should not have been added";
		assertFalse(msg, con.hasStatement(bob, FOAF.NAME, f.createLiteral("foo"), true));
		assertFalse(msg, con.hasStatement(alice, FOAF.NAME, f.createLiteral("foo"), true));
	}

	@Test
	public void testUpdateSequenceInsertDelete2() throws Exception {
		logger.debug("executing testUpdateSequenceInsertDelete2");

		String update = getNamespaceDeclarations() +
				"INSERT { GRAPH ex:graph2 { ?s ?p ?o } } WHERE { GRAPH ex:graph1 { ?s ?p ?o . FILTER (?s = ex:bob) } }; "
				+
				"WITH ex:graph1 DELETE { ?s ?p ?o } WHERE {?s ?p ?o . FILTER (?s = ex:bob) } ";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		assertTrue(con.hasStatement(bob, FOAF.NAME, f.createLiteral("Bob"), true, graph1));
		assertTrue(con.hasStatement(alice, FOAF.NAME, f.createLiteral("Alice"), true, graph2));

		operation.execute();

		String msg = "statements about bob should have been removed from graph1";
		assertFalse(msg, con.hasStatement(bob, null, null, true, graph1));

		msg = "statements about bob should have been added to graph2";
		assertTrue(msg, con.hasStatement(bob, FOAF.NAME, f.createLiteral("Bob"), true, graph2));
		assertTrue(msg, con.hasStatement(bob, FOAF.MBOX, null, true, graph2));
		assertTrue(msg, con.hasStatement(bob, FOAF.KNOWS, alice, true, graph2));
	}

	@Test
	public void testUpdateSequenceInsertDeleteExample9() throws Exception {
		logger.debug("executing testUpdateSequenceInsertDeleteExample9");

		// replace the standard dataset with one specific to this case.
		con.clear();
		loadDataset("/testdata-update/dataset-update-example9.trig");

		IRI book1 = f.createIRI("http://example/book1");
		IRI book3 = f.createIRI("http://example/book3");
		IRI bookStore = f.createIRI("http://example/bookStore");
		IRI bookStore2 = f.createIRI("http://example/bookStore2");

		String update = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
				"prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>  " +
				"prefix xsd: <http://www.w3.org/2001/XMLSchema#>  " +
				"prefix dc: <http://purl.org/dc/elements/1.1/>  " +
				"prefix dcmitype: <http://purl.org/dc/dcmitype/>  " +
				"INSERT  { GRAPH <http://example/bookStore2> { ?book ?p ?v } } " +
				" WHERE " +
				" { GRAPH  <http://example/bookStore> " +
				"   { ?book dc:date ?date . " +
				"       FILTER ( ?date < \"2000-01-01T00:00:00-02:00\"^^xsd:dateTime ) " +
				"       ?book ?p ?v " +
				"      } " +
				" } ;" +
				"WITH <http://example/bookStore> " +
				" DELETE { ?book ?p ?v } " +
				" WHERE " +
				" { ?book dc:date ?date ; " +
				"         a dcmitype:PhysicalObject ." +
				"    FILTER ( ?date < \"2000-01-01T00:00:00-02:00\"^^xsd:dateTime ) " +
				"   ?book ?p ?v" +
				" } ";

		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);

		operation.execute();

		String msg = "statements about book1 should have been removed from bookStore";
		assertFalse(msg, con.hasStatement(book1, null, null, true, bookStore));

		msg = "statements about book1 should have been added to bookStore2";
		assertTrue(msg, con.hasStatement(book1, RDF.TYPE, null, true, bookStore2));
		assertTrue(msg, con.hasStatement(book1, DC.DATE, null, true, bookStore2));
		assertTrue(msg, con.hasStatement(book1, DC.TITLE, null, true, bookStore2));
	}

	@Test
	public void testUpdateSequenceWithRelativeIRI() throws Exception {
		logger.debug("executing testUpdateSequenceWithRelativeIRI");
		String update = "base <http://example.com/resource/>\n" + "prefix em: <http://example.com/resource/ontology/>\n" //
				+ "insert {\n" //
				+ "  graph <relations> { ?company em:parent ?other. ?other em:subsidiary ?company }\n" + "} where {\n" //
				+ "  values ?type {<relation/parent> <relation/acquisition>}\n"
				+ "  [em:type ?type; em:company ?company; em:investor ?other]\n" //
				+ "};\n" // inherit base <http://example.com/resource/>
				+ "insert {\n" //
				+ "  graph <relations> { ?company em:hasCompetitor ?other. ?other em:hasCompetitor ?company. }\n"
				+ "} where {\n" //
				+ "  [em:type <relation/competition>; em:company ?company; em:company ?other]\n"
				+ "  filter(str(?company)<str(?other)) \n" //
				+ "};";
		Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);
		operation.execute();
	}

	@Test
	public void contextualInsertDeleteData()
			throws RepositoryException, MalformedQueryException, UpdateExecutionException {
		String insert = getNamespaceDeclarations() +
				"INSERT DATA { ex:alice foaf:knows ex:bob. ex:alice foaf:mbox \"alice@example.org\" .} ";

		SimpleDataset ds = new SimpleDataset();
		ds.setDefaultInsertGraph(graph2);
		ds.addDefaultRemoveGraph(graph2);

		Update updInsert = con.prepareUpdate(QueryLanguage.SPARQL, insert);
		updInsert.setDataset(ds);
		updInsert.execute();

		assertTrue(con.hasStatement(alice, FOAF.KNOWS, bob, true, graph2));
		assertTrue(con.hasStatement(alice, FOAF.MBOX, f.createLiteral("alice@example.org"), true, graph2));

		String update = getNamespaceDeclarations() +
				"DELETE DATA { ex:alice foaf:knows ex:bob. ex:alice foaf:mbox \"alice@example.org\" .} ";

		Update updDelete = con.prepareUpdate(QueryLanguage.SPARQL, update);
		updDelete.setDataset(ds);
		updDelete.execute();

		String msg = "statement should have been deleted.";
		assertFalse(msg, con.hasStatement(alice, FOAF.KNOWS, bob, true, graph2));
		assertFalse(msg, con.hasStatement(alice, FOAF.MBOX, f.createLiteral("alice@example.org"), true, graph2));
	}

	@Test(expected = MalformedQueryException.class)
	public void testInvalidInsertUpdate() {
		RepositoryConnection connection = rep.getConnection();
		Update update = connection.prepareUpdate(QueryLanguage.SPARQL, "insert data { ?s ?p ?o }");
	}

	@Test(expected = MalformedQueryException.class)
	public void testInvalidDeleteUpdate() {
		RepositoryConnection connection = rep.getConnection();
		Update delete = connection.prepareUpdate(QueryLanguage.SPARQL, "delete data { ?s ?p ?o }");
	}

	/*
	 * @Test public void testLoad() throws Exception { String update =
	 * "LOAD <http://www.daml.org/2001/01/gedcom/royal92.daml>"; String ns =
	 * "http://www.daml.org/2001/01/gedcom/gedcom#"; Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update);
	 * operation.execute(); assertTrue(con.hasStatement(null, RDF.TYPE, f.createIRI(ns, "Family"), true)); }
	 *
	 * @Test public void testLoadIntoGraph() throws Exception { String ns =
	 * "http://www.daml.org/2001/01/gedcom/gedcom#"; String update =
	 * "LOAD <http://www.daml.org/2001/01/gedcom/royal92.daml> INTO GRAPH <" + ns + "> "; Update operation =
	 * con.prepareUpdate(QueryLanguage.SPARQL, update); operation.execute();
	 * assertFalse(con.hasStatement((Resource)null, RDF.TYPE, f.createIRI(ns, "Family"), true, (Resource)null));
	 * assertTrue(con.hasStatement((Resource)null, RDF.TYPE, f.createIRI(ns, "Family"), true, f.createIRI(ns))); }
	 */

	/* protected methods */

	protected void loadDataset(String datasetFile) throws RDFParseException, RepositoryException, IOException {
		logger.debug("loading dataset...");
		try (InputStream dataset = SPARQLUpdateTest.class.getResourceAsStream(datasetFile)) {
			con.add(dataset, "", RDFFormat.TRIG);
		}
		logger.debug("dataset loaded.");
	}

	/**
	 * Get a set of useful namespace prefix declarations.
	 *
	 * @return namespace prefix declarations for rdf, rdfs, dc, foaf and ex.
	 */
	protected String getNamespaceDeclarations() {
		String declarations = "PREFIX rdf: <" + RDF.NAMESPACE + "> \n" +
				"PREFIX rdfs: <" + RDFS.NAMESPACE + "> \n" +
				"PREFIX dc: <" + DC.NAMESPACE + "> \n" +
				"PREFIX foaf: <" + FOAF.NAMESPACE + "> \n" +
				"PREFIX ex: <" + EX_NS + "> \n" +
				"PREFIX xsd: <" + XSD.NAMESPACE + "> \n" +
				"\n";

		return declarations;
	}

	/**
	 * Creates, initializes and clears a repository.
	 *
	 * @return an initialized empty repository.
	 * @throws Exception
	 */
	protected Repository createRepository() throws Exception {
		Repository repository = newRepository();
		try (RepositoryConnection con = repository.getConnection()) {
			con.clear();
			con.clearNamespaces();
		}
		return repository;
	}

	/**
	 * Create a new Repository object. Subclasses are expected to implement this method to supply the test case with a
	 * specific Repository type and configuration.
	 *
	 * @return a new (uninitialized) Repository
	 * @throws Exception
	 */
	protected abstract Repository newRepository() throws Exception;
}
