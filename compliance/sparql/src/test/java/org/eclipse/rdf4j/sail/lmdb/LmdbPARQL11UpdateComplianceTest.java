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

import java.io.File;
import java.util.UUID;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.dataset.DatasetRepository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.testsuite.query.parser.sparql.manifest.SPARQL11UpdateComplianceTest;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test SPARQL 1.1 Update functionality on LMDB store.
 */
public class LmdbPARQL11UpdateComplianceTest extends SPARQL11UpdateComplianceTest {

	@TempDir
	public File folder;

	@Override
	protected Repository newRepository() throws Exception {

		var temp = new File(folder, UUID.randomUUID().toString());
		temp.mkdir();

		return new DatasetRepository(
				new SailRepository(new LmdbStore(temp, new LmdbStoreConfig("spoc"))));
	}
}
