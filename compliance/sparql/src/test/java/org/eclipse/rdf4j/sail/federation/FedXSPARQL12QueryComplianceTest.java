/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
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
import org.eclipse.rdf4j.testsuite.query.parser.sparql.manifest.SPARQL12QueryComplianceTest;
import org.junit.jupiter.api.io.TempDir;

public class FedXSPARQL12QueryComplianceTest extends SPARQL12QueryComplianceTest {

	@TempDir
	public File tempFolder;

	private RepositoryManager manager;

	public FedXSPARQL12QueryComplianceTest() {
		super();
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
