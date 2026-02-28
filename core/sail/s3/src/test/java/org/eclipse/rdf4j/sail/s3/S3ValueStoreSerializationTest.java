/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.s3.storage.FileSystemObjectStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class S3ValueStoreSerializationTest {

	@TempDir
	Path tempDir;

	@Test
	void roundTrip_iri() {
		FileSystemObjectStore store = new FileSystemObjectStore(tempDir);
		S3ValueStore vs = new S3ValueStore();

		IRI iri = vs.createIRI("http://example.org/test");
		long id = vs.storeValue(iri);

		vs.serialize(store);

		S3ValueStore vs2 = new S3ValueStore();
		vs2.deserialize(store, vs.getNextId());

		Value restored = vs2.getValue(id);
		assertNotNull(restored);
		assertTrue(restored instanceof IRI);
		assertEquals("http://example.org/test", restored.stringValue());
		assertEquals(id, vs2.getId(iri));
	}

	@Test
	void roundTrip_literal() {
		FileSystemObjectStore store = new FileSystemObjectStore(tempDir);
		S3ValueStore vs = new S3ValueStore();

		Literal lit = vs.createLiteral("hello world");
		long id = vs.storeValue(lit);

		vs.serialize(store);

		S3ValueStore vs2 = new S3ValueStore();
		vs2.deserialize(store, vs.getNextId());

		Value restored = vs2.getValue(id);
		assertNotNull(restored);
		assertTrue(restored instanceof Literal);
		assertEquals("hello world", ((Literal) restored).getLabel());
	}

	@Test
	void roundTrip_literalWithLanguage() {
		FileSystemObjectStore store = new FileSystemObjectStore(tempDir);
		S3ValueStore vs = new S3ValueStore();

		Literal lit = vs.createLiteral("bonjour", "fr");
		long id = vs.storeValue(lit);

		vs.serialize(store);

		S3ValueStore vs2 = new S3ValueStore();
		vs2.deserialize(store, vs.getNextId());

		Value restored = vs2.getValue(id);
		assertNotNull(restored);
		assertTrue(restored instanceof Literal);
		assertEquals("bonjour", ((Literal) restored).getLabel());
		assertTrue(((Literal) restored).getLanguage().isPresent());
		assertEquals("fr", ((Literal) restored).getLanguage().get());
	}

	@Test
	void roundTrip_bnode() {
		FileSystemObjectStore store = new FileSystemObjectStore(tempDir);
		S3ValueStore vs = new S3ValueStore();

		BNode bnode = vs.createBNode("node123");
		long id = vs.storeValue(bnode);

		vs.serialize(store);

		S3ValueStore vs2 = new S3ValueStore();
		vs2.deserialize(store, vs.getNextId());

		Value restored = vs2.getValue(id);
		assertNotNull(restored);
		assertTrue(restored instanceof BNode);
		assertEquals("node123", ((BNode) restored).getID());
	}

	@Test
	void roundTrip_multipleValues() {
		FileSystemObjectStore store = new FileSystemObjectStore(tempDir);
		S3ValueStore vs = new S3ValueStore();

		IRI iri1 = vs.createIRI("http://example.org/s1");
		IRI iri2 = vs.createIRI("http://example.org/p1");
		Literal lit = vs.createLiteral("value");
		BNode bnode = vs.createBNode("b1");

		long id1 = vs.storeValue(iri1);
		long id2 = vs.storeValue(iri2);
		long id3 = vs.storeValue(lit);
		long id4 = vs.storeValue(bnode);

		vs.serialize(store);

		S3ValueStore vs2 = new S3ValueStore();
		vs2.deserialize(store, vs.getNextId());

		assertEquals(iri1, vs2.getValue(id1));
		assertEquals(iri2, vs2.getValue(id2));
		assertEquals(lit.getLabel(), ((Literal) vs2.getValue(id3)).getLabel());
		assertEquals(bnode.getID(), ((BNode) vs2.getValue(id4)).getID());
	}
}
