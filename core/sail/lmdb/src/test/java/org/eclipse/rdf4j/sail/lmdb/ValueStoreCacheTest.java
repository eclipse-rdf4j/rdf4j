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
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Focused test to prove that the ValueStore cache path is exercised by existing operations.
 */
class ValueStoreCacheTest {

	@Test
	void cachedValuePath(@TempDir File dataDir) throws Exception {
		LmdbStore store = new LmdbStore(dataDir);
		store.init();
		try {
			ValueFactory vf = store.getValueFactory();
			// ValueFactory is actually the package-private ValueStore
			ValueStore vs = (ValueStore) vf;

			IRI iri = vf.createIRI("urn:example:foo");
			long id = vs.getId(iri, true);

			// On lazy retrieval, the cache should not be touched
			LmdbValue v1 = vs.getLazyValue(id);
			assertNotNull(v1);
			LmdbValue cached1 = vs.cachedValue(id);
			assertNull(cached1);

			// After initializing the value, it should be cached
			assertEquals(v1.stringValue(), "urn:example:foo");
			LmdbValue cached2 = vs.cachedValue(id);
			assertSame(v1, cached2);

			// Direct cache hit
			LmdbValue v2 = vs.cachedValue(id);
			assertSame(v1, v2);
		} finally {
			store.shutDown();
		}
	}
}
