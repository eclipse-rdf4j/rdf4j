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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
  */
public class TestLmdbStoreUpgrade {

	@Rule
	public final TemporaryFolder tmpDir = new TemporaryFolder();

	@Test
	public void testDevel() throws IOException, SailException {
		File dataDir = tmpDir.getRoot();
		LmdbStore store = new LmdbStore(dataDir);
		try {
			store.init();
			try (NotifyingSailConnection con = store.getConnection()) {
				ValueFactory vf = store.getValueFactory();
				con.begin();
				con.addStatement(RDF.VALUE, RDFS.LABEL, vf.createLiteral("value"));
				con.commit();
			}
		} finally {
			store.shutDown();
		}
		new File(dataDir, "lmdbrdf.ver").delete();
		assertValue(dataDir);
		assertTrue(new File(dataDir, "lmdbrdf.ver").exists());
	}

	public void assertValue(File dataDir) throws SailException {
		LmdbStore store = new LmdbStore(dataDir);
		try {
			store.init();
			try (NotifyingSailConnection con = store.getConnection()) {
				ValueFactory vf = store.getValueFactory();
				CloseableIteration<? extends Statement, SailException> iter;
				iter = con.getStatements(RDF.VALUE, RDFS.LABEL, vf.createLiteral("value"), false);
				try {
					assertTrue(iter.hasNext());
				} finally {
					iter.close();
				}
			}
		} finally {
			store.shutDown();
		}
	}
}
