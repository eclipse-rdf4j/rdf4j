package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.assertj.core.util.Files;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;
import pl.allegro.tech.embeddedelasticsearch.PopularProperties;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class ElasticsearchStoreTransactionsTest {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchStoreTransactionsTest.class);
	private static final SimpleValueFactory vf = SimpleValueFactory.getInstance();

	private static EmbeddedElastic embeddedElastic;

	private static File installLocation = Files.newTemporaryFolder();

	private static ElasticsearchStore elasticsearchStore;

	@BeforeClass
	public static void beforeClass() throws IOException, InterruptedException {

		String version = "6.5.4";

		embeddedElastic = EmbeddedElastic.builder()
				.withElasticVersion(version)
				.withSetting(PopularProperties.TRANSPORT_TCP_PORT, 9350)
				.withSetting(PopularProperties.CLUSTER_NAME, "cluster1")
				.withInstallationDirectory(installLocation)
				.withDownloadDirectory(new File("tempElasticsearchDownload"))
//			.withPlugin("analysis-stempel")
//			.withIndex("cars", IndexSettings.builder()
//				.withType("car", getSystemResourceAsStream("car-mapping.json"))
//				.build())
//			.withIndex("books", IndexSettings.builder()
//				.withType(PAPER_BOOK_INDEX_TYPE, getSystemResourceAsStream("paper-book-mapping.json"))
//				.withType("audio_book", getSystemResourceAsStream("audio-book-mapping.json"))
//				.withSettings(getSystemResourceAsStream("elastic-settings.json"))
//				.build())
				.withStartTimeout(5, TimeUnit.MINUTES)
				.build();

		embeddedElastic.start();

		elasticsearchStore = new ElasticsearchStore("localhost", 9350, "testindex");

	}

	@AfterClass
	public static void afterClass() throws IOException {

		elasticsearchStore.shutDown();
		embeddedElastic.stop();

		FileUtils.deleteDirectory(installLocation);
	}

	@Before
	public void before() throws UnknownHostException {

		try (NotifyingSailConnection connection = elasticsearchStore.getConnection()) {
			connection.begin();
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
			System.out.println();
			System.out.println("INDEX: " + index);
			try {
				List<String> strings = embeddedElastic.fetchAllDocuments(index);

				for (String string : strings) {
					System.out.println(string);
					System.out.println();
				}

			} catch (UnknownHostException e) {
				throw new RuntimeException(e);
			}

			System.out.println();
		}
	}

	private void deleteAllIndexes() {
		for (String index : getIndexes()) {
			System.out.println("deleting: " + index);
			embeddedElastic.deleteIndex(index);

		}
	}

	private String[] getIndexes() {

		Settings settings = Settings.builder().put("cluster.name", "cluster1").build();
		try (TransportClient client = new PreBuiltTransportClient(settings)) {
			client.addTransportAddress(new TransportAddress(InetAddress.getByName("localhost"), 9350));

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
			connection.begin();
			connection.addStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();
		}

	}

	@Test
	public void testGetData() {
		try (NotifyingSailConnection connection = elasticsearchStore.getConnection()) {
			connection.begin();
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

			Literal label = vf.createLiteral("rdf:type label");
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

	private HashSet<Statement> asSet(Statement... statements) {
		return new HashSet<>(Arrays.asList(statements));
	}

}
