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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringReader;

import org.eclipse.rdf4j.common.exception.ValidationException;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ShaclValidationReportIT {

	private static TestServer server;

	private static final ValueFactory vf = SimpleValueFactory.getInstance();

	@BeforeClass
	public static void startServer() throws Exception {
		server = new TestServer();
		try {
			server.start();
		} catch (Exception e) {
			server.stop();
			throw e;
		}
	}

	@AfterClass
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
			"\tsh:property ex:PersonShapeProperty  .\n" +
			"\n" +
			"\n" +
			"ex:PersonShapeProperty\n" +
			"        sh:path rdfs:label ;\n" +
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

}
