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

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Basic sanity for back-to-back awaitDurable calls. This does not attempt to deterministically reproduce the race but
 * ensures that in normal use two sequential awaits complete promptly.
 */
class ValueStoreWALRetainPendingForceTest {

	@TempDir
	Path tempDir;

	@Test
	void backToBackAwaitDoesNotHang() throws Exception {
		var walDir = tempDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		Files.createDirectories(walDir);

		ValueStoreWalConfig cfg = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString())
				.syncPolicy(ValueStoreWalConfig.SyncPolicy.COMMIT)
				.build();

		try (ValueStoreWAL wal = ValueStoreWAL.open(cfg)) {
			long lsn1 = wal.logMint(1, ValueStoreWalValueKind.LITERAL, "x", "http://dt", "", 123);
			waitUntilLastAppendedAtLeast(wal, lsn1);

			CompletableFuture<Void> first = CompletableFuture.runAsync(() -> {
				try {
					wal.awaitDurable(lsn1);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});

			long lsn2 = wal.logMint(2, ValueStoreWalValueKind.LITERAL, "y", "http://dt", "", 456);

			CompletableFuture<Void> second = CompletableFuture.runAsync(() -> {
				try {
					wal.awaitDurable(lsn2);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});

			CompletableFuture.allOf(first, second).orTimeout(5, TimeUnit.SECONDS).join();
		}
	}

	private static void waitUntilLastAppendedAtLeast(ValueStoreWAL wal, long targetLsn) throws Exception {
		Field f = ValueStoreWAL.class.getDeclaredField("lastAppendedLsn");
		f.setAccessible(true);
		AtomicLong lastAppended = (AtomicLong) f.get(wal);
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (System.nanoTime() < deadline) {
			if (lastAppended.get() >= targetLsn) {
				return;
			}
			Thread.sleep(1);
		}
		throw new AssertionError("writer thread did not append record in time");
	}
}
