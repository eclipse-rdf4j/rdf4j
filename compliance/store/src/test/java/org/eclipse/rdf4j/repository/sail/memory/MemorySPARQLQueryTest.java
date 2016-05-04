/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sail.memory;

import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.parser.sparql.manifest.SPARQL11ManifestTest;
import org.eclipse.rdf4j.query.parser.sparql.manifest.SPARQLQueryTest;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.dataset.DatasetRepository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import junit.framework.Test;

public class MemorySPARQLQueryTest extends SPARQLQueryTest {

	public static Test suite()
		throws Exception
	{
		return SPARQL11ManifestTest.suite(new Factory() {

			public MemorySPARQLQueryTest createSPARQLQueryTest(String testURI, String name,
					String queryFileURL, String resultFileURL, Dataset dataSet, boolean laxCardinality)
			{
				return createSPARQLQueryTest(testURI, name, queryFileURL, resultFileURL, dataSet,
						laxCardinality, false);
			}

			public MemorySPARQLQueryTest createSPARQLQueryTest(String testURI, String name,
					String queryFileURL, String resultFileURL, Dataset dataSet, boolean laxCardinality,
					boolean checkOrder)
			{
				String[] ignoredTests = {
						// test case incompatible with RDF 1.1 - see
						// http://lists.w3.org/Archives/Public/public-sparql-dev/2013AprJun/0006.html
						"STRDT   TypeErrors",
						// test case incompatible with RDF 1.1 - see
						// http://lists.w3.org/Archives/Public/public-sparql-dev/2013AprJun/0006.html
						"STRLANG   TypeErrors",
						// known issue: SES-937
						"sq03 - Subquery within graph pattern, graph variable is not bound" };

				return new MemorySPARQLQueryTest(testURI, name, queryFileURL, resultFileURL, dataSet,
						laxCardinality, checkOrder, ignoredTests);
			}
			// skip 'service' tests because it requires the test rig to start up
			// a remote endpoint
		}, true, true, false, "service");
	}

	protected MemorySPARQLQueryTest(String testURI, String name, String queryFileURL, String resultFileURL,
			Dataset dataSet, boolean laxCardinality, boolean checkOrder, String[] ignoredTests)
	{
		super(testURI, name, queryFileURL, resultFileURL, dataSet, laxCardinality, checkOrder, ignoredTests);
	}

	protected Repository newRepository() {
		return new DatasetRepository(new SailRepository(new MemoryStore()));
	}
}
