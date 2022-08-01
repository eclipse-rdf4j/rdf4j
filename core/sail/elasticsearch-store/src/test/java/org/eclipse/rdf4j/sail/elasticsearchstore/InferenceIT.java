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

package org.eclipse.rdf4j.sail.elasticsearchstore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.assertj.core.util.Files;
import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InferenceIT {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchStoreIT.class);
	private static final SimpleValueFactory vf = SimpleValueFactory.getInstance();

	private static ElasticsearchClusterRunner runner;
	private static SingletonClientProvider singletonClientProvider;

	private static final File installLocation = Files.newTemporaryFolder();

	@BeforeClass
	public static void beforeClass() throws IOException, InterruptedException {
		runner = TestHelpers.startElasticsearch(installLocation);
		singletonClientProvider = new SingletonClientProvider("localhost",
				TestHelpers.getPort(runner), TestHelpers.CLUSTER);
	}

	@AfterClass
	public static void afterClass() throws Exception {
		singletonClientProvider.close();
		TestHelpers.stopElasticsearch(runner);
	}

	@After
	public void after() {
		runner.admin().indices().refresh(Requests.refreshRequest("*")).actionGet();
		deleteAllIndexes();
	}

	private void deleteAllIndexes() {
		for (String index : getIndexes()) {
			logger.info("deleting: " + index);
			runner.admin().indices().delete(Requests.deleteIndexRequest(index)).actionGet();
		}
	}

	private String[] getIndexes() {

		Settings settings = Settings.builder().put("cluster.name", TestHelpers.CLUSTER).build();
		try (TransportClient client = new PreBuiltTransportClient(settings)) {
			client.addTransportAddress(
					new TransportAddress(InetAddress.getByName("localhost"), TestHelpers.getPort(runner)));

			return client.admin()
					.indices()
					.getIndex(new GetIndexRequest())
					.actionGet()
					.getIndices();
		} catch (UnknownHostException e) {
			throw new IllegalStateException(e);
		}

	}

	@Test
	public void initiallyInferredStatementsTest() {
		ElasticsearchStore elasticsearchStore = new ElasticsearchStore(singletonClientProvider, "testindex");

		SailRepository sailRepository = new SailRepository(new SchemaCachingRDFSInferencer(elasticsearchStore));

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			long explicitStatements = connection.getStatements(null, null, null, false).stream().count();
			assertEquals(0, explicitStatements);

			long inferredStatements = connection.getStatements(null, null, null, true).stream().count();
			assertEquals(141, inferredStatements);
		}

		sailRepository.shutDown();
	}

	@Test
	public void simpleInferenceTest() {
		ElasticsearchStore elasticsearchStore = new ElasticsearchStore(singletonClientProvider, "testindex");

		SailRepository sailRepository = new SailRepository(new SchemaCachingRDFSInferencer(elasticsearchStore));

		IRI graph1 = vf.createIRI("http://example.com/graph1");

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.add(vf.createBNode(), RDFS.LABEL, vf.createLiteral("label"), graph1);

			long explicitStatements = connection.getStatements(null, null, null, false, graph1).stream().count();
			assertEquals(1, explicitStatements);

			long inferredStatements = connection.getStatements(null, null, null, true, graph1).stream().count();
			assertEquals(2, inferredStatements);
		}

		sailRepository.shutDown();
	}

	@Test
	public void removeInferredData() {
		ElasticsearchStore elasticsearchStore = new ElasticsearchStore(singletonClientProvider, "testindex");

		SailRepository sailRepository = new SailRepository(new SchemaCachingRDFSInferencer(elasticsearchStore));

		IRI graph1 = vf.createIRI("http://example.com/graph1");

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.begin();
			connection.add(vf.createBNode(), RDFS.LABEL, vf.createLiteral("label"), graph1);
			connection.commit();

			long explicitStatements = connection.getStatements(null, null, null, false, graph1).stream().count();
			assertEquals(1, explicitStatements);

			long inferredStatements = connection.getStatements(null, null, null, true, graph1).stream().count();
			assertEquals(2, inferredStatements);

			connection.begin();
			connection.remove((Resource) null, RDFS.LABEL, null, graph1);
			connection.commit();

			explicitStatements = connection.getStatements(null, null, null, false, graph1).stream().count();
			assertEquals(0, explicitStatements);

			inferredStatements = connection.getStatements(null, null, null, true, graph1).stream().count();
			assertEquals(0, inferredStatements);

		}

		sailRepository.shutDown();
	}

	@Test
	public void addInferredStatement() {
		ElasticsearchStore elasticsearchStore = new ElasticsearchStore(singletonClientProvider, "testindex");

		SailRepository sailRepository = new SailRepository(new SchemaCachingRDFSInferencer(elasticsearchStore));

		IRI graph1 = vf.createIRI("http://example.com/graph1");

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			BNode bNode = vf.createBNode();
			BNode bNode2 = vf.createBNode();

			connection.begin();
			connection.add(bNode, RDFS.LABEL, vf.createLiteral("label"), graph1);
			connection.commit();

			assertTrue(connection.hasStatement(bNode, RDF.TYPE, RDFS.RESOURCE, true));

			connection.begin();
			connection.add(bNode, RDF.TYPE, RDFS.RESOURCE, graph1);
			connection.add(bNode2, RDFS.LABEL, vf.createLiteral("label2"), graph1);

			connection.commit();

			assertTrue(connection.hasStatement(bNode, RDF.TYPE, RDFS.RESOURCE, false));

			connection.begin();
			connection.remove(bNode, RDFS.LABEL, vf.createLiteral("label"));
			connection.commit();

			assertTrue(connection.hasStatement(bNode2, RDFS.LABEL, null, false));

			assertTrue(connection.hasStatement(bNode, RDF.TYPE, RDFS.RESOURCE, true));
			assertTrue(connection.hasStatement(bNode, RDF.TYPE, RDFS.RESOURCE, false));
			assertFalse(connection.hasStatement(bNode, RDFS.LABEL, null, true));

		}

		sailRepository.shutDown();
	}

}
