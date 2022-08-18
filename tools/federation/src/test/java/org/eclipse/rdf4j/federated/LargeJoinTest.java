/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.federated.FedXWithLocalRepositoryManagerTest.TestLocalRepositoryManager;
import org.eclipse.rdf4j.federated.repository.FedXRepository;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class LargeJoinTest extends FedXBaseTest {

	@TempDir
	Path tempDir;

	private TestLocalRepositoryManager repoManager;

	@BeforeEach
	public void before() throws Exception {
		File baseDir = new File(tempDir.toFile(), "data");
		repoManager = new TestLocalRepositoryManager(baseDir);
		repoManager.init();
	}

	@AfterEach
	public void after() throws Exception {
		repoManager.shutDown();
	}

	@Override
	protected FederationContext federationContext() {
		throw new UnsupportedOperationException("Not available in this context.");
	}

	@Test
	@Disabled
	public void testWithLocalRepositoryManager() throws Exception {

		addNativeStore("repo1");
		addNativeStore("repo2");

		try (RepositoryConnection conn = repoManager.getRepository("repo1").getConnection()) {
			for (int i = 0; i < 100000; i++) {
				conn.add(Values.iri("http://example.com/p" + i), RDF.TYPE, FOAF.PERSON);
			}

			Assertions.assertEquals(100000, conn.size());
		}

		try (RepositoryConnection conn = repoManager.getRepository("repo2").getConnection()) {
			for (int i = 0; i < 100000; i += 1000) {
				conn.add(Values.iri("http://example.com/p" + i), RDFS.LABEL, Values.literal("p" + i));
			}

			Assertions.assertEquals(100, conn.size());
		}

		FedXRepository repo = FedXFactory.newFederation()
				.withResolvableEndpoint("repo1")
				.withResolvableEndpoint("repo2")
				.withRepositoryResolver(repoManager)
				.create();
		try {

			repo.init();
			try (RepositoryConnection conn = repo.getConnection()) {
				TupleQuery tq = conn
						.prepareTupleQuery(
								"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
										+ "SELECT * WHERE {\n"
										+ "    SERVICE <http://repo1> {\n"
										+ "      ?person a <http://xmlns.com/foaf/0.1/Person>\n"
										+ "    }\n"
										+ "    OPTIONAL {\n"
										+ "       SERVICE <http://repo2> {\n"
										+ "          ?person rdfs:label ?label .\n"
										+ "       }\n"
										+ "    }\n"
										+ "}");

				List<BindingSet> res = Iterations.asList(tq.evaluate());
				Assertions.assertEquals(100000, res.size());
				List<BindingSet> personsWithName = res.stream()
						.filter(bs -> bs.hasBinding("label"))
						.collect(Collectors.toList());
				Assertions.assertEquals(100, personsWithName.size());
			}

		} finally {
			repo.shutDown();
		}

	}

	protected void addNativeStore(String repoId) throws Exception {

		RepositoryImplConfig implConfig = new SailRepositoryConfig(new NativeStoreConfig());
		RepositoryConfig config = new RepositoryConfig(repoId, implConfig);
		repoManager.addRepositoryConfig(config);
	}

	protected void addData(String repoId, Iterable<Statement> model) {

		Repository repo = repoManager.getRepository(repoId);

		try (RepositoryConnection conn = repo.getConnection()) {
			conn.add(model);
		}
	}
}
