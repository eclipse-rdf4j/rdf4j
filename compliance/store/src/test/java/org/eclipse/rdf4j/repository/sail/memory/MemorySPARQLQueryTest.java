/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sail.memory;

import junit.framework.Test;

import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.parser.sparql.manifest.ManifestTest;
import org.eclipse.rdf4j.query.parser.sparql.manifest.SPARQLQueryTest;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.dataset.DatasetRepository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

public class MemorySPARQLQueryTest extends SPARQLQueryTest {

	public static Test suite()
		throws Exception
	{
		return ManifestTest.suite(new Factory() {

			public MemorySPARQLQueryTest createSPARQLQueryTest(String testURI, String name, String queryFileURL,
					String resultFileURL, Dataset dataSet, boolean laxCardinality)
			{
				return createSPARQLQueryTest(testURI, name, queryFileURL, resultFileURL, dataSet, laxCardinality, false);
			}
			
			public MemorySPARQLQueryTest createSPARQLQueryTest(String testURI, String name, String queryFileURL,
					String resultFileURL, Dataset dataSet, boolean laxCardinality, boolean checkOrder)
			{
				return new MemorySPARQLQueryTest(testURI, name, queryFileURL, resultFileURL, dataSet,
						laxCardinality, checkOrder);
			}
		});
	}

	protected MemorySPARQLQueryTest(String testURI, String name, String queryFileURL, String resultFileURL,
			Dataset dataSet, boolean laxCardinality, boolean checkOrder)
	{
		super(testURI, name, queryFileURL, resultFileURL, dataSet, laxCardinality, checkOrder);
	}
	
	protected Repository newRepository() {
		return new DatasetRepository(new SailRepository(new MemoryStore()));
	}
}
