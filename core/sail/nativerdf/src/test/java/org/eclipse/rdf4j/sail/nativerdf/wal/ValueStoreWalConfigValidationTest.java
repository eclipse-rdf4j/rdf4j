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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ValueStoreWalConfigValidationTest {

	@TempDir
	Path tempDir;

	@Test
	void requiresWalDirectory() {
		ValueStoreWalConfig.Builder b = ValueStoreWalConfig.builder().storeUuid(UUID.randomUUID().toString());
		assertThatThrownBy(b::build).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("walDirectory");
	}

	@Test
	void requiresStoreUuid() {
		ValueStoreWalConfig.Builder b = ValueStoreWalConfig.builder().walDirectory(tempDir);
		assertThatThrownBy(b::build).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("storeUuid");
	}

	@Test
	void validatesPositiveSizes() {
		// maxSegmentBytes must be > 0
		ValueStoreWalConfig.Builder base1 = ValueStoreWalConfig.builder()
				.walDirectory(tempDir)
				.storeUuid(UUID.randomUUID().toString());
		assertThatThrownBy(() -> base1.maxSegmentBytes(0).build())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("maxSegmentBytes");

		// queueCapacity must be > 0
		ValueStoreWalConfig.Builder base2 = ValueStoreWalConfig.builder()
				.walDirectory(tempDir)
				.storeUuid(UUID.randomUUID().toString());
		assertThatThrownBy(() -> base2.queueCapacity(0).build())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("queueCapacity");

		// batchBufferBytes must be > 4KB
		ValueStoreWalConfig.Builder base3 = ValueStoreWalConfig.builder()
				.walDirectory(tempDir)
				.storeUuid(UUID.randomUUID().toString());
		assertThatThrownBy(() -> base3.batchBufferBytes(4096).build())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("batchBufferBytes");
	}
}
