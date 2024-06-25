/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation;

import java.io.File;
import java.io.IOException;

import org.eclipse.rdf4j.federated.FedXFactory;
import org.eclipse.rdf4j.federated.repository.FedXRepository;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.dataset.DatasetRepository;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryProvider;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig;
import org.eclipse.rdf4j.testsuite.query.parser.sparql.manifest.SPARQL11QueryComplianceTest;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author jeen
 */
public class FedXSPARQL11QueryComplianceTest extends SPARQL11QueryComplianceTest {

	@TempDir
	public File tempFolder;

	private RepositoryManager manager;

	public FedXSPARQL11QueryComplianceTest() {
		super();

		// FIXME see https://github.com/eclipse/rdf4j/issues/2173
		addIgnoredTest("sq04 - Subquery within graph pattern, default graph does not apply");
	}

	private void initManager() {
		if (manager == null) {
			manager = RepositoryProvider.getRepositoryManager(tempFolder);
		}
	}

	@Override
	protected Repository newRepository() {
		initManager();

		addMemoryStore("repo1");
		addMemoryStore("repo2");
		FedXRepository repo = FedXFactory.newFederation()
				.withResolvableEndpoint("repo1", true)
				.withResolvableEndpoint("repo2")
				.withRepositoryResolver(manager)
				.create();

		// Use DatasetRepository to handle on-the-fly loading of local datasets, as specified in the test manifest
		return new DatasetRepository(repo);
	}

	private void addMemoryStore(String repoId) throws RepositoryConfigException, RepositoryException {
		RepositoryImplConfig implConfig = new SailRepositoryConfig(new MemoryStoreConfig());
		RepositoryConfig config = new RepositoryConfig(repoId, implConfig);
		manager.addRepositoryConfig(config);
	}
}
