/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.rdf4j.common.io.FileUtil;
import org.eclipse.rdf4j.sail.nativerdf.TripleStore;
import org.junit.Test;

public class DefaultIndexTest {

	@Test
	public void testDefaultIndex() throws Exception {
		File dir = FileUtil.createTempDir("nativerdf");
		TripleStore store = new TripleStore(dir, null);
		store.close();
		// check that the triple store used the default index
		assertEquals("spoc,posc", findIndex(dir));
		FileUtil.deleteDir(dir);
	}

	@Test
	public void testExistingIndex() throws Exception {
		File dir = FileUtil.createTempDir("nativerdf");
		// set a non-default index
		TripleStore store = new TripleStore(dir, "spoc,opsc");
		store.close();
		String before = findIndex(dir);
		// check that the index is preserved with a null value
		store = new TripleStore(dir, null);
		store.close();
		assertEquals(before, findIndex(dir));
		FileUtil.deleteDir(dir);
	}

	private String findIndex(File dir) throws Exception {
		Properties properties = new Properties();
		InputStream in = new FileInputStream(new File(dir, "triples.prop"));
		try {
			properties.clear();
			properties.load(in);
		} finally {
			in.close();
		}
		return (String) properties.get("triple-indexes");
	}

}
