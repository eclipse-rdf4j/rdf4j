/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.repository.optimistic;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.testsuite.repository.OptimisticIsolationTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test isolation behavior on removal operations
 *
 * @author jeen
 *
 */
public class RemoveIsolationTest {

	@BeforeClass
	public static void setUpClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	@AfterClass
	public static void afterClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
	}

	private Repository repo;

	private RepositoryConnection con;

	private ValueFactory f;

	private final IsolationLevel level = IsolationLevels.SNAPSHOT_READ;

	@Before
	public void setUp() throws Exception {
		repo = OptimisticIsolationTest.getEmptyInitializedRepository(RemoveIsolationTest.class);
		con = repo.getConnection();
		f = con.getValueFactory();
	}

	@After
	public void tearDown() throws Exception {
		try {
			con.close();
		} finally {
			repo.shutDown();
		}
	}

	@Test
	public void testRemoveOptimisticIsolation() throws Exception {
		con.begin(level);

		con.add(f.createIRI("http://example.org/people/alice"), f.createIRI("http://example.org/ontology/name"),
				f.createLiteral("Alice"));

		try (RepositoryResult<Statement> stats = con.getStatements(null, null, null, true)) {
			con.remove(stats);
		}

		try (RepositoryResult<Statement> stats = con.getStatements(null, null, null, true)) {
			assertEquals(Collections.emptyList(), QueryResults.asList(stats));
		}
		con.rollback();
	}

	@Test
	public void testRemoveIsolation() throws Exception {
		con.begin(level);

		con.add(f.createIRI("http://example.org/people/alice"), f.createIRI("http://example.org/ontology/name"),
				f.createLiteral("Alice"));

		try (RepositoryResult<Statement> stats = con.getStatements(null, null, null, true)) {
			con.remove(stats);
		}

		try (RepositoryResult<Statement> stats = con.getStatements(null, null, null, true)) {
			assertEquals(Collections.emptyList(), QueryResults.asList(stats));
		}
		con.rollback();
	}
}
