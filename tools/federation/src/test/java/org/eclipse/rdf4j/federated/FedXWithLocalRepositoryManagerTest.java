/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.federated.repository.FedXRepository;
import org.eclipse.rdf4j.federated.repository.FedXRepositoryConfig;
import org.eclipse.rdf4j.federated.util.Vocabulary.FEDX;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.jsonldjava.shaded.com.google.common.collect.Lists;

public class FedXWithLocalRepositoryManagerTest extends FedXBaseTest {

	@TempDir
	Path tempDir;

	private LocalRepositoryManager repoManager;

	@BeforeEach
	public void before() throws Exception {
		File baseDir = new File(tempDir.toFile(), "data");
		repoManager = new LocalRepositoryManager(baseDir);
		repoManager.init();
	}

	@AfterEach
	public void after() throws Exception {
		repoManager.shutDown();
	}

	@Test
	public void testWithLocalRepositoryManager() throws Exception {

		addMemoryStore("repo1");
		addMemoryStore("repo2");

		ValueFactory vf = SimpleValueFactory.getInstance();
		addData("repo1", Lists.newArrayList(
				vf.createStatement(vf.createIRI("http://ex.org/p1"), RDF.TYPE, FOAF.PERSON)));
		addData("repo2", Lists.newArrayList(
				vf.createStatement(vf.createIRI("http://ex.org/p2"), RDF.TYPE, FOAF.PERSON)));

		FedXRepository repo = FedXFactory.newFederation()
				.withResolvableEndpoint("repo1")
				.withResolvableEndpoint("repo2")
				.withRepositoryResolver(repoManager)
				.create();
		try {

			repo.init();
			try (RepositoryConnection conn = repo.getConnection()) {

				List<Statement> sts = Iterations.asList(conn.getStatements(null, RDF.TYPE, FOAF.PERSON));
				Assertions.assertEquals(2, sts.size()); // two persons
			}

		} finally {
			repo.shutDown();
		}

	}

	@Test
	public void testWithLocalRepositoryManager_FactoryInitialization() throws Exception {

		addMemoryStore("repo1");
		addMemoryStore("repo2");

		ValueFactory vf = SimpleValueFactory.getInstance();
		addData("repo1", Lists.newArrayList(
				vf.createStatement(vf.createIRI("http://ex.org/p1"), RDF.TYPE, FOAF.PERSON)));
		addData("repo2", Lists.newArrayList(
				vf.createStatement(vf.createIRI("http://ex.org/p2"), RDF.TYPE, FOAF.PERSON)));

		Model members = new TreeModel();
		members.add(vf.createIRI("http://ex.org/repo1"), FEDX.STORE, vf.createLiteral("ResolvableRepository"));
		members.add(vf.createIRI("http://ex.org/repo1"), FEDX.REPOSITORY_NAME, vf.createLiteral("repo1"));
		members.add(vf.createIRI("http://ex.org/repo2"), FEDX.STORE, vf.createLiteral("ResolvableRepository"));
		members.add(vf.createIRI("http://ex.org/repo2"), FEDX.REPOSITORY_NAME, vf.createLiteral("repo2"));

		FedXRepositoryConfig fedXRepoConfig = new FedXRepositoryConfig();
		fedXRepoConfig.setMembers(members);

		repoManager.addRepositoryConfig(new RepositoryConfig("federation", fedXRepoConfig));

		Repository repo = repoManager.getRepository("federation");

		try (RepositoryConnection conn = repo.getConnection()) {

			List<Statement> sts = Iterations.asList(conn.getStatements(null, RDF.TYPE, FOAF.PERSON));
			Assertions.assertEquals(2, sts.size()); // two persons
		}

	}

	protected void addMemoryStore(String repoId) throws Exception {

		RepositoryImplConfig implConfig = new SailRepositoryConfig(new MemoryStoreConfig());
		RepositoryConfig config = new RepositoryConfig(repoId, implConfig);
		repoManager.addRepositoryConfig(config);
	}

	protected void addData(String repoId, Iterable<Statement> model) {

		Repository repo = repoManager.getRepository(repoId);

		try (RepositoryConnection conn = repo.getConnection()) {
			conn.add(model);
		}
	}

	@Override
	protected FederationContext federationContext() {
		throw new UnsupportedOperationException("Not available in this context.");
	}

}
