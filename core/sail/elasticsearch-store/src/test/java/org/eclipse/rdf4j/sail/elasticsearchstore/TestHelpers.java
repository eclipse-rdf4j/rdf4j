/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearchstore;

import java.io.File;
import java.io.IOException;

import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.elasticsearch.common.settings.Settings.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestHelpers {
	public static final String CLUSTER = "cluster1";

	private static final Logger logger = LoggerFactory.getLogger(TestHelpers.class);

	public static ElasticsearchClusterRunner startElasticsearch(File installLocation)
			throws IOException, InterruptedException {

		ElasticsearchClusterRunner runner = new ElasticsearchClusterRunner();
		runner.onBuild(new ElasticsearchClusterRunner.Builder() {
			@Override
			public void build(int number, Builder settingsBuilder) {
				// settingsBuilder.put("indices.breaker.total.limit", "1100m");
			}
		});
		runner.build(ElasticsearchClusterRunner.newConfigs()
				.numOfNode(1)
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
