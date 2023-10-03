/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.http.server;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.exception.ValidationException;
import org.eclipse.rdf4j.http.client.shacl.RemoteShaclValidationException;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ShaclValidationReportIT {

	private static TestServer server;

	private static final ValueFactory vf = SimpleValueFactory.getInstance();

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

	String shacl = "@base <http://example.com/ns> .\n" +
			"@prefix ex: <http://example.com/ns#> .\n" +
			"@prefix owl: <http://www.w3.org/2002/07/owl#> .\n" +
			"@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
			"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
			"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
			"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
			"\n" +
			"ex:PersonShape\n" +
			"\ta sh:NodeShape  ;\n" +
			"\tsh:targetClass rdfs:Resource ;\n" +
			"\tsh:property _:bnode  .\n" +
			"\n" +
			"\n" +
			"_:bnode\n" +
			"        sh:path rdfs:label ;\n" +
			"        rdfs:label \"abc\" ;\n" +
			"        sh:minCount 1 .";

	@Test
	public void testSparqlUpdate() throws IOException {

		Repository systemRepo = new HTTPRepository(
				Protocol.getRepositoryLocation(TestServer.SERVER_URL, TestServer.TEST_SHACL_REPO_ID));
		try (RepositoryConnection connection = systemRepo.getConnection()) {
			connection.begin();
			connection.add(new StringReader(shacl), "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();

			try {
				connection.begin();
				connection.prepareUpdate("INSERT DATA {[] a rdfs:Resource}").execute();
				connection.commit();
				fail();
			} catch (Throwable e) {
				assertExceptionIsShaclReport(e);
			}
		}

	}

	private void assertExceptionIsShaclReport(Throwable e) {
		ValidationException remoteShaclSailValidationException = null;
		if (e instanceof ValidationException) {
			remoteShaclSailValidationException = (ValidationException) e;
		} else if (e.getCause() instanceof ValidationException) {
			remoteShaclSailValidationException = (ValidationException) e.getCause();
		}

		assert remoteShaclSailValidationException != null;
		assertTrue(remoteShaclSailValidationException.validationReportAsModel().contains(null, SHACL.CONFORMS, null));
	}

	@Test
	public void testAddingData() throws IOException {

		Repository systemRepo = new HTTPRepository(
				Protocol.getRepositoryLocation(TestServer.SERVER_URL, TestServer.TEST_SHACL_REPO_ID));
		try (RepositoryConnection connection = systemRepo.getConnection()) {
			connection.begin();
			connection.add(new StringReader(shacl), "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();

			try {
				connection.begin();
				connection.add(vf.createBNode(), RDF.TYPE, RDFS.RESOURCE);
				connection.commit();
				fail();
			} catch (Exception e) {
				assertExceptionIsShaclReport(e);
			}
		}

	}

	@Test
	public void testBlankNodeIdsPreserved() throws IOException {

		Repository repository = new HTTPRepository(
				Protocol.getRepositoryLocation(TestServer.SERVER_URL, TestServer.TEST_SHACL_REPO_ID));

		try (RepositoryConnection connection = repository.getConnection()) {
			connection.begin();
			connection.add(new StringReader(shacl), "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();
		}

		try (RepositoryConnection connection = repository.getConnection()) {
			connection.begin();
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();
		} catch (RepositoryException repositoryException) {

			Model validationReport = ((RemoteShaclValidationException) repositoryException.getCause())
					.validationReportAsModel();

			BNode shapeBnode = (BNode) validationReport
					.filter(null, SHACL.SOURCE_SHAPE, null)
					.objects()
					.stream()
					.findAny()
					.orElseThrow();

			try (RepositoryConnection connection = repository.getConnection()) {
				List<Statement> collect = connection
						.getStatements(shapeBnode, null, null, RDF4J.SHACL_SHAPE_GRAPH)
						.stream()
						.collect(Collectors.toList());

				Assertions.assertEquals(3, collect.size());

				Value rdfsLabel = collect
						.stream()
						.filter(s -> s.getPredicate().equals(RDFS.LABEL))
						.map(Statement::getObject)
						.findAny()
						.orElseThrow();

				Assertions.assertEquals(Values.literal("abc"), rdfsLabel);

			}
		}

	}

}
