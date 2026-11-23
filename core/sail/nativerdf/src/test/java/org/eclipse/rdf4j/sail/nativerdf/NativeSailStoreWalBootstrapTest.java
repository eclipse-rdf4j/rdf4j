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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.nativerdf.wal.ValueStoreWalConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("slow")
class NativeSailStoreWalBootstrapTest {

	@TempDir
	Path tempDir;

	@Test
	void enablingWalOnNonEmptyValueStoreRebuildsWal() throws Exception {
		try (ValueStore store = new ValueStore(tempDir.toFile(), false)) {
			store.storeValue(SimpleValueFactory.getInstance().createIRI("http://example.com/existing"));
		}

		NativeStore nativeStore = new NativeStore(tempDir.toFile());
		try {
			nativeStore.init();
		} finally {
			nativeStore.shutDown();
		}

		Path walDir = tempDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
//		Path marker = walDir.resolve("bootstrap.info");
//		assertThat(Files.exists(marker)).isTrue();
//		String markerContent = Files.readString(marker, StandardCharsets.UTF_8);
//		assertThat(markerContent).contains("enabled-rebuild-existing-values");
		try (var stream = Files.list(walDir)) {
			List<String> segments = stream
					.filter(p -> p.getFileName().toString().startsWith("wal-"))
					.map(p -> p.getFileName().toString())
					.collect(Collectors.toList());
			assertThat(segments).isNotEmpty();
			assertThat(segments).allMatch(name -> name.matches("wal-[1-9]\\d*\\.v1(?:\\.gz)?"));
		}
	}

	@Test
	void opensAfterInterruptedDictionaryInstallBeforeCreatingWal() throws Exception {
		var vf = SimpleValueFactory.getInstance();
		try (ValueStore live = new ValueStore(tempDir.toFile())) {
			live.storeValue(vf.createBNode("old-generation"));
		}
		Path staging = Files.createDirectory(tempDir.resolve("values-recovery"));
		int restoredId;
		try (ValueStore staged = new ValueStore(staging.toFile())) {
			staged.storeValue(vf.createBNode("first-in-replacement"));
			restoredId = staged.storeValue(vf.createBNode("second-in-replacement"));
		}
		Files.createFile(staging.resolve("ready"));
		Files.copy(staging.resolve("values.dat"), tempDir.resolve("values.dat"),
				StandardCopyOption.REPLACE_EXISTING);
		Path walDir = tempDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		assertThat(walDir).doesNotExist();

		NativeStore store = new NativeStore(tempDir.toFile());
		store.setWalSyncBootstrapOnOpen(true);
		try {
			store.init();
			ValueStore dictionary = (ValueStore) store.getValueFactory();
			assertThat(dictionary.getValue(restoredId)).isEqualTo(vf.createBNode("second-in-replacement"));
			assertThat(staging).doesNotExist();
		} finally {
			store.shutDown();
		}
		assertThat(walDir).isDirectory();
	}
}
