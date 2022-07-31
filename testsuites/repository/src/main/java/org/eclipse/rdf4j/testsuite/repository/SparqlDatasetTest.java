/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public abstract class SparqlDatasetTest {

	@BeforeClass
	public static void setUpClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	@AfterClass
	public static void afterClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
	}

	public String queryNoFrom = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
			+ "SELECT (COUNT(DISTINCT ?name) as ?c) \n" + " WHERE { ?x foaf:name  ?name . } ";

	public String queryWithFrom = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
			+ "SELECT (COUNT(DISTINCT ?name) as ?c) \n" + " FROM <http://example.org/graph1> "
			+ " WHERE { ?x foaf:name  ?name . } ";

	private Repository repository;

	private RepositoryConnection conn;

	private ValueFactory vf;

	private SimpleDataset dataset;

	private IRI graph1;

	private IRI george;

	private IRI paul;

	private IRI john;

	private IRI ringo;

	@Test
	public void testNoFrom() throws Exception {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryNoFrom);
		TupleQueryResult result = query.evaluate();

		assertTrue(result.hasNext());

		if (result.hasNext()) {
			BindingSet bs = result.next();
			assertFalse(result.hasNext());

			Literal count = (Literal) bs.getValue("c");
			assertEquals(4, count.intValue());
		}
		result.close();

		query.setDataset(dataset);
		result = query.evaluate();

		assertTrue(result.hasNext());

		if (result.hasNext()) {
			BindingSet bs = result.next();
			assertFalse(result.hasNext());

			Literal count = (Literal) bs.getValue("c");
			assertEquals(2, count.intValue());
		}
		result.close();
	}

	@Test
	public void testWithFrom() throws Exception {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryWithFrom);
		TupleQueryResult result = query.evaluate();

		assertTrue(result.hasNext());

		if (result.hasNext()) {
			BindingSet bs = result.next();
			assertFalse(result.hasNext());

			Literal count = (Literal) bs.getValue("c");
			assertEquals(2, count.intValue());
		}
		result.close();

		query.setDataset(dataset);
		result = query.evaluate();

		assertTrue(result.hasNext());

		if (result.hasNext()) {
			BindingSet bs = result.next();
			assertFalse(result.hasNext());

			Literal count = (Literal) bs.getValue("c");
			assertEquals(2, count.intValue());
		}
		result.close();
	}

	@Before
	public void setUp() throws Exception {
		repository = createRepository();
		vf = repository.getValueFactory();
		graph1 = vf.createIRI("http://example.org/graph1");
		john = createUser("john", "John Lennon", "john@example.org");
		paul = createUser("paul", "Paul McCartney", "paul@example.org");
		george = createUser("george", "George Harrison", "george@example.org", graph1);
		ringo = createUser("ringo", "Ringo Starr", "ringo@example.org", graph1);
		conn = repository.getConnection();

		dataset = new SimpleDataset();
		dataset.addDefaultGraph(graph1);
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

	private IRI createUser(String id, String name, String email, Resource... context) throws RepositoryException {
		RepositoryConnection conn = repository.getConnection();
		IRI subj = vf.createIRI("http://example.org/ns#", id);
		IRI foafName = vf.createIRI("http://xmlns.com/foaf/0.1/", "name");
		IRI foafMbox = vf.createIRI("http://xmlns.com/foaf/0.1/", "mbox");

		conn.add(subj, RDF.TYPE, vf.createIRI("http://xmlns.com/foaf/0.1/", "Person"), context);
		conn.add(subj, foafName, vf.createLiteral(name), context);
		conn.add(subj, foafMbox, vf.createIRI("mailto:", email), context);
		conn.close();

		return subj;
	}

	@Test
	public void testSelectAllIsSameAsCount() throws Exception {
		try (RepositoryConnection con = repository.getConnection()) {
			TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT ?s ?p ?o WHERE {?s ?p ?o}");
			TupleQueryResult res = tupleQuery.evaluate();

			assertTrue("expect a result", res.hasNext());
			long count = 0;
			while (res.hasNext()) {
				BindingSet bs = res.next();
				assertNotNull(bs);
				assertTrue(bs.hasBinding("s"));
				assertTrue(bs.hasBinding("p"));
				assertTrue(bs.hasBinding("o"));
				count++;
			}
			assertEquals("expect same number of solutions", count, con.size());
		}
	}
}
