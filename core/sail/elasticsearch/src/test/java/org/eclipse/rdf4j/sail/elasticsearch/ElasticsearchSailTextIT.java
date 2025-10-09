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
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.lucene.SearchFields;
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
class ElasticsearchSailTextIT {

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
	private static final IRI EX_TITLE = VF.createIRI("http://example.org/title");
	private static final IRI EX_COMMENT = VF.createIRI("http://example.org/comment");

	@BeforeAll
	static void up() {
		assertTrue(elastic.isRunning());
	}

	@AfterAll
	static void down() {
		// handled by @Container
	}

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
	void highlightAcrossAllProperties() {
		SailRepository repo = newRepository();
		ValueFactory vf = repo.getValueFactory();
		IRI s1 = vf.createIRI("http://example.org/s1");
		IRI s2 = vf.createIRI("http://example.org/s2");
		IRI s3 = vf.createIRI("http://example.org/s3");

		try (SailRepositoryConnection cx = repo.getConnection()) {
			cx.begin();
			cx.add(s1, EX_TITLE, vf.createLiteral("The quick brown fox jumps"));
			cx.add(s1, EX_COMMENT, vf.createLiteral("Over the lazy dog"));
			cx.add(s2, EX_TITLE, vf.createLiteral("Some other text"));
			cx.add(s3, EX_COMMENT, vf.createLiteral("quick again appears here"));
			cx.commit();
		}

		String q = String.join("\n",
				"PREFIX search: <http://www.openrdf.org/contrib/lucenesail#>",
				"SELECT ?s ?snip WHERE {",
				"  ?s search:matches [",
				"    search:query \"quick\" ;",
				"    search:snippet ?snip",
				"  ] .",
				"}");

		List<String> snippets = new ArrayList<>();
		List<String> results = new ArrayList<>();
		try (SailRepositoryConnection cx = repo.getConnection()) {
			var tq = cx.prepareTupleQuery(q);
			try (var res = tq.evaluate()) {
				while (res.hasNext()) {
					var bs = res.next();
					results.add(bs.getValue("s").stringValue());
					snippets.add(bs.getValue("snip").stringValue());
				}
			}
		}

		assertTrue(results.contains(s1.stringValue()));
		assertTrue(results.contains(s3.stringValue()));
		assertFalse(results.contains(s2.stringValue()));
		assertTrue(snippets.stream().anyMatch(s -> s.contains(SearchFields.HIGHLIGHTER_PRE_TAG)));

		repo.shutDown();
	}

	@Test
	void limitNumDocs() {
		SailRepository repo = newRepository();
		ValueFactory vf = repo.getValueFactory();
		for (int i = 0; i < 3; i++) {
			IRI s = vf.createIRI("http://example.org/r" + i);
			try (SailRepositoryConnection cx = repo.getConnection()) {
				cx.begin();
				cx.add(s, EX_TITLE, vf.createLiteral("fox fox fox"));
				cx.commit();
			}
		}

		String q = String.join("\n",
				"PREFIX search: <http://www.openrdf.org/contrib/lucenesail#>",
				"SELECT ?s WHERE {",
				"  ?s search:matches [",
				"    search:query \"fox\" ;",
				"    search:numDocs \"1\"",
				"  ] .",
				"}");

		int count;
		try (SailRepositoryConnection cx = repo.getConnection()) {
			var tq = cx.prepareTupleQuery(q);
			try (var res = tq.evaluate()) {
				count = 0;
				while (res.hasNext()) {
					res.next();
					count++;
				}
			}
		}

		assertEquals(1, count, "Expected query to limit results to 1");
		repo.shutDown();
	}

	@Test
	void propertyVarBindingAndSnippet() {
		SailRepository repo = newRepository();
		ValueFactory vf = repo.getValueFactory();
		IRI s1 = vf.createIRI("http://example.org/s1");
		IRI s2 = vf.createIRI("http://example.org/s2");

		try (SailRepositoryConnection cx = repo.getConnection()) {
			cx.begin();
			cx.add(s1, EX_TITLE, vf.createLiteral("quick fox"));
			cx.add(s2, EX_COMMENT, vf.createLiteral("quick dog"));
			cx.commit();
		}

		String q = String.join("\n",
				"PREFIX search: <http://www.openrdf.org/contrib/lucenesail#>",
				"PREFIX ex: <http://example.org/>",
				"SELECT ?s ?p ?snip WHERE {",
				"  ?s search:matches [",
				"    search:query \"quick\" ;",
				"    search:property ?p ;",
				"    search:snippet ?snip",
				"  ] .",
				"}");

		List<String> props = new ArrayList<>();
		List<String> subjects = new ArrayList<>();
		try (SailRepositoryConnection cx = repo.getConnection()) {
			var tq = cx.prepareTupleQuery(q);
			try (var res = tq.evaluate()) {
				while (res.hasNext()) {
					var bs = res.next();
					subjects.add(bs.getValue("s").stringValue());
					props.add(bs.getValue("p").stringValue());
				}
			}
		}

		assertTrue(subjects.contains(s1.stringValue()));
		assertTrue(subjects.contains(s2.stringValue()));
		assertTrue(props.contains(EX_TITLE.stringValue()));
		assertTrue(props.contains(EX_COMMENT.stringValue()));

		repo.shutDown();
	}

	@Test
	void propertyFilterReturnsOnlyMatchingProperty() {
		SailRepository repo = newRepository();
		ValueFactory vf = repo.getValueFactory();
		IRI s1 = vf.createIRI("http://example.org/s1");
		IRI s2 = vf.createIRI("http://example.org/s2");

		try (SailRepositoryConnection cx = repo.getConnection()) {
			cx.begin();
			cx.add(s1, EX_TITLE, vf.createLiteral("contains quick"));
			cx.add(s2, EX_COMMENT, vf.createLiteral("contains quick"));
			cx.commit();
		}

		String q = String.join("\n",
				"PREFIX search: <http://www.openrdf.org/contrib/lucenesail#>",
				"PREFIX ex: <http://example.org/>",
				"SELECT ?s WHERE {",
				"  ?s search:matches [",
				"    search:property ex:title ;",
				"    search:query \"quick\"",
				"  ] .",
				"}");

		List<String> results = new ArrayList<>();
		try (SailRepositoryConnection cx = repo.getConnection()) {
			var tq = cx.prepareTupleQuery(q);
			try (var res = tq.evaluate()) {
				while (res.hasNext()) {
					results.add(res.next().getValue("s").stringValue());
				}
			}
		}

		assertTrue(results.contains(s1.stringValue()));
		assertFalse(results.contains(s2.stringValue()));

		repo.shutDown();
	}

	@Test
	void multiParamQueryUnsupported() {
		SailRepository repo = newRepository();

		// Two search:query statements inside same matches node -> ElasticsearchIndex should reject
		String q = String.join("\n",
				"PREFIX search: <http://www.openrdf.org/contrib/lucenesail#>",
				"SELECT ?s WHERE {",
				"  ?s search:matches [",
				"    search:query \"a\" ;",
				"    search:query \"b\"",
				"  ] .",
				"}");

		try (SailRepositoryConnection cx = repo.getConnection()) {
			var tq = cx.prepareTupleQuery(q);
			assertThrows(Exception.class, () -> {
				try (var res = tq.evaluate()) {
					while (res.hasNext()) {
						res.next();
					}
				}
			});
		}

		repo.shutDown();
	}

	@Test
	void textQueryRestrictedByGraphContext() {
		SailRepository repo = newRepository();
		ValueFactory vf = repo.getValueFactory();
		IRI s1 = vf.createIRI("http://example.org/s1");
		IRI s2 = vf.createIRI("http://example.org/s2");
		IRI ctx1 = vf.createIRI("http://example.org/ctx1");
		IRI ctx2 = vf.createIRI("http://example.org/ctx2");

		try (SailRepositoryConnection cx = repo.getConnection()) {
			cx.begin();
			cx.add(s1, EX_TITLE, vf.createLiteral("quick term"), ctx1);
			cx.add(s2, EX_TITLE, vf.createLiteral("quick term"), ctx2);
			cx.commit();
		}

		String q = String.join("\n",
				"PREFIX search: <http://www.openrdf.org/contrib/lucenesail#>",
				"PREFIX ex: <http://example.org/>",
				"SELECT ?s WHERE {",
				"  GRAPH <" + ctx1 + "> { ?s ex:title ?o . }",
				"  ?s search:matches [",
				"    search:property ex:title ;",
				"    search:query \"quick\"",
				"  ] .",
				"}");

		List<String> results = new ArrayList<>();
		try (SailRepositoryConnection cx = repo.getConnection()) {
			var tq = cx.prepareTupleQuery(q);
			try (var res = tq.evaluate()) {
				while (res.hasNext()) {
					results.add(res.next().getValue("s").stringValue());
				}
			}
		}

		assertTrue(results.contains(s1.stringValue()));
		assertFalse(results.contains(s2.stringValue()));
		repo.shutDown();
	}
}
