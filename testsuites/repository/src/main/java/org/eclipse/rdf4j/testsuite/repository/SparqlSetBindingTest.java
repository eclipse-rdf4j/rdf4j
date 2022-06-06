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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
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

public abstract class SparqlSetBindingTest {

	@BeforeClass
	public static void setUpClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	@AfterClass
	public static void afterClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
	}

	public String queryBinding = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" + "SELECT ?name ?mbox\n"
			+ " WHERE { ?x foaf:name  ?name ;\n" + "            foaf:mbox  ?mbox .\n" + " } ";

	public String queryBindingSubselect = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" + "SELECT ?name ?mbox\n"
			+ " WHERE { ?x foaf:name  ?name ;\n" + "            foaf:mbox  ?mbox .\n"
			+ "        { SELECT ?x WHERE { ?x a foaf:Person } } } ";

	private Repository repository;

	private RepositoryConnection conn;

	private ValueFactory vf;

	private Literal ringo;

	private IRI ringoRes;

	@Test
	public void testBinding() throws Exception {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryBinding);
		query.setBinding("name", ringo);
		TupleQueryResult result = query.evaluate();
		assertEquals(ringo, result.next().getValue("name"));
		assertFalse(result.hasNext());
		result.close();
	}

	@Test
	public void testBindingSubselect() throws Exception {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryBindingSubselect);
		query.setBinding("x", ringoRes);
		TupleQueryResult result = query.evaluate();
		assertEquals(ringo, result.next().getValue("name"));
		assertFalse(result.hasNext());
		result.close();
	}

	@Before
	public void setUp() throws Exception {
		repository = createRepository();
		vf = repository.getValueFactory();
		ringo = vf.createLiteral("Ringo Starr");
		ringoRes = vf.createIRI("http://example.org/ns#", "ringo");

		createUser("john", "John Lennon", "john@example.org");
		createUser("paul", "Paul McCartney", "paul@example.org");
		createUser("ringo", "Ringo Starr", "ringo@example.org");
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

		conn.add(subj, RDF.TYPE, vf.createIRI("http://xmlns.com/foaf/0.1/", "Person"));
		conn.add(subj, foafName, vf.createLiteral(name));
		conn.add(subj, foafMbox, vf.createIRI("mailto:", email));
		conn.close();
	}
}
