/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.eclipse.rdf4j.common.io.FileUtil;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DefaultIndexTest {

	@Test
	public void testDefaultIndex(@TempDir File dir) throws Exception {
		StoreProperties properties = new StoreProperties(dir);
		TripleStore store = new TripleStore(dir, properties, new LmdbStoreConfig(), null);
		store.close();
		// check that the triple store used the default index
		assertEquals("spoc,posc", properties.getTripleIndexes());
		FileUtil.deleteDir(dir);
	}

	@Test
	public void testExistingIndex(@TempDir File dir) throws Exception {
		// set a non-default index
		StoreProperties properties = new StoreProperties(dir);
		TripleStore store = new TripleStore(dir, properties, new LmdbStoreConfig("spoc,opsc"), null);
		properties.save();
		store.close();
		properties = new StoreProperties(dir);
		properties.load();
		String before = properties.getTripleIndexes();
		// check that the index is preserved with a null value
		store = new TripleStore(dir, properties, new LmdbStoreConfig(null), null);
		store.close();
		assertEquals(before, properties.getTripleIndexes());
		FileUtil.deleteDir(dir);
	}
}
