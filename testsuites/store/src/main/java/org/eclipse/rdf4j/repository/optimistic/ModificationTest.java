/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.optimistic;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.URI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.repository.OptimisticIsolationTest;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ModificationTest {
	private Repository repo;
	private RepositoryConnection con;
	private IsolationLevel level = IsolationLevels.SNAPSHOT_READ;
	private String NS = "http://rdf.example.org/";
	private URI PAINTER;
	private URI PICASSO;

	@Before
	public void setUp() throws Exception {
		repo = OptimisticIsolationTest.getEmptyInitializedRepository(ModificationTest.class);
		ValueFactory uf = repo.getValueFactory();
		PAINTER = uf.createURI(NS, "Painter");
		PICASSO = uf.createURI(NS, "picasso");
		con = repo.getConnection();
	}

	@After
	public void tearDown() throws Exception {
		con.close();
		repo.shutDown();
	}

	@Test
	public void testAdd() throws Exception {
		con.begin(level);
		con.add(PICASSO, RDF.TYPE, PAINTER);
		con.commit();
		assertTrue(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false));
	}

	@Test
	public void testAutoCommit() throws Exception {
		con.add(PICASSO, RDF.TYPE, PAINTER);
		con.close();
		con = repo.getConnection();
		assertTrue(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false));
	}

	@Test
	public void testInsertData() throws Exception {
		con.begin(level);
		con.prepareUpdate(QueryLanguage.SPARQL, "INSERT DATA { <picasso> a <Painter> }", NS).execute();
		con.commit();
		assertTrue(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false));
	}

	@Test
	public void testInsertDataAutoCommit() throws Exception {
		con.prepareUpdate(QueryLanguage.SPARQL, "INSERT DATA { <picasso> a <Painter> }", NS).execute();
		con.close();
		con = repo.getConnection();
		assertTrue(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false));
	}

	@Test
	public void testRemove() throws Exception {
		con.begin(level);
		con.add(PICASSO, RDF.TYPE, PAINTER);
		con.commit();
		con.begin(level);
		con.remove(PICASSO, RDF.TYPE, PAINTER);
		con.commit();
		assertFalse(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false));
	}

	@Test
	public void testAddIn() throws Exception {
		con.begin(level);
		con.add(PICASSO, RDF.TYPE, PAINTER, PICASSO);
		con.commit();
		assertTrue(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false, PICASSO));
	}

	@Test
	public void testRemoveFrom() throws Exception {
		con.begin(level);
		con.add(PICASSO, RDF.TYPE, PAINTER, PICASSO);
		con.commit();
		con.begin(level);
		con.remove(PICASSO, RDF.TYPE, PAINTER, PICASSO);
		con.commit();
		assertFalse(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false, PICASSO));
	}

	@Test
	public void testMove() throws Exception {
		con.begin(level);
		con.add(PICASSO, RDF.TYPE, PAINTER, PICASSO);
		con.commit();
		con.begin(level);
		con.remove(PICASSO, RDF.TYPE, PAINTER, PICASSO);
		con.add(PICASSO, RDF.TYPE, PAINTER, PAINTER);
		con.commit();
		assertFalse(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false, PICASSO));
		assertTrue(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false, PAINTER));
	}

	@Test
	public void testMoveOut() throws Exception {
		con.begin(level);
		con.add(PICASSO, RDF.TYPE, PAINTER, PICASSO);
		con.commit();
		con.begin(level);
		con.remove(PICASSO, RDF.TYPE, PAINTER, PICASSO);
		con.add(PICASSO, RDF.TYPE, PAINTER);
		con.commit();
		assertFalse(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false, PICASSO));
		assertTrue(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false));
	}

	@Test
	public void testCancel() throws Exception {
		con.begin(level);
		con.add(PICASSO, RDF.TYPE, PAINTER, PICASSO);
		con.remove(PICASSO, RDF.TYPE, PAINTER, PICASSO);
		con.commit();
		assertFalse(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false, PICASSO));
	}

	@Test
	public void testRemoveDuplicate() throws Exception {
		con.add(PICASSO, RDF.TYPE, PAINTER, PICASSO, PAINTER);
		assertTrue(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false, PAINTER));
		assertTrue(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false, PICASSO));
		con.begin(level);
		con.remove(PICASSO, RDF.TYPE, PAINTER, PAINTER);
		assertFalse(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false, PAINTER));
		assertTrue(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false, PICASSO));
		assertTrue(con.prepareBooleanQuery(QueryLanguage.SPARQL, "ASK {<"+PICASSO+"> a <"+PAINTER+">}").evaluate());
		con.commit();
		assertFalse(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false, PAINTER));
		assertTrue(con.hasStatement(PICASSO, RDF.TYPE, PAINTER, false, PICASSO));
		assertTrue(con.prepareBooleanQuery(QueryLanguage.SPARQL, "ASK {<"+PICASSO+"> a <"+PAINTER+">}").evaluate());
	}

}
