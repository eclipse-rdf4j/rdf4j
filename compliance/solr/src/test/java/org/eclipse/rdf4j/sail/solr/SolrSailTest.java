/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.solr;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.solr.SolrIndexTest.PropertiesReader;
import org.eclipse.testsuite.rdf4j.sail.lucene.AbstractLuceneSailTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class SolrSailTest extends AbstractLuceneSailTest {

	private static final String DATA_DIR = "target/test-data";

	private static String toRestoreSolrHome = null;

	@BeforeClass
	public static void setUpClass() throws Exception {
		toRestoreSolrHome = System.getProperty("solr.solr.home");
		PropertiesReader reader = new PropertiesReader("maven-config.properties");
		String testSolrHome = reader.getProperty("test.solr.home");
		System.setProperty("solr.solr.home", testSolrHome);
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		System.setProperty("solr.solr.home", toRestoreSolrHome == null ? "" : toRestoreSolrHome);
		toRestoreSolrHome = null;
	}

	@Override
	protected void configure(LuceneSail sail) {
		sail.setParameter(LuceneSail.INDEX_CLASS_KEY, SolrIndex.class.getName());
		sail.setParameter(SolrIndex.SERVER_KEY, "embedded:");
	}

	@Override
	public void tearDown() throws IOException, RepositoryException {
		super.tearDown();
		FileUtils.deleteDirectory(new File(DATA_DIR));
	}
}
