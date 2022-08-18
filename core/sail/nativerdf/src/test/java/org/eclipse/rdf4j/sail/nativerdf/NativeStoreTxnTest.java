/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import static java.nio.charset.StandardCharsets.US_ASCII;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.nio.file.Files;

import org.eclipse.rdf4j.common.io.NioFile;
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
import org.eclipse.rdf4j.sail.nativerdf.btree.RecordIterator;
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
		Assert.assertEquals(15, repoDir.listFiles().length);

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

	@Test
	public void testOldTxnStatusFile() throws Exception {

		TripleStore tripleStore = new TripleStore(repo.getDataDir(), "spoc");
		try {
			tripleStore.startTransaction();
			tripleStore.storeTriple(1, 2, 3, 4);
			// forget to commit or rollback
		} finally {
			tripleStore.close();
		}

		File txnStatusFile = new File(repo.getDataDir().getAbsolutePath() + "/txn-status");

		// write old format of txn-status
		NioFile nioFile = new NioFile(txnStatusFile);
		byte[] bytes = "COMMITTING".getBytes(US_ASCII);
		nioFile.truncate(bytes.length);
		nioFile.writeBytes(bytes, 0);

		// Try to restore from the uncompleted transaction
		tripleStore = new TripleStore(repo.getDataDir(), "spoc");
		try {
			try (RecordIterator iter = tripleStore.getTriples(-1, -1, -1, -1)) {
				// iter should contain exactly one element
				assertNotNull(iter.next());
				assertNull(iter.next());
			}
		} finally {
			tripleStore.close();
		}

		TxnStatusFile.TxnStatus currentStatus = new TxnStatusFile(repo.getDataDir()).getTxnStatus();

		assertEquals(TxnStatusFile.TxnStatus.NONE, currentStatus);

	}

	@Test
	public void testOldTxnStatusesDoNotConflict() {
		String[] oldTxtStatuses = { "NONE", "ACTIVE", "COMMITTING", "ROLLING_BACK", "UNKNOWN" };

		for (String value1 : oldTxtStatuses) {
			for (TxnStatusFile.TxnStatus value2 : TxnStatusFile.TxnStatus.values()) {
				byte firstByteInOldValue = value1.getBytes(US_ASCII)[0];
				byte firstByteInNewValue = value2.getOnDisk()[0];
				assertNotEquals(firstByteInOldValue, firstByteInNewValue);
			}
		}

	}
}
