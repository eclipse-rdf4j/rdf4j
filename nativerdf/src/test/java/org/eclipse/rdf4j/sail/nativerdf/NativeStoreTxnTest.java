/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import java.io.File;
import java.nio.file.Files;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class NativeStoreTxnTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	protected Repository repo;

	protected ValueFactory vf = SimpleValueFactory.getInstance();

	protected IRI ctx = vf.createIRI("http://ex.org/ctx");

	@Before
	public void before() throws Exception {

		File dataDir = tempFolder.newFolder("dbmodel");
		repo = new SailRepository(new NativeStore(dataDir, "spoc,posc"));
		repo.init();
	}

	@After
	public void after() throws Exception {
		repo.shutDown();
	}

	@Test
	public void testTxncacheCleanup() throws Exception {

		/*
		 * Test for issue # On windows the txncacheXXX.dat files did not get properly deleted, as the file is locked
		 * when the deletion is attempted.
		 */

		IRI res = vf.createIRI("http://ex.org/s");

		addStmt(vf.createStatement(res, RDF.TYPE, FOAF.PERSON));

		Statement st1 = vf.createStatement(res, RDFS.LABEL, vf.createLiteral("test"));
		addStmt(st1);

		Statement remStmt = vf.createStatement(res, RDFS.LABEL, vf.createLiteral("test"), ctx);
		removeStmt(remStmt);

		File repoDir = repo.getDataDir();
		System.out.println("Data dir: " + repoDir);

		for (File file : repoDir.listFiles()) {
			System.out.println("# " + file.getName());
		}
		Assert.assertEquals(14, repoDir.listFiles().length);

		// make sure there is no txncacheXXX.dat file
		Assert.assertFalse(Files.list(repoDir.getAbsoluteFile().toPath())
				.anyMatch(file -> file.toFile().getName().matches("txncache[0-9]+.*dat")));

		try (RepositoryConnection conn = repo.getConnection()) {
			Assert.assertEquals(1, conn.size());
		}
	}

	protected void addStmt(Statement stmt) {

		try (RepositoryConnection conn = repo.getConnection()) {

			conn.begin();
			conn.add(stmt, ctx);
			conn.commit();
		}
	}

	protected void removeStmt(Statement stmt) {
		try (RepositoryConnection conn = repo.getConnection()) {

			conn.begin();
			conn.remove(stmt);
			conn.commit();
		}
	}
}
