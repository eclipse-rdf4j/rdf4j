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
package org.eclipse.rdf4j.query.parser.sparql;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.testsuite.query.parser.sparql.SPARQLUpdateTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author jeen
 */
public class HTTPSparqlUpdateTest extends SPARQLUpdateTest {

	private static SPARQLEmbeddedServer server;

	private static final String repositoryId = "test-sparql";

	@BeforeClass
	public static void startServer() throws Exception {
		server = new SPARQLEmbeddedServer(List.of(repositoryId));
		try {
			server.start();
		} catch (Exception e) {
			server.stop();
			throw e;
		}
	}

	@AfterClass
	public static void stopServer() throws Exception {
		server.stop();
	}

	@Override
	protected Repository newRepository() throws Exception {
		return new HTTPRepository(server.getRepositoryUrl(repositoryId));
	}

	@Ignore
	@Test
	@Override
	public void testAutoCommitHandling() {
		// transaction isolation is not supported for HTTP connections. disabling
		// test.
		System.err.println("temporarily disabled testAutoCommitHandling() for HTTPRepository. See SES-1652");
	}

	@Test
	public void testBindingsInUpdateTransaction() throws Exception {
		// See issue SES-1889
		logger.debug("executing test testBindingsInUpdateTransaction");

		StringBuilder update1 = new StringBuilder();
		update1.append(getNamespaceDeclarations());
		update1.append("DELETE { ?x foaf:name ?y } WHERE {?x foaf:name ?y }");

		try {
			assertTrue(con.hasStatement(bob, FOAF.NAME, f.createLiteral("Bob"), true));
			assertTrue(con.hasStatement(alice, FOAF.NAME, f.createLiteral("Alice"), true));

			con.begin();
			Update operation = con.prepareUpdate(QueryLanguage.SPARQL, update1.toString());
			operation.setBinding("x", bob);

			operation.execute();

			con.commit();

			// only bob's name should have been deleted (due to the binding)
			assertFalse(con.hasStatement(bob, FOAF.NAME, f.createLiteral("Bob"), true));
			assertTrue(con.hasStatement(alice, FOAF.NAME, f.createLiteral("Alice"), true));

		} catch (Exception e) {
			if (con.isActive()) {
				con.rollback();
			}
		}
	}

	@Ignore
	@Test
	@Override
	public void testConsecutiveUpdatesInSameTransaction() {
		// transaction isolation is not supported for HTTP connections. disabling
		// test.
		System.err.println(
				"temporarily disabled testConsecutiveUpdatesInSameTransaction() for HTTPRepository. See SES-1652");
	}

	@Ignore
	@Test(expected = MalformedQueryException.class)
	@Override
	public void testInvalidInsertUpdate() {
		// disabling test
		System.err.println("temporarily disabled testInvalidInsertUpdate for HTTPRepository. See Issue #420");
	}

	@Ignore
	@Test(expected = MalformedQueryException.class)
	@Override
	public void testInvalidDeleteUpdate() {
		// disabling test
		System.err.println("temporarily disabled testInvalidDeleteUpdate for HTTPRepository. See Issue #420");
	}
}
