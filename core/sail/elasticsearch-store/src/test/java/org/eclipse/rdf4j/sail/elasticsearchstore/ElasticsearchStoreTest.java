package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.eclipse.rdf4j.common.iteration.Iterations;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class ElasticsearchStoreTest {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchStoreTest.class);

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

		embeddedElastic.deleteIndices();

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
			connection.addStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
		}

	}

	@Test
	public void testGetData() {
		ElasticsearchStore elasticsearchStore = new ElasticsearchStore("localhost", 9350, "testindex");
		try (NotifyingSailConnection connection = elasticsearchStore.getConnection()) {
			connection.addStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.flush();
			List<? extends Statement> statements = Iterations.asList(connection.getStatements(null, null, null, true));

			System.out.println(Arrays.toString(statements.toArray()));

			assertEquals(1, statements.size());
			assertEquals(SimpleValueFactory.getInstance().createStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE),
					statements.get(0));
		}

	}

}
