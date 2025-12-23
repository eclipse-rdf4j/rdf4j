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
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.CRC32C;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.nativerdf.ValueStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * Reproduces incorrect WAL hash computation (CRC32C state reuse across calls).
 */
class ValueStoreWalHashTest {

	@TempDir
	Path tempDir;

	@Test
	@Timeout(10)
	void walHashesMatchFreshCrcForEachRecord() throws Exception {
		// Arrange: temp data dir and WAL config
		Path dataDir = tempDir.resolve("store");
		Path walDir = tempDir.resolve("wal");
		dataDir.toFile().mkdirs();
		walDir.toFile().mkdirs();

		String storeUuid = UUID.randomUUID().toString();
		ValueStoreWalConfig config = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(storeUuid)
				.build();
		ValueStoreWAL wal = ValueStoreWAL.open(config);

		try (ValueStore vs = new ValueStore(new File(dataDir.toString()), false,
				ValueStore.VALUE_CACHE_SIZE, ValueStore.VALUE_ID_CACHE_SIZE,
				ValueStore.NAMESPACE_CACHE_SIZE, ValueStore.NAMESPACE_ID_CACHE_SIZE, wal)) {

			IRI a = SimpleValueFactory.getInstance().createIRI("http://example.org/a");
			IRI b = SimpleValueFactory.getInstance().createIRI("http://example.org/b");

			// Act: mint two values in sequence on the same thread
			int idA = vs.storeValue(a);
			int idB = vs.storeValue(b);
			assertThat(idA).isGreaterThan(0);
			assertThat(idB).isGreaterThan(0);
		}

		// Ensure WAL is fully flushed and closed
		wal.close();

		// Assert: read back WAL and verify each record's hash equals a fresh CRC of its own fields
		try (ValueStoreWalReader reader = ValueStoreWalReader.open(config)) {
			var it = reader.iterator();
			int seen = 0;
			while (it.hasNext()) {
				ValueStoreWalRecord r = it.next();
				int expected = freshCrc32c(r.valueKind(), r.lexical(), r.datatype(), r.language());
				// This assertion will fail on the second record with the buggy implementation
				assertThat(r.hash())
						.as("hash should equal CRC32C(kind,lex,dt,lang) for id=" + r.id())
						.isEqualTo(expected);
				seen++;
			}
			assertThat(seen).isGreaterThanOrEqualTo(2);
		}
	}

	private static int freshCrc32c(ValueStoreWalValueKind kind, String lexical, String datatype, String language) {
		CRC32C crc32c = new CRC32C();
		crc32c.update((byte) kind.code());
		update(crc32c, lexical);
		crc32c.update((byte) 0);
		update(crc32c, datatype);
		crc32c.update((byte) 0);
		update(crc32c, language);
		return (int) crc32c.getValue();
	}

	private static void update(CRC32C crc32c, String value) {
		if (value == null || value.isEmpty()) {
			return;
		}
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		crc32c.update(bytes, 0, bytes.length);
	}
}
