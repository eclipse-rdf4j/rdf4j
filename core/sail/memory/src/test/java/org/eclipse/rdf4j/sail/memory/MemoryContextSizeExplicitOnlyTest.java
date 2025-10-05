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

package org.eclipse.rdf4j.sail.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.sail.SailConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Mirrors LMDB context-size tests using MemoryStoreConnection, ensuring size(context) and size() count explicit-only
 * statements even when inferred statements exist.
 */
public class MemoryContextSizeExplicitOnlyTest {

	private MemoryStore store;

	@BeforeEach
	public void setUp() {
		store = new MemoryStore();
		store.init();
	}

	@AfterEach
	public void tearDown() {
		if (store != null) {
			store.shutDown();
		}
	}

	@Test
	public void sizeContext_excludesInferred_afterCommit() {
		try (SailConnection raw = store.getConnection()) {
			MemoryStoreConnection conn = (MemoryStoreConnection) raw;
			conn.begin();
			ValueFactory vf = store.getValueFactory();

			IRI ctx = vf.createIRI("urn:ctx");
			IRI p = vf.createIRI("urn:p");

			conn.addStatement(vf.createIRI("urn:s1"), p, vf.createLiteral("x"), ctx);
			conn.addInferredStatement(vf.createIRI("urn:s2"), p, vf.createLiteral("y"), ctx);
			conn.commit();

			long contextSize = conn.size(ctx);
			assertEquals(1L, contextSize, "size(context) must exclude inferred statements");

			long totalSize = conn.size();
			assertEquals(1L, totalSize, "total size must exclude inferred statements");
		}
	}

	@Test
	public void sizeContext_excludesInferred_duringTxn() {
		try (SailConnection raw = store.getConnection()) {
			MemoryStoreConnection conn = (MemoryStoreConnection) raw;
			conn.begin();
			ValueFactory vf = store.getValueFactory();

			IRI ctx = vf.createIRI("urn:ctx");
			IRI p = vf.createIRI("urn:p");

			conn.addStatement(vf.createIRI("urn:s1"), p, vf.createLiteral("x"), ctx);
			conn.addInferredStatement(vf.createIRI("urn:s2"), p, vf.createLiteral("y"), ctx);

			long contextSize = conn.size(ctx);
			assertEquals(1L, contextSize, "size(context) must exclude inferred statements");

			long totalSize = conn.size();
			assertEquals(1L, totalSize, "total size must exclude inferred statements");

			conn.commit();
		}
	}

	@Test
	public void sizeContext_onlyExplicit() {
		try (SailConnection raw = store.getConnection()) {
			MemoryStoreConnection conn = (MemoryStoreConnection) raw;
			conn.begin();
			ValueFactory vf = store.getValueFactory();

			IRI ctx = vf.createIRI("urn:ctx");
			IRI p = vf.createIRI("urn:p");

			conn.addStatement(vf.createIRI("urn:s1"), p, vf.createLiteral("x"), ctx);
			conn.commit();

			long contextSize = conn.size(ctx);
			assertEquals(1L, contextSize, "size(context) must exclude inferred statements");

			long totalSize = conn.size();
			assertEquals(1L, totalSize, "total size must exclude inferred statements");
		}
	}
}
