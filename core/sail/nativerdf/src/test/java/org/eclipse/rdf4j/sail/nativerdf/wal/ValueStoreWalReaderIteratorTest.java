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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.nativerdf.ValueStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for a streaming/iterator-style ValueStoreWalReader API that yields one record at a time in order.
 */
class ValueStoreWalReaderIteratorTest {

	@TempDir
	Path tempDir;

	@Test
	void iteratesRecordsInOrderAndMatchesScan() throws Exception {
		Path walDir = tempDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		Files.createDirectories(walDir);
		ValueStoreWalConfig config = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString())
				.build();

		// Write a few values to generate WAL records
		try (ValueStoreWAL wal = ValueStoreWAL.open(config)) {
			Path valuesDir = tempDir.resolve("values");
			Files.createDirectories(valuesDir);
			try (ValueStore store = new ValueStore(
					valuesDir.toFile(), false,
					ValueStore.VALUE_CACHE_SIZE, ValueStore.VALUE_ID_CACHE_SIZE,
					ValueStore.NAMESPACE_CACHE_SIZE, ValueStore.NAMESPACE_ID_CACHE_SIZE, wal)) {
				store.storeValue(SimpleValueFactory.getInstance().createLiteral("r1"));
				store.storeValue(SimpleValueFactory.getInstance().createIRI("http://ex/r2"));
				store.storeValue(SimpleValueFactory.getInstance().createLiteral("r3", "en"));
				OptionalLong lsn = store.drainPendingWalHighWaterMark();
				assertThat(lsn).isPresent();
				wal.awaitDurable(lsn.getAsLong());
			}
		}

		// Existing API for comparison
		List<ValueStoreWalRecord> scanned;
		long lastValidLsn;
		try (ValueStoreWalReader reader = ValueStoreWalReader.open(config)) {
			ValueStoreWalReader.ScanResult res = reader.scan();
			scanned = res.records();
			lastValidLsn = res.lastValidLsn();
		}

		// New iterator API (to be implemented): iterate without preloading all
		List<ValueStoreWalRecord> iterated = new ArrayList<>();
		long iterLast = ValueStoreWAL.NO_LSN;
		try (ValueStoreWalReader reader = ValueStoreWalReader.open(config)) {
			Iterator<ValueStoreWalRecord> it = reader.iterator(); // expected new API
			while (it.hasNext()) {
				ValueStoreWalRecord r = it.next();
				iterated.add(r);
				if (r.lsn() > iterLast) {
					iterLast = r.lsn();
				}
			}
			// After iteration, lastValidLsn() should reflect last good record
			assertThat(reader.lastValidLsn()).isEqualTo(iterLast);
		}

		assertThat(iterated).usingRecursiveComparison().isEqualTo(scanned);
		assertThat(iterLast).isEqualTo(lastValidLsn);
	}
}
