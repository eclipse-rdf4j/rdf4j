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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
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

		try (RepositoryConnection connection = repo.getConnection()) {
			connection.add(connection.getValueFactory().createIRI("urn:subj"),
					connection.getValueFactory().createIRI("urn:pred"),
					connection.getValueFactory().createLiteral("value", lang));
		}

		repo.shutDown();

		SailRepository reopened = new SailRepository(new NativeStore(dataDir));
		reopened.init();

		assertDoesNotThrow(() -> {
			try (RepositoryConnection connection = reopened.getConnection()) {
				RepositoryResult<Statement> statements = connection.getStatements(null, null, null, true);
				List<Statement> list = Iterations.asList(statements);
				assertEquals(1, list.size());

				Literal literal = (Literal) list.get(0).getObject();
				assertEquals(lang, literal.getLanguage().orElseThrow());
			}
		});

		reopened.shutDown();
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
