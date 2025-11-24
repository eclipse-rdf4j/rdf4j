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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.nativerdf.ValueStore;
import org.eclipse.rdf4j.sail.nativerdf.model.NativeValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ValueStoreWalClearPurgeTest {

	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	@TempDir
	Path tempDir;

	@Test
	void clearMustPurgeWalToPreventResurrection() throws Exception {
		Path walDir = tempDir.resolve("wal-clear");
		Files.createDirectories(walDir);

		ValueStoreWalConfig config = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString())
				.recoverValueStoreOnOpen(true)
				.build();

		IRI iri = VF.createIRI("http://example.com/resurrect-me");

		File valuesDir = tempDir.resolve("values-clear").toFile();
		Files.createDirectories(valuesDir.toPath());

		// Write a value and ensure it is durably logged in the WAL
		try (ValueStoreWAL wal = ValueStoreWAL.open(config);
				ValueStore store = new ValueStore(valuesDir, false,
						ValueStore.VALUE_CACHE_SIZE,
						ValueStore.VALUE_ID_CACHE_SIZE,
						ValueStore.NAMESPACE_CACHE_SIZE,
						ValueStore.NAMESPACE_ID_CACHE_SIZE,
						wal)) {
			store.storeValue(iri);
			OptionalLong lsn = store.drainPendingWalHighWaterMark();
			assertThat(lsn).isPresent();
			wal.awaitDurable(lsn.getAsLong());

			// Now clear the value store
			store.clear();
		}

		// Simulate restart with recovery enabled: if WAL was not purged on clear(),
		// recovery would resurrect the value into an otherwise empty store.
		try (ValueStoreWAL wal2 = ValueStoreWAL.open(config);
				ValueStore store2 = new ValueStore(valuesDir, false,
						ValueStore.VALUE_CACHE_SIZE,
						ValueStore.VALUE_ID_CACHE_SIZE,
						ValueStore.NAMESPACE_CACHE_SIZE,
						ValueStore.NAMESPACE_ID_CACHE_SIZE,
						wal2)) {
			int id = store2.getID(iri);
			assertThat(id)
					.as("After clear() the WAL must not resurrect deleted values upon recovery")
					.isEqualTo(NativeValue.UNKNOWN_ID);
		}
	}
}
