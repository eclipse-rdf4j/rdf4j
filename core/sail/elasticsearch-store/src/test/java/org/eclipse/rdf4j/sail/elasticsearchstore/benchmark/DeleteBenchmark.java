/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.elasticsearchstore.benchmark;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.elasticsearchstore.ElasticsearchStore;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;
import pl.allegro.tech.embeddedelasticsearch.JavaHomeOption;
import pl.allegro.tech.embeddedelasticsearch.PopularProperties;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author Håvard Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms8G", "-Xmx8G", "-Xmn4G", "-XX:+UseSerialGC" })
//@Fork(value = 1, jvmArgs = {"-Xms8G", "-Xmx8G", "-Xmn4G", "-XX:+UseSerialGC", "-XX:+UnlockCommercialFeatures", "-XX:StartFlightRecording=delay=60s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class DeleteBenchmark {

	@Param({ "1", "10", "100", "1000" })
	public int NUMBER_OF_STATEMENTS = 10;

	private static EmbeddedElastic embeddedElastic;

	private static File installLocation = Files.newTemporaryFolder();

	SailRepository elasticsearchStore;

	@Setup(Level.Trial)
	public void beforeClass() throws IOException, InterruptedException {

		String version = "6.5.4";

		File tempElasticsearchDownload = new File("tempElasticsearchDownload");

		System.out.println("Download directory: " + tempElasticsearchDownload.getAbsolutePath());

		embeddedElastic = EmbeddedElastic.builder()
				.withElasticVersion(version)
				.withSetting(PopularProperties.TRANSPORT_TCP_PORT, 9350)
				.withSetting(PopularProperties.CLUSTER_NAME, "cluster1")
				.withInstallationDirectory(installLocation)
				.withDownloadDirectory(tempElasticsearchDownload)
				.withJavaHome(JavaHomeOption.path("/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home"))
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

		elasticsearchStore = new SailRepository(new ElasticsearchStore("localhost", 9350, "testindex"));

		System.gc();

	}

	@TearDown(Level.Trial)
	public void afterClass() throws IOException {

		elasticsearchStore.shutDown();
		embeddedElastic.stop();

		FileUtils.deleteDirectory(installLocation);
	}

	@TearDown(Level.Invocation)
	public void afterInvocation() throws IOException {

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			connection.begin();
			connection.remove((Resource) null, null, null);
			connection.commit();
		}
	}

	@Benchmark
	public long addAndClear() {

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			connection.begin();
			for (int i = 0; i < NUMBER_OF_STATEMENTS; i++) {
				connection.add(RDF.TYPE, RDFS.LABEL, SimpleValueFactory.getInstance().createLiteral(i));
			}
			connection.commit();
		}
		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			connection.begin();
			connection.clear();
			connection.commit();
		}

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			return connection.size();
		}
	}

	@Benchmark
	public long addAndDelete() {

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			connection.begin();
			for (int i = 0; i < NUMBER_OF_STATEMENTS; i++) {
				connection.add(RDF.TYPE, RDFS.LABEL, SimpleValueFactory.getInstance().createLiteral(i));
			}
			connection.commit();
		}
		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			connection.begin();
			connection.remove(RDF.TYPE, RDFS.LABEL, null);
			connection.commit();
		}

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			return connection.size();
		}
	}

	@Benchmark
	public long addAndSize() {

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			connection.begin();
			for (int i = 0; i < NUMBER_OF_STATEMENTS; i++) {
				connection.add(RDF.TYPE, RDFS.LABEL, SimpleValueFactory.getInstance().createLiteral(i));
			}
			connection.commit();
		}

		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
			return connection.size();
		}
	}

}
