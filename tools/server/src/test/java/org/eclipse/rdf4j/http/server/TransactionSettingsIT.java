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

import java.io.StringReader;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.http.client.shacl.RemoteShaclValidationException;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TransactionSettingsIT {

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

	@Before
	public void before() {
		Repository repository = new HTTPRepository(
				Protocol.getRepositoryLocation(TestServer.SERVER_URL, TestServer.TEST_SHACL_REPO_ID));
		try (RepositoryConnection connection = repository.getConnection()) {
			connection.setIsolationLevel(IsolationLevels.NONE);
			connection.begin();
			connection.remove((Resource) null, null, null);
			connection.remove((Resource) null, null, null, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();
		}
	}

	@Test
	public void testValid() throws Exception {

		Repository repository = new HTTPRepository(
				Protocol.getRepositoryLocation(TestServer.SERVER_URL, TestServer.TEST_SHACL_REPO_ID));
		try (RepositoryConnection connection = repository.getConnection()) {

			connection.begin(ShaclSail.TransactionSettings.ValidationApproach.Bulk, IsolationLevels.NONE);

			connection.add(new StringReader(shacl), "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.commit();

		}

	}

	@Test(expected = RemoteShaclValidationException.class)
	public void testInvalid() throws Throwable {

		Repository repository = new HTTPRepository(
				Protocol.getRepositoryLocation(TestServer.SERVER_URL, TestServer.TEST_SHACL_REPO_ID));
		try (RepositoryConnection connection = repository.getConnection()) {

			connection.begin(ShaclSail.TransactionSettings.ValidationApproach.Bulk, IsolationLevels.NONE);

			connection.add(new StringReader(shacl), "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			try {
				connection.commit();
			} catch (RepositoryException e) {
				throw e.getCause();
			}

		}

	}

	@Test(expected = RemoteShaclValidationException.class)
	public void testInvalidSnapshot() throws Throwable {

		Repository repository = new HTTPRepository(
				Protocol.getRepositoryLocation(TestServer.SERVER_URL, TestServer.TEST_SHACL_REPO_ID));
		try (RepositoryConnection connection = repository.getConnection()) {

			connection.begin(ShaclSail.TransactionSettings.ValidationApproach.Bulk, IsolationLevels.SNAPSHOT);

			connection.add(new StringReader(shacl), "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			try {
				connection.commit();
			} catch (RepositoryException e) {
				throw e.getCause();
			}

		}

	}

	@Test
	public void testInvalidRollsBackCorrectly() {

		Repository repository = new HTTPRepository(
				Protocol.getRepositoryLocation(TestServer.SERVER_URL, TestServer.TEST_SHACL_REPO_ID));
		try (RepositoryConnection connection = repository.getConnection()) {

			connection.begin(ShaclSail.TransactionSettings.ValidationApproach.Bulk, IsolationLevels.NONE);

			connection.add(new StringReader(shacl), "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			connection.commit();

		} catch (Exception ignored) {

		}

		try (RepositoryConnection connection = repository.getConnection()) {

			connection.begin(IsolationLevels.NONE);
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();

		}

	}

	@Test(expected = RemoteShaclValidationException.class)
	public void testValidationDisabled() throws Throwable {

		Repository repository = new HTTPRepository(
				Protocol.getRepositoryLocation(TestServer.SERVER_URL, TestServer.TEST_SHACL_REPO_ID));
		try (RepositoryConnection connection = repository.getConnection()) {

			connection.begin(ShaclSail.TransactionSettings.ValidationApproach.Disabled, IsolationLevels.NONE);

			connection.add(new StringReader(shacl), "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			connection.commit();

			connection.begin(ShaclSail.TransactionSettings.ValidationApproach.Bulk, IsolationLevels.SNAPSHOT);
			try (RepositoryConnection connection1 = repository.getConnection()) {

				try {
					connection.commit();
				} catch (RepositoryException e) {
					throw e.getCause();
				}
			}

		}

	}

	@Test
	public void testValidationDisabledSnapshotSerializableValidation() throws Throwable {

		Repository repository = new HTTPRepository(
				Protocol.getRepositoryLocation(TestServer.SERVER_URL, TestServer.TEST_SHACL_REPO_ID));
		try (RepositoryConnection connection = repository.getConnection()) {

			connection.begin(ShaclSail.TransactionSettings.ValidationApproach.Disabled, IsolationLevels.SNAPSHOT);

			connection.add(new StringReader(shacl), "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);

			connection.commit();

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.CLASS);

			connection.begin(ShaclSail.TransactionSettings.ValidationApproach.Disabled, IsolationLevels.SNAPSHOT);

			try (RepositoryConnection connection1 = repository.getConnection()) {

				connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

				connection.commit();

			}

		}

	}

}
