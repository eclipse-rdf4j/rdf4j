/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearchstore;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.apache.commons.lang3.time.StopWatch;
import org.assertj.core.util.Files;
import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
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
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticsearchStoreTransactionsIT {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchStoreTransactionsIT.class);
	private static final SimpleValueFactory vf = SimpleValueFactory.getInstance();

	private static final File installLocation = Files.newTemporaryFolder();
	private static ElasticsearchClusterRunner runner;

	private static ElasticsearchStore elasticsearchStore;

	@BeforeClass
	public static void beforeClass() throws IOException, InterruptedException {

		runner = TestHelpers.startElasticsearch(installLocation);

		elasticsearchStore = new ElasticsearchStore("localhost", TestHelpers.getPort(runner), TestHelpers.CLUSTER,
				"test");

	}

	@AfterClass
	public static void afterClass() throws IOException {
		elasticsearchStore.shutDown();
		TestHelpers.stopElasticsearch(runner);
	}

	@Before
	public void before() throws UnknownHostException {

		elasticsearchStore.setElasticsearchScrollTimeout(60000);

		try (NotifyingSailConnection connection = elasticsearchStore.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.removeStatements(null, null, null);
			connection.commit();
		}

	}

	public static void logTime(StopWatch stopWatch, String message, TimeUnit timeUnit) {

		if (timeUnit == TimeUnit.SECONDS) {
			logger.info("`{}` took {} seconds", message, stopWatch.getTime(TimeUnit.MILLISECONDS) / 1000.0);

		} else if (timeUnit == TimeUnit.MILLISECONDS) {
			logger.info("`{}` took {} ms", message, stopWatch.getTime(TimeUnit.MILLISECONDS));

		} else {
			throw new RuntimeException("Unknow time unit: " + timeUnit);
		}

	}

	private void printAllDocs() {
		for (String index : getIndexes()) {
			logger.info("INDEX: " + index);
			ActionFuture<SearchResponse> res = runner.client().search(Requests.searchRequest(index));
			SearchHits hits = res.actionGet().getHits();
			for (SearchHit hit : hits) {
				logger.info(hit.getSourceAsString());
			}
		}
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
	public void testAddData() {
		try (NotifyingSailConnection connection = elasticsearchStore.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.addStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();
		}

	}

	@Test
	public void testGetData() {
		try (NotifyingSailConnection connection = elasticsearchStore.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.addStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.flush();
			List<? extends Statement> statements = Iterations.asList(connection.getStatements(null, null, null, true));

			connection.commit();

			System.out.println(Arrays.toString(statements.toArray()));

			assertEquals(1, statements.size());
			assertEquals(SimpleValueFactory.getInstance().createStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE),
					statements.get(0));
		}

	}

	@Test
	public void testGetDataSailRepository() {
		SailRepository elasticsearchStore = new SailRepository(this.elasticsearchStore);
		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			connection.add(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);

			List<? extends Statement> statements = Iterations.asList(connection.getStatements(null, null, null, true));

			System.out.println(Arrays.toString(statements.toArray()));

			assertEquals(1, statements.size());
			assertEquals(SimpleValueFactory.getInstance().createStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE),
					statements.get(0));
		}

	}

	@Test
	public void testGetDataSailRepositorySpecificStatement() {
		SailRepository elasticsearchStore = new SailRepository(this.elasticsearchStore);
		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			connection.add(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);

			List<? extends Statement> statements = Iterations
					.asList(connection.getStatements(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE, true));

			System.out.println(Arrays.toString(statements.toArray()));

			assertEquals(1, statements.size());
			assertEquals(SimpleValueFactory.getInstance().createStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE),
					statements.get(0));
		}

	}

	@Test
	public void testGetDataSailRepositoryBNodeSubject() {
		SailRepository elasticsearchStore = new SailRepository(this.elasticsearchStore);
		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {

			BNode bNode = vf.createBNode();
			connection.add(bNode, RDF.TYPE, RDFS.RESOURCE);

			List<? extends Statement> statements = Iterations
					.asList(connection.getStatements(bNode, RDF.TYPE, RDFS.RESOURCE, true));

			System.out.println(Arrays.toString(statements.toArray()));

			assertEquals(1, statements.size());

			assertEquals(bNode, statements.get(0).getSubject());

		}

	}

	@Test
	public void testGetDataSailRepositoryBNodeObject() {
		SailRepository elasticsearchStore = new SailRepository(this.elasticsearchStore);
		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {

			BNode bNode = vf.createBNode();
			connection.add(bNode, RDF.TYPE, bNode);

			List<? extends Statement> statements = Iterations
					.asList(connection.getStatements(bNode, RDF.TYPE, bNode, true));

			System.out.println(Arrays.toString(statements.toArray()));

			assertEquals(1, statements.size());

			assertEquals(bNode, statements.get(0).getSubject());
			assertEquals(bNode, statements.get(0).getObject());

		}

	}

	@Test
	public void testGetDataSailRepositoryStringObject() {
		SailRepository elasticsearchStore = new SailRepository(this.elasticsearchStore);
		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {

			Literal label = vf.createLiteral("label1");
			connection.add(RDF.TYPE, RDFS.LABEL, label);

			List<? extends Statement> statements = Iterations
					.asList(connection.getStatements(null, null, label, true));

			System.out.println(Arrays.toString(statements.toArray()));

			assertEquals(1, statements.size());

			assertEquals(label, statements.get(0).getObject());

		}

	}

	@Test
	public void testGetDataSailRepositoryStringObjectWhitespace() {
		SailRepository elasticsearchStore = new SailRepository(this.elasticsearchStore);
		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {

			Literal label = vf.createLiteral("rdf:type label \n jfelwkfjl \r fjklwejf \t åøæ");
			connection.add(RDF.TYPE, RDFS.LABEL, label);

			List<? extends Statement> statements = Iterations
					.asList(connection.getStatements(null, null, label, true));

			System.out.println(Arrays.toString(statements.toArray()));

			assertEquals(1, statements.size());

			assertEquals(label, statements.get(0).getObject());

		}

	}

	@Test
	public void testGetDataSailRepositoryLongString() {
		SailRepository elasticsearchStore = new SailRepository(this.elasticsearchStore);
		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {

			StringBuilder sb = new StringBuilder();
			IntStream.range(0, 100000).forEach(i -> sb.append(i + ""));

			Literal label = vf.createLiteral(sb.toString());
			connection.add(RDF.TYPE, RDFS.LABEL, label);

			List<? extends Statement> statements = Iterations
					.asList(connection.getStatements(null, null, label, true));

			System.out.println(Arrays.toString(statements.toArray()));

			assertEquals(1, statements.size());

			assertEquals(label, statements.get(0).getObject());

		}

	}

	@Test
	public void testGetDataSailRepositoryDate() {
		SailRepository elasticsearchStore = new SailRepository(this.elasticsearchStore);
		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {

			Literal label = vf.createLiteral(new Date());
			connection.add(RDF.TYPE, RDFS.LABEL, label);

			List<? extends Statement> statements = Iterations
					.asList(connection.getStatements(null, null, label, true));

			System.out.println(Arrays.toString(statements.toArray()));

			assertEquals(1, statements.size());

			assertEquals(label, statements.get(0).getObject());

		}

	}

	@Test
	public void testGetDataSailRepositoryLang() {
		SailRepository elasticsearchStore = new SailRepository(this.elasticsearchStore);
		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {

			Literal label = vf.createLiteral("norsk bokmål", "nb");
			connection.add(RDF.TYPE, RDFS.LABEL, label);

			List<? extends Statement> statements = Iterations
					.asList(connection.getStatements(null, null, label, true));

			System.out.println(Arrays.toString(statements.toArray()));

			assertEquals(1, statements.size());

			assertEquals(label, statements.get(0).getObject());

		}

	}

	@Test
	public void testGetDataSailRepositoryIRISubjectWhitespace() {
		SailRepository elasticsearchStore = new SailRepository(this.elasticsearchStore);
		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {

			IRI iri = vf.createIRI("http://example.com/ space /test");
			connection.add(iri, RDF.TYPE, RDFS.RESOURCE);

			StopWatch stopWatch = StopWatch.createStarted();

			List<? extends Statement> statements = Iterations
					.asList(connection.getStatements(iri, RDF.TYPE, RDFS.RESOURCE, true));

			stopWatch.stop();
			logTime(stopWatch, "Query", TimeUnit.MILLISECONDS);

			System.out.println(Arrays.toString(statements.toArray()));

			assertEquals(1, statements.size());

			assertEquals(iri, statements.get(0).getSubject());

		}

	}

	@Test
	public void testGetDataSailRepositoryContextIRI() {
		SailRepository elasticsearchStore = new SailRepository(this.elasticsearchStore);
		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {

			Resource context1 = vf.createIRI("http://example.com/context1");
			Resource context2 = vf.createBNode();
			Resource context3 = vf.createIRI("http://example.com/context3");

			connection.add(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE, context1);
			connection.add(RDF.TYPE, RDF.TYPE, RDF.PROPERTY, context2);
			connection.add(RDF.TYPE, RDF.TYPE, RDF.PREDICATE, context3);
			connection.add(RDF.TYPE, RDF.TYPE, vf.createLiteral("no context"));

			printAllDocs();

			StopWatch stopWatch = StopWatch.createStarted();
			Set<Statement> context1Statements = Iterations
					.asSet(connection.getStatements(null, RDF.TYPE, null, context1));

			stopWatch.stop();
			logTime(stopWatch, "Query", TimeUnit.MILLISECONDS);

			stopWatch = StopWatch.createStarted();

			Set<Statement> context2Statements = Iterations
					.asSet(connection.getStatements(null, RDF.TYPE, null, context2));

			stopWatch.stop();
			logTime(stopWatch, "Query", TimeUnit.MILLISECONDS);

			stopWatch = StopWatch.createStarted();

			Set<Statement> context3Statements = Iterations
					.asSet(connection.getStatements(null, RDF.TYPE, null, context3));

			stopWatch.stop();
			logTime(stopWatch, "Query", TimeUnit.MILLISECONDS);

			stopWatch = StopWatch.createStarted();

			Set<Statement> contextNoneStatements = Iterations
					.asSet(connection.getStatements(null, RDF.TYPE, null, true, (Resource) null));

			stopWatch.stop();
			logTime(stopWatch, "Query", TimeUnit.MILLISECONDS);

			stopWatch = StopWatch.createStarted();

			Set<Statement> contextAllStatements = Iterations
					.asSet(connection.getStatements(null, RDF.TYPE, null));

			stopWatch.stop();
			logTime(stopWatch, "Query", TimeUnit.MILLISECONDS);

			stopWatch = StopWatch.createStarted();

			Set<Statement> contextContext1And2Statements = Iterations
					.asSet(connection.getStatements(null, RDF.TYPE, null, context1, context2));

			stopWatch.stop();
			logTime(stopWatch, "Query", TimeUnit.MILLISECONDS);

			Statement statementContext1 = vf.createStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE, context1);
			Statement statementContext2 = vf.createStatement(RDF.TYPE, RDF.TYPE, RDF.PROPERTY, context2);
			Statement statementContext3 = vf.createStatement(RDF.TYPE, RDF.TYPE, RDF.PREDICATE, context3);
			Statement statementContextNone = vf.createStatement(RDF.TYPE, RDF.TYPE, vf.createLiteral("no context"));

			assertEquals(asSet(statementContext1), context1Statements);
			assertEquals(asSet(statementContext2), context2Statements);
			assertEquals(asSet(statementContext3), context3Statements);
			assertEquals(asSet(statementContext1, statementContext2), contextContext1And2Statements);
			assertEquals(asSet(statementContextNone), contextNoneStatements);
			assertEquals(asSet(statementContext1, statementContext2, statementContext3, statementContextNone),
					contextAllStatements);

		}

	}

	@Test
	public void sparqlTest() throws IOException {
		SailRepository elasticsearchStore = new SailRepository(this.elasticsearchStore);
		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.add(ElasticsearchStoreTransactionsIT.class.getClassLoader().getResourceAsStream("testFile.ttl"),
					"",
					RDFFormat.TURTLE);
			connection.commit();

			StopWatch stopwatch = StopWatch.createStarted();

			TupleQuery tupleQuery = connection.prepareTupleQuery(String.join("\n", "",
					"PREFIX sh: <http://www.w3.org/ns/shacl#>",
					"select * where {",
					"	?a a sh:NodeShape ;",
					"		sh:property ?property .",
					"",
					"	?property sh:path ?path;",
					"				 sh:minCount ?minCount.",
					"}"));

			List<BindingSet> bindingSets = Iterations.asList(tupleQuery.evaluate());

			stopwatch.stop();
			logTime(stopwatch, "query", TimeUnit.MILLISECONDS);

			assertEquals(1, bindingSets.size());
			assertEquals("http://example.com/ns#PersonShape", bindingSets.get(0).getValue("a").stringValue());
			assertEquals("http://www.w3.org/2000/01/rdf-schema#label",
					bindingSets.get(0).getValue("path").stringValue());
			assertEquals("1", bindingSets.get(0).getValue("minCount").stringValue());

		}

	}

	@Test
	public void testAddDelete() {
		SailRepository elasticsearchStore = new SailRepository(this.elasticsearchStore);
		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {

			connection.begin(IsolationLevels.NONE);
			connection.add(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();

			assertEquals(1, connection.size());

			connection.begin(IsolationLevels.NONE);
			connection.remove(RDF.TYPE, null, null);
			connection.commit();

			assertEquals(0, connection.size());
		}

	}

	@Test
	public void testRollback() {
		SailRepository elasticsearchStore = new SailRepository(this.elasticsearchStore);
		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {

			connection.begin(IsolationLevels.READ_COMMITTED);
			connection.add(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.rollback();

			assertEquals(0, connection.size());
		}

	}

	@Test
	public void testRollback2() {
		SailRepository elasticsearchStore = new SailRepository(this.elasticsearchStore);
		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {

			connection.begin(IsolationLevels.READ_COMMITTED);
			connection.add(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			assertEquals(1, connection.size());
			connection.rollback();

			assertEquals(0, connection.size());
		}

	}

	@Test
	public void testRollback3() {
		SailRepository elasticsearchStore = new SailRepository(this.elasticsearchStore);
		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {

			connection.begin(IsolationLevels.READ_COMMITTED);
			assertEquals(0, connection.size());
			connection.add(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			assertEquals(1, connection.size());
			connection.rollback();

			assertEquals(0, connection.size());
		}

	}

	@Test
	public void testRollbackClear() {
		SailRepository elasticsearchStore = new SailRepository(this.elasticsearchStore);
		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {

			BNode context = vf.createBNode();

			connection.begin(IsolationLevels.READ_COMMITTED);
			connection.add(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDF.TYPE, RDF.TYPE, RDF.PROPERTY, context);
			connection.commit();
			assertEquals(2, connection.size());

			connection.begin();
			connection.clear(context);
			connection.commit();
			assertEquals(1, connection.size());

			connection.begin();
			connection.clear();
			assertEquals(0, connection.size());

			connection.rollback();

			assertEquals(1, connection.size());
		}

	}

	@Test
	public void testRollbackClearSimple() {
		SailRepository elasticsearchStore = new SailRepository(this.elasticsearchStore);
		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {

			BNode context = vf.createBNode();

			connection.begin(IsolationLevels.READ_COMMITTED);
			connection.add(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDF.TYPE, RDF.TYPE, RDF.PROPERTY, context);
			connection.commit();

			connection.begin();
			connection.clear();
			assertEquals(0, connection.size());
			connection.rollback();

			assertEquals(2, connection.size());

		}

	}

	@Test
	public void testHashCollision() {
		String fb = "FB";
		String ea = "Ea";

		assertEquals(fb.hashCode(), ea.hashCode());

		SailRepository elasticsearchStore = new SailRepository(this.elasticsearchStore);
		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			Literal fbLiteral = vf.createLiteral(fb);
			Literal eaLiteral = vf.createLiteral(ea);

			assertEquals(fbLiteral.stringValue().hashCode(), eaLiteral.stringValue().hashCode());

			connection.begin(IsolationLevels.NONE);
			connection.add(RDF.TYPE, RDFS.LABEL, fbLiteral);
			connection.add(RDF.TYPE, RDFS.LABEL, eaLiteral);
			connection.commit();

			assertEquals(2, connection.size());

			List<Statement> fbLiteralList = Iterations.asList(connection.getStatements(null, null, fbLiteral));
			assertEquals(1, fbLiteralList.size());

			List<Statement> eaLiteralList = Iterations.asList(connection.getStatements(null, null, eaLiteral));
			assertEquals(1, eaLiteralList.size());

		}
	}

	// TODO: this throws a SearchPhaseExecutionException, even thought it should have gotten wrapped at some point in a
	// RepositoryException or something like that
	@Ignore("slow test")
	@Test(expected = RepositoryException.class)
	public void testScrollTimeout() throws InterruptedException {
		SailRepository elasticsearchStore = new SailRepository(this.elasticsearchStore);
		this.elasticsearchStore.setElasticsearchScrollTimeout(1);

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {

			connection.begin(IsolationLevels.NONE);
			for (int i = 0; i < 2000; i++) {
				connection.add(RDF.TYPE, RDF.TYPE, vf.createLiteral(i));

			}
			connection.commit();

			try (RepositoryResult<Statement> statements = connection.getStatements(null, null, null, false)) {
				int count = 0;
				while (statements.hasNext()) {
					if (count++ % 1000 == 999) {
						Thread.sleep(60000);
					}
					statements.next();
				}
			}

		}

	}

	@Test
	public void testAddSameStatement() {

		SailRepository elasticsearchStore = new SailRepository(this.elasticsearchStore);

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {

			connection.begin(IsolationLevels.NONE);
			connection.add(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();

			connection.begin(IsolationLevels.NONE);
			for (int i = 0; i < 2000; i++) {
				connection.add(RDF.TYPE, RDFS.LABEL, vf.createLiteral(i + ""));
			}
			connection.commit();

			List<Statement> typeStatements = Iterations.asList(connection.getStatements(null, RDF.TYPE, null));
			assertEquals(1, typeStatements.size());

			connection.begin(IsolationLevels.NONE);
			connection.add(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();

			typeStatements = Iterations.asList(connection.getStatements(null, RDF.TYPE, null));
			assertEquals(1, typeStatements.size());

		}
	}

	@Test
	public void testAddSameStatement2() {

		SailRepository elasticsearchStore = new SailRepository(this.elasticsearchStore);

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {

			connection.begin(IsolationLevels.NONE);
			for (int i = 0; i < 2000; i++) {
				connection.add(RDF.TYPE, RDFS.LABEL, vf.createLiteral(i + ""));
			}
			connection.commit();

			connection.begin(IsolationLevels.NONE);
			connection.add(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);

			List<Statement> typeStatements = Iterations.asList(connection.getStatements(null, RDF.TYPE, null));
			assertEquals(1, typeStatements.size());

			connection.commit();

			typeStatements = Iterations.asList(connection.getStatements(null, RDF.TYPE, null));
			assertEquals(1, typeStatements.size());

		}
	}

	@Test
	public void testNamespace() {

		SailRepository elasticsearchStore = new SailRepository(this.elasticsearchStore);

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			connection.begin();
			connection.setNamespace(SHACL.PREFIX, SHACL.NAMESPACE);
			connection.commit();
		}

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			String namespace = connection.getNamespace(SHACL.PREFIX);
			assertEquals(SHACL.NAMESPACE, namespace);
		}
	}

	@Test
	public void testClear() {
		SailRepository elasticsearchStore = new SailRepository(this.elasticsearchStore);
		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {

			BNode context = vf.createBNode();

			connection.begin(IsolationLevels.READ_COMMITTED);
			connection.add(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDF.TYPE, RDF.TYPE, RDF.PROPERTY, context);
			connection.commit();

			connection.begin(IsolationLevels.NONE);
			connection.clear();
			connection.commit();

		}

	}

	private Set<Statement> asSet(Statement... statements) {
		Set<Statement> set = new TreeSet<>(Comparator.comparing(Object::toString));
		set.addAll(Arrays.asList(statements));
		return set;
	}

}
