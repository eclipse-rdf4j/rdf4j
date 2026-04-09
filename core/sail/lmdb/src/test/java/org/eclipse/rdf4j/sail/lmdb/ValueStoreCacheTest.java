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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.File;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Focused test to prove that the ValueStore cache path is exercised by existing operations.
 */
class ValueStoreCacheTest {

	@Test
	void cachedValuePath(@TempDir File dataDir) throws Exception {
		ValueStore vs = new ValueStore(new File(dataDir, "values"), new LmdbStoreConfig());
		try {
			IRI iri = Values.iri("urn:example:foo");

			// Store the IRI inside a write transaction so getId(create=true) can write to LMDB
			vs.startTransaction(true);
			long id = vs.getId(iri, true);
			vs.commit();

			// On lazy retrieval, the cache should not be touched
			LmdbValue v1 = vs.getLazyValue(id);
			assertNotNull(v1);
			LmdbValue cached1 = vs.cachedValue(id);
			assertNull(cached1);

			// After initializing the value, it should be cached
			assertEquals("urn:example:foo", v1.stringValue());
			LmdbValue cached2 = vs.cachedValue(id);
			assertSame(v1, cached2);

			// Direct cache hit
			LmdbValue v2 = vs.cachedValue(id);
			assertSame(v1, v2);
		} finally {
			vs.close();
		}
	}
}
