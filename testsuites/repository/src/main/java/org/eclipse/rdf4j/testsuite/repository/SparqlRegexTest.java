/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public abstract class SparqlRegexTest {

	@BeforeClass
	public static void setUpClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	@AfterClass
	public static void afterClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
	}

	public String queryInline = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" + "SELECT ?name ?mbox\n"
			+ " WHERE { ?x foaf:name  ?name ;\n" + "            foaf:mbox  ?mbox .\n"
			+ "         FILTER regex(str(?mbox), \"@Work.example\", \"i\") }";

	public String queryBinding = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" + "SELECT ?name ?mbox\n"
			+ " WHERE { ?x foaf:name  ?name ;\n" + "            foaf:mbox  ?mbox .\n"
			+ "         FILTER regex(str(?mbox), ?pattern) }";

	public String queryBindingFlags = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" + "SELECT ?name ?mbox\n"
			+ " WHERE { ?x foaf:name  ?name ;\n" + "            foaf:mbox  ?mbox .\n"
			+ "         FILTER regex(str(?mbox), ?pattern, ?flags) }";

	public String queryExpr = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" + "SELECT ?name ?mbox\n"
			+ " WHERE { ?x foaf:name  ?name ;\n" + "            foaf:mbox  ?mbox .\n"
			+ "         ?y <http://example.org/ns#pattern>  ?pattern .\n"
			+ "         ?y <http://example.org/ns#flags>  ?flags .\n"
			+ "         FILTER regex(str(?mbox), ?pattern, ?flags) }";

	private Repository repository;

	private RepositoryConnection conn;

	private ValueFactory vf;

	private Literal hunt;

	@Test
	public void testInline() throws Exception {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryInline);
		TupleQueryResult result = query.evaluate();
		assertEquals(hunt, result.next().getValue("name"));
		assertFalse(result.hasNext());
		result.close();
	}

	@Test
	public void testBinding() throws Exception {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryBinding);
		query.setBinding("pattern", vf.createLiteral("@work.example"));
		TupleQueryResult result = query.evaluate();
		assertEquals(hunt, result.next().getValue("name"));
		assertFalse(result.hasNext());
		result.close();
	}

	@Test
	public void testBindingFlags() throws Exception {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryBindingFlags);
		query.setBinding("pattern", vf.createLiteral("@Work.example"));
		query.setBinding("flags", vf.createLiteral("i"));
		TupleQueryResult result = query.evaluate();
		assertEquals(hunt, result.next().getValue("name"));
		assertFalse(result.hasNext());
		result.close();
	}

	@Test
	public void testExpr() throws Exception {
		IRI pattern = vf.createIRI("http://example.org/ns#", "pattern");
		IRI flags = vf.createIRI("http://example.org/ns#", "flags");
		BNode bnode = vf.createBNode();
		conn.add(bnode, pattern, vf.createLiteral("@Work.example"));
		conn.add(bnode, flags, vf.createLiteral("i"));
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryExpr);
		TupleQueryResult result = query.evaluate();
		assertEquals(hunt, result.next().getValue("name"));
		assertFalse(result.hasNext());
		result.close();
	}

	@Before
	public void setUp() throws Exception {
		repository = createRepository();
		vf = repository.getValueFactory();
		hunt = vf.createLiteral("James Leigh Hunt");
		createUser("james", "James Leigh", "james@leigh");
		createUser("megan", "Megan Leigh", "megan@leigh");
		createUser("hunt", "James Leigh Hunt", "james@work.example");
		conn = repository.getConnection();
	}

	protected Repository createRepository() throws Exception {
		Repository repository = newRepository();
		try (RepositoryConnection con = repository.getConnection()) {
			con.clear();
			con.clearNamespaces();
		}
		return repository;
	}

	protected abstract Repository newRepository() throws Exception;

	@After
	public void tearDown() throws Exception {
		conn.close();
		conn = null;

		repository.shutDown();
		repository = null;
	}

	private void createUser(String id, String name, String email) throws RepositoryException {
		RepositoryConnection conn = repository.getConnection();
		IRI subj = vf.createIRI("http://example.org/ns#", id);
		IRI foafName = vf.createIRI("http://xmlns.com/foaf/0.1/", "name");
		IRI foafMbox = vf.createIRI("http://xmlns.com/foaf/0.1/", "mbox");
		conn.add(subj, foafName, vf.createLiteral(name));
		conn.add(subj, foafMbox, vf.createIRI("mailto:", email));
		conn.close();
	}
}
