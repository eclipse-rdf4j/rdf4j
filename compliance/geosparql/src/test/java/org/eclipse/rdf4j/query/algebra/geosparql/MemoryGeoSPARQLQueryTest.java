/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.geosparql;

import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

public class MemoryGeoSPARQLQueryTest extends GeoSPARQLManifestTest {

	public MemoryGeoSPARQLQueryTest(String displayName, String testURI, String name, String queryFileURL,
			String resultFileURL, Dataset dataset, boolean ordered) {
		super(displayName, testURI, name, queryFileURL, resultFileURL, dataset, ordered);
	}

	@Override
	protected Repository newRepository() throws Exception {
		return new SailRepository(new MemoryStore());
	}
}
