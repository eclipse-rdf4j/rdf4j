package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.assertj.core.util.Files;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class ElasticsearchStoreTest {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchStoreTest.class);
	private static final SimpleValueFactory vf = SimpleValueFactory.getInstance();

	private static EmbeddedElastic embeddedElastic;

	private static File installLocation = Files.newTemporaryFolder();

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
	}

	@AfterClass
	public static void afterClass() throws IOException {

		embeddedElastic.stop();

		FileUtils.deleteDirectory(installLocation);
	}

	@After
	public void after() throws UnknownHostException {

		printAllDocs();
		embeddedElastic.refreshIndices();

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
	public void testInstantiate() {
		ElasticsearchStore elasticsearchStore = new ElasticsearchStore("localhost", 9350, "testindex");
	}

	@Test
	public void testGetConneciton() {
		ElasticsearchStore elasticsearchStore = new ElasticsearchStore("localhost", 9350, "testindex");
		try (NotifyingSailConnection connection = elasticsearchStore.getConnection()) {
		}

	}

	@Test
	public void testSailRepository() {
		SailRepository elasticsearchStore = new SailRepository(new ElasticsearchStore("localhost", 9350, "testindex"));
	}

	@Test
	public void testGetSailRepositoryConneciton() {
		SailRepository elasticsearchStore = new SailRepository(new ElasticsearchStore("localhost", 9350, "testindex"));
		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
		}
	}

	@Test
	public void testAddData() {
		ElasticsearchStore elasticsearchStore = new ElasticsearchStore("localhost", 9350, "testindex");
		try (NotifyingSailConnection connection = elasticsearchStore.getConnection()) {
			connection.begin();
			connection.addStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();
		}

	}

	@Test
	public void testGetData() {
		ElasticsearchStore elasticsearchStore = new ElasticsearchStore("localhost", 9350, "testindex");
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
		SailRepository elasticsearchStore = new SailRepository(new ElasticsearchStore("localhost", 9350, "testindex"));
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
		SailRepository elasticsearchStore = new SailRepository(new ElasticsearchStore("localhost", 9350, "testindex"));
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
		SailRepository elasticsearchStore = new SailRepository(new ElasticsearchStore("localhost", 9350, "testindex"));
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
		SailRepository elasticsearchStore = new SailRepository(new ElasticsearchStore("localhost", 9350, "testindex"));
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
		SailRepository elasticsearchStore = new SailRepository(new ElasticsearchStore("localhost", 9350, "testindex"));
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
		SailRepository elasticsearchStore = new SailRepository(new ElasticsearchStore("localhost", 9350, "testindex"));
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
		SailRepository elasticsearchStore = new SailRepository(new ElasticsearchStore("localhost", 9350, "testindex"));
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
		SailRepository elasticsearchStore = new SailRepository(new ElasticsearchStore("localhost", 9350, "testindex"));
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
		SailRepository elasticsearchStore = new SailRepository(new ElasticsearchStore("localhost", 9350, "testindex"));
		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {

			IRI iri = vf.createIRI("http://example.com/ space /test");
			connection.add(iri, RDF.TYPE, RDFS.RESOURCE);

			List<? extends Statement> statements = Iterations
					.asList(connection.getStatements(iri, RDF.TYPE, RDFS.RESOURCE, true));

			System.out.println(Arrays.toString(statements.toArray()));

			assertEquals(1, statements.size());

			assertEquals(iri, statements.get(0).getSubject());

		}

	}

	@Test
	public void testShutdownAndRecreate() {
		ElasticsearchStore elasticsearchStore = new ElasticsearchStore("localhost", 9350, "testindex");
		try (NotifyingSailConnection connection = elasticsearchStore.getConnection()) {
			connection.begin();
			connection.addStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();
		}
		elasticsearchStore.shutDown();
		elasticsearchStore = new ElasticsearchStore("localhost", 9350, "testindex");
		try (NotifyingSailConnection connection = elasticsearchStore.getConnection()) {
			connection.begin();
			connection.addStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();
		}
		elasticsearchStore.shutDown();

	}

	@Test
	public void testShutdownAndReinit() {
		ElasticsearchStore elasticsearchStore = new ElasticsearchStore("localhost", 9350, "testindex");
		try (NotifyingSailConnection connection = elasticsearchStore.getConnection()) {
			connection.begin();
			connection.addStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();
		}
		elasticsearchStore.shutDown();

		elasticsearchStore.init();

		try (NotifyingSailConnection connection = elasticsearchStore.getConnection()) {
			connection.begin();
			connection.addStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();
		}
		elasticsearchStore.shutDown();

	}

	@Test
	public void testAddLargeDataset() {
		SailRepository elasticsearchStore = new SailRepository(new ElasticsearchStore("localhost", 9350, "testindex"));
		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			StopWatch stopWatch = StopWatch.createStarted();
			connection.begin();
			int count = 100000;
			for (int i = 0; i < count; i++) {
				connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral(i));
			}
			connection.commit();
			stopWatch.stop();
			logger.info("Adding data took: " + stopWatch.getTime(TimeUnit.MILLISECONDS) + "s");

			stopWatch = StopWatch.createStarted();
			assertEquals(count, connection.size());
			stopWatch.stop();
			logger.info("Getting size took: " + stopWatch.getTime(TimeUnit.MILLISECONDS) + "s");

		}

	}

}
