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
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ValueStoreWALMonotonicLsnTest {

	@TempDir
	File tempDir;

	@Test
	void lsnMonotonicAcrossRestart() throws Exception {
		Path walDir = tempDir.toPath().resolve("wal");

		ValueStoreWalConfig config = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid("test-store-uuid")
				.build();

		long firstLsn;
		try (ValueStoreWAL wal = ValueStoreWAL.open(config)) {
			firstLsn = wal.logMint(1, ValueStoreWalValueKind.IRI, "lex", "dt", "en", 123);
			wal.awaitDurable(firstLsn);
		}

		long secondLsn;
		try (ValueStoreWAL wal = ValueStoreWAL.open(config)) {
			secondLsn = wal.logMint(2, ValueStoreWalValueKind.IRI, "lex2", "dt", "en", 456);
		}

		assertThat(secondLsn)
				.as("WAL LSN must be strictly increasing across restarts")
				.isGreaterThan(firstLsn);
	}
}
