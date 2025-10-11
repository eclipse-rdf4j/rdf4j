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
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Ensures WAL segment numbering remains monotonic across restarts by including gzipped segments when determining the
 * next segment sequence.
 */
class ValueStoreWALMonotonicSegmentTest {

	private static final Pattern SEGMENT_GZ = Pattern.compile("wal-(\\d+)\\.v1\\.gz");

	@TempDir
	Path tempDir;

	@Test
	void segmentNumberingMonotonicAcrossRestart() throws Exception {
		Path walDir = tempDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		Files.createDirectories(walDir);

		ValueStoreWalConfig cfg = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString())
				.maxSegmentBytes(4096) // small to force rotation and gzip
				.build();

		// 1) Start WAL and generate enough records to produce at least one compressed segment
		int minted = 200;
		long lastLsn;
		try (ValueStoreWAL wal = ValueStoreWAL.open(cfg)) {
			lastLsn = mintMany(wal, minted);
			wal.awaitDurable(lastLsn);
		}

		int beforeMax = maxCompressedSeq(walDir);
		assertThat(beforeMax).withFailMessage("Expected at least one gzipped segment after initial rotation")
				.isGreaterThanOrEqualTo(1);

		// Ensure there are NO bare segments left before restart, simulating an environment
		// where only gzipped segments are present on startup
		compressAllBareSegments(walDir);

		// 2) Restart WAL; on open it creates the next bare segment immediately
		int expectedNext = maxCompressedSeq(walDir) + 1;
		try (ValueStoreWAL wal = ValueStoreWAL.open(cfg)) {
			long lsn = wal.logMint(minted + 1, ValueStoreWalValueKind.LITERAL, "restart", "http://example/dt", "", 17);
			wal.awaitDurable(lsn);
		}

		int openedSeq = currentBareSegmentSeq(walDir);
		// The newly opened bare segment must be numbered after the max compressed sequence
		// If gz files are ignored when scanning, numbering restarts at 1
		assertThat(openedSeq).isEqualTo(expectedNext);
	}

	private static long mintMany(ValueStoreWAL wal, int count) throws IOException {
		long lsn = -1;
		for (int i = 0; i < count; i++) {
			// Minimal payload; IDs and hashes vary to avoid identical frames
			lsn = wal.logMint(i + 1, ValueStoreWalValueKind.LITERAL, "lex-" + i, "http://example/dt", "", 31 * i);
		}
		return lsn;
	}

	private static int maxCompressedSeq(Path walDir) throws IOException {
		int max = 0;
		try (var stream = Files.list(walDir)) {
			for (Path path : (Iterable<Path>) stream::iterator) {
				if (SEGMENT_GZ.matcher(path.getFileName().toString()).matches()) {
					int seq = ValueStoreWalTestUtils.readSegmentSequence(path);
					if (seq > max) {
						max = seq;
					}
				}
			}
		}
		return max;
	}

	private static void compressAllBareSegments(Path walDir) throws IOException {
		try (var stream = Files.list(walDir)) {
			for (Path p : (Iterable<Path>) stream::iterator) {
				String name = p.getFileName().toString();
				if (name.startsWith("wal-") && name.endsWith(".v1")) {
					Path gz = p.resolveSibling(name + ".gz");
					try (var in = Files.newInputStream(p);
							var out = new GZIPOutputStream(Files.newOutputStream(gz))) {
						byte[] buf = new byte[1 << 16];
						int r;
						while ((r = in.read(buf)) >= 0) {
							out.write(buf, 0, r);
						}
						out.finish();
					}
					Files.deleteIfExists(p);
				}
			}
		}
	}

	private static int currentBareSegmentSeq(Path walDir) throws IOException {
		int seq = 0;
		try (var stream = Files.list(walDir)) {
			for (Path p : (Iterable<Path>) stream::iterator) {
				String name = p.getFileName().toString();
				if (name.startsWith("wal-") && name.endsWith(".v1")) {
					int current = ValueStoreWalTestUtils.readSegmentSequence(p);
					if (current > seq) {
						seq = current;
					}
				}
			}
		}
		return seq;
	}
}
