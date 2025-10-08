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
import java.util.Arrays;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.GEO;
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
class ElasticsearchSailGeoIT {

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

	@BeforeAll
	static void up() {
		assertTrue(elastic.isRunning());
	}

	@AfterAll
	static void down() {
		/* @Container lifecycle */ }

	private static SailRepository newRepository() {
		String host = elastic.getHost();
		Integer transport = elastic.getMappedPort(9300);

		LuceneSail lucene = new LuceneSail();
		lucene.setParameter(LuceneSail.INDEX_CLASS_KEY, ElasticsearchIndex.class.getName());
		lucene.setParameter(ElasticsearchIndex.TRANSPORT_KEY, host + ":" + transport);
		lucene.setParameter(ElasticsearchIndex.WAIT_FOR_STATUS_KEY, "yellow");
		lucene.setParameter(ElasticsearchIndex.WAIT_FOR_NO_RELOCATING_SHARDS_KEY, "true");
		lucene.setParameter(ElasticsearchIndex.ELASTICSEARCH_KEY_PREFIX + "client.transport.ignore_cluster_name",
				"true");
		lucene.setParameter(ElasticsearchIndex.ELASTICSEARCH_KEY_PREFIX + "client.transport.sniff", "false");
		lucene.setBaseSail(new MemoryStore());

		SailRepository repo = new SailRepository(lucene);
		repo.init();
		return repo;
	}

	@Test
	void withinDistanceMetre_samePointMatches() {
		SailRepository repo = newRepository();
		ValueFactory vf = repo.getValueFactory();
		IRI s1 = vf.createIRI("http://example.org/pt1");
		IRI s2 = vf.createIRI("http://example.org/pt2");

		try (SailRepositoryConnection cx = repo.getConnection()) {
			cx.begin();
			cx.add(s1, GEO.AS_WKT, vf.createLiteral("POINT (1 2)", GEO.WKT_LITERAL));
			cx.add(s2, GEO.AS_WKT, vf.createLiteral("POINT (10 10)", GEO.WKT_LITERAL));
			cx.commit();
		}

		String q = String.join("\n",
				"PREFIX geo: <http://www.opengis.net/ont/geosparql#>",
				"PREFIX geof: <http://www.opengis.net/def/function/geosparql/>",
				"PREFIX uom: <http://www.opengis.net/def/uom/OGC/1.0/>",
				"SELECT ?s ?w WHERE {",
				"  ?s geo:asWKT ?w .",
				"  FILTER(geof:distance(\"POINT (1 2)\"^^geo:wktLiteral, ?w, uom:metre) < 1000)",
				"}");

		List<String> results = new ArrayList<>();
		try (SailRepositoryConnection cx = repo.getConnection()) {
			var tq = cx.prepareTupleQuery(q);
			try (var res = tq.evaluate()) {
				while (res.hasNext()) {
					var bs = res.next();
					results.add(bs.getValue("s").stringValue());
				}
			}
		}
		System.out.println(Arrays.toString(results.toArray()));

		assertTrue(results.contains(s1.stringValue()));
		assertFalse(results.contains(s2.stringValue()));

		repo.shutDown();
	}
}
