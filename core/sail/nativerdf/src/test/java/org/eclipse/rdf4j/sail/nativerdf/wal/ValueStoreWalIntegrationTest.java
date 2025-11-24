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
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.sail.nativerdf.ValueStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ValueStoreWalIntegrationTest {

	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	@TempDir
	Path tempDir;

	@Test
	void purgeDropsQueuedFramesOnClear() throws Exception {
		Path walDir = tempDir.resolve("wal-purge");
		Files.createDirectories(walDir);
		ValueStoreWalConfig config = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString())
				// Default COMMIT policy: do not auto-flush unless forced
				.syncPolicy(ValueStoreWalConfig.SyncPolicy.COMMIT)
				.build();

		File valueDir = tempDir.resolve("values-purge").toFile();
		Files.createDirectories(valueDir.toPath());

		// Enqueue a single value and immediately clear() the store, which purges the WAL.
		try (ValueStoreWAL wal = ValueStoreWAL.open(config);
				ValueStore store = new ValueStore(valueDir, false, ValueStore.VALUE_CACHE_SIZE,
						ValueStore.VALUE_ID_CACHE_SIZE, ValueStore.NAMESPACE_CACHE_SIZE,
						ValueStore.NAMESPACE_ID_CACHE_SIZE, wal)) {
			store.storeValue(VF.createLiteral("to-be-dropped"));
			// Intentionally do not awaitDurable: the record remains queued/in-memory
			store.clear(); // triggers WAL purge

			// Now add a post-clear value and force durability. If the purge didn't drop queued frames,
			// the pre-clear value will be flushed together with this post-clear record.
			store.storeValue(VF.createLiteral("after-clear"));
			var lsn = store.drainPendingWalHighWaterMark();
			if (lsn.isPresent()) {
				wal.awaitDurable(lsn.getAsLong());
			}
		}

		// Give the background writer a brief window to act after purge.
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
		boolean hasMinted = false;
		while (System.nanoTime() < deadline) {
			try (ValueStoreWalReader reader = ValueStoreWalReader.open(config)) {
				var scan = reader.scan();
				hasMinted = scan.records().stream().anyMatch(r -> "to-be-dropped".equals(r.lexical()));
			}
			if (hasMinted) {
				break; // if bug exists, record may appear quickly
			}
			TimeUnit.MILLISECONDS.sleep(25);
		}

		// After purge, no pre-clear minted value must be recoverable from the WAL.
		assertThat(hasMinted).isFalse();
	}

	void logsMintedValueRecords() throws Exception {
		Path walDir = tempDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		Files.createDirectories(walDir);
		ValueStoreWalConfig config = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString())
				.build();

		try (ValueStoreWAL wal = ValueStoreWAL.open(config)) {
			File valueDir = tempDir.resolve("values").toFile();
			Files.createDirectories(valueDir.toPath());
			try (ValueStore store = new ValueStore(valueDir, false, ValueStore.VALUE_CACHE_SIZE,
					ValueStore.VALUE_ID_CACHE_SIZE, ValueStore.NAMESPACE_CACHE_SIZE,
					ValueStore.NAMESPACE_ID_CACHE_SIZE, wal)) {
				Literal literal = VF.createLiteral("hello");
				store.storeValue(literal);

				OptionalLong lsn = store.drainPendingWalHighWaterMark();
				assertThat(lsn).isPresent();

				wal.awaitDurable(lsn.getAsLong());
			}

			ValueStoreWalReader reader = ValueStoreWalReader.open(config);
			ValueStoreWalReader.ScanResult scan = reader.scan();
			reader.close();

			assertThat(scan.records()).hasSize(3);
			assertThat(scan.records())
					.anyMatch(record -> record.valueKind() == ValueStoreWalValueKind.NAMESPACE
							&& record.lexical().equals(XMLSchema.NAMESPACE));
			assertThat(scan.records())
					.anyMatch(record -> record.valueKind() == ValueStoreWalValueKind.IRI
							&& record.lexical().equals(XMLSchema.STRING.stringValue()));
			assertThat(scan.records())
					.anyMatch(record -> record.valueKind() == ValueStoreWalValueKind.LITERAL
							&& record.lexical().equals("hello")
							&& record.datatype().equals(XMLSchema.STRING.stringValue()));
		}
	}

	@Test
	void recoveryRebuildsMintedEntries() throws Exception {
		Path walDir = tempDir.resolve("wal2");
		Files.createDirectories(walDir);
		ValueStoreWalConfig config = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString())
				.build();

		Literal literal = VF.createLiteral("world", "en");
		IRI datatype = VF.createIRI("http://example.com/datatype");

		try (ValueStoreWAL wal = ValueStoreWAL.open(config)) {
			File valueDir = tempDir.resolve("values2").toFile();
			Files.createDirectories(valueDir.toPath());
			try (ValueStore store = new ValueStore(valueDir, false, ValueStore.VALUE_CACHE_SIZE,
					ValueStore.VALUE_ID_CACHE_SIZE, ValueStore.NAMESPACE_CACHE_SIZE,
					ValueStore.NAMESPACE_ID_CACHE_SIZE, wal)) {
				store.storeValue(literal);
				store.storeValue(VF.createIRI("http://example.com/resource"));
				store.storeValue(datatype);
				OptionalLong lsn = store.drainPendingWalHighWaterMark();
				assertThat(lsn).isPresent();
				wal.awaitDurable(lsn.getAsLong());
			}
		}

		try (ValueStoreWalReader reader = ValueStoreWalReader.open(config)) {
			ValueStoreWalRecovery recovery = new ValueStoreWalRecovery();
			Map<Integer, ValueStoreWalRecord> dictionary = recovery.replay(reader);
			assertThat(dictionary).isNotEmpty();
			assertThat(dictionary.values())
					.anyMatch(record -> record.valueKind() == ValueStoreWalValueKind.LITERAL
							&& record.lexical().equals("world"));
			assertThat(dictionary.values())
					.anyMatch(record -> record.valueKind() == ValueStoreWalValueKind.IRI
							&& record.lexical().equals("http://example.com/resource"));
		}
	}

	@Test
	void enablingWalOnPopulatedStoreRebuildsExistingEntries() throws Exception {
		Path valuesPath = tempDir.resolve("values-existing");
		Files.createDirectories(valuesPath);
		File valueDir = valuesPath.toFile();

		IRI existingIri = VF.createIRI("http://example.com/existing/one");
		Literal existingLiteral = VF.createLiteral("existing-literal", "en");

		try (ValueStore store = new ValueStore(valueDir, false, ValueStore.VALUE_CACHE_SIZE,
				ValueStore.VALUE_ID_CACHE_SIZE, ValueStore.NAMESPACE_CACHE_SIZE,
				ValueStore.NAMESPACE_ID_CACHE_SIZE, null)) {
			store.storeValue(existingIri);
			store.storeValue(existingLiteral);
		}

		Path walDir = tempDir.resolve("wal-existing");
		Files.createDirectories(walDir);
		ValueStoreWalConfig config = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString())
				.build();

		IRI newIri = VF.createIRI("http://example.com/new");

		try (ValueStoreWAL wal = ValueStoreWAL.open(config);
				ValueStore store = new ValueStore(valueDir, false, ValueStore.VALUE_CACHE_SIZE,
						ValueStore.VALUE_ID_CACHE_SIZE, ValueStore.NAMESPACE_CACHE_SIZE,
						ValueStore.NAMESPACE_ID_CACHE_SIZE, wal)) {

			store.storeValue(newIri);
			OptionalLong lsn = store.drainPendingWalHighWaterMark();
			if (lsn.isPresent()) {
				wal.awaitDurable(lsn.getAsLong());
			}

			ValueStoreWalRecovery recovery = new ValueStoreWalRecovery();
			Map<Integer, ValueStoreWalRecord> dictionary = Map.of();
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			boolean hasExistingIri = false;
			boolean hasExistingLiteral = false;
			while (System.nanoTime() < deadline && (!hasExistingIri || !hasExistingLiteral)) {
				try (ValueStoreWalReader reader = ValueStoreWalReader.open(config)) {
					dictionary = recovery.replay(reader);
				}
				hasExistingIri = dictionary.values()
						.stream()
						.anyMatch(record -> record.valueKind() == ValueStoreWalValueKind.IRI
								&& record.lexical().equals(existingIri.stringValue()));
				hasExistingLiteral = dictionary.values()
						.stream()
						.anyMatch(record -> record.valueKind() == ValueStoreWalValueKind.LITERAL
								&& record.lexical().equals(existingLiteral.getLabel())
								&& Objects.toString(record.language(), "")
										.equals(existingLiteral.getLanguage().orElse("")));
				if (!hasExistingIri || !hasExistingLiteral) {
					TimeUnit.MILLISECONDS.sleep(25);
				}
			}

			assertThat(hasExistingIri).isTrue();
			assertThat(hasExistingLiteral).isTrue();
			assertThat(dictionary.values())
					.anyMatch(record -> record.valueKind() == ValueStoreWalValueKind.IRI
							&& record.lexical().equals(newIri.stringValue()));
		}

		try (var stream = Files.list(walDir)) {
			assertThat(stream
					.filter(Files::isRegularFile)
					.map(path -> path.getFileName().toString())
					.filter(name -> name.startsWith("wal-")))
					.allMatch(name -> name.matches("wal-[1-9]\\d*\\.v1(?:\\.gz)?"));
		}
	}
}
