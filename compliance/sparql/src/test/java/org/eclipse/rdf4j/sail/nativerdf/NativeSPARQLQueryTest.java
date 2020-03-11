/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.rdf4j.common.io.FileUtil;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.parser.sparql.manifest.SPARQL11ManifestTest;
import org.eclipse.rdf4j.query.parser.sparql.manifest.SPARQLQueryTest;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.dataset.DatasetRepository;
import org.eclipse.rdf4j.repository.sail.SailRepository;

import junit.framework.Test;

public class NativeSPARQLQueryTest extends SPARQLQueryTest {

	public static Test suite() throws Exception {
		URL manifestUrl = SPARQL11ManifestTest.class.getResource("/testcases-sparql-1.1-w3c/manifest-all.ttl");

		return SPARQL11ManifestTest.suite(new Factory() {
			@Override
			public NativeSPARQLQueryTest createSPARQLQueryTest(String testURI, String name, String queryFileURL,
					String resultFileURL, Dataset dataSet, boolean laxCardinality) {
				return createSPARQLQueryTest(testURI, name, queryFileURL, resultFileURL, dataSet, laxCardinality,
						false);
			}

			@Override
			public NativeSPARQLQueryTest createSPARQLQueryTest(String testURI, String name, String queryFileURL,
					String resultFileURL, Dataset dataSet, boolean laxCardinality, boolean checkOrder) {
				String[] ignoredTests = {
						// test case incompatible with RDF 1.1 - see
						// http://lists.w3.org/Archives/Public/public-sparql-dev/2013AprJun/0006.html
						"STRDT   TypeErrors",
						// test case incompatible with RDF 1.1 - see
						// http://lists.w3.org/Archives/Public/public-sparql-dev/2013AprJun/0006.html
						"STRLANG   TypeErrors",
						// known issue: SES-937
						"sq03 - Subquery within graph pattern, graph variable is not bound" };
				return new NativeSPARQLQueryTest(testURI, name, queryFileURL, resultFileURL, dataSet, laxCardinality,
						checkOrder, ignoredTests);
			}
			// skip 'service' tests because it requires the test rig to start up
			// a remote endpoint
		}, manifestUrl.toString(), true, "service");
	}

	private File dataDir;

	protected NativeSPARQLQueryTest(String testURI, String name, String queryFileURL, String resultFileURL,
			Dataset dataSet, boolean laxCardinality, boolean checkOrder, String[] ignoredTests) {
		super(testURI, name, queryFileURL, resultFileURL, dataSet, laxCardinality, checkOrder, ignoredTests);
	}

	@Override
	protected Repository newRepository() throws IOException {
		dataDir = FileUtil.createTempDir("nativestore");
		return new DatasetRepository(new SailRepository(new NativeStore(dataDir, "spoc")));
	}

	@Override
	protected void tearDown() throws Exception {
		try {
			super.tearDown();
		} finally {
			FileUtil.deleteDir(dataDir);
		}
	}
}
