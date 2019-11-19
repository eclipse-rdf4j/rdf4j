/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.apache.commons.io.FileUtils;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;
import pl.allegro.tech.embeddedelasticsearch.JavaHomeOption;
import pl.allegro.tech.embeddedelasticsearch.PopularProperties;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TestHelpers {

	public static final String VERSION = "6.5.4";
	public static final String CLUSTER = "cluster1";
	public static final String ELASTICSEARCH_DOWNLOAD_DIRECTORY = "tempElasticsearchDownload";

	public static EmbeddedElastic startElasticsearch(File installLocation) throws IOException, InterruptedException {

		EmbeddedElastic embeddedElastic = EmbeddedElastic.builder()
				.withElasticVersion(VERSION)
				.withSetting(PopularProperties.TRANSPORT_TCP_PORT, 9350)
				.withSetting(PopularProperties.CLUSTER_NAME, "cluster1")
				.withInstallationDirectory(installLocation)
				.withDownloadDirectory(new File("tempElasticsearchDownload"))
//			.withPlugin("analysis-stempel")
				.withStartTimeout(5, TimeUnit.MINUTES)
				.build();

		embeddedElastic.start();
		return embeddedElastic;
	}

	public static void stopElasticsearch(EmbeddedElastic embeddedElastic, File installLocation) {
		embeddedElastic.stop();

		try {
			FileUtils.deleteDirectory(installLocation);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static EmbeddedElastic startElasticsearch(File installLocation, String javaHomePath)
			throws IOException, InterruptedException {

		EmbeddedElastic embeddedElastic = EmbeddedElastic.builder()
				.withElasticVersion(VERSION)
				.withSetting(PopularProperties.TRANSPORT_TCP_PORT, 9350)
				.withSetting(PopularProperties.CLUSTER_NAME, CLUSTER)
				.withInstallationDirectory(installLocation)
				.withJavaHome(JavaHomeOption.path(javaHomePath))
				.withDownloadDirectory(new File(ELASTICSEARCH_DOWNLOAD_DIRECTORY))
//			.withPlugin("analysis-stempel")
				.withStartTimeout(5, TimeUnit.MINUTES)
				.build();

		embeddedElastic.start();
		return embeddedElastic;
	}
}
