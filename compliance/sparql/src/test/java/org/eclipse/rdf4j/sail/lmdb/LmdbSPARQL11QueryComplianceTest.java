/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.dataset.DatasetRepository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.testsuite.query.parser.sparql.manifest.SPARQL11QueryComplianceTest;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 * Test SPARQL 1.1 query functionality on LMDB store.
 *
 */
public class LmdbSPARQL11QueryComplianceTest extends SPARQL11QueryComplianceTest {

	public LmdbSPARQL11QueryComplianceTest(String displayName, String testURI, String name, String queryFileURL,
			String resultFileURL, Dataset dataset, boolean ordered, boolean laxCardinality) {
		super(displayName, testURI, name, queryFileURL, resultFileURL, dataset, ordered, laxCardinality);
	}

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Override
	protected Repository newRepository() throws Exception {
		return new DatasetRepository(
				new SailRepository(new LmdbStore(folder.newFolder(), new LmdbStoreConfig("spoc"))));
	}

}
