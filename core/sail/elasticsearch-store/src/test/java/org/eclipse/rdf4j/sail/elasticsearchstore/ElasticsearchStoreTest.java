package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;
import pl.allegro.tech.embeddedelasticsearch.PopularProperties;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

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

	@Test
	public void testInstantiate() {
		ElasticsearchStore elasticsearchStore = new ElasticsearchStore();
	}

	@Test
	public void testGetConneciton() {
		ElasticsearchStore elasticsearchStore = new ElasticsearchStore();
		try (NotifyingSailConnection connection = elasticsearchStore.getConnection()) {
		}

	}

	@Test
	public void testSailRepository() {
		SailRepository elasticsearchStore = new SailRepository(new ElasticsearchStore());
	}

	@Test
	public void testGetSailRepositoryConneciton() {
		SailRepository elasticsearchStore = new SailRepository(new ElasticsearchStore());
		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
		}
	}

	@Test
	public void testAddData() {
		ElasticsearchStore elasticsearchStore = new ElasticsearchStore();
		try (NotifyingSailConnection connection = elasticsearchStore.getConnection()) {
			connection.addStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
		}

	}

}
