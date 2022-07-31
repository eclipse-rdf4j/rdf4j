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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.rdf4j.common.io.FileUtil;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DefaultIndexTest {
	@Rule
	public final TemporaryFolder tmpDir = new TemporaryFolder();

	@Test
	public void testDefaultIndex() throws Exception {
		File dir = tmpDir.newFolder();
		TripleStore store = new TripleStore(dir, new LmdbStoreConfig());
		store.close();
		// check that the triple store used the default index
		assertEquals("spoc,posc", findIndex(dir));
		FileUtil.deleteDir(dir);
	}

	@Test
	public void testExistingIndex() throws Exception {
		File dir = tmpDir.newFolder();
		// set a non-default index
		TripleStore store = new TripleStore(dir, new LmdbStoreConfig("spoc,opsc"));
		store.close();
		String before = findIndex(dir);
		// check that the index is preserved with a null value
		store = new TripleStore(dir, new LmdbStoreConfig(null));
		store.close();
		assertEquals(before, findIndex(dir));
		FileUtil.deleteDir(dir);
	}

	private String findIndex(File dir) throws Exception {
		Properties properties = new Properties();
		try (InputStream in = new FileInputStream(new File(dir, "triples.prop"))) {
			properties.clear();
			properties.load(in);
		}
		return (String) properties.get("triple-indexes");
	}

}
