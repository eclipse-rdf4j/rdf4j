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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ValueStoreWALNoopAndDoubleCloseTest {

	@TempDir
	Path tempDir;

	@Test
	void awaitDurableNoopForNoLsnAndClosedWal() throws Exception {
		ValueStoreWalConfig cfg = ValueStoreWalConfig.builder()
				.walDirectory(tempDir)
				.storeUuid(UUID.randomUUID().toString())
				.build();
		try (ValueStoreWAL wal = ValueStoreWAL.open(cfg)) {
			assertDoesNotThrow(() -> wal.awaitDurable(ValueStoreWAL.NO_LSN));
		}
		try (ValueStoreWAL wal = ValueStoreWAL.open(cfg)) {
			wal.close();
			assertDoesNotThrow(() -> wal.awaitDurable(123));
		}
	}

	@Test
	void closeIsIdempotent() throws Exception {
		ValueStoreWalConfig cfg = ValueStoreWalConfig.builder()
				.walDirectory(tempDir)
				.storeUuid(UUID.randomUUID().toString())
				.build();
		ValueStoreWAL wal = ValueStoreWAL.open(cfg);
		wal.close();
		assertDoesNotThrow(wal::close);
	}
}
