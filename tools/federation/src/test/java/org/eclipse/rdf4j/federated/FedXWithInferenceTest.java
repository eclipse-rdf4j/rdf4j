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
package org.eclipse.rdf4j.federated;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.federated.repository.FedXRepository;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.sail.inferencer.fc.config.SchemaCachingRDFSInferencerConfig;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.jsonldjava.shaded.com.google.common.collect.Lists;

public class FedXWithInferenceTest extends FedXBaseTest {

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
	public void testFederationWithRDFSInference() throws Exception {

		addMemoryStoreWithRDFS("repo1");
		addMemoryStore("repo2");

		ValueFactory vf = SimpleValueFactory.getInstance();
		addData("repo1", Lists.newArrayList(
				vf.createStatement(vf.createIRI("http://ex.org/p1"), RDF.TYPE, FOAF.PERSON),
				vf.createStatement(vf.createIRI("http://ex.org/p2"), RDF.TYPE, vf.createIRI("http://ex.org/Manager")),
				vf.createStatement(vf.createIRI("http://ex.org/Manager"), RDFS.SUBCLASSOF, FOAF.PERSON)));
		addData("repo2", Lists.newArrayList(
				vf.createStatement(vf.createIRI("http://ex.org/p3"), RDF.TYPE, FOAF.PERSON)));

		FedXRepository repo = FedXFactory.newFederation()
				.withResolvableEndpoint("repo1")
				.withResolvableEndpoint("repo2")
				.withRepositoryResolver(repoManager)
				.create();

		try {

			repo.init();
			try (RepositoryConnection conn = repo.getConnection()) {

				// 1. get statements
				List<Statement> sts = Iterations.asList(conn.getStatements(null, RDF.TYPE, FOAF.PERSON));
				Assertions.assertEquals(3, sts.size()); // three persons

				sts = Iterations.asList(conn.getStatements(null, RDF.TYPE, FOAF.PERSON, true));
				Assertions.assertEquals(3, sts.size()); // three persons

				sts = Iterations.asList(conn.getStatements(null, RDF.TYPE, FOAF.PERSON, false));
				Assertions.assertEquals(2, sts.size()); // two persons

				// 2. simple SELECT
				TupleQuery tq = conn.prepareTupleQuery("SELECT * WHERE { ?p a <http://xmlns.com/foaf/0.1/Person> }");
				try (TupleQueryResult tqr = tq.evaluate()) {
					List<BindingSet> res = Iterations.asList(tqr);
					Assertions.assertEquals(3, res.size()); // three persons
				}

				tq.setIncludeInferred(true);
				try (TupleQueryResult tqr = tq.evaluate()) {
					List<BindingSet> res = Iterations.asList(tqr);
					Assertions.assertEquals(3, res.size()); // three persons
				}

				tq.setIncludeInferred(false);
				try (TupleQueryResult tqr = tq.evaluate()) {
					List<BindingSet> res = Iterations.asList(tqr);
					Assertions.assertEquals(2, res.size()); // two persons
				}

				// 3. simple CONSTRUCT
				GraphQuery gq = conn.prepareGraphQuery(
						"CONSTRUCT { ?p a <http://xmlns.com/foaf/0.1/Person> } WHERE { ?p a <http://xmlns.com/foaf/0.1/Person> }");
				try (GraphQueryResult gqr = gq.evaluate()) {
					List<Statement> res = Iterations.asList(gqr);
					Assertions.assertEquals(3, res.size()); // three persons
				}

				gq.setIncludeInferred(true);
				try (GraphQueryResult gqr = gq.evaluate()) {
					List<Statement> res = Iterations.asList(gqr);
					Assertions.assertEquals(3, res.size()); // three persons
				}

				gq.setIncludeInferred(false);
				try (GraphQueryResult gqr = gq.evaluate()) {
					List<Statement> res = Iterations.asList(gqr);
					Assertions.assertEquals(2, res.size()); // three persons
				}
			}

		} finally {
			repo.shutDown();
		}

	}

	protected void addMemoryStore(String repoId) throws Exception {

		RepositoryImplConfig implConfig = new SailRepositoryConfig(new MemoryStoreConfig());
		RepositoryConfig config = new RepositoryConfig(repoId, implConfig);
		repoManager.addRepositoryConfig(config);
	}

	protected void addMemoryStoreWithRDFS(String repoId) throws Exception {

		RepositoryImplConfig implConfig = new SailRepositoryConfig(
				new SchemaCachingRDFSInferencerConfig(new MemoryStoreConfig()));
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
