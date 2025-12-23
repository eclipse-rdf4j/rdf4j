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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.nativerdf.ValueStore;
import org.eclipse.rdf4j.sail.nativerdf.datastore.DataStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies that ValueStore resumes WAL bootstrap after a partial prior run (segments exist but no completion marker).
 */
public class ValueStoreWalBootstrapResumeTest {

	@TempDir
	Path tmp;

	@Test
	@Timeout(value = 5, unit = TimeUnit.MINUTES)
	void resumesBootstrapAfterPartialRun() throws Exception {
		// 1) Create a ValueStore with some existing values, without WAL
		Path data = tmp.resolve("data");
		Files.createDirectories(data);
		try (ValueStore vs = new ValueStore(data.toFile())) {
			// Create enough values to ensure bootstrap takes noticeable time
			for (int i = 0; i < 5000; i++) {
				IRI v = SimpleValueFactory.getInstance().createIRI("urn:test:" + i);
				vs.storeValue(v);
			}
			vs.sync();
		}

		// 2) Open with WAL enabled (async bootstrap), let it start and create at least one segment
		Path walDir = tmp.resolve("wal");
		Files.createDirectories(walDir);
		ValueStoreWalConfig cfg = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid("test-" + UUID.randomUUID())
				.syncPolicy(ValueStoreWalConfig.SyncPolicy.ALWAYS)
				.syncBootstrapOnOpen(false)
				.build();

		ValueStoreWAL wal = ValueStoreWAL.open(cfg);
		ValueStore vs2 = new ValueStore(data.toFile(), false,
				ValueStore.VALUE_CACHE_SIZE, ValueStore.VALUE_ID_CACHE_SIZE,
				ValueStore.NAMESPACE_CACHE_SIZE, ValueStore.NAMESPACE_ID_CACHE_SIZE,
				wal);

		// Simulate sudden crash by closing WAL directly almost immediately; bootstrap
		// thread will observe isClosed and stop early (partial run or none)
		Thread.sleep(5);
		wal.close();
		vs2.close();

		// 3) Reopen with WAL; after partial run, resume bootstrap and bring WAL dictionary
		// to cover all existing ValueStore IDs.
		int expectedMaxId;
		try (DataStore ds = new DataStore(data.toFile(), "values")) {
			expectedMaxId = ds.getMaxID();
		}

		try (ValueStoreWAL wal2 = ValueStoreWAL.open(cfg);
				ValueStore vs3 = new ValueStore(data.toFile(), false,
						ValueStore.VALUE_CACHE_SIZE, ValueStore.VALUE_ID_CACHE_SIZE,
						ValueStore.NAMESPACE_CACHE_SIZE, ValueStore.NAMESPACE_ID_CACHE_SIZE,
						wal2)) {

			waitUntil(() -> {
				try (ValueStoreWalReader reader = ValueStoreWalReader.open(cfg)) {
					ValueStoreWalRecovery.ReplayReport report = new ValueStoreWalRecovery().replayWithReport(reader);
					return report.dictionary().size() == expectedMaxId;
				}
			}, Duration.ofSeconds(120));
		}
	}

	private static void waitUntil(Condition cond, Duration timeout) throws Exception {
		long deadline = System.nanoTime() + timeout.toNanos();
		while (System.nanoTime() < deadline) {
			if (cond.ok())
				return;
			Thread.sleep(20);
		}
		// one last check before failing
		if (!cond.ok()) {
			throw new AssertionError("Condition not met within timeout: " + timeout);
		}
	}

	@FunctionalInterface
	private interface Condition {
		boolean ok() throws Exception;
	}
}
