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

package org.eclipse.rdf4j.sail.nativerdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NativeSailStoreWalBootstrapTest {

	@TempDir
	Path tempDir;

	@Test
	void enablingWalOnNonEmptyValueStoreSkipsWal() throws Exception {
		try (ValueStore store = new ValueStore(tempDir.toFile(), false)) {
			store.storeValue(SimpleValueFactory.getInstance().createIRI("http://example.com/existing"));
		}

		NativeStore nativeStore = new NativeStore(tempDir.toFile());
		try {
			nativeStore.init();
		} finally {
			nativeStore.shutDown();
		}

		Path walDir = tempDir.resolve("wal");
		Path marker = walDir.resolve("bootstrap.info");
		assertThat(Files.exists(marker)).isTrue();
		String markerContent = Files.readString(marker, StandardCharsets.UTF_8);
		assertThat(markerContent).contains("disabled-existing-values");
		try (var stream = Files.list(walDir)) {
			assertThat(stream.filter(p -> p.getFileName().toString().startsWith("wal-")).findAny()).isEmpty();
		}
	}
}
