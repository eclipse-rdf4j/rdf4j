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
 * Focused tests that directly exercise TripleStore.TripleIndex#toEntry to provide coverage for behavior-neutral
 * optimizations such as internal encoding caching.
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

		ByteBuffer actualKey = ByteBuffer.allocate(Varint.calcLengthUnsigned(subj) + Varint.calcLengthUnsigned(pred));
		ByteBuffer actualValue = ByteBuffer.allocate(2 * (Long.BYTES + 1));
		index.toEntry(actualKey, actualValue, subj, pred, obj, context);
		actualKey.flip();
		actualValue.flip();

		// Expected: varints in spoc order
		ByteBuffer expectedKey = ByteBuffer.allocate(actualKey.capacity());
		Varint.writeUnsigned(expectedKey, subj);
		Varint.writeUnsigned(expectedKey, pred);
		ByteBuffer expectedValue = ByteBuffer.allocate(actualValue.capacity());
		Varint.writeUnsigned(expectedValue, obj);
		Varint.writeUnsigned(expectedValue, context);
		expectedValue.position(actualValue.capacity());

		assertArrayEquals(expectedKey.array(), actualKey.array());
		assertArrayEquals(expectedValue.array(), actualValue.array());
	}

	@Test
	void posc_predicateBound_othersWildcard() throws Exception {
		// Given: only predicate is bound, others are wildcards (Long.MAX_VALUE)
		long subj = Long.MAX_VALUE;
		long pred = 456L;
		long obj = Long.MAX_VALUE;
		long context = Long.MAX_VALUE;

		TripleStore.TripleIndex index = tripleStore.new TripleIndex("posc");

		ByteBuffer actualKey = ByteBuffer.allocate(Varint.calcLengthUnsigned(subj) + Varint.calcLengthUnsigned(pred));
		ByteBuffer actualValue = ByteBuffer.allocate(2 * (Long.BYTES + 1));
		index.toEntry(actualKey, actualValue, subj, pred, obj, context);
		actualKey.flip();
		actualValue.flip();

		// Expected: varints in spoc order
		ByteBuffer expectedKey = ByteBuffer.allocate(actualKey.capacity());
		Varint.writeUnsigned(expectedKey, pred);
		Varint.writeUnsigned(expectedKey, obj);
		ByteBuffer expectedValue = ByteBuffer.allocate(actualValue.capacity());
		Varint.writeUnsigned(expectedValue, subj);
		Varint.writeUnsigned(expectedValue, context);
		expectedValue.position(actualValue.capacity());

		assertArrayEquals(expectedKey.array(), actualKey.array());
		assertArrayEquals(expectedValue.array(), actualValue.array());
	}
}
