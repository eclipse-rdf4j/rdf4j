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

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public abstract class GraphQueryResultTest {

	@BeforeClass
	public static void setUpClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	@AfterClass
	public static void afterClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
	}

	private Repository rep;

	private RepositoryConnection con;

	private String emptyDescribeQuery;

	private String singleDescribeQuery;

	private String multipleDescribeQuery;

	private String emptyConstructQuery;

	private String singleConstructQuery;

	private String multipleConstructQuery;

	@Before
	public void setUp() throws Exception {
		rep = createRepository();
		con = rep.getConnection();

		buildQueries();
		addData();

	}

	@After
	public void tearDown() throws Exception {
		try {
			con.close();
			con = null;
		} finally {
			rep.shutDown();
			rep = null;
		}
	}

	protected Repository createRepository() throws Exception {
		Repository repository = newRepository();

		try (RepositoryConnection con = repository.getConnection()) {
			con.begin(IsolationLevels.NONE);
			con.clear();
			con.clearNamespaces();
			con.commit();
		}

		return repository;
	}

	protected abstract Repository newRepository() throws Exception;

	/*
	 * build some simple SPARQL queries to use for testing the result set object.
	 */
	private void buildQueries() {
		emptyDescribeQuery = "DESCRIBE <urn:test:non-existent-uri>";
		singleDescribeQuery = "DESCRIBE <" + OWL.THING.stringValue() + ">";
		multipleDescribeQuery = "DESCRIBE <" + OWL.CLASS.stringValue() + ">";

		emptyConstructQuery = "CONSTRUCT { <urn:test:non-existent-uri> ?p ?o . } WHERE { <urn:test:non-existent-uri> ?p ?o . }";
		singleConstructQuery = "CONSTRUCT { ?s ?p <" + OWL.THING.stringValue() + "> . } WHERE { ?s ?p <"
				+ OWL.THING.stringValue() + "> . }";
		multipleConstructQuery = "CONSTRUCT { ?s ?p <" + OWL.CLASS.stringValue() + "> . } WHERE { ?s ?p <"
				+ OWL.CLASS.stringValue() + "> . }";
	}

	private void addData() throws IOException, UnsupportedRDFormatException, RDFParseException, RepositoryException {
		try (InputStream defaultGraph = GraphQueryResultTest.class.getResourceAsStream("/testcases/graph3.ttl")) {
			con.begin(IsolationLevels.NONE);
			con.add(defaultGraph, "", RDFFormat.TURTLE);
			con.commit();
		}
	}

	@Test
	public void testDescribeEmpty() throws Exception {
		GraphQueryResult result = con.prepareGraphQuery(QueryLanguage.SPARQL, emptyDescribeQuery).evaluate();
		assertFalse("Query result should be empty", result.hasNext());

		Model model = QueryResults.asModel(result);
		assertTrue("Query result should be empty", model.isEmpty());
	}

	@Test
	public void testDescribeSingle() throws Exception {
		GraphQueryResult result = con.prepareGraphQuery(QueryLanguage.SPARQL, singleDescribeQuery).evaluate();
		assertTrue("Query result should not be empty", result.hasNext());

		Model model = QueryResults.asModel(result);
		assertFalse("Query result should not be empty", model.isEmpty());
		assertEquals(1, model.size());
	}

	@Test
	public void testDescribeMultiple() throws Exception {
		GraphQueryResult result = con.prepareGraphQuery(QueryLanguage.SPARQL, multipleDescribeQuery).evaluate();
		assertTrue("Query result should not be empty", result.hasNext());

		Model model = QueryResults.asModel(result);
		assertFalse("Query result should not be empty", model.isEmpty());
		assertEquals(4, model.size());
	}

	@Test
	public void testConstructEmpty() throws Exception {
		GraphQueryResult result = con.prepareGraphQuery(QueryLanguage.SPARQL, emptyConstructQuery).evaluate();
		assertFalse("Query result should be empty", result.hasNext());

		Model model = QueryResults.asModel(result);
		assertTrue("Query result should be empty", model.isEmpty());
	}

	@Test
	public void testConstructSingle() throws Exception {
		GraphQueryResult result = con.prepareGraphQuery(QueryLanguage.SPARQL, singleConstructQuery).evaluate();
		assertTrue("Query result should not be empty", result.hasNext());

		Model model = QueryResults.asModel(result);
		assertFalse("Query result should not be empty", model.isEmpty());
		assertEquals(1, model.size());
	}

	@Test
	public void testConstructMultiple() throws Exception {
		GraphQueryResult result = con.prepareGraphQuery(QueryLanguage.SPARQL, multipleConstructQuery).evaluate();
		assertTrue("Query result should not be empty", result.hasNext());

		Model model = QueryResults.asModel(result);
		assertFalse("Query result should not be empty", model.isEmpty());
		assertEquals(4, model.size());
	}

}
