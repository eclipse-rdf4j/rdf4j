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
import java.util.List;
import java.util.UUID;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.sail.nativerdf.ValueStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ValueStoreWalReaderJacksonTest {

	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	@TempDir
	Path tempDir;

	@Test
	void scanReturnsMintedRecordsWithEscapes() throws Exception {
		Path walDir = tempDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		Files.createDirectories(walDir);
		ValueStoreWalConfig config = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString())
				.build();

		String specialText = "He said: \"Hello\\World\"\nNew line";
		try (ValueStoreWAL wal = ValueStoreWAL.open(config)) {
			File valueDir = tempDir.resolve("values").toFile();
			Files.createDirectories(valueDir.toPath());
			try (ValueStore store = new ValueStore(valueDir, false, ValueStore.VALUE_CACHE_SIZE,
					ValueStore.VALUE_ID_CACHE_SIZE, ValueStore.NAMESPACE_CACHE_SIZE,
					ValueStore.NAMESPACE_ID_CACHE_SIZE, wal)) {
				IRI iri = VF.createIRI("http://example.com/resource");
				Literal lit = VF.createLiteral(specialText, XMLSchema.STRING);
				store.storeValue(iri);
				store.storeValue(lit);

				var lsn = store.drainPendingWalHighWaterMark();
				assertThat(lsn).isPresent();
				wal.awaitDurable(lsn.getAsLong());
			}
		}

		try (ValueStoreWalReader reader = ValueStoreWalReader.open(config)) {
			ValueStoreWalReader.ScanResult scan = reader.scan();
			List<ValueStoreWalRecord> records = scan.records();
			assertThat(records).isNotEmpty();
			assertThat(records.stream()
					.anyMatch(r -> r.valueKind() == ValueStoreWalValueKind.IRI
							&& r.lexical().equals("http://example.com/resource")))
									.isTrue();
			assertThat(records.stream()
					.anyMatch(r -> r.valueKind() == ValueStoreWalValueKind.LITERAL
							&& r.lexical().equals(specialText)))
									.isTrue();
			assertThat(scan.lastValidLsn()).isGreaterThan(ValueStoreWAL.NO_LSN);
		}
	}
}
