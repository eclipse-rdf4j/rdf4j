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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbLiteral;
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
		valueStore.startTransaction(true);
		for (int i = 0; i < values.length; i++) {
			values[i] = valueStore.createLiteral("This is a random literal:" + random.nextLong());
			valueStore.storeValue(values[i]);
		}
		valueStore.commit();

		ValueStoreRevision revBefore = valueStore.getRevision();

		valueStore.startTransaction(true);
		Set<Long> ids = new HashSet<>();
		for (int i = 0; i < 30; i++) {
			ids.add(values[i].getInternalID());
		}
		valueStore.gcIds(ids, new HashSet<>());
		valueStore.commit();

		ValueStoreRevision revAfter = valueStore.getRevision();

		assertNotEquals("revisions must change after gc of IDs", revBefore, revAfter);

		Arrays.fill(values, null);
		// GC would collect revision at some point in time
		// just add revision ID to free list for this test as forcing GC is not possible
		valueStore.unusedRevisionIds.add(revBefore.getRevisionId());

		valueStore.forceEvictionOfValues();
		valueStore.startTransaction(true);
		valueStore.commit();

		valueStore.startTransaction(true);
		for (int i = 0; i < 30; i++) {
			LmdbValue value = valueStore.createLiteral("This is a random literal:" + random.nextLong());
			values[i] = value;
			valueStore.storeValue(value);
			// this ID should have been reused
			ids.remove(value.getInternalID());
		}
		valueStore.commit();

		assertEquals("IDs should have been reused", Collections.emptySet(), ids);
	}

	@Test
	public void testGcValuesAfterRestart() throws Exception {
		Random random = new Random(1337);
		LmdbValue values[] = new LmdbValue[1000];
		valueStore.startTransaction(true);
		for (int i = 0; i < values.length; i++) {
			values[i] = valueStore.createLiteral("This is a random literal:" + random.nextLong());
			valueStore.storeValue(values[i]);
		}
		valueStore.commit();

		valueStore.startTransaction(true);
		Set<Long> ids = new HashSet<>();
		for (int i = 0; i < 30; i++) {
			ids.add(values[i].getInternalID());
		}
		valueStore.gcIds(ids, new HashSet<>());
		valueStore.commit();

		// close and recreate store
		valueStore.close();
		valueStore = createValueStore();

		valueStore.startTransaction(true);
		for (int i = 0; i < 30; i++) {
			LmdbValue value = valueStore.createLiteral("This is a random literal:" + random.nextLong());
			values[i] = value;
			valueStore.storeValue(value);
			// this ID should have been reused
			ids.remove(value.getInternalID());
		}
		valueStore.commit();

		assertEquals("IDs should have been reused", Collections.emptySet(), ids);
	}

	@Test
	public void testGcDatatypes() throws Exception {
		IRI[] types = new IRI[] { XSD.STRING, XSD.INTEGER, XSD.DOUBLE, XSD.DECIMAL, XSD.FLOAT };
		LmdbValue values[] = new LmdbValue[types.length];
		valueStore.startTransaction(true);
		for (int i = 0; i < values.length; i++) {
			values[i] = valueStore.createLiteral("123", types[i]);
			valueStore.storeValue(values[i]);
		}
		valueStore.commit();

		valueStore.startTransaction(true);
		List<Long> datatypeIds = new LinkedList<>();
		for (int i = 1; i < types.length; i++) {
			datatypeIds.add(valueStore.storeValue(types[i]));
		}
		valueStore.commit();

		valueStore.startTransaction(true);
		valueStore.gcIds(Collections.singleton(values[0].getInternalID()), new HashSet<>());
		valueStore.gcIds(datatypeIds, new HashSet<>());
		valueStore.commit();

		// close and recreate store
		valueStore.close();
		valueStore = createValueStore();

		assertNull(valueStore.getValue(values[0].getInternalID()));
		// the first datatype is not directly garbage collected and must not be
		// removed from the store if the related literal is removed
		assertNotNull(valueStore.getValue(datatypeIds.remove(0)));

		for (int i = 1; i < values.length; i++) {
			Value v = valueStore.getValue(values[i].getInternalID());
			IRI datatype = ((Literal) v).getDatatype();
			assertEquals(types[i], datatype);
			assertNotNull(valueStore.getValue(((LmdbValue) datatype).getInternalID()));
			datatypeIds.remove(((LmdbValue) datatype).getInternalID());
		}

		assertTrue("Datatype IDs should not have been deleted", datatypeIds.isEmpty());
	}

	@Test
	public void testGcURIs() throws Exception {
		for (boolean storeAndGcUri : List.of(false, true)) {
			valueStore.startTransaction(true);
			LmdbLiteral literal = valueStore.createLiteral("123", XSD.STRING);
			valueStore.storeValue(literal);
			if (storeAndGcUri) {
				valueStore.storeValue(XSD.STRING);
			}
			valueStore.commit();

			long typeId = valueStore.getId(XSD.STRING);
			assertTrue(typeId != 0);

			Set<Long> nextGcIds = new HashSet<>();
			valueStore.startTransaction(true);
			valueStore.gcIds(Collections.singleton(literal.getInternalID()), nextGcIds);
			assertEquals(1, nextGcIds.size());
			assertTrue(nextGcIds.contains(typeId));

			if (storeAndGcUri) {
				valueStore.gcIds(nextGcIds, new HashSet<>());
			}

			valueStore.commit();

			// close and recreate store
			valueStore.close();
			valueStore = createValueStore();

			assertNull(valueStore.getValue(literal.getInternalID()));
			if (!storeAndGcUri) {
				assertNotNull(valueStore.getValue(typeId));
			} else {
				assertNull(valueStore.getValue(typeId));
			}
		}
	}

	@AfterEach
	public void after() throws Exception {
		valueStore.close();
	}
}
