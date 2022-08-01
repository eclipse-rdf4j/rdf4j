/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.assertj.core.util.Files;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.testsuite.sail.RDFNotifyingStoreTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * An extension of RDFStoreTest for testing the class {@link LmdbStore}.
 */
public class LmdbStoreTest extends RDFNotifyingStoreTest {

	/*-----------*
	 * Variables *
	 *-----------*/

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();
	private File dataDir;

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected NotifyingSail createSail() throws SailException {
		try {
			dataDir = tempDir.newFolder();
			NotifyingSail sail = new LmdbStore(dataDir, new LmdbStoreConfig("spoc,posc"));
			sail.init();
			return sail;
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	// Test for SES-542
	@Test()
	public void testGetNamespacePersistence() throws Exception {
		con.begin();
		con.setNamespace("rdf", RDF.NAMESPACE);
		con.commit();
		assertEquals(RDF.NAMESPACE, con.getNamespace("rdf"));

		con.close();
		sail.shutDown();
		sail.init();
		con = sail.getConnection();

		assertEquals(RDF.NAMESPACE, con.getNamespace("rdf"));
	}

	@Test
	public void testContextCacheReconstruction() throws Exception {
		con.begin();
		con.addStatement(RDF.TYPE, RDF.TYPE, RDF.TYPE, RDF.ALT);
		con.commit();
		con.close();
		sail.shutDown();

		File contextFile = new File(dataDir, "/contexts.dat");
		Files.delete(contextFile);

		sail.init();
		con = sail.getConnection();

		assertTrue(contextFile.exists());
		assertThat(QueryResults.asList(con.getContextIDs()).size()).isEqualTo(1);
	}

}
