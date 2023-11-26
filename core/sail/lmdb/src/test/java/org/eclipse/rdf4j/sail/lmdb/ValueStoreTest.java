/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Low-level tests for {@link ValueStore}.
 */
public class ValueStoreTest {

	private ValueStore valueStore;
	private File dataDir;

	@BeforeEach
	public void before(@TempDir File dataDir) throws Exception {
		this.dataDir = dataDir;
		this.valueStore = createValueStore();
	}

	private ValueStore createValueStore() throws IOException {
		return new ValueStore(new File(dataDir, "values"), new LmdbStoreConfig());
	}

	@Test
	public void testGcValues() throws Exception {
		Random random = new Random(1337);
		LmdbValue values[] = new LmdbValue[1000];
		valueStore.startTransaction();
		for (int i = 0; i < values.length; i++) {
			values[i] = valueStore.createLiteral("This is a random literal:" + random.nextLong());
			valueStore.storeValue(values[i]);
		}
		valueStore.commit();

		ValueStoreRevision revBefore = valueStore.getRevision();

		valueStore.startTransaction();
		Set<Long> ids = new HashSet<>();
		for (int i = 0; i < 30; i++) {
			ids.add(values[i].getInternalID());
		}
		valueStore.gcIds(ids);
		valueStore.commit();

		ValueStoreRevision revAfter = valueStore.getRevision();

		assertNotEquals(revBefore, revAfter, "revisions must change after gc of IDs");

		Arrays.fill(values, null);
		// GC would collect revision at some point in time
		// just add revision ID to free list for this test as forcing GC is not possible
		valueStore.unusedRevisionIds.add(revBefore.getRevisionId());

		valueStore.forceEvictionOfValues();
		valueStore.startTransaction();
		valueStore.commit();

		valueStore.startTransaction();
		for (int i = 0; i < 30; i++) {
			LmdbValue value = valueStore.createLiteral("This is a random literal:" + random.nextLong());
			values[i] = value;
			valueStore.storeValue(value);
			// this ID should have been reused
			ids.remove(value.getInternalID());
		}
		valueStore.commit();

		assertEquals(Collections.emptySet(), ids, "IDs should have been reused");
	}

	@Test
	public void testGcValuesAfterRestart() throws Exception {
		Random random = new Random(1337);
		LmdbValue values[] = new LmdbValue[1000];
		valueStore.startTransaction();
		for (int i = 0; i < values.length; i++) {
			values[i] = valueStore.createLiteral("This is a random literal:" + random.nextLong());
			valueStore.storeValue(values[i]);
		}
		valueStore.commit();

		valueStore.startTransaction();
		Set<Long> ids = new HashSet<>();
		for (int i = 0; i < 30; i++) {
			ids.add(values[i].getInternalID());
		}
		valueStore.gcIds(ids);
		valueStore.commit();

		// close and recreate store
		valueStore.close();
		valueStore = createValueStore();

		valueStore.startTransaction();
		for (int i = 0; i < 30; i++) {
			LmdbValue value = valueStore.createLiteral("This is a random literal:" + random.nextLong());
			values[i] = value;
			valueStore.storeValue(value);
			// this ID should have been reused
			ids.remove(value.getInternalID());
		}
		valueStore.commit();

		assertEquals(Collections.emptySet(), ids, "IDs should have been reused");
	}

	@AfterEach
	public void after() throws Exception {
		valueStore.close();
	}
}
