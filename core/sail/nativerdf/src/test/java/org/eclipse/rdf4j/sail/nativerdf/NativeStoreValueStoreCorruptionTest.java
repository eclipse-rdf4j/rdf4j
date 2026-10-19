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

package org.eclipse.rdf4j.sail.nativerdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Demonstrates how data written through normal repository operations can corrupt the underlying {@link ValueStore}.
 */
public class NativeStoreValueStoreCorruptionTest {

	@TempDir
	File dataDir;

	@Test
	public void longLanguageTagShouldNotCorruptValueStore() throws Exception {
		SailRepository repo = new SailRepository(new NativeStore(dataDir));
		repo.init();

		String lang = buildLanguageTag(256);

		assertThrows(RepositoryException.class, () -> {

			try (RepositoryConnection connection = repo.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				connection.add(connection.getValueFactory().createIRI("urn:subj"),
						connection.getValueFactory().createIRI("urn:pred"),
						connection.getValueFactory().createLiteral("value", lang));
				connection.commit();
			}
		});

		repo.shutDown();

		SailRepository reopened = new SailRepository(new NativeStore(dataDir));
		reopened.init();

		try (RepositoryConnection connection = reopened.getConnection()) {
			try (RepositoryResult<Statement> statements = connection.getStatements(null, null, null, true)) {
				List<Statement> list = Iterations.asList(statements);
				assertEquals(0, list.size());
			}
		}

		reopened.shutDown();
	}

	@Test
	public void longLanguageTagShouldNotCorruptValueStoreIncremental() throws Exception {
		NativeStore sail = new NativeStore(dataDir);
		sail.setWalEnabled(false);
		SailRepository repo = new SailRepository(sail);
		repo.init();

		for (int i = 0; i < 256; i++) {
			String lang = buildLanguageTag(i);

			try (RepositoryConnection connection = repo.getConnection()) {
				connection.begin(IsolationLevels.NONE);
				connection.add(connection.getValueFactory().createIRI("urn:subj"),
						connection.getValueFactory().createIRI("urn:pred"),
						connection.getValueFactory().createLiteral("value", lang));
				connection.commit();
			}

			repo.shutDown();

			NativeStore sail1 = new NativeStore(dataDir);
			sail1.setWalEnabled(false);
			SailRepository reopened = new SailRepository(sail1);
			reopened.init();

			try (RepositoryConnection connection = reopened.getConnection()) {
				try (RepositoryResult<Statement> statements = connection.getStatements(null, null, null, true)) {
					List<Statement> list = Iterations.asList(statements);
					assertEquals(1, list.size());
					Literal literal = (Literal) list.get(0).getObject();
					assertEquals(lang, literal.getLanguage().orElseThrow());
				}
			}

			try (SailRepositoryConnection connection = reopened.getConnection()) {
				connection.clear();
			}

			reopened.shutDown();
		}
	}

	@Test
	public void longDatatypeShouldNotCorruptValueStore() throws Exception {
		SailRepository repo = new SailRepository(new NativeStore(dataDir));
		repo.init();

		String longDatatype = buildIRIOfLength(5000);

		try (RepositoryConnection connection = repo.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.add(connection.getValueFactory().createIRI("urn:subj"),
					connection.getValueFactory().createIRI("urn:pred"),
					connection.getValueFactory()
							.createLiteral("value",
									connection.getValueFactory().createIRI(longDatatype)));
			connection.commit();
		}

		repo.shutDown();

		SailRepository reopened = new SailRepository(new NativeStore(dataDir));
		reopened.init();

		try (RepositoryConnection connection = reopened.getConnection()) {
			try (RepositoryResult<Statement> statements = connection.getStatements(null, null, null, true)) {
				List<Statement> list = Iterations.asList(statements);
				assertEquals(1, list.size());
				Literal literal = (Literal) list.get(0).getObject();
				assertEquals(longDatatype, literal.getDatatype().stringValue());
			}
		}

		reopened.shutDown();
	}

	@Test
	public void longLabelShouldNotCorruptValueStore() throws Exception {
		SailRepository repo = new SailRepository(new NativeStore(dataDir));
		repo.init();

		String longLabel = buildString(20000);

		try (RepositoryConnection connection = repo.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.add(connection.getValueFactory().createIRI("urn:subj"),
					connection.getValueFactory().createIRI("urn:pred"),
					connection.getValueFactory().createLiteral(longLabel));
			connection.commit();
		}

		repo.shutDown();

		SailRepository reopened = new SailRepository(new NativeStore(dataDir));
		reopened.init();

		try (RepositoryConnection connection = reopened.getConnection()) {
			try (RepositoryResult<Statement> statements = connection.getStatements(null, null, null, true)) {
				List<Statement> list = Iterations.asList(statements);
				assertEquals(1, list.size());
				Literal literal = (Literal) list.get(0).getObject();
				assertEquals(longLabel, literal.getLabel());
			}
		}

		reopened.shutDown();
	}

	@Test
	public void longIRIShouldNotCorruptValueStore() throws Exception {
		SailRepository repo = new SailRepository(new NativeStore(dataDir));
		repo.init();

		String longIri = buildIRIOfLength(5000);

		try (RepositoryConnection connection = repo.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.add(connection.getValueFactory().createIRI(longIri),
					connection.getValueFactory().createIRI("urn:pred"),
					connection.getValueFactory().createLiteral("value"));
			connection.commit();
		}

		repo.shutDown();

		SailRepository reopened = new SailRepository(new NativeStore(dataDir));
		reopened.init();

		try (RepositoryConnection connection = reopened.getConnection()) {
			try (RepositoryResult<Statement> statements = connection.getStatements(null, null, null, true)) {
				List<Statement> list = Iterations.asList(statements);
				assertEquals(1, list.size());
				Statement st = list.get(0);
				assertEquals(longIri, st.getSubject().stringValue());
			}
		}

		reopened.shutDown();
	}

	@Test
	public void longBNodeShouldNotCorruptValueStore() throws Exception {
		SailRepository repo = new SailRepository(new NativeStore(dataDir));
		repo.init();

		String longBnodeId = buildString(10000);

		try (RepositoryConnection connection = repo.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.add(connection.getValueFactory().createBNode(longBnodeId),
					connection.getValueFactory().createIRI("urn:pred"),
					connection.getValueFactory().createLiteral("value"));
			connection.commit();
		}

		repo.shutDown();

		SailRepository reopened = new SailRepository(new NativeStore(dataDir));
		reopened.init();

		try (RepositoryConnection connection = reopened.getConnection()) {
			try (RepositoryResult<Statement> statements = connection.getStatements(null, null, null, true)) {
				List<Statement> list = Iterations.asList(statements);
				assertEquals(1, list.size());
				Statement st = list.get(0);
				// ensure subject is a BNode with the long id
				assertEquals(longBnodeId, st.getSubject().stringValue());
			}
		}

		reopened.shutDown();
	}

	private String buildString(int targetLength) {
		StringBuilder builder = new StringBuilder(targetLength);
		while (builder.length() < targetLength) {
			builder.append('x');
		}
		return builder.toString();
	}

	private String buildIRIOfLength(int targetLength) {
		String base = "http://example.org/";
		StringBuilder builder = new StringBuilder(targetLength);
		builder.append(base);
		while (builder.length() < targetLength) {
			builder.append('a');
		}
		return builder.toString();
	}

	private String buildLanguageTag(int targetLength) {
		StringBuilder builder = new StringBuilder(targetLength);
		builder.append("en");
		while (builder.length() < targetLength) {
			builder.append('-');
			int segmentLength = Math.min(8, targetLength - builder.length());
			for (int i = 0; i < segmentLength; i++) {
				builder.append('a');
			}
		}
		return builder.toString();
	}
}
