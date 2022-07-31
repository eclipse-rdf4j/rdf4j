/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql.manifest;

import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.dataset.DatasetRepository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.testsuite.query.parser.sparql.manifest.SPARQL10QueryComplianceTest;

/**
 * Checks conformance of SPARQL query evaluation against the W3C-approved SPARQL 1.0 query test cases
 *
 * @author Jeen Broekstra
 */
public class W3CApprovedSPARQL10QueryTest extends SPARQL10QueryComplianceTest {

	/**
	 * @param displayName
	 * @param testURI
	 * @param name
	 * @param queryFileURL
	 * @param resultFileURL
	 * @param dataset
	 * @param ordered
	 */
	public W3CApprovedSPARQL10QueryTest(String displayName, String testURI, String name, String queryFileURL,
			String resultFileURL, Dataset dataset, boolean ordered, boolean laxCardinality) {
		super(displayName, testURI, name, queryFileURL, resultFileURL, dataset, ordered, laxCardinality);
	}

	@Override
	protected Repository newRepository() {
		return new DatasetRepository(new SailRepository(new MemoryStore()));
	}
}
