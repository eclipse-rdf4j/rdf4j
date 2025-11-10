/**
 * ******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * ******************************************************************************
 */
package org.eclipse.rdf4j.sail.lmdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.Set;

import org.eclipse.rdf4j.common.order.StatementOrder;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for LMDB supported statement orders, used by merge-join optimization.
 */
public class LmdbSupportedOrdersTest {

	private File dataDir;
	private LmdbStore store;

	@BeforeEach
	public void setup() throws Exception {
		dataDir = Files.createTempDirectory("rdf4j-lmdb-supported-orders-test").toFile();
		store = new LmdbStore(dataDir, new LmdbStoreConfig("spoc,posc"));
		// Force initialization so that backing store is available
		try (NotifyingSailConnection ignored = store.getConnection()) {
			// no-op
		}
	}

	@AfterEach
	public void tearDown() {
		if (store != null) {
			store.shutDown();
		}
		if (dataDir != null) {
			deleteRecursively(dataDir);
		}
	}

	private static void deleteRecursively(File f) {
		if (f.isDirectory()) {
			File[] files = f.listFiles();
			if (files != null) {
				for (File c : files) {
					deleteRecursively(c);
				}
			}
		}
		// ignore result; best-effort cleanup
		f.delete();
	}

	@Test
	public void noBindings_defaultIndexes_supports_S_and_P() throws Exception {
		LmdbSailStore internal = getBackingStore(store);
		try (var dataset = internal.getExplicitSailSource().dataset(IsolationLevels.NONE)) {
			Set<StatementOrder> supported = dataset.getSupportedOrders(null, null, null);
			assertThat(supported).isEqualTo(EnumSet.of(StatementOrder.S, StatementOrder.P));
		}
	}

	@Test
	public void predicateBound_supports_P_and_O() throws Exception {
		LmdbSailStore internal = getBackingStore(store);
		try (var dataset = internal.getExplicitSailSource().dataset(IsolationLevels.NONE)) {
			Set<StatementOrder> supported = dataset.getSupportedOrders(null, RDFS.LABEL, null);
			assertThat(supported).isEqualTo(EnumSet.of(StatementOrder.P, StatementOrder.O));
		}
	}

	@Test
	public void subjectAndPredicateBound_supports_S_P_O() throws Exception {
		IRI subj = Values.iri("urn:s");
		LmdbSailStore internal = getBackingStore(store);
		try (var dataset = internal.getExplicitSailSource().dataset(IsolationLevels.NONE)) {
			Set<StatementOrder> supported = dataset.getSupportedOrders((Resource) subj, RDFS.LABEL, (Value) null);
			assertThat(supported).isEqualTo(EnumSet.of(StatementOrder.S, StatementOrder.P, StatementOrder.O));
		}
	}

	private static LmdbSailStore getBackingStore(LmdbStore store) throws Exception {
		var field = LmdbStore.class.getDeclaredField("backingStore");
		field.setAccessible(true);
		return (LmdbSailStore) field.get(store);
	}
}
