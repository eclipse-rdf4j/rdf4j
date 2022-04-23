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
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
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

public abstract class SparqlOrderByTest {

	@BeforeClass
	public static void setUpClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	@AfterClass
	public static void afterClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
	}

	private final String query1 = "PREFIX foaf:    <http://xmlns.com/foaf/0.1/>\n" + "SELECT ?name\n"
			+ "WHERE { ?x foaf:name ?name }\n" + "ORDER BY ?name\n";

	private final String query2 = "PREFIX     :    <http://example.org/ns#>\n"
			+ "PREFIX foaf:    <http://xmlns.com/foaf/0.1/>\n" + "PREFIX xsd:     <http://www.w3.org/2001/XMLSchema#>\n"
			+ "SELECT ?name\n" + "WHERE { ?x foaf:name ?name ; :empId ?emp }\n" + "ORDER BY DESC(?emp)\n";

	private final String query3 = "PREFIX     :    <http://example.org/ns#>\n"
			+ "PREFIX foaf:    <http://xmlns.com/foaf/0.1/>\n" + "SELECT ?name\n"
			+ "WHERE { ?x foaf:name ?name ; :empId ?emp }\n" + "ORDER BY ?name DESC(?emp)\n";

	private Repository repository;

	private RepositoryConnection conn;

	@Test
	public void testQuery1() throws Exception {
		assertTrue("James Leigh".compareTo("James Leigh Hunt") < 0);
		assertResult(query1, Arrays.asList("James Leigh", "James Leigh", "James Leigh Hunt", "Megan Leigh"));
	}

	@Test
	public void testQuery2() throws Exception {
		assertResult(query2, Arrays.asList("Megan Leigh", "James Leigh", "James Leigh Hunt", "James Leigh"));
	}

	@Test
	public void testQuery3() throws Exception {
		assertResult(query3, Arrays.asList("James Leigh", "James Leigh", "James Leigh Hunt", "Megan Leigh"));
	}

	@Before
	public void setUp() throws Exception {
		repository = createRepository();
		createEmployee("james", "James Leigh", 123);
		createEmployee("jim", "James Leigh", 244);
		createEmployee("megan", "Megan Leigh", 1234);
		createEmployee("hunt", "James Leigh Hunt", 243);
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

	private void createEmployee(String id, String name, int empId) throws RepositoryException {
		ValueFactory vf = repository.getValueFactory();
		String foafName = "http://xmlns.com/foaf/0.1/name";
		String exEmpId = "http://example.org/ns#empId";
		RepositoryConnection conn = repository.getConnection();
		conn.add(vf.createIRI("http://example.org/ns#" + id), vf.createIRI(foafName), vf.createLiteral(name));
		conn.add(vf.createIRI("http://example.org/ns#" + id), vf.createIRI(exEmpId), vf.createLiteral(empId));
		conn.close();
	}

	private void assertResult(String queryStr, List<String> names)
			throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryStr);
		TupleQueryResult result = query.evaluate();
		for (String name : names) {
			Value value = result.next().getValue("name");
			assertEquals(name, ((Literal) value).getLabel());
		}
		assertFalse(result.hasNext());
		result.close();
	}
}
