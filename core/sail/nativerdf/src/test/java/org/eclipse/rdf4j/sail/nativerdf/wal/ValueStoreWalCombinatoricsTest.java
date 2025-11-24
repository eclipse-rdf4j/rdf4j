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
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Parameterized combinatorics tests that exercise the ValueStore WAL writer under a variety of sync, durability and
 * purge permutations. The goal is to ensure that no matter which combination is chosen the WAL produces a consistent,
 * monotonically ordered set of records without leaking stale segments.
 */
@TestInstance(Lifecycle.PER_CLASS)
class ValueStoreWalCombinatoricsTest {

	private static final Duration SYNC_INTERVAL = Duration.ofMillis(2);
	private static final Duration IDLE_POLL_INTERVAL = Duration.ofMillis(1);
	private static final long MAX_SEGMENT_BYTES = 2048;
	private static final int BATCH_BUFFER_BYTES = 1 << 15;
	private static final int QUEUE_CAPACITY = 16;
	private static final int SEED_RECORDS = 24;

	private enum ForceMode {
		NONE,
		FINAL,
		EACH
	}

	private enum PurgeMode {
		NEVER,
		MID_STREAM
	}

	private enum InitialState {
		EMPTY,
		SEEDED
	}

	@TempDir
	Path tempDir;

	private final AtomicInteger idCounter = new AtomicInteger();
	private String storeUuid;

	@BeforeEach
	void setUp() {
		storeUuid = UUID.randomUUID().toString();
	}

	@AfterEach
	void tearDown() {
		idCounter.set(0);
	}

	@ParameterizedTest(name = "{index}: policy={0}, force={1}, purge={2}, seed={3}")
	@MethodSource("walCombinationCases")
	void walHandlesCombinations(ValueStoreWalConfig.SyncPolicy syncPolicy, ForceMode forceMode, PurgeMode purgeMode,
			InitialState initialState) throws Exception {

		Path walDir = createWalDirectory(syncPolicy, forceMode, purgeMode, initialState);

		List<String> expectedLexicals = new ArrayList<>();
		if (initialState == InitialState.SEEDED) {
			expectedLexicals.addAll(seedInitialSegments(walDir));
		}

		ValueStoreWalConfig config = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(storeUuid)
				.maxSegmentBytes(MAX_SEGMENT_BYTES)
				.queueCapacity(QUEUE_CAPACITY)
				.batchBufferBytes(BATCH_BUFFER_BYTES)
				.syncPolicy(syncPolicy)
				.syncInterval(SYNC_INTERVAL)
				.idlePollInterval(IDLE_POLL_INTERVAL)
				.build();

		try (ValueStoreWAL wal = ValueStoreWAL.open(config)) {
			BatchResult firstBatch = mintBatch(wal, "first", 6, forceMode);
			expectedLexicals.addAll(firstBatch.lexicals());

			if (purgeMode == PurgeMode.MID_STREAM) {
				wal.purgeAllSegments();
				expectedLexicals.clear();
			}

			BatchResult secondBatch = mintBatch(wal, "second", 5, forceMode);
			expectedLexicals.addAll(secondBatch.lexicals());
		}

		ValueStoreWalReader.ScanResult result = ValueStoreWalReader.open(config).scan();
		List<String> actualLexicals = result.records()
				.stream()
				.map(ValueStoreWalRecord::lexical)
				.collect(Collectors.toList());

		assertThat(actualLexicals).containsExactlyElementsOf(expectedLexicals);
		if (purgeMode == PurgeMode.NEVER) {
			assertThat(result.complete())
					.as("WAL scan should be complete when no purge occurs")
					.isTrue();
		}

		List<Long> lsns = result.records()
				.stream()
				.map(ValueStoreWalRecord::lsn)
				.collect(Collectors.toList());
		for (int i = 1; i < lsns.size(); i++) {
			assertThat(lsns.get(i)).isGreaterThan(lsns.get(i - 1));
		}

		if (expectedLexicals.isEmpty()) {
			assertThat(lsns).isEmpty();
			assertThat(result.lastValidLsn()).isEqualTo(ValueStoreWAL.NO_LSN);
		} else {
			assertThat(lsns).isNotEmpty();
			assertThat(result.lastValidLsn()).isGreaterThanOrEqualTo(lsns.get(lsns.size() - 1));
		}
	}

	private Stream<Arguments> walCombinationCases() {
		List<Arguments> arguments = new ArrayList<>();
		for (ValueStoreWalConfig.SyncPolicy policy : ValueStoreWalConfig.SyncPolicy.values()) {
			for (ForceMode forceMode : ForceMode.values()) {
				for (PurgeMode purgeMode : PurgeMode.values()) {
					for (InitialState seed : EnumSet.allOf(InitialState.class)) {
						arguments.add(Arguments.of(policy, forceMode, purgeMode, seed));
					}
				}
			}
		}
		return arguments.stream();
	}

	private Path createWalDirectory(ValueStoreWalConfig.SyncPolicy syncPolicy, ForceMode forceMode,
			PurgeMode purgeMode, InitialState seed) throws IOException {
		String dirName = (syncPolicy.name() + "-" + forceMode.name() + "-" + purgeMode.name() + "-" + seed.name())
				.toLowerCase();
		Path dir = tempDir.resolve(dirName);
		Files.createDirectories(dir);
		return dir;
	}

	private List<String> seedInitialSegments(Path walDir) throws Exception {
		ValueStoreWalConfig seedConfig = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(storeUuid)
				.maxSegmentBytes(MAX_SEGMENT_BYTES)
				.queueCapacity(QUEUE_CAPACITY)
				.batchBufferBytes(BATCH_BUFFER_BYTES)
				.syncPolicy(ValueStoreWalConfig.SyncPolicy.ALWAYS)
				.syncInterval(SYNC_INTERVAL)
				.idlePollInterval(IDLE_POLL_INTERVAL)
				.build();

		try (ValueStoreWAL wal = ValueStoreWAL.open(seedConfig)) {
			return new ArrayList<>(mintBatch(wal, "seed", SEED_RECORDS, ForceMode.FINAL).lexicals());
		}
	}

	private BatchResult mintBatch(ValueStoreWAL wal, String prefix, int count, ForceMode forceMode)
			throws IOException, InterruptedException {
		List<String> lexicals = new ArrayList<>(count);
		long lastLsn = ValueStoreWAL.NO_LSN;
		for (int i = 0; i < count; i++) {
			int id = idCounter.incrementAndGet();
			String lexical = lexicalToken(prefix, id);
			long lsn = wal.logMint(id, ValueStoreWalValueKind.LITERAL, lexical, "http://example/dt", "",
					lexical.hashCode());
			lexicals.add(lexical);
			if (forceMode == ForceMode.EACH) {
				wal.awaitDurable(lsn);
			}
			lastLsn = lsn;
		}
		if (forceMode == ForceMode.FINAL && lastLsn > ValueStoreWAL.NO_LSN) {
			wal.awaitDurable(lastLsn);
		}
		return new BatchResult(lexicals, lastLsn);
	}

	private static String lexicalToken(String prefix, int id) {
		return prefix + "-" + id + "-payload-0123456789abcdefghijklmnopqrstuvwxyz";
	}

	private static final class BatchResult {
		private final List<String> lexicals;
		private final long lastLsn;

		private BatchResult(List<String> lexicals, long lastLsn) {
			this.lexicals = List.copyOf(lexicals);
			this.lastLsn = lastLsn;
		}

		private List<String> lexicals() {
			return lexicals;
		}

		private long lastLsn() {
			return lastLsn;
		}
	}
}
