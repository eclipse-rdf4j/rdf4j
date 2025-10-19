/**
 * ******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * ******************************************************************************
 */
package org.eclipse.rdf4j.sail.lmdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.order.StatementOrder;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that LMDB store exposes statement ordering capabilities needed for merge-join optimization.
 */
public class LmdbOrderedTest {

	private File dataDir;
	private LmdbStore store;

	@BeforeEach
	public void setup() throws Exception {
		dataDir = Files.createTempDirectory("rdf4j-lmdb-ordered-test").toFile();
		// Ensure both S and P primary indexes are available (default is spoc,posc, but set explicitly here)
		store = new LmdbStore(dataDir, new LmdbStoreConfig("spoc,posc"));
	}

	@AfterEach
	public void tearDown() {
		if (store != null) {
			store.shutDown();
		}
		if (dataDir != null) {
			deleteRecursively(dataDir);
		}
	}

	private static void deleteRecursively(File f) {
		if (f.isDirectory()) {
			File[] files = f.listFiles();
			if (files != null) {
				for (File c : files) {
					deleteRecursively(c);
				}
			}
		}
		// ignore result; best-effort cleanup
		f.delete();
	}

	@Test
	public void getStatements_orderBySubject_returnsSorted() {
		String NS = "http://example.org/";
		try (NotifyingSailConnection conn = store.getConnection()) {
			conn.begin();
			conn.addStatement(Values.iri(NS, "d"), RDFS.LABEL, Values.literal("b"));
			conn.addStatement(Values.iri(NS, "e"), RDFS.LABEL, Values.literal("a"));
			conn.addStatement(Values.iri(NS, "a"), RDFS.LABEL, Values.literal("e"));
			conn.addStatement(Values.iri(NS, "c"), RDFS.LABEL, Values.literal("c"));
			conn.addStatement(Values.iri(NS, "b"), RDFS.LABEL, Values.literal("d"));
			conn.commit();

			conn.begin(IsolationLevels.NONE);
			try (CloseableIteration<? extends Statement> it = conn.getStatements(StatementOrder.S, null, null, null,
					true)) {
				List<String> subjects = it.stream()
						.map(Statement::getSubject)
						.map(v -> (IRI) v)
						.map(IRI::getLocalName)
						.collect(Collectors.toList());
				// LMDB orders by internal value-id (insertion order), not lexical
				assertThat(subjects).isEqualTo(List.of("d", "e", "a", "c", "b"));
			}
			conn.commit();
		}
	}
}
