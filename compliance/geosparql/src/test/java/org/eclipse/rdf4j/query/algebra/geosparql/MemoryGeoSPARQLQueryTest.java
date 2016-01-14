/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.geosparql;

import junit.framework.Test;

import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.geosparql.GeoSPARQLManifestTest;
import org.eclipse.rdf4j.query.parser.sparql.manifest.SPARQLQueryTest;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

public class MemoryGeoSPARQLQueryTest extends SPARQLQueryTest {

	public static Test suite()
		throws Exception
	{
		return GeoSPARQLManifestTest.suite(new Factory() {

			@Override
			public SPARQLQueryTest createSPARQLQueryTest(String testURI, String name, String queryFileURL,
					String resultFileURL, Dataset dataSet, boolean laxCardinality)
			{
				return createSPARQLQueryTest(testURI, name, queryFileURL, resultFileURL, dataSet, laxCardinality,
						false);
			}

			@Override
			public SPARQLQueryTest createSPARQLQueryTest(String testURI, String name, String queryFileURL,
					String resultFileURL, Dataset dataSet, boolean laxCardinality, boolean checkOrder)
			{
				return new MemoryGeoSPARQLQueryTest(testURI, name, queryFileURL, resultFileURL, dataSet,
						laxCardinality, checkOrder);
			}

		});
	}

	protected MemoryGeoSPARQLQueryTest(String testURI, String name, String queryFileURL, String resultFileURL,
			Dataset dataSet, boolean laxCardinality, boolean checkOrder)
	{
		super(testURI, name, queryFileURL, resultFileURL, dataSet, laxCardinality, checkOrder);
	}

	@Override
	protected Repository newRepository()
		throws Exception
	{
		return new SailRepository(new MemoryStore());
	}
}
