/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearchstore;

import java.io.File;
import java.io.IOException;

import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.elasticsearch.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestHelpers {
	public static final String CLUSTER = "cluster1";

	private static final Logger logger = LoggerFactory.getLogger(TestHelpers.class);

	public static ElasticsearchClusterRunner startElasticsearch(File installLocation)
			throws IOException, InterruptedException {

		ElasticsearchStore.indexSettings = Settings.builder()
				.put("index.refresh_interval", "-1")
				.put("index.translog.durability", "async")
				.put("index.translog.sync_interval", "600s")
				.build();

		ElasticsearchClusterRunner runner = new ElasticsearchClusterRunner();

		runner.onBuild((number, settingsBuilder) -> {
			settingsBuilder.put("discovery.type", "single-node")
					.put("cluster.max_shards_per_node", "1")
					.put("index.store.type", "hybridfs");
		});

		runner.build(ElasticsearchClusterRunner.newConfigs()
				.numOfNode(1)
				.indexStoreType("hybridfs")
				.basePath(installLocation.toString())
				.clusterName(CLUSTER));

		runner.ensureYellow();

		return runner;
	}

	public static int getPort(ElasticsearchClusterRunner runner) {
		return runner.node().settings().getAsInt("transport.port", 9300);
	}

	public static void stopElasticsearch(ElasticsearchClusterRunner runner) {
		try {
			runner.close();
		} catch (IOException ioe) {
			logger.error("Error closing ES cluster", ioe);
		}
		runner.clean();
	}
}
