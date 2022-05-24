/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation;

import java.io.IOException;

import org.eclipse.rdf4j.federated.FedXFactory;
import org.eclipse.rdf4j.federated.repository.FedXRepository;
import org.eclipse.rdf4j.query.Dataset;
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
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 * @author jeen
 *
 */
public class FedXSPARQL11QueryComplianceTest extends SPARQL11QueryComplianceTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private static final String dirName = "testmanager";

	private RepositoryManager manager;

	public FedXSPARQL11QueryComplianceTest(String displayName, String testURI, String name, String queryFileURL,
			String resultFileURL, Dataset dataset, boolean ordered, boolean laxCardinality) {
		super(displayName, testURI, name, queryFileURL, resultFileURL, dataset, ordered, laxCardinality);

		// FIXME see https://github.com/eclipse/rdf4j/issues/2173
		addIgnoredTest("sq04 - Subquery within graph pattern, default graph does not apply");
	}

	private void initManager() {
		if (manager == null) {
			try {
				manager = RepositoryProvider.getRepositoryManager(tempFolder.newFolder(dirName));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	protected Repository newRepository() throws Exception {
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

	private void addMemoryStore(String repoId) throws RepositoryConfigException, RepositoryException, IOException {
		RepositoryImplConfig implConfig = new SailRepositoryConfig(new MemoryStoreConfig());
		RepositoryConfig config = new RepositoryConfig(repoId, implConfig);
		manager.addRepositoryConfig(config);
	}
}
