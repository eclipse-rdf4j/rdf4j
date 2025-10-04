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
package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.File;
import java.nio.ByteBuffer;

import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Focused tests that directly exercise TripleStore.TripleIndex#toKey to provide coverage for behavior-neutral
 * optimizations such as internal key encoding caching.
 */
class TripleIndexToKeyCacheTest {

	private TripleStore tripleStore;

	@BeforeEach
	void setup(@TempDir File dataDir) throws Exception {
		// Create a small store; index set is irrelevant for constructing standalone TripleIndex instances
		tripleStore = new TripleStore(dataDir, new LmdbStoreConfig("spoc,posc"), null);
	}

	@AfterEach
	void tearDown() throws Exception {
		if (tripleStore != null) {
			tripleStore.close();
		}
	}

	@Test
	void spoc_subjectBound_othersWildcard() throws Exception {
		// Given: only subject is bound, others are wildcards (Long.MAX_VALUE)
		long subj = 123L;
		long pred = Long.MAX_VALUE;
		long obj = Long.MAX_VALUE;
		long context = Long.MAX_VALUE;

		TripleStore.TripleIndex index = tripleStore.new TripleIndex("spoc");

		int len = Varint.calcListLengthUnsigned(subj, pred, obj, context);
		ByteBuffer actual = ByteBuffer.allocate(len);
		index.toKey(actual, subj, pred, obj, context);
		actual.flip();

		// Expected: varints in spoc order
		ByteBuffer expected = ByteBuffer.allocate(len);
		Varint.writeUnsigned(expected, subj);
		Varint.writeUnsigned(expected, pred);
		Varint.writeUnsigned(expected, obj);
		Varint.writeUnsigned(expected, context);
		expected.flip();

		assertArrayEquals(expected.array(), actual.array());
	}

	@Test
	void posc_predicateBound_othersWildcard() throws Exception {
		// Given: only predicate is bound, others are wildcards (Long.MAX_VALUE)
		long subj = Long.MAX_VALUE;
		long pred = 456L;
		long obj = Long.MAX_VALUE;
		long context = Long.MAX_VALUE;

		TripleStore.TripleIndex index = tripleStore.new TripleIndex("posc");

		int len = Varint.calcListLengthUnsigned(subj, pred, obj, context);
		ByteBuffer actual = ByteBuffer.allocate(len);
		index.toKey(actual, subj, pred, obj, context);
		actual.flip();

		// Expected: varints in posc order
		ByteBuffer expected = ByteBuffer.allocate(len);
		Varint.writeUnsigned(expected, pred);
		Varint.writeUnsigned(expected, obj);
		Varint.writeUnsigned(expected, subj);
		Varint.writeUnsigned(expected, context);
		expected.flip();

		assertArrayEquals(expected.array(), actual.array());
	}
}
