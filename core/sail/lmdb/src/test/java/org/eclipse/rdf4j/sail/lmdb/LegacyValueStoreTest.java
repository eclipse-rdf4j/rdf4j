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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.TripleTerm;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LegacyValueStoreTest {

	private final ValueFactory vf = SimpleValueFactory.getInstance();
	private LegacyValueStore store;

	@TempDir
	File dataDir;

	@AfterEach
	void closeStore() throws Exception {
		if (store != null) {
			store.close();
		}
	}

	@Test
	void writesLegacyIdsAndContinuesTheSequenceAfterReopen() throws Exception {
		LmdbStoreConfig config = new LmdbStoreConfig().setInlineLiterals(true).setValueEvictionInterval(0);
		store = new LegacyValueStore(dataDir, config);

		IRI iri = vf.createIRI("urn:test:value");
		Literal literal = vf.createLiteral(42);
		var bnode = vf.createBNode("legacy-node");
		long iriId;
		long literalId;
		long bnodeId;
		store.startTransaction(true);
		try {
			iriId = store.storeValue(iri);
			literalId = store.storeValue(literal);
			bnodeId = store.storeValue(bnode);
			store.commit();
		} catch (Throwable t) {
			store.rollback();
			throw t;
		}

		assertEquals(0, iriId & 0x3L);
		assertEquals(1, literalId & 0x3L);
		assertEquals(2, bnodeId & 0x3L);
		assertFalse(store.isInlined(literalId));

		store.close();
		store = new LegacyValueStore(dataDir, config);

		assertEquals(iriId, store.getId(iri));
		assertEquals(literalId, store.getId(literal));
		assertEquals(bnodeId, store.getId(bnode));
		assertEquals(iri, store.getValue(iriId));
		assertEquals(literal, store.getValue(literalId));
		assertEquals(bnode, store.getValue(bnodeId));

		store.startTransaction(true);
		long nextId;
		try {
			nextId = store.storeValue(vf.createIRI("urn:test:after-reopen"));
			store.commit();
		} catch (Throwable t) {
			store.rollback();
			throw t;
		}
		assertEquals(Math.max(iriId >>> 2, Math.max(literalId >>> 2, bnodeId >>> 2)) + 1, nextId >>> 2);
	}

	@Test
	void reopensLongValuesStoredThroughThe53HashPrefixes() throws Exception {
		LmdbStoreConfig config = new LmdbStoreConfig().setInlineLiterals(true).setValueEvictionInterval(0);
		store = new LegacyValueStore(dataDir, config);
		Literal longLiteral = vf.createLiteral("legacy-" + "x".repeat(128));

		store.startTransaction(true);
		long id;
		try {
			id = store.storeValue(longLiteral);
			store.commit();
		} catch (Throwable t) {
			store.rollback();
			throw t;
		}

		assertEquals(1, id & 0x3L);
		store.close();
		store = new LegacyValueStore(dataDir, config);
		assertEquals(id, store.getId(longLiteral));
		assertEquals(longLiteral, store.getValue(id));
	}

	@Test
	void writesThe53LiteralRecordByteLayout() throws Exception {
		store = new LegacyValueStore(dataDir,
				new LmdbStoreConfig().setInlineLiterals(true).setValueEvictionInterval(0));
		Literal literal = vf.createLiteral("hello", "en");

		store.startTransaction(true);
		long id;
		try {
			id = store.storeValue(literal);
			store.commit();
		} catch (Throwable t) {
			store.rollback();
			throw t;
		}

		byte[] data = store.getData(id);
		ByteBuffer bb = ByteBuffer.wrap(data);
		assertEquals(1, bb.get() & 0xFF, "legacy literal marker");
		long datatypeId = Varint.readUnsignedHeap(bb);
		assertEquals(ValueStoreFormat.IdKind.IRI, ValueStoreFormat.LEGACY_5_3.getKind(datatypeId));
		assertEquals(2, bb.get() & 0xFF, "5.3.x stores the full 8-bit language length without direction bits");
		byte[] language = new byte[2];
		bb.get(language);
		assertArrayEquals("en".getBytes(StandardCharsets.UTF_8), language);
		byte[] label = new byte[bb.remaining()];
		bb.get(label);
		assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), label);
	}

	@Test
	void currentStoreContinuesAtTheNextSevenBitOrdinalAfterReopen() throws Exception {
		LmdbStoreConfig config = new LmdbStoreConfig().setInlineLiterals(false).setValueEvictionInterval(0);
		ValueStore current = new ValueStore(dataDir, config);
		long firstId;
		try {
			current.startTransaction(true);
			try {
				firstId = current.storeValue(vf.createIRI("urn:sequence:first"));
				current.commit();
			} catch (Throwable t) {
				current.rollback();
				throw t;
			}
		} finally {
			current.close();
		}

		current = new ValueStore(dataDir, config);
		try {
			current.startTransaction(true);
			long secondId;
			try {
				secondId = current.storeValue(vf.createIRI("urn:sequence:second"));
				current.commit();
			} catch (Throwable t) {
				current.rollback();
				throw t;
			}
			assertEquals(ValueIds.getValue(firstId) + 1, ValueIds.getValue(secondId));
		} finally {
			current.close();
		}
	}

	@Test
	void rejectsValuesThatHaveNo53Representation() throws Exception {
		store = new LegacyValueStore(dataDir, new LmdbStoreConfig());
		IRI subject = vf.createIRI("urn:test:s");
		IRI predicate = vf.createIRI("urn:test:p");
		TripleTerm tripleTerm = vf.createTripleTerm(subject, predicate, vf.createLiteral("o"));
		Literal directed = vf.createLiteral("hello", "en", Literal.BaseDirection.LTR);

		store.startTransaction(true);
		try {
			assertThrows(IllegalArgumentException.class, () -> store.storeValue(tripleTerm));
			assertThrows(IllegalArgumentException.class, () -> store.storeValue(directed));
		} finally {
			store.rollback();
		}
	}
}
