/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql.manifest;

import java.net.URL;

import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.dataset.DatasetRepository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import junit.framework.Test;

/**
 * SPARQL Query tests using testcase from the SPARQL 1.2 Working Group
 * 
 * @author Jeen Broekstra
 *
 */
public class SPARQL12QueryTest extends SPARQLQueryTest {

	public static Test suite() throws Exception {
		URL manifestUrl = SPARQL11ManifestTest.class.getResource("/testcases-sparql-1.2/manifest-all.ttl");

		return SPARQL11ManifestTest.suite(new Factory() {

			@Override
			public SPARQL12QueryTest createSPARQLQueryTest(String testURI, String name, String queryFileURL,
					String resultFileURL, Dataset dataSet, boolean laxCardinality) {
				return createSPARQLQueryTest(testURI, name, queryFileURL, resultFileURL, dataSet, laxCardinality,
						false);
			}

			@Override
			public SPARQL12QueryTest createSPARQLQueryTest(String testURI, String name, String queryFileURL,
					String resultFileURL, Dataset dataSet, boolean laxCardinality, boolean checkOrder) {
				return new SPARQL12QueryTest(testURI, name, queryFileURL, resultFileURL, dataSet,
						laxCardinality, checkOrder);
			}
		}, manifestUrl.toString(), false);
	}

	protected SPARQL12QueryTest(String testURI, String name, String queryFileURL, String resultFileURL,
			Dataset dataSet, boolean laxCardinality, String... ignoredTests) {
		this(testURI, name, queryFileURL, resultFileURL, dataSet, laxCardinality, false, ignoredTests);
	}

	protected SPARQL12QueryTest(String testURI, String name, String queryFileURL, String resultFileURL,
			Dataset dataSet, boolean laxCardinality, boolean checkOrder, String... ignoredTests) {
		super(testURI, name, queryFileURL, resultFileURL, dataSet, laxCardinality, checkOrder, ignoredTests);
	}

	@Override
	protected Repository newRepository() {
		return new DatasetRepository(new SailRepository(new MemoryStore()));
	}
}
