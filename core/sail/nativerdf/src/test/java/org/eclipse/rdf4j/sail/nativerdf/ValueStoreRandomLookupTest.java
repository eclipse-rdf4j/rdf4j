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

package org.eclipse.rdf4j.sail.nativerdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreConfig;
import org.eclipse.rdf4j.sail.nativerdf.datastore.DataStore;
import org.eclipse.rdf4j.sail.nativerdf.wal.ValueKind;
import org.eclipse.rdf4j.sail.nativerdf.wal.WalConfig;
import org.eclipse.rdf4j.sail.nativerdf.wal.WalReader;
import org.eclipse.rdf4j.sail.nativerdf.wal.WalRecord;
import org.eclipse.rdf4j.sail.nativerdf.wal.WalRecovery;
import org.eclipse.rdf4j.sail.nativerdf.wal.WalSearch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ValueStoreRandomLookupTest {

	@TempDir
	File dataDir;

	@Test
	void randomLookup50() throws Exception {
		// 1) Use NativeStore config to set WAL segment size to 1 MiB and load TTL via repository
		NativeStoreConfig cfg = new NativeStoreConfig("spoc,ospc,psoc");
		cfg.setWalMaxSegmentBytes(1 << 20);
		NativeStore store = (NativeStore) new org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreFactory().getSail(cfg);
		store.setDataDir(dataDir);
		SailRepository repository = new SailRepository(store);
		repository.init();
		try (SailRepositoryConnection connection = repository.getConnection()) {
			try (InputStream in = getClass().getClassLoader()
					.getResourceAsStream("benchmarkFiles/datagovbe-valid.ttl")) {
				assertThat(in).as("benchmarkFiles/datagovbe-valid.ttl should be on classpath").isNotNull();
				connection.add(in, "", RDFFormat.TURTLE);
			}
		}
		repository.shutDown();
		// Resolve WAL location + store UUID created by NativeStore
		Path walDir = dataDir.toPath().resolve("wal");
		String storeUuid = Files.readString(walDir.resolve("store.uuid"), StandardCharsets.UTF_8).trim();

		// 2) Open ValueStore and DataStore for ID bounds and lookup, and read WAL dictionary
		try (DataStore ds = new DataStore(dataDir, "values");
				ValueStore vs = new ValueStore(dataDir, false)) {

			int maxId = ds.getMaxID();
			assertThat(maxId).isGreaterThan(0);

			// Load WAL mapping id -> record (reader config need not set segment size)
			WalConfig walConfig = WalConfig.builder().walDirectory(walDir).storeUuid(storeUuid).build();
			WalRecovery recovery = new WalRecovery();
			java.util.Map<Integer, WalRecord> dict;
			try (WalReader reader = WalReader.open(walConfig)) {
				dict = recovery.replay(reader);
			}
			assertThat(dict).isNotEmpty();

			java.util.ArrayList<Integer> ids = new java.util.ArrayList<>();
			for (var e : dict.entrySet()) {
				ValueKind k = e.getValue().valueKind();
				if (k == ValueKind.IRI || k == ValueKind.BNODE || k == ValueKind.LITERAL) {
					ids.add(e.getKey());
				}
			}
			assertThat(ids).isNotEmpty();
			int found = 0;
			WalSearch search = WalSearch.open(walConfig);
			for (int i = 0; i < 50; i++) {
				int id = ids.get(ThreadLocalRandom.current().nextInt(ids.size()));
				// Sanity: within bounds of current store
				assertThat(id).isBetween(1, maxId);
				Value v = vs.getValue(id);
				Value w = search.findValueById(id);
				assertThat(v).as("value not null for id %s", id).isNotNull();
				assertThat(w).as("walsearch value not null for id %s", id).isNotNull();
				assertThat(v).as("value equals WalSearch for id %s", id).isEqualTo(w);
				found++;
			}

			assertThat(found).as("Should resolve 50 values by random ID lookups").isEqualTo(50);
		}

	}

	// WalSearch is used for WAL lookups in this test
}
