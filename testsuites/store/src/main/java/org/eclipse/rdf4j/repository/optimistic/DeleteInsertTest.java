/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.optimistic;

import static org.junit.Assert.assertTrue;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.repository.OptimisticIsolationTest;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DeleteInsertTest {
	private Repository repo;
	private String NS = "http://example.org/";
	private RepositoryConnection con;
	private IsolationLevel level = IsolationLevels.SNAPSHOT_READ;
	private ClassLoader cl = getClass().getClassLoader();

	@Before
	public void setUp() throws Exception {
		repo = OptimisticIsolationTest.getEmptyInitializedRepository(DeleteInsertTest.class);
		con = repo.getConnection();
	}

	@After
	public void tearDown() throws Exception {
		con.close();
		repo.shutDown();
	}

	@Test
	public void test() throws Exception {
		String load = IOUtil.readString(cl.getResource("test/insert-data.ru"));
		con.prepareUpdate(QueryLanguage.SPARQL, load, NS).execute();
		con.begin(level);
		String modify = IOUtil.readString(cl.getResource("test/delete-insert.ru"));
		con.prepareUpdate(QueryLanguage.SPARQL, modify, NS).execute();
		con.commit();
		String ask = IOUtil.readString(cl.getResource("test/ask.rq"));
		assertTrue(con.prepareBooleanQuery(QueryLanguage.SPARQL, ask, NS).evaluate());
	}
}
