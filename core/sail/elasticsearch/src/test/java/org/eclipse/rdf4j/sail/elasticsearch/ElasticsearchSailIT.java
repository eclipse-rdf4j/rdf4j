/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearch;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class ElasticsearchSailIT {

	private static final DockerImageName ES_IMAGE = DockerImageName
			.parse("docker.elastic.co/elasticsearch/elasticsearch:7.15.2");

	@Container
	static final GenericContainer<?> elastic = new GenericContainer<>(ES_IMAGE)
			.withEnv("xpack.security.enabled", "false")
			.withEnv("discovery.type", "single-node")
			.withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
			.withExposedPorts(9200, 9300)
			.waitingFor(Wait.forListeningPort());

	private static final ValueFactory VF = SimpleValueFactory.getInstance();
	private static final IRI EX_LABEL = VF.createIRI("http://example.org/label");

	@BeforeAll
	static void checkRunning() {
		assertTrue(elastic.isRunning(), "Elasticsearch testcontainer must be running");
	}

	@AfterAll
	static void stop() {
		// container is stopped automatically by @Container lifecycle
	}

	@Test
	void indexAndSearchByProperty() throws Exception {
		// Arrange: create a LuceneSail backed by ElasticsearchIndex
		String host = elastic.getHost();
		Integer transport = elastic.getMappedPort(9300);

		LuceneSail lucene = new LuceneSail();
		lucene.setParameter(ElasticsearchIndex.INDEX_NAME_KEY, "es-it-" + Long.toHexString(System.nanoTime()));
		lucene.setParameter(LuceneSail.INDEX_CLASS_KEY, ElasticsearchIndex.class.getName());
		// provide ES Transport address (host:port)
		lucene.setParameter(ElasticsearchIndex.TRANSPORT_KEY, host + ":" + transport);
		// be lenient about cluster name matching/sniffing in tests
		lucene.setParameter(ElasticsearchIndex.WAIT_FOR_STATUS_KEY, "yellow");
		lucene.setParameter(ElasticsearchIndex.WAIT_FOR_NO_RELOCATING_SHARDS_KEY, "true");
		lucene.setParameter(ElasticsearchIndex.ELASTICSEARCH_KEY_PREFIX + "client.transport.ignore_cluster_name",
				"true");
		lucene.setParameter(ElasticsearchIndex.ELASTICSEARCH_KEY_PREFIX + "client.transport.sniff", "false");

		MemoryStore base = new MemoryStore();
		lucene.setBaseSail(base);

		SailRepository repo = new SailRepository(lucene);
		repo.init();

		ValueFactory vf = repo.getValueFactory();
		IRI exS = vf.createIRI("http://example.org/s");

		try (SailRepositoryConnection cx = repo.getConnection()) {
			cx.begin();
			cx.add(exS, EX_LABEL, vf.createLiteral("The quick brown fox jumps"));
			cx.add(vf.createIRI("http://example.org/t2"), EX_LABEL, vf.createLiteral("A lazy dog"));
			cx.commit();
		}

		// Act: run a LuceneSail search over the property with snippet/score
		String q = String.join("\n",
				"PREFIX search: <http://www.openrdf.org/contrib/lucenesail#>",
				"PREFIX ex: <http://example.org/>",
				"SELECT ?s ?score ?snip WHERE {",
				"  ?s search:matches [",
				"    search:property ex:label ;",
				"    search:query \"quick\" ;",
				"    search:score ?score ;",
				"    search:snippet ?snip",
				"  ] .",
				"}");

		List<String> subjects = new ArrayList<>();
		try (SailRepositoryConnection cx = repo.getConnection()) {
			var tq = cx.prepareTupleQuery(q);
			try (var res = tq.evaluate()) {
				while (res.hasNext()) {
					var bs = res.next();
					subjects.add(bs.getValue("s").stringValue());
					// score should exist and be numeric
					assertNotNull(bs.getValue("score"));
					// snippet is optional but should appear for matches
					assertNotNull(bs.getValue("snip"));
				}
			}
		}

		// Assert: the subject with the 'quick' literal is returned
		assertTrue(subjects.contains(exS.stringValue()), "Expected match for subject with 'quick'");

		// Cleanup
		repo.shutDown();
	}
}
