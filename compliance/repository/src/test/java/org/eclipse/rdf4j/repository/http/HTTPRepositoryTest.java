/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.repository.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.model.util.Values.iri;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.testsuite.repository.RepositoryTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import com.github.mjeanroy.junit.servers.annotations.TestServerConfiguration;
import com.github.mjeanroy.junit.servers.jetty.EmbeddedJettyConfiguration;
import com.github.mjeanroy.junit.servers.jupiter.JunitServerExtension;
import com.github.mjeanroy.junit.servers.servers.EmbeddedServer;

@ExtendWith(JunitServerExtension.class)
public class HTTPRepositoryTest extends RepositoryTest {
	@TempDir
	static File dataDir;
	private static EmbeddedServer<?> server;

	@TestServerConfiguration
	static EmbeddedJettyConfiguration serverConfiguration() {
		return EmbeddedJettyConfiguration.builder()
				.withWebapp(HTTPMemServer.getWebappDir())
				.withProperty("org.eclipse.rdf4j.appdata.basedir", dataDir.getAbsolutePath())
				.build();
	}

	@BeforeAll
	static void beforeAll(EmbeddedServer<?> server) {
		HTTPRepositoryTest.server = server;
		RemoteRepositoryManager manager = RemoteRepositoryManager.getInstance(server.getUrl());
		HTTPMemServer.createTestRepositories(manager);
	}

	@Override
	protected Repository createRepository() {
		return new HTTPRepository(Protocol.getRepositoryLocation(server.getUrl(), HTTPMemServer.TEST_REPO_ID));
	}

	@Test
	@Timeout(value = 10)
	public void testSubqueryDeadlock() throws Exception {
		String mainQueryStr = "SELECT ?property WHERE { ?property a rdf:Property . }";
		String subQueryStr = "SELECT ?range WHERE { ?property rdfs:range ?range . }";

		try (RepositoryConnection conn = this.testRepository.getConnection()) {

			conn.begin();
			// we need sufficient data for the main query to not complete immediately - it should still be
			// background-parsing when the subquery is executed to trigger potential deadlock
			for (int i = 0; i < 1_000; i++) {
				IRI subject = iri("foo:bar-" + i);
				conn.add(subject, RDF.TYPE, RDF.PROPERTY);
				conn.add(subject, RDFS.RANGE, FOAF.PERSON);
			}
			conn.commit();

			final TupleQuery main = conn.prepareTupleQuery(mainQueryStr);
			final TupleQueryResult mainResult = main.evaluate();
			while (mainResult.hasNext()) {
				final BindingSet current = mainResult.next();
				final IRI u = (IRI) current.getValue("property");

				final TupleQuery subQuery = conn.prepareTupleQuery(subQueryStr);
				subQuery.setBinding("property", u);
				try (final TupleQueryResult sqResult = subQuery.evaluate()) {
					final Set<IRI> rangesSparql = sqResult.stream()
							.map(bs -> bs.getValue("range"))
							.filter(Value::isIRI)
							.map(v -> (IRI) v)
							.collect(Collectors.toSet());
					assertThat(rangesSparql).hasSize(1);
				}
			}
		}
	}
}
