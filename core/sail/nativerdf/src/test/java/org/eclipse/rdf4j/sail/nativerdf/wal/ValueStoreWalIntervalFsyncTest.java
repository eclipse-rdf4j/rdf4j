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
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.nativerdf.ValueStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ValueStoreWalIntervalFsyncTest {

	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	@TempDir
	Path tempDir;

	@AfterEach
	void clearListener() {
		ValueStoreWalDebug.clearForceListener();
	}

	@Test
	void intervalForcesOnRotationAndCompression() throws Exception {
		Path walDir = tempDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		Files.createDirectories(walDir);

		List<Path> forced = new ArrayList<>();
		ValueStoreWalDebug.setForceListener(path -> {
			synchronized (forced) {
				forced.add(path);
			}
		});

		ValueStoreWalConfig config = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString())
				.maxSegmentBytes(2 * 1024)
				.batchBufferBytes(8 * 1024)
				.syncPolicy(ValueStoreWalConfig.SyncPolicy.INTERVAL)
				.syncInterval(Duration.ofHours(1))
				.build();

		Path valuesDir = tempDir.resolve("values");
		Files.createDirectories(valuesDir);

		try (ValueStoreWAL wal = ValueStoreWAL.open(config);
				ValueStore store = new ValueStore(valuesDir.toFile(), false, ValueStore.VALUE_CACHE_SIZE,
						ValueStore.VALUE_ID_CACHE_SIZE, ValueStore.NAMESPACE_CACHE_SIZE,
						ValueStore.NAMESPACE_ID_CACHE_SIZE, wal)) {

			Literal literal = VF.createLiteral(repeat('x', 8_192));
			store.storeValue(literal);
			OptionalLong pending = store.drainPendingWalHighWaterMark();
			assertThat(pending).isPresent();
			store.awaitWalDurable(pending.getAsLong());

			waitFor(() -> containsFileWithSuffix(walDir, ".v1.gz"));
		}

		waitFor(() -> containsForcedPath(forced, ".v1"));
		waitFor(() -> containsForcedPath(forced, ".v1.gz"));
	}

	private static boolean containsFileWithSuffix(Path dir, String suffix) {
		try {
			return Files.list(dir).anyMatch(path -> path.getFileName().toString().endsWith(suffix));
		} catch (IOException e) {
			return false;
		}
	}

	private static boolean containsForcedPath(List<Path> forced, String suffix) {
		synchronized (forced) {
			return forced.stream().anyMatch(path -> path.getFileName().toString().endsWith(suffix));
		}
	}

	private static void waitFor(BooleanSupplier condition) throws InterruptedException {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (System.nanoTime() < deadline) {
			if (condition.getAsBoolean()) {
				return;
			}
			Thread.sleep(10);
		}
		fail("condition not met before timeout");
	}

	private static String repeat(char ch, int count) {
		StringBuilder builder = new StringBuilder(count);
		for (int i = 0; i < count; i++) {
			builder.append(ch);
		}
		return builder.toString();
	}
}
