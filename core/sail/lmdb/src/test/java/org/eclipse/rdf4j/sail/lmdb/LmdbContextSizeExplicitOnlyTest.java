/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.sail.SailConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Reproduces regression: context-size must be explicit-only. The LMDB fast path currently counts inferred statements
 * for context-only size queries.
 */
public class LmdbContextSizeExplicitOnlyTest {

	private Path tmpDir;
	private LmdbStore store;

	@BeforeEach
	public void setUp() throws IOException {
		tmpDir = Files.createTempDirectory("rdf4j-lmdb-test-");
		store = new LmdbStore(tmpDir.toFile());
		store.init();
	}

	@AfterEach
	public void tearDown() throws IOException {
		if (store != null) {
			store.shutDown();
		}
		if (tmpDir != null) {
			// best-effort cleanup
			Files.walk(tmpDir)
					.sorted((a, b) -> b.compareTo(a))
					.forEach(p -> {
						try {
							Files.deleteIfExists(p);
						} catch (IOException ignore) {
						}
					});
		}
	}

	@Test
	public void sizeContext_excludesInferred() throws Exception {
		try (SailConnection raw = store.getConnection()) {
			LmdbStoreConnection conn = (LmdbStoreConnection) raw;
			conn.begin();
			ValueFactory vf = store.getValueFactory();

			IRI ctx = vf.createIRI("urn:ctx");
			IRI p = vf.createIRI("urn:p");

			// one explicit in ctx
			conn.addStatement(vf.createIRI("urn:s1"), p, vf.createLiteral("x"), ctx);
			// one inferred in the same ctx (simulate inference via addInferredStatement)
			conn.addInferredStatement(vf.createIRI("urn:s2"), p, vf.createLiteral("y"), ctx);
			conn.commit();

			// size must exclude inferred statements
			long contextSize = conn.size(ctx);
			assertEquals(1L, contextSize, "size(context) must exclude inferred statements");

			long totalSize = conn.size();
			assertEquals(1L, totalSize, "total size must exclude inferred statements");
		}
	}
}
