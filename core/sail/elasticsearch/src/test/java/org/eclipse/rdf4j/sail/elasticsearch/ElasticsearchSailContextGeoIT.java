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
class ElasticsearchSailContextGeoIT {

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
		/* handled by Testcontainers */ }

	private static SailRepository newRepository() {
		String host = elastic.getHost();
		Integer transport = elastic.getMappedPort(9300);

		LuceneSail lucene = new LuceneSail();
		lucene.setParameter(ElasticsearchIndex.INDEX_NAME_KEY, "es-it-" + Long.toHexString(System.nanoTime()));
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
	void restrictByGraphContext() {
		SailRepository repo = newRepository();
		ValueFactory vf = repo.getValueFactory();

		IRI ctx1 = vf.createIRI("http://example.org/ctx1");
		IRI ctx2 = vf.createIRI("http://example.org/ctx2");
		IRI s1 = vf.createIRI("http://example.org/pt1");
		IRI s2 = vf.createIRI("http://example.org/pt2");

		try (SailRepositoryConnection cx = repo.getConnection()) {
			cx.begin();
			cx.add(s1, GEO.AS_WKT, vf.createLiteral("POINT (1 2)", GEO.WKT_LITERAL), ctx1);
			cx.add(s2, GEO.AS_WKT, vf.createLiteral("POINT (1 2)", GEO.WKT_LITERAL), ctx2);
			cx.commit();
		}

		// Query without context restriction returns both
		String qAll = String.join("\n",
				"PREFIX geo: <http://www.opengis.net/ont/geosparql#>",
				"PREFIX geof: <http://www.opengis.net/def/function/geosparql/>",
				"PREFIX uom: <http://www.opengis.net/def/uom/OGC/1.0/>",
				"SELECT ?s WHERE {",
				"  ?s geo:asWKT ?w .",
				"  FILTER(geof:distance(\"POINT (1 2)\"^^geo:wktLiteral, ?w, uom:metre) < 1)",
				"}");
		List<String> all = new ArrayList<>();
		try (SailRepositoryConnection cx = repo.getConnection()) {
			var tq = cx.prepareTupleQuery(qAll);
			try (var res = tq.evaluate()) {
				while (res.hasNext()) {
					all.add(res.next().getValue("s").stringValue());
				}
			}
		}
		assertTrue(all.contains(s1.stringValue()));
		assertTrue(all.contains(s2.stringValue()));

		// Restrict by GRAPH ctx1 - only s1 should match.
		String qCtx1 = String.join("\n",
				"PREFIX geo: <http://www.opengis.net/ont/geosparql#>",
				"PREFIX geof: <http://www.opengis.net/def/function/geosparql/>",
				"PREFIX uom: <http://www.opengis.net/def/uom/OGC/1.0/>",
				"SELECT ?s WHERE {",
				"  GRAPH <" + ctx1 + "> { ?s geo:asWKT ?w . }",
				"  FILTER(geof:distance(\"POINT (1 2)\"^^geo:wktLiteral, ?w, uom:metre) < 1)",
				"}");
		List<String> onlyCtx1 = new ArrayList<>();
		try (SailRepositoryConnection cx = repo.getConnection()) {
			var tq = cx.prepareTupleQuery(qCtx1);
			try (var res = tq.evaluate()) {
				while (res.hasNext()) {
					onlyCtx1.add(res.next().getValue("s").stringValue());
				}
			}
		}
		assertTrue(onlyCtx1.contains(s1.stringValue()));
		assertFalse(onlyCtx1.contains(s2.stringValue()));

		repo.shutDown();
	}
}
