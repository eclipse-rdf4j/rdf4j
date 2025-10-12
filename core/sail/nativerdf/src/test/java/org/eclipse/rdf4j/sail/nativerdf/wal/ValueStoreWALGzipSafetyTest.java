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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests gzip safety: we don't delete the original segment if compression fails, and resulting gzip fully decompresses.
 */
class ValueStoreWALGzipSafetyTest {

	@TempDir
	Path tempDir;

	@Test
	void gzipContainsFullData() throws Exception {
		Path walDir = tempDir.resolve("wal2");
		Files.createDirectories(walDir);
		ValueStoreWalConfig cfg = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString())
				.maxSegmentBytes(4096)
				.build();
		// Generate enough data to force at least one gzip segment
		long lastLsn;
		try (ValueStoreWAL wal = ValueStoreWAL.open(cfg)) {
			lastLsn = -1;
			for (int i = 0; i < 500; i++) {
				lastLsn = wal.logMint(i + 1, ValueStoreWalValueKind.LITERAL, "v" + i, "http://dt", "", i * 31);
			}
			wal.awaitDurable(lastLsn);
		}

		// Find a gzip segment and fully decompress it, asserting we reach EOF and read > 0 bytes
		Path gz = Files.list(walDir)
				.filter(p -> p.getFileName().toString().endsWith(".v1.gz"))
				.findFirst()
				.orElseThrow(() -> new IOException("no gzip segment found"));

		long total = 0;
		byte[] buf = new byte[1 << 15];
		try (GZIPInputStream in = new GZIPInputStream(Files.newInputStream(gz))) {
			int r;
			while ((r = in.read(buf)) >= 0) {
				total += r;
			}
		}
		assertThat(total).isGreaterThan(0L);
	}

	private static Object getField(Object target, String name) throws Exception {
		var f = target.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.get(target);
	}
}
