/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
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
import java.util.Arrays;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.federated.repository.FedXRepository;
import org.eclipse.rdf4j.federated.repository.FedXRepositoryConfig;
import org.eclipse.rdf4j.federated.repository.FedXRepositoryConfigBuilder;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.AbstractFederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedService;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.repository.sparql.federation.RepositoryFederatedService;
import org.eclipse.rdf4j.repository.sparql.federation.SPARQLServiceResolver;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.jsonldjava.shaded.com.google.common.collect.Lists;
import com.github.jsonldjava.shaded.com.google.common.collect.Sets;

public class FedXWithLocalRepositoryManagerTest extends FedXBaseTest {

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

		FedXRepositoryConfig fedXRepoConfig = FedXRepositoryConfigBuilder.create()
				.withResolvableEndpoint(Arrays.asList("repo1", "repo2"))
				.build();
		repoManager.addRepositoryConfig(new RepositoryConfig("federation", fedXRepoConfig));

		Repository repo = repoManager.getRepository("federation");

		try (RepositoryConnection conn = repo.getConnection()) {

			List<Statement> sts = Iterations.asList(conn.getStatements(null, RDF.TYPE, FOAF.PERSON));
			Assertions.assertEquals(2, sts.size()); // two persons
		}

	}

	@Test
	public void testWithLocalRepositoryManager_CustomFederatedServiceResolver() throws Exception {

		addMemoryStore("repo1");
		addMemoryStore("repo2");
		addMemoryStore("serviceRepo");

		// register custom federated service resolver
		AbstractFederatedServiceResolver serviceResolver = new SPARQLServiceResolver() {
			@Override
			protected FederatedService createService(String serviceUrl) throws QueryEvaluationException {
				if (serviceUrl.equals("http://serviceRepo")) {
					Repository serviceRepo = repoManager.getRepository("serviceRepo");
					return new RepositoryFederatedService(serviceRepo, false);
				}
				throw new IllegalArgumentException("Service url cannot be resolved: " + serviceUrl);
			}
		};
		repoManager.externalResolver = serviceResolver;

		ValueFactory vf = SimpleValueFactory.getInstance();
		addData("repo1", Lists.newArrayList(
				vf.createStatement(vf.createIRI("http://ex.org/p1"), RDF.TYPE, FOAF.PERSON)));
		addData("repo2", Lists.newArrayList(
				vf.createStatement(vf.createIRI("http://ex.org/p2"), RDF.TYPE, FOAF.PERSON)));

		addData("serviceRepo", Lists.newArrayList(
				vf.createStatement(vf.createIRI("http://ex.org/p1"), FOAF.NAME, vf.createLiteral("Person 1")),
				vf.createStatement(vf.createIRI("http://ex.org/p2"), FOAF.NAME, vf.createLiteral("Person 2"))));

		FedXRepositoryConfig fedXRepoConfig = FedXRepositoryConfigBuilder.create()
				.withResolvableEndpoint(Arrays.asList("repo1", "repo2"))
				.build();
		repoManager.addRepositoryConfig(new RepositoryConfig("federation", fedXRepoConfig));

		Repository repo = repoManager.getRepository("federation");

		try (RepositoryConnection conn = repo.getConnection()) {

			TupleQuery tq = conn.prepareTupleQuery(
					"SELECT * WHERE { ?person a ?PERSON . { SERVICE <http://serviceRepo> { ?person ?NAME ?name} } }");
			tq.setBinding("PERSON", FOAF.PERSON);
			tq.setBinding("NAME", FOAF.NAME);

			List<BindingSet> bindings = Iterations.asList(tq.evaluate());
			Assertions.assertEquals(2, bindings.size()); // two persons

			BindingSet b1 = bindings.get(0);
			BindingSet b2 = bindings.get(1);

			if (b1.getValue("person").equals(vf.createIRI("http://ex.org/p1"))) {
				Assertions.assertEquals(vf.createLiteral("Person 1"), b1.getValue("name"));
				Assertions.assertEquals(vf.createLiteral("Person 2"), b2.getValue("name"));
			} else {
				Assertions.assertEquals(vf.createLiteral("Person 2"), b1.getValue("name"));
				Assertions.assertEquals(vf.createLiteral("Person 1"), b2.getValue("name"));
			}

		}

	}

	@Test
	public void testMultipleFederationInstances() throws Exception {

		addMemoryStore("repo1");
		addMemoryStore("repo2");
		addMemoryStore("repo3");

		ValueFactory vf = SimpleValueFactory.getInstance();
		addData("repo1", Lists.newArrayList(
				vf.createStatement(vf.createIRI("http://ex.org/p1"), RDF.TYPE, FOAF.PERSON)));
		addData("repo2", Lists.newArrayList(
				vf.createStatement(vf.createIRI("http://ex.org/p2"), RDF.TYPE, FOAF.PERSON)));
		addData("repo3", Lists.newArrayList(
				vf.createStatement(vf.createIRI("http://ex.org/p3"), RDF.TYPE, FOAF.PERSON)));

		FedXRepositoryConfig fedXRepo1Config = FedXRepositoryConfigBuilder.create()
				.withResolvableEndpoint(Arrays.asList("repo1", "repo2"))
				.build();
		repoManager.addRepositoryConfig(new RepositoryConfig("federation1", fedXRepo1Config));

		FedXRepositoryConfig fedXRepo2Config = FedXRepositoryConfigBuilder.create()
				.withResolvableEndpoint(Arrays.asList("repo1", "repo3"))
				.build();
		repoManager.addRepositoryConfig(new RepositoryConfig("federation2", fedXRepo2Config));

		// query federation 1 (contains person1 and person2)
		Repository fedRepo1 = repoManager.getRepository("federation1");
		try (RepositoryConnection conn = fedRepo1.getConnection()) {

			Model m = new TreeModel(Iterations.asList(conn.getStatements(null, RDF.TYPE, FOAF.PERSON)));
			Assertions.assertEquals(2, m.size()); // two persons
			Assertions.assertEquals(Sets.newHashSet(vf.createIRI("http://ex.org/p1"), vf.createIRI("http://ex.org/p2")),
					m.subjects());
		}

		// query federation 1 (contains person1 and person3)
		Repository fedRepo2 = repoManager.getRepository("federation2");
		try (RepositoryConnection conn = fedRepo2.getConnection()) {

			Model m = new TreeModel(Iterations.asList(conn.getStatements(null, RDF.TYPE, FOAF.PERSON)));
			Assertions.assertEquals(2, m.size()); // two persons
			Assertions.assertEquals(Sets.newHashSet(vf.createIRI("http://ex.org/p1"), vf.createIRI("http://ex.org/p3")),
					m.subjects());
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

	static class TestLocalRepositoryManager extends LocalRepositoryManager {

		protected AbstractFederatedServiceResolver externalResolver;

		public TestLocalRepositoryManager(File baseDir) {
			super(baseDir);
		}

		@Override
		protected FederatedServiceResolver getFederatedServiceResolver() {
			if (externalResolver != null) {
				return externalResolver;
			}
			return super.getFederatedServiceResolver();
		}

		@Override
		public void shutDown() {
			if (externalResolver != null) {
				externalResolver.shutDown();
			}
			super.shutDown();
		}
	}

}
