/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql.manifest;

import junit.framework.Test;

import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.parser.sparql.manifest.SPARQL11ManifestTest;
import org.eclipse.rdf4j.query.parser.sparql.manifest.SPARQLQueryTest;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.dataset.DatasetRepository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

public class W3CApprovedSPARQL11QueryTest extends SPARQLQueryTest {

	public static Test suite()
		throws Exception
	{
		return SPARQL11ManifestTest.suite(new Factory() {

			public W3CApprovedSPARQL11QueryTest createSPARQLQueryTest(String testURI, String name,
					String queryFileURL, String resultFileURL, Dataset dataSet, boolean laxCardinality)
			{
				return createSPARQLQueryTest(testURI, name, queryFileURL, resultFileURL, dataSet, laxCardinality,
						false);
			}

			public W3CApprovedSPARQL11QueryTest createSPARQLQueryTest(String testURI, String name,
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

				return new W3CApprovedSPARQL11QueryTest(testURI, name, queryFileURL, resultFileURL, dataSet,
						laxCardinality, checkOrder, ignoredTests);
			}
		}, true, true, false, "service");
	}

	protected W3CApprovedSPARQL11QueryTest(String testURI, String name, String queryFileURL,
			String resultFileURL, Dataset dataSet, boolean laxCardinality, String... ignoredTests)
	{
		this(testURI, name, queryFileURL, resultFileURL, dataSet, laxCardinality, false, ignoredTests);
	}

	protected W3CApprovedSPARQL11QueryTest(String testURI, String name, String queryFileURL,
			String resultFileURL, Dataset dataSet, boolean laxCardinality, boolean checkOrder,
			String... ignoredTests)
	{
		super(testURI, name, queryFileURL, resultFileURL, dataSet, laxCardinality, checkOrder, ignoredTests);
	}

	protected Repository newRepository() {
		return new DatasetRepository(new SailRepository(new MemoryStore()));
	}
}
