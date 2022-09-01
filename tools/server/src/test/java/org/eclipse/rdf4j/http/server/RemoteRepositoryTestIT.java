/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.http.server;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RemoteRepositoryTestIT {

	public static final IRI CONTEXT1 = Values.iri("http://example.com/context1");
	public static final IRI CONTEXT2 = Values.iri("http://example.com/context2");
	public static final IRI CONTEXT3 = Values.iri("http://example.com/context3");
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

	@BeforeEach
	public void beforeEach() {
		Repository systemRepo = new HTTPRepository(
				Protocol.getRepositoryLocation(TestServer.SERVER_URL, TestServer.TEST_REPO_ID));
		try (RepositoryConnection connection = systemRepo.getConnection()) {
			connection.begin();
			connection.clear();
			connection.commit();

			connection.begin();

			// Add two statements to the default graph
			connection.add(Values.bnode("_bnode1"), RDF.TYPE, RDFS.RESOURCE);
			connection.add(Values.bnode("_bnode2"), RDF.TYPE, RDFS.RESOURCE);

			// Add three statements to three different named graphs
			connection.add(Values.bnode("_bnode3"), RDF.TYPE, RDFS.RESOURCE, CONTEXT1);
			connection.add(Values.bnode("_bnode3"), RDF.TYPE, RDFS.RESOURCE, CONTEXT2);
			connection.add(Values.bnode("_bnode4"), RDF.TYPE, RDFS.RESOURCE, CONTEXT3);
			connection.commit();
		}
	}

	@Test
	public void testClearDefaultContextOnly() {

		Repository systemRepo = new HTTPRepository(
				Protocol.getRepositoryLocation(TestServer.SERVER_URL, TestServer.TEST_REPO_ID));

		try (RepositoryConnection connection = systemRepo.getConnection()) {

			// clear the default graph
			connection.begin();
			connection.clear(((Resource) null));
			connection.commit();

			connection.begin();
			try (Stream<Statement> stream = connection.getStatements(null, null, null, false).stream()) {
				List<Statement> collect = stream.collect(Collectors.toList());
				for (Statement statement : collect) {
					Assertions.assertNotNull(statement.getContext());
				}
				Assertions.assertEquals(3, collect.size());
			}
			connection.commit();

		}

	}

	@Test
	public void testClearDefaultContextOnlySingleTransaction() {

		Repository systemRepo = new HTTPRepository(
				Protocol.getRepositoryLocation(TestServer.SERVER_URL, TestServer.TEST_REPO_ID));

		try (RepositoryConnection connection = systemRepo.getConnection()) {
			connection.begin();
			// clear the default graph
			connection.clear(((Resource) null));

			try (Stream<Statement> stream = connection.getStatements(null, null, null, false).stream()) {
				List<Statement> collect = stream.collect(Collectors.toList());
				Assertions.assertEquals(3, collect.size());
				for (Statement statement : collect) {
					Assertions.assertNotNull(statement.getContext());
				}
			}

			connection.commit();

		}

	}

	@Test
	public void testClearSingleContextSingleTransaction() {

		Repository systemRepo = new HTTPRepository(
				Protocol.getRepositoryLocation(TestServer.SERVER_URL, TestServer.TEST_REPO_ID));

		try (RepositoryConnection connection = systemRepo.getConnection()) {
			connection.begin();
			connection.clear(CONTEXT1);

			try (Stream<Statement> stream = connection.getStatements(null, null, null, false).stream()) {
				List<Statement> collect = stream.collect(Collectors.toList());
				Assertions.assertEquals(4, collect.size());
				for (Statement statement : collect) {
					Assertions.assertNotEquals(CONTEXT1, statement.getContext());
				}
			}

			connection.commit();

		}

	}

	@Test
	public void testClearAllContextsSingleTransaction() {

		Repository systemRepo = new HTTPRepository(
				Protocol.getRepositoryLocation(TestServer.SERVER_URL, TestServer.TEST_REPO_ID));

		try (RepositoryConnection connection = systemRepo.getConnection()) {
			connection.begin();
			// clear the default graph
			connection.clear();

			try (Stream<Statement> stream = connection.getStatements(null, null, null, false).stream()) {
				List<Statement> collect = stream.collect(Collectors.toList());
				Assertions.assertEquals(0, collect.size());
			}

			connection.commit();

		}

	}

	@Test
	public void testClearAllContexts() {

		Repository systemRepo = new HTTPRepository(
				Protocol.getRepositoryLocation(TestServer.SERVER_URL, TestServer.TEST_REPO_ID));

		try (RepositoryConnection connection = systemRepo.getConnection()) {
			// clear the all graphs
			connection.begin();
			connection.clear();
			connection.commit();

			connection.begin();
			try (Stream<Statement> stream = connection.getStatements(null, null, null, false).stream()) {
				List<Statement> collect = stream.collect(Collectors.toList());
				Assertions.assertEquals(0, collect.size());
			}
			connection.commit();

		}

	}

	@Test
	public void testClearSpecificContexts() {

		Repository systemRepo = new HTTPRepository(
				Protocol.getRepositoryLocation(TestServer.SERVER_URL, TestServer.TEST_REPO_ID));

		try (RepositoryConnection connection = systemRepo.getConnection()) {
			// clear the all graphs
			connection.begin();
			connection.clear(CONTEXT1, CONTEXT2);
			connection.commit();

			connection.begin();
			try (Stream<Statement> stream = connection.getStatements(null, null, null, false).stream()) {
				List<Statement> collect = stream.collect(Collectors.toList());
				Assertions.assertEquals(3, collect.size());
				for (Statement statement : collect) {
					Assertions.assertTrue(statement.getContext() == null || statement.getContext().equals(CONTEXT3));
				}
			}
			connection.commit();

		}

	}

}
