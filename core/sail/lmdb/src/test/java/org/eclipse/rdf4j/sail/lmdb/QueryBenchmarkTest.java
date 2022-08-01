/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.lmdb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
   */
public class QueryBenchmarkTest {

	private static SailRepository repository;

	public static TemporaryFolder tempDir = new TemporaryFolder();

	private static final String query1;
	private static final String query2;
	private static final String query3;
	private static final String query4;
	private static final String query5;

	static List<Statement> statementList;

	static {
		try {
			query1 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query1.qr"), StandardCharsets.UTF_8);
			query2 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query2.qr"), StandardCharsets.UTF_8);
			query3 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query3.qr"), StandardCharsets.UTF_8);
			query4 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query4.qr"), StandardCharsets.UTF_8);
			query5 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query5.qr"), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@BeforeClass
	public static void beforeClass() throws IOException {
		tempDir.create();
		File file = tempDir.newFolder();

		LmdbStoreConfig config = new LmdbStoreConfig("spoc,ospc,psoc");
		repository = new SailRepository(new LmdbStore(file, config));

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.add(getResourceAsStream("benchmarkFiles/datagovbe-valid.ttl"), "", RDFFormat.TURTLE);
			connection.commit();
		}

		try (SailRepositoryConnection connection = repository.getConnection()) {
			statementList = Iterations.asList(connection.getStatements(null, RDF.TYPE, null, false));
		}
	}

	private static InputStream getResourceAsStream(String name) {
		return QueryBenchmarkTest.class.getClassLoader().getResourceAsStream(name);
	}

	@AfterClass
	public static void afterClass() throws IOException {
		tempDir.delete();
		repository.shutDown();
		tempDir = null;
		repository = null;
		statementList = null;
	}

	@Test
	public void groupByQuery() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			long count = connection
					.prepareTupleQuery(query1)
					.evaluate()
					.stream()
					.count();
			System.out.println(count);
		}
	}

	@Test
	public void complexQuery() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			long count = connection
					.prepareTupleQuery(query4)
					.evaluate()
					.stream()
					.count();
			System.out.println("count: " + count);
		}
	}

	@Test
	public void distinctPredicatesQuery() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			long count = connection
					.prepareTupleQuery(query5)
					.evaluate()
					.stream()
					.count();
			System.out.println(count);
		}
	}

	@Test
	public void removeByQuery() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.remove((Resource) null, RDF.TYPE, null);
			connection.commit();
			connection.begin(IsolationLevels.NONE);
			connection.add(statementList);
			connection.commit();
		}
		hasStatement();

	}

	@Test
	public void removeByQueryReadCommitted() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.READ_COMMITTED);
			connection.remove((Resource) null, RDF.TYPE, null);
			connection.commit();
			connection.begin(IsolationLevels.READ_COMMITTED);
			connection.add(statementList);
			connection.commit();
		}
		hasStatement();

	}

	@Test
	public void simpleUpdateQueryIsolationReadCommitted() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.READ_COMMITTED);
			connection.prepareUpdate(query2).execute();
			connection.commit();
		}

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.READ_COMMITTED);
			connection.prepareUpdate(query3).execute();
			connection.commit();
		}
		hasStatement();

	}

	@Test
	public void simpleUpdateQueryIsolationNone() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.prepareUpdate(query2).execute();
			connection.commit();
		}

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.prepareUpdate(query3).execute();
			connection.commit();
		}
		hasStatement();

	}

	private boolean hasStatement() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			return connection.hasStatement(RDF.TYPE, RDF.TYPE, RDF.TYPE, true);
		}
	}

}
