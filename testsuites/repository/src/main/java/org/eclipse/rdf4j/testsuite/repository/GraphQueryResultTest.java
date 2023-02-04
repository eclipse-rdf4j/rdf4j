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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class GraphQueryResultTest {

	@BeforeAll
	public static void setUpClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	@AfterAll
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

	@BeforeEach
	public void setUp() throws Exception {
		rep = createRepository();
		con = rep.getConnection();

		buildQueries();
		addData();

	}

	@AfterEach
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
		assertFalse(result.hasNext(), "Query result should be empty");

		Model model = QueryResults.asModel(result);
		assertTrue(model.isEmpty(), "Query result should be empty");
	}

	@Test
	public void testDescribeSingle() throws Exception {
		GraphQueryResult result = con.prepareGraphQuery(QueryLanguage.SPARQL, singleDescribeQuery).evaluate();
		assertTrue(result.hasNext(), "Query result should not be empty");

		Model model = QueryResults.asModel(result);
		assertFalse(model.isEmpty(), "Query result should not be empty");
		assertEquals(1, model.size());
	}

	@Test
	public void testDescribeMultiple() throws Exception {
		GraphQueryResult result = con.prepareGraphQuery(QueryLanguage.SPARQL, multipleDescribeQuery).evaluate();
		assertTrue(result.hasNext(), "Query result should not be empty");

		Model model = QueryResults.asModel(result);
		assertFalse(model.isEmpty(), "Query result should not be empty");
		assertEquals(4, model.size());
	}

	@Test
	public void testConstructEmpty() throws Exception {
		GraphQueryResult result = con.prepareGraphQuery(QueryLanguage.SPARQL, emptyConstructQuery).evaluate();
		assertFalse(result.hasNext(), "Query result should be empty");

		Model model = QueryResults.asModel(result);
		assertTrue(model.isEmpty(), "Query result should be empty");
	}

	@Test
	public void testConstructSingle() throws Exception {
		GraphQueryResult result = con.prepareGraphQuery(QueryLanguage.SPARQL, singleConstructQuery).evaluate();
		assertTrue(result.hasNext(), "Query result should not be empty");

		Model model = QueryResults.asModel(result);
		assertFalse(model.isEmpty(), "Query result should not be empty");
		assertEquals(1, model.size());
	}

	@Test
	public void testConstructMultiple() throws Exception {
		GraphQueryResult result = con.prepareGraphQuery(QueryLanguage.SPARQL, multipleConstructQuery).evaluate();
		assertTrue(result.hasNext(), "Query result should not be empty");

		Model model = QueryResults.asModel(result);
		assertFalse(model.isEmpty(), "Query result should not be empty");
		assertEquals(4, model.size());
	}

}
