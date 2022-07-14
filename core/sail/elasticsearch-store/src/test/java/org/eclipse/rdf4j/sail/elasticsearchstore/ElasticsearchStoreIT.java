/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearchstore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.assertj.core.util.Files;
import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticsearchStoreIT {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchStoreIT.class);
	private static final SimpleValueFactory vf = SimpleValueFactory.getInstance();

	private static ElasticsearchClusterRunner runner;
	private static final File installLocation = Files.newTemporaryFolder();

	@BeforeClass
	public static void beforeClass() throws IOException, InterruptedException {
		runner = TestHelpers.startElasticsearch(installLocation);
	}

	@AfterClass
	public static void afterClass() throws IOException {
		TestHelpers.stopElasticsearch(runner);
	}

	@After
	public void after() throws UnknownHostException {
		runner.admin().indices().refresh(Requests.refreshRequest("*")).actionGet();
		printAllDocs();

		deleteAllIndexes();

	}

	@Before
	public void before() throws UnknownHostException {
//		embeddedElastic.refreshIndices();
//
//		embeddedElastic.deleteIndices();

	}

	private void printAllDocs() {
		for (String index : getIndexes()) {
			logger.info("INDEX: " + index);
			ActionFuture<SearchResponse> res = runner.client().search(Requests.searchRequest(index));
			SearchHits hits = res.actionGet().getHits();
			for (SearchHit hit : hits) {
				logger.info(" doc " + hit.getSourceAsString());
			}
		}
	}

	private void deleteAllIndexes() {
		for (String index : getIndexes()) {
			logger.info("deleting index: " + index);
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
	public void testInstantiate() {
		ElasticsearchStore elasticsearchStore = new ElasticsearchStore("localhost",
				TestHelpers.getPort(runner), TestHelpers.CLUSTER, "testindex");
		elasticsearchStore.shutDown();
	}

	@Test
	public void testGetConneciton() {
		ElasticsearchStore elasticsearchStore = new ElasticsearchStore("localhost",
				TestHelpers.getPort(runner), TestHelpers.CLUSTER, "testindex");
		try (NotifyingSailConnection connection = elasticsearchStore.getConnection()) {
		}
		elasticsearchStore.shutDown();

	}

	@Test
	public void testSailRepository() {
		SailRepository elasticsearchStore = new SailRepository(
				new ElasticsearchStore("localhost", TestHelpers.getPort(runner), TestHelpers.CLUSTER, "testindex"));
		elasticsearchStore.shutDown();
	}

	@Test
	public void testGetSailRepositoryConneciton() {
		SailRepository elasticsearchStore = new SailRepository(
				new ElasticsearchStore("localhost", TestHelpers.getPort(runner), TestHelpers.CLUSTER, "testindex"));
		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
		}
		elasticsearchStore.shutDown();
	}

	@Test
	public void testShutdownAndRecreate() {
		ElasticsearchStore elasticsearchStore = new ElasticsearchStore("localhost",
				TestHelpers.getPort(runner), TestHelpers.CLUSTER, "testindex");
		try (NotifyingSailConnection connection = elasticsearchStore.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.addStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();
		}
		elasticsearchStore.shutDown();
		elasticsearchStore = new ElasticsearchStore("localhost", TestHelpers.getPort(runner), TestHelpers.CLUSTER,
				"testindex");
		try (NotifyingSailConnection connection = elasticsearchStore.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.addStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();
		}
		elasticsearchStore.shutDown();

	}

	@Test(expected = SailException.class)
	public void testShutdownAndReinit() {
		ElasticsearchStore elasticsearchStore = new ElasticsearchStore("localhost",
				TestHelpers.getPort(runner), TestHelpers.CLUSTER, "testindex");
		try (NotifyingSailConnection connection = elasticsearchStore.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.addStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();
		}
		elasticsearchStore.shutDown();

		try (NotifyingSailConnection connection = elasticsearchStore.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.addStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();
		}
		elasticsearchStore.shutDown();

	}

	@Test
	public void testAddRemoveData() {
		ElasticsearchStore elasticsearchStore = new ElasticsearchStore("localhost",
				TestHelpers.getPort(runner), TestHelpers.CLUSTER, "testindex");
		try (NotifyingSailConnection connection = elasticsearchStore.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.addStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();
			connection.begin(IsolationLevels.NONE);
			connection.removeStatements(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();

			List<? extends Statement> statements = Iterations.asList(connection.getStatements(null, null, null, true));
			assertEquals(0, statements.size());

		}
		elasticsearchStore.shutDown();

	}

	@Test
	public void testAddLargeDataset() {
		StopWatch stopWatch = StopWatch.createStarted();
		SailRepository elasticsearchStore = new SailRepository(
				new ElasticsearchStore("localhost", TestHelpers.getPort(runner), TestHelpers.CLUSTER, "testindex"));

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			stopWatch.stop();

			ElasticsearchStoreTransactionsIT.logTime(stopWatch, "Creating repo and getting connection",
					TimeUnit.SECONDS);

			stopWatch = StopWatch.createStarted();
			connection.begin(IsolationLevels.NONE);
			int count = 100000;
			for (int i = 0; i < count; i++) {
				connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral(i));
			}
			connection.commit();
			stopWatch.stop();
			ElasticsearchStoreTransactionsIT.logTime(stopWatch, "Adding data", TimeUnit.SECONDS);

			stopWatch = StopWatch.createStarted();
			assertEquals(count, connection.size());
			stopWatch.stop();
			ElasticsearchStoreTransactionsIT.logTime(stopWatch, "Getting size", TimeUnit.SECONDS);

		}

	}

	@Test
	public void testGC() {

		ClientProvider clientProvider = initElasticsearchStoreForGcTest();

		for (int i = 0; i < 100 && !clientProvider.isClosed(); i++) {
			System.gc();
			try {
				Thread.sleep(i * 100);
			} catch (InterruptedException ignored) {
			}
		}

		assertTrue(clientProvider.isClosed());

	}

	private ClientProvider initElasticsearchStoreForGcTest() {
		ElasticsearchStore sail = new ElasticsearchStore("localhost", TestHelpers.getPort(runner), TestHelpers.CLUSTER,
				"testindex");

		ClientProvider clientProvider = sail.clientProvider;
		SailRepository elasticsearchStore = new SailRepository(sail);

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("label"));
		}
		return clientProvider;
	}

	@Test
	public void testNamespacePersistenc() {

		SailRepository elasticsearchStore = new SailRepository(
				new ElasticsearchStore("localhost", TestHelpers.getPort(runner), TestHelpers.CLUSTER, "testindex"));

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			connection.begin();
			connection.setNamespace(SHACL.PREFIX, SHACL.NAMESPACE);
			connection.commit();
		}

		elasticsearchStore.shutDown();
		elasticsearchStore = new SailRepository(
				new ElasticsearchStore("localhost", TestHelpers.getPort(runner), TestHelpers.CLUSTER, "testindex"));

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			String namespace = connection.getNamespace(SHACL.PREFIX);
			assertEquals(SHACL.NAMESPACE, namespace);
		}
	}

}
