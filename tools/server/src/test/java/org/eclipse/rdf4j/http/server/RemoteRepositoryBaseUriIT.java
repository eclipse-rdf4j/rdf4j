/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.http.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class RemoteRepositoryBaseUriIT {

	private static TestServer server;

	@BeforeAll
	public static void startServer() throws Exception {
		server = new TestServer();
		try {
			server.start();
		} catch (Exception e) {
			server.stop();
			throw e;
		}
	}

	@AfterAll
	public static void stopServer() throws Exception {
		server.stop();
	}

	@Test
	public void testAddDataUsesPlainBaseURI() throws Exception {
		Repository repository = new HTTPRepository(
				Protocol.getRepositoryLocation(TestServer.SERVER_URL, TestServer.TEST_REPO_ID));

		try (RepositoryConnection connection = repository.getConnection()) {
			connection.begin();
			connection.clear();
			connection.commit();
		}

		String baseIRI = "http://example.com/base/";
		String rdfXml = String.join("\n",
				"<?xml version=\"1.0\"?>",
				"<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">",
				"    <rdf:Description rdf:about=\"relative\">",
				"        <rdf:type rdf:resource=\"http://example.com/Type\" />",
				"    </rdf:Description>",
				"</rdf:RDF>");

		try (RepositoryConnection connection = repository.getConnection()) {
			connection.begin();
			connection.add(new ByteArrayInputStream(rdfXml.getBytes(java.nio.charset.StandardCharsets.UTF_8)), baseIRI,
					RDFFormat.RDFXML);
			connection.commit();
		}

		try (RepositoryConnection connection = repository.getConnection()) {
			List<Statement> statements = connection.getStatements(null, null, null, false)
					.stream()
					.collect(Collectors.toList());

			assertThat(statements).hasSize(1);
			Statement statement = statements.get(0);
			assertThat(statement.getSubject()).isEqualTo(Values.iri(baseIRI + "relative"));
			assertThat(statement.getPredicate()).isEqualTo(RDF.TYPE);
			assertThat(statement.getObject()).isEqualTo(Values.iri("http://example.com/Type"));
		}
	}
}
