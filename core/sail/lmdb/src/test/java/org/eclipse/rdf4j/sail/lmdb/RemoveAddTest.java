/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests that values and statements correctly exists after (partial) removal and addition.
 */
public class RemoveAddTest {

	private static SailRepository repository;

	public static TemporaryFolder tempDir = new TemporaryFolder();
	static List<Statement> statementList;

	@BeforeAll
	public static void beforeClass() throws IOException {
		tempDir.create();
		File file = tempDir.newFolder();

		LmdbStoreConfig config = new LmdbStoreConfig("spoc,ospc,psoc");
		repository = new SailRepository(new LmdbStore(file, config));
	}

	@AfterAll
	public static void afterClass() {
		repository.shutDown();
		tempDir.delete();
		tempDir = null;
		repository = null;
		statementList = null;
	}

	@Test
	public void removeAndAdd() {
		IRI alice;
		IRI bob;
		int expectedTypeStatements;

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);

			ValueFactory vf = connection.getValueFactory();
			alice = vf.createIRI("urn:person:alice");
			bob = vf.createIRI("urn:person:bob");

			connection.add(alice, RDF.TYPE, FOAF.PERSON);
			connection.add(alice, FOAF.NAME, vf.createLiteral("Alice"));
			connection.add(alice, FOAF.AGE, vf.createLiteral(34));
			connection.add(alice, FOAF.MBOX, vf.createIRI("mailto:alice@example.org"));
			connection.add(alice, FOAF.KNOWS, bob);

			connection.add(bob, RDF.TYPE, FOAF.PERSON);
			connection.add(bob, FOAF.NAME, vf.createLiteral("Bob"));
			connection.add(bob, FOAF.AGE, vf.createLiteral(29));
			connection.add(bob, FOAF.MBOX, vf.createIRI("mailto:bob@example.org"));

			connection.commit();

			expectedTypeStatements = Iterations.asList(connection.getStatements(null, RDF.TYPE, null, false)).size();
		}

		try (SailRepositoryConnection connection = repository.getConnection()) {
			statementList = Iterations.asList(connection.getStatements(null, RDF.TYPE, null, false));
		}

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.remove((Resource) null, RDF.TYPE, null);
			connection.commit();
			connection.begin(IsolationLevels.NONE);
			connection.add(statementList);
			connection.commit();
		}

		try (SailRepositoryConnection connection = repository.getConnection()) {
			int typeStatementsAfterRestore = Iterations
					.asList(connection.getStatements(null, RDF.TYPE, null, false))
					.size();

			assertEquals(expectedTypeStatements, typeStatementsAfterRestore,
					"rdf:type statements should be restored after remove-and-add");
			assertTrue(connection.hasStatement(alice, RDF.TYPE, FOAF.PERSON, false));
			assertTrue(connection.hasStatement(alice, FOAF.NAME, null, false));
			assertTrue(connection.hasStatement(alice, FOAF.AGE, null, false));
			assertTrue(connection.hasStatement(alice, FOAF.KNOWS, bob, false));
			assertTrue(connection.hasStatement(bob, RDF.TYPE, FOAF.PERSON, false));
			assertTrue(connection.hasStatement(bob, FOAF.NAME, null, false));
			assertTrue(connection.hasStatement(bob, FOAF.AGE, null, false));
		}
	}
}
