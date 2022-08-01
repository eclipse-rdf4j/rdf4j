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
package org.eclipse.rdf4j.sail.lmdb;

import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.dataset.DatasetRepository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.testsuite.query.parser.sparql.manifest.SPARQL11UpdateComplianceTest;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 * Test SPARQL 1.1 Update functionality on LMDB store.
 *
 */
public class LmdbPARQL11UpdateComplianceTest extends SPARQL11UpdateComplianceTest {

	public LmdbPARQL11UpdateComplianceTest(String displayName, String testURI, String name, String requestFile,
			IRI defaultGraphURI, Map<String, IRI> inputNamedGraphs, IRI resultDefaultGraphURI,
			Map<String, IRI> resultNamedGraphs) {
		super(displayName, testURI, name, requestFile, defaultGraphURI, inputNamedGraphs, resultDefaultGraphURI,
				resultNamedGraphs);
		// TODO Auto-generated constructor stub
	}

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Override
	protected Repository newRepository() throws Exception {
		return new DatasetRepository(
				new SailRepository(new LmdbStore(folder.newFolder(), new LmdbStoreConfig("spoc"))));
	}
}
