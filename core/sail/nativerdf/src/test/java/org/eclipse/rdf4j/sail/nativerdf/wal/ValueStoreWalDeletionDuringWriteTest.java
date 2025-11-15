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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.nativerdf.ValueStore;
import org.eclipse.rdf4j.sail.nativerdf.datastore.DataStore;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ValueStoreWalDeletionDuringWriteTest {

	private static final Pattern SEGMENT_PATTERN = Pattern.compile("wal-(\\d{8})\\.v1");

	@TempDir
	Path tempDir;

	@Test
	void asyncWalContinuesAfterCurrentSegmentDeletion() throws Exception {
		Path walDir = tempDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		ValueStoreWalConfig config = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString())
				.maxSegmentBytes(1 << 12)
				.syncPolicy(ValueStoreWalConfig.SyncPolicy.COMMIT)
				.build();

		Path valuesDir = tempDir.resolve("values");
		Files.createDirectories(valuesDir);

		List<Integer> beforeDeletion = new ArrayList<>();
		List<Integer> afterDeletion = new ArrayList<>();
		try (ValueStoreWAL wal = ValueStoreWAL.open(config);
				ValueStore store = new ValueStore(valuesDir.toFile(), false, ValueStore.VALUE_CACHE_SIZE,
						ValueStore.VALUE_ID_CACHE_SIZE, ValueStore.NAMESPACE_CACHE_SIZE,
						ValueStore.NAMESPACE_ID_CACHE_SIZE, wal)) {

			for (int i = 0; i < 80; i++) {
				beforeDeletion.add(mintUniqueIri(store, "before-" + i));
			}
			drainAndAwait(store);

			Path currentSegment = locateCurrentSegment(walDir);
			assertThat(currentSegment).as("current WAL segment").isNotNull();
			Files.deleteIfExists(currentSegment);

			for (int i = 80; i < 160; i++) {
				afterDeletion.add(mintUniqueIri(store, "after-" + i));
			}
			drainAndAwait(store);
		}

		try (ValueStoreWalReader reader = ValueStoreWalReader.open(config);
				DataStore ds = new DataStore(valuesDir.toFile(), "values")) {
			ValueStoreWalRecovery recovery = new ValueStoreWalRecovery();
			var dictionary = recovery.replay(reader);
			assertThat(afterDeletion).isNotEmpty();
			assertThat(dictionary.keySet()).as("WAL should retain post-deletion ids")
					.containsAll(afterDeletion);
			for (Integer id : beforeDeletion) {
				assertThat(ds.getData(id)).as("ValueStore data should exist for id %s", id).isNotNull();
			}
			for (Integer id : afterDeletion) {
				assertThat(ds.getData(id)).as("ValueStore data should exist for id %s", id).isNotNull();
			}
		}
	}

	private static int mintUniqueIri(ValueStore store, String token) throws IOException {
		IRI iri = SimpleValueFactory.getInstance().createIRI("http://example.com/value/" + token);
		return store.storeValue(iri);
	}

	private static void drainAndAwait(ValueStore store) throws IOException {
		OptionalLong pending = store.drainPendingWalHighWaterMark();
		if (pending.isPresent()) {
			store.awaitWalDurable(pending.getAsLong());
		}
	}

	private static Path locateCurrentSegment(Path walDir) throws IOException {
		if (!Files.isDirectory(walDir)) {
			return null;
		}
		try (var stream = Files.list(walDir)) {
			return stream.filter(path -> path.getFileName().toString().endsWith(".v1"))
					.max(Comparator.comparingInt(ValueStoreWalDeletionDuringWriteTest::segmentSequence))
					.orElse(null);
		}
	}

	private static int segmentSequence(Path path) {
		Matcher matcher = SEGMENT_PATTERN.matcher(path.getFileName().toString());
		if (!matcher.matches()) {
			return -1;
		}
		return Integer.parseInt(matcher.group(1));
	}
}
