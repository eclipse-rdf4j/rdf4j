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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalLong;
import java.util.UUID;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.nativerdf.ValueStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ValueStoreWalLargeRecordTest {

	@TempDir
	Path tempDir;

	@Test
	void logsLargeLiteralExceedingBuffer() throws Exception {
		// Create a WAL with default config (1 MiB batch buffer)
		Path walDir = tempDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		Files.createDirectories(walDir);
		ValueStoreWalConfig config = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString())
				.build();

		// Build a ~128 MiB ASCII literal (bytes == chars)
		int sizeBytes = 128 * 1024 * 1024; // 128 MiB
		String large = "a".repeat(sizeBytes);
		Literal largeLiteral = SimpleValueFactory.getInstance().createLiteral(large);

		try (ValueStoreWAL wal = ValueStoreWAL.open(config)) {
			File valueDir = tempDir.resolve("values").toFile();
			Files.createDirectories(valueDir.toPath());
			try (ValueStore store = new ValueStore(valueDir, false, ValueStore.VALUE_CACHE_SIZE,
					ValueStore.VALUE_ID_CACHE_SIZE, ValueStore.NAMESPACE_CACHE_SIZE,
					ValueStore.NAMESPACE_ID_CACHE_SIZE, wal)) {
				// Store the large literal and wait for durability
				store.storeValue(largeLiteral);
				OptionalLong lsn = store.drainPendingWalHighWaterMark();
				assertThat(lsn).isPresent();

				// This currently fails due to BufferOverflowException in the writer thread
				wal.awaitDurable(lsn.getAsLong());
			}
		}

		// Sanity: ensure scan can see the record and its size matches
		try (ValueStoreWalReader reader = ValueStoreWalReader.open(config)) {
			ValueStoreWalReader.ScanResult scan = reader.scan();
			assertThat(scan.records()).anyMatch(r -> r.valueKind() == ValueStoreWalValueKind.LITERAL
					&& r.lexical().length() == sizeBytes);
		}
	}

	@Test
	void logsLargeLiteralWithSmallSegmentLimit() throws Exception {
		Path walDir = tempDir.resolve("wal-small");
		Files.createDirectories(walDir);
		ValueStoreWalConfig config = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString())
				.maxSegmentBytes(32 * 1024)
				.build();

		int sizeBytes = 50 * 1024; // 50 KiB > segment limit
		String large = "b".repeat(sizeBytes);
		Literal literal = SimpleValueFactory.getInstance().createLiteral(large);

		try (ValueStoreWAL wal = ValueStoreWAL.open(config)) {
			File valueDir = tempDir.resolve("values-small").toFile();
			Files.createDirectories(valueDir.toPath());
			try (ValueStore store = new ValueStore(valueDir, false, ValueStore.VALUE_CACHE_SIZE,
					ValueStore.VALUE_ID_CACHE_SIZE, ValueStore.NAMESPACE_CACHE_SIZE,
					ValueStore.NAMESPACE_ID_CACHE_SIZE, wal)) {
				store.storeValue(literal);
				OptionalLong lsn = store.drainPendingWalHighWaterMark();
				assertThat(lsn).isPresent();
				wal.awaitDurable(lsn.getAsLong());
			}
		}

		try (ValueStoreWalReader reader = ValueStoreWalReader.open(config)) {
			ValueStoreWalReader.ScanResult scan = reader.scan();
			assertThat(scan.records())
					.anyMatch(r -> r.valueKind() == ValueStoreWalValueKind.LITERAL && r.lexical().equals(large));
		}

		ValueStoreWalSearch search = ValueStoreWalSearch.open(config);
		ValueStoreWalValueKind[] foundKind = new ValueStoreWalValueKind[1];
		try (ValueStoreWalReader reader = ValueStoreWalReader.open(config)) {
			for (ValueStoreWalRecord rec : reader.scan().records()) {
				if (rec.lexical().equals(large)) {
					foundKind[0] = rec.valueKind();
					break;
				}
			}
		}
		assertThat(foundKind[0]).isEqualTo(ValueStoreWalValueKind.LITERAL);
	}
}
