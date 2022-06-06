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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
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

public abstract class SparqlAggregatesTest {

	@BeforeClass
	public static void setUpClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	@AfterClass
	public static void afterClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
	}

	public String selectNameMbox = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" + "SELECT ?name ?mbox\n"
			+ " WHERE { ?x foaf:name  ?name; foaf:mbox  ?mbox }";

	public String concatMbox = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
			+ "SELECT ?name (group_concat(?mbox) AS ?mbox)\n"
			+ " WHERE { ?x foaf:name  ?name; foaf:mbox  ?mbox } GROUP BY ?name";

	public String concatOptionalMbox = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
			+ "SELECT ?name (group_concat(?mbox) AS ?mbox)\n"
			+ " WHERE { ?x foaf:name  ?name OPTIONAL { ?x foaf:mbox  ?mbox } } GROUP BY ?name";

	public String countMbox = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" + "SELECT ?name (count(?mbox) AS ?mbox)\n"
			+ " WHERE { ?x foaf:name  ?name; foaf:mbox  ?mbox } GROUP BY ?name";

	public String countOptionalMbox = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
			+ "SELECT ?name (count(?mb) AS ?mbox)\n"
			+ " WHERE { ?x foaf:name  ?name OPTIONAL { ?x foaf:mbox  ?mb } } GROUP BY ?name";

	private Repository repository;

	private RepositoryConnection conn;

	private ValueFactory vf;

	@Test
	public void testSelect() throws Exception {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, selectNameMbox);
		TupleQueryResult result = query.evaluate();
		assertTrue(result.hasNext());
		result.next();
		result.next();
		assertFalse(result.hasNext());
		result.close();
	}

	@Test
	public void testConcat() throws Exception {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, concatMbox);
		TupleQueryResult result = query.evaluate();
		assertTrue(result.hasNext());
		assertNotNull(result.next().getValue("mbox"));
		assertNotNull(result.next().getValue("mbox"));
		assertFalse(result.hasNext());
		result.close();
	}

	@Test
	public void testConcatOptional() throws Exception {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, concatOptionalMbox);
		TupleQueryResult result = query.evaluate();
		assertTrue(result.hasNext());
		result.next();
		result.next();
		result.next();
		assertFalse(result.hasNext());
		result.close();
	}

	@Test
	public void testCount() throws Exception {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, countMbox);
		TupleQueryResult result = query.evaluate();
		assertTrue(result.hasNext());
		assertEquals("1", result.next().getValue("mbox").stringValue());
		assertEquals("1", result.next().getValue("mbox").stringValue());
		assertFalse(result.hasNext());
		result.close();
	}

	@Test
	public void testCountOptional() throws Exception {
		Set<String> zeroOr1 = new HashSet<>();
		zeroOr1.add("0");
		zeroOr1.add("1");
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, countOptionalMbox);
		TupleQueryResult result = query.evaluate();
		assertTrue(result.hasNext());
		assertTrue(zeroOr1.contains(result.next().getValue("mbox").stringValue()));
		assertTrue(zeroOr1.contains(result.next().getValue("mbox").stringValue()));
		assertTrue(zeroOr1.contains(result.next().getValue("mbox").stringValue()));
		assertFalse(result.hasNext());
		result.close();
	}

	@Before
	public void setUp() throws Exception {
		repository = createRepository();
		vf = repository.getValueFactory();
		createUser("james", "James Leigh", "james@leigh");
		createUser("megan", "Megan Leigh", "megan@leigh");
		createUser("hunt", "James Leigh Hunt", null);
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
		if (email != null) {
			conn.add(subj, foafMbox, vf.createIRI("mailto:", email));
		}
		conn.close();
	}
}
