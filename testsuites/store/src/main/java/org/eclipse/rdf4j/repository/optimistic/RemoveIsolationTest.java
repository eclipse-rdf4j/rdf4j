/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.optimistic;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.OptimisticIsolationTest;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RemoveIsolationTest {

	private Repository repo;

	private RepositoryConnection con;

	private ValueFactory f;

	private IsolationLevel level = IsolationLevels.SNAPSHOT_READ;

	@Before
	public void setUp()
		throws Exception
	{
		repo = OptimisticIsolationTest.getEmptyInitializedRepository(RemoveIsolationTest.class);
		con = repo.getConnection();
		f = con.getValueFactory();
	}

	@After
	public void tearDown()
		throws Exception
	{
		con.close();
		repo.shutDown();
	}

	@Test
	public void testRemoveOptimisticIsolation()
		throws Exception
	{
		con.begin(level);

		con.add(f.createURI("http://example.org/people/alice"),
				f.createURI("http://example.org/ontology/name"), f.createLiteral("Alice"));

		con.remove(con.getStatements(null, null, null, true));

		RepositoryResult<Statement> stats = con.getStatements(null, null, null, true);
		assertEquals(Collections.emptyList(), QueryResults.asList(stats));
		con.rollback();
	}

	@Test
	public void testRemoveIsolation()
		throws Exception
	{
		con.begin(level);

		con.add(f.createURI("http://example.org/people/alice"),
				f.createURI("http://example.org/ontology/name"), f.createLiteral("Alice"));

		con.remove(con.getStatements(null, null, null, true));

		RepositoryResult<Statement> stats = con.getStatements(null, null, null, true);
		assertEquals(Collections.emptyList(), QueryResults.asList(stats));
		con.rollback();
	}
}
