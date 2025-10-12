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

package org.eclipse.rdf4j.sail.nativerdf.wal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Random;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.sail.nativerdf.ValueStore;
import org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreConfig;
import org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ValueStoreWalSearchTest {

	@TempDir
	File dataDir;

	@Test
	void findsValueByIdViaSegmentProbe() throws Exception {
		// Configure NativeStore with small WAL segment size to ensure multiple segments possible
		NativeStoreConfig cfg = new NativeStoreConfig("spoc,ospc,psoc");
		cfg.setWalMaxSegmentBytes(64 * 1024); // 64 KiB
		NativeStore store = (NativeStore) new NativeStoreFactory().getSail(cfg);
		store.setDataDir(dataDir);
		SailRepository repo = new SailRepository(store);
		repo.init();
		try (SailRepositoryConnection conn = repo.getConnection()) {
			try (var in = getClass().getClassLoader().getResourceAsStream("benchmarkFiles/datagovbe-valid.ttl")) {
				assertThat(in).isNotNull();
				conn.add(in, "", RDFFormat.TURTLE);
			}
		}
		repo.shutDown();

		Path walDir = dataDir.toPath().resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		String storeUuid = Files.readString(walDir.resolve("store.uuid"), StandardCharsets.UTF_8).trim();
		ValueStoreWalConfig cfgRead = ValueStoreWalConfig.builder().walDirectory(walDir).storeUuid(storeUuid).build();

		// Build dictionary of minted values from WAL and pick a random entry
		Map<Integer, ValueStoreWalRecord> dict;
		try (ValueStoreWalReader reader = ValueStoreWalReader.open(cfgRead)) {
			dict = new ValueStoreWalRecovery().replay(reader);
		}
		assertThat(dict).isNotEmpty();
		Integer[] ids = dict.keySet().toArray(Integer[]::new);
		Integer pickId = ids[new Random().nextInt(ids.length)];

		ValueStoreWalSearch search = ValueStoreWalSearch.open(cfgRead);
		Value found = search.findValueById(pickId);
		assertThat(found).as("ValueStoreWalSearch should find value by id").isNotNull();

		// Cross-check against ValueStore
		try (ValueStore vs = new ValueStore(dataDir, false)) {
			Value vsValue = vs.getValue(pickId);
			assertThat(vsValue).isNotNull();
			assertThat(found).isEqualTo(vsValue);
		}
	}
}
