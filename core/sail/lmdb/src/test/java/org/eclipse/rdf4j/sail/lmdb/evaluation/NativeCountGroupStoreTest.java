/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import java.nio.file.*;
import java.util.*;
import java.util.function.LongUnaryOperator;

import org.eclipse.rdf4j.sail.lmdb.LmdbQueryMemoryManager;
import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.*;
import org.junit.jupiter.api.Test;

public class NativeCountGroupStoreTest {
	static void check(boolean b, String why) {
		if (!b)
			throw new AssertionError(why);
	}

	static void eq(Object a, Object b) {
		if (!Objects.equals(a, b))
			throw new AssertionError(a + " != " + b);
	}

	static List<String> drain(NativeCountGroupStore store, int width) {
		store.finish();
		List<String> rows = new ArrayList<>();
		long[] r = new long[width];
		while (store.next(r))
			rows.add(Arrays.toString(r));
		return rows;
	}

	static LmdbQueryMemoryManager manager() {
		return LmdbQueryMemoryManager.createForTesting(16L << 20, 16L << 20);
	}

	static Set<Path> files() throws Exception {
		try (var f = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
			return f.filter(p -> p.getFileName().toString().startsWith("rdf4j-lmdb-native-sort-"))
					.collect(java.util.stream.Collectors.toSet());
		}
	}

	static class State {
		final long[] original, counts;
		final Set<Long>[] distinct;

		@SuppressWarnings("unchecked")
		State(long[] key, int n) {
			original = key.clone();
			counts = new long[n];
			distinct = new Set[n];
			for (int i = 0; i < n; i++)
				distinct[i] = new HashSet<>();
		}

		String row(boolean[] ds) {
			long[] r = Arrays.copyOf(original, original.length + counts.length);
			for (int i = 0; i < counts.length; i++)
				r[original.length + i] = ds[i] ? distinct[i].size() : counts[i];
			return Arrays.toString(r);
		}
	}

	@Test
	public void weightedGroupsAndMultipleDistinctChannelsMatchIndependentOracle() throws Exception {
		Set<Path> before = files();
		Random random = new Random(0x191919);
		for (int trial = 0; trial < 60; trial++) {
			int width = trial % 5, n = 1 + trial % 4;
			boolean[] ds = new boolean[n];
			for (int j = 0; j < n; j++)
				ds[j] = (j + trial) % 3 == 0;
			LongUnaryOperator key = id -> id < 0 ? id : (id & 31L); // aliases use the same canonical ID; high-bit
																	// values remain distinct
			Map<List<Long>, State> expected = new LinkedHashMap<>();
			LmdbQueryMemoryManager manager = manager();
			try (var ledger = manager.openQuery();
					var store = new NativeCountGroupStore(width, ds, key, null, ledger,
							trial % 2 == 0 ? 4096 : 1L << 20)) {
				int count = trial == 0 ? 0 : 300 + trial;
				for (int i = 0; i < count; i++) {
					long[] group = new long[width], values = new long[n];
					List<Long> normalized = new ArrayList<>();
					for (int j = 0; j < width; j++) {
						group[j] = random.nextInt(11) == 0 ? -1L : random.nextInt(71);
						normalized.add(group[j] == -1 ? -1 : key.applyAsLong(group[j]));
					}
					for (int j = 0; j < n; j++)
						values[j] = random.nextInt(7) == 0 ? -1L : random.nextInt(53);
					long weight = 1 + random.nextInt(9);
					store.add(group, values, weight);
					State state = expected.computeIfAbsent(normalized, k -> new State(group, n));
					for (int j = 0; j < n; j++)
						if (values[j] != -1L) {
							if (ds[j])
								state.distinct[j].add(key.applyAsLong(values[j]));
							else
								state.counts[j] = Math.addExact(state.counts[j], weight);
						}
				}
				if (count == 0 && width == 0)
					expected.put(List.of(), new State(new long[0], n));
				eq(expected.values().stream().map(s -> s.row(ds)).toList(), drain(store, width + n));
			}
			eq(0L, manager.usedBytes());
		}
		eq(before, files());
	}

	@Test
	public void oneHugeDistinctGroupIsUnionedAcrossManyRuns() throws Exception {
		Set<Path> before = files();
		LmdbQueryMemoryManager manager = manager();
		try (var ledger = manager.openQuery();
				var s = new NativeCountGroupStore(1, new boolean[] { true, true, false }, x -> x, null, ledger, 4096)) {
			for (int pass = 0; pass < 4; pass++)
				for (int i = 0; i < 5000; i++)
					s.add(new long[] { Long.MIN_VALUE }, new long[] { i, i % 73, 0 }, 3);
			List<String> result = drain(s, 4);
			eq(List.of("[" + Long.MIN_VALUE + ", 5000, 73, 60000]"), result);
			check(s.spilledRecords() > 0, "no spill");
			check(s.peakRunPaths() <= 64, "linear run list");
		}
		eq(0L, manager.usedBytes());
		eq(before, files());
	}

	@Test
	public void distinctWeightsAreNotSummedAndAllNullGroupsSurvive() {
		try (var s = new NativeCountGroupStore(1, new boolean[] { true, false }, x -> x, null, null, 4096)) {
			for (int i = 0; i < 100; i++)
				s.add(new long[] { 8 }, new long[] { 5, -1 }, Long.MAX_VALUE);
			s.add(new long[] { 9 }, new long[] { -1, -1 }, 2);
			s.add(new long[] { 10 }, new long[] { -1, 0 }, 0);
			eq(List.of("[8, 1, 0]", "[9, 0, 0]"), drain(s, 3));
		}
	}

	@Test
	public void wholeRowDistinctKeepsOriginalFirstRepresentativesAcrossSpills() {
		LmdbQueryMemoryManager manager = manager();
		try (var ledger = manager.openQuery();
				var s = new NativeCountGroupStore(2, new boolean[0], x -> x < 0 ? x : x % 1000, null, ledger, 4096)) {
			for (int i = 499; i >= 0; i--)
				s.add(new long[] { 1000 + i, i % 3 }, new long[0], 1);
			for (int i = 0; i < 500; i++)
				s.add(new long[] { i, i % 3 }, new long[0], 1);
			List<String> expected = new ArrayList<>();
			for (int i = 499; i >= 0; i--)
				expected.add("[" + (1000 + i) + ", " + i % 3 + "]");
			eq(expected, drain(s, 2));
		}
		eq(0L, manager.usedBytes());
	}

	@Test
	public void emptyGlobalAndZeroWidthDistinctHaveDifferentSemantics() {
		try (var s = new NativeCountGroupStore(0, new boolean[] { false, true }, x -> x, null, null, 4096)) {
			eq(List.of("[0, 0]"), drain(s, 2));
		}
		try (var s = new NativeCountGroupStore(0, new boolean[0], x -> x, null, null, 4096)) {
			eq(List.of(), drain(s, 0));
		}
		try (var s = new NativeCountGroupStore(0, new boolean[0], x -> x, null, null, 4096)) {
			s.add(new long[0], new long[0], 1);
			s.add(new long[0], new long[0], 8);
			eq(List.of("[]"), drain(s, 0));
		}
	}

	@Test
	public void countOverflowAcrossRunsClosesFilesAndReservations() throws Exception {
		Set<Path> before = files();
		LmdbQueryMemoryManager manager = manager();
		try (var ledger = manager.openQuery();
				var s = new NativeCountGroupStore(1, new boolean[] { false }, x -> x, null, ledger, 4096)) {
			s.add(new long[] { 0 }, new long[] { 0 }, Long.MAX_VALUE);
			for (int i = 1; i < 100; i++)
				s.add(new long[] { i }, new long[] { 0 }, 1);
			s.add(new long[] { 0 }, new long[] { 0 }, 1);
			try {
				s.finish();
				throw new AssertionError("missing overflow");
			} catch (ArithmeticException good) {
			}
		}
		eq(0L, manager.usedBytes());
		eq(before, files());
	}

	@Test
	public void residentCountOverflowIsChecked() {
		try (var s = new NativeCountGroupStore(1, new boolean[] { false }, x -> x, null, null, 4096)) {
			s.add(new long[] { 0 }, new long[] { 0 }, Long.MAX_VALUE);
			try {
				s.add(new long[] { 0 }, new long[] { 0 }, 1);
				throw new AssertionError("wrapped");
			} catch (ArithmeticException good) {
			}
		}
	}

	@Test
	public void earlyCloseDeletesSpillsAndCanBeRepeated() throws Exception {
		Set<Path> before = files();
		LmdbQueryMemoryManager manager = manager();
		try (var ledger = manager.openQuery()) {
			NativeCountGroupStore s = new NativeCountGroupStore(2, new boolean[] { false }, x -> x, null, ledger, 4096);
			for (int i = 0; i < 600; i++)
				s.add(new long[] { i, -i }, new long[] { 0 }, 2);
			s.finish();
			check(s.next(new long[3]), "empty");
			s.close();
			s.close();
			eq(0L, manager.usedBytes());
		}
		eq(before, files());
	}

	@Test
	public void cancellationDuringInputAndFinalMergeReleasesClaims() throws Exception {
		Set<Path> before = files();
		for (boolean finish : new boolean[] { false, true }) {
			LmdbQueryMemoryManager manager = manager();
			KernelCancellation cancel = new KernelCancellation(System.nanoTime() + 1_000_000_000_000L);
			try (var ledger = manager.openQuery();
					var s = new NativeCountGroupStore(1, new boolean[] { true }, x -> x, cancel, ledger, 4096)) {
				for (int i = 0; i < 400; i++)
					s.add(new long[] { i % 3 }, new long[] { i }, 1);
				cancel.cancel();
				try {
					if (finish)
						s.finish();
					else
						for (int i = 400; i < 10000; i++)
							s.add(new long[] { 1 }, new long[] { i }, 1);
					throw new AssertionError("not cancelled");
				} catch (KernelCancelledException good) {
				}
			}
			eq(0L, manager.usedBytes());
		}
		eq(before, files());
	}

	@Test
	public void failedCanonicalizationKeepsPrimaryAndReleasesOwnedFiles() throws Exception {
		Set<Path> before = files();
		RuntimeException primary = new IllegalArgumentException("identity changed");
		boolean[] fail = { false };
		LmdbQueryMemoryManager manager = manager();
		try (var ledger = manager.openQuery(); var s = new NativeCountGroupStore(1, new boolean[] { false }, x -> {
			if (fail[0])
				throw primary;
			return x;
		}, null, ledger, 4096)) {
			for (int i = 0; i < 200; i++)
				s.add(new long[] { i }, new long[] { 0 }, 1);
			fail[0] = true;
			try {
				s.add(new long[] { 0 }, new long[] { 0 }, 1);
				throw new AssertionError();
			} catch (RuntimeException actual) {
				check(actual == primary, "changed failure");
			}
		}
		eq(0L, manager.usedBytes());
		eq(before, files());
	}

	@Test
	public void sharedLedgerRefusesUnboundedFallbackAndReclaimsOnClose() {
		LmdbQueryMemoryManager m = LmdbQueryMemoryManager.createForTesting(1L << 20, 1L << 20);
		try (var q = m.openQuery();
				var block = q.reserve((1L << 20) - 64, null);
				var s = new NativeCountGroupStore(1, new boolean[] { true }, x -> x, null, q, 4096)) {
			try {
				s.add(new long[] { 1 }, new long[] { 2 }, 1);
				throw new AssertionError("admitted without bytes");
			} catch (IllegalStateException good) {
			}
			eq((1L << 20) - 64, m.usedBytes());
		}
		eq(0L, m.usedBytes());
	}

	@Test
	public void pressureShrinksSpillWorkspaceInsteadOfRestartingUnbounded() {
		LmdbQueryMemoryManager m = LmdbQueryMemoryManager.createForTesting(2L << 20, 2L << 20);
		try (var q = m.openQuery();
				var block = q.reserve(512L << 10, null);
				var s = new NativeCountGroupStore(1, new boolean[] { false }, x -> x, null, q, 16L << 20)) {
			for (int i = 0; i < 20000; i++)
				s.add(new long[] { i }, new long[] { 0 }, 1);
			eq(20000, drain(s, 2).size());
			check(m.peakUsedBytes() <= 2L << 20, "reservation exceeded");
		}
		eq(0L, m.usedBytes());
	}

	@Test
	public void invalidInputAndLifecycleAreRejected() {
		try (var s = new NativeCountGroupStore(1, new boolean[] { false }, x -> x, null, null, 4096)) {
			try {
				s.add(new long[0], new long[] { 0 }, 1);
				throw new AssertionError();
			} catch (IllegalArgumentException good) {
			}
			try {
				s.next(new long[2]);
				throw new AssertionError();
			} catch (IllegalStateException good) {
			}
			s.finish();
			try {
				s.add(new long[] { 1 }, new long[] { 0 }, 1);
				throw new AssertionError();
			} catch (IllegalStateException good) {
			}
		}
	}

	@Test
	public void scalarSpecializationAndGenericUpdatesCanAlternateAcrossSpills() {
		Random random = new Random(19196);
		Map<Long, long[]> expected = new LinkedHashMap<>();
		LmdbQueryMemoryManager manager = manager();
		try (var q = manager.openQuery();
				var store = new NativeCountGroupStore(1, new boolean[] { false }, x -> x < 0 ? x : x % 97, null, q,
						4096)) {
			for (int i = 0; i < 6000; i++) {
				long original = 1000 + random.nextInt(400), key = original % 97, value = i % 7 == 0 ? -1 : 9,
						weight = 1 + i % 5;
				if ((i & 1) == 0)
					store.addSingleCount(original, value, weight);
				else
					store.add(new long[] { original }, new long[] { value }, weight);
				long[] v = expected.computeIfAbsent(key, x -> new long[] { original, 0 });
				if (value != -1)
					v[1] += weight;
			}
			eq(expected.values().stream().map(Arrays::toString).toList(), drain(store, 2));
			check(store.spilledRecords() > 0, "no spill");
		}
		eq(0L, manager.usedBytes());
	}

	@Test
	public void scalarHotGroupDoesNotGrowAFullArena() {
		LmdbQueryMemoryManager manager = manager();
		try (var q = manager.openQuery();
				var store = new NativeCountGroupStore(1, new boolean[] { false }, x -> x, null, q, 4096)) {
			for (int i = 0; i < 16; i++)
				store.addSingleCount(i, 0, 1);
			long bytes = manager.usedBytes();
			for (int i = 0; i < 10000; i++)
				store.addSingleCount(i % 16, 0, 2);
			eq(bytes, manager.usedBytes());
			eq(0L, store.spilledRecords());
			eq(16, drain(store, 2).size());
		}
	}

	@Test
	public void independentQueriesShareOneReservationCeiling() throws Exception {
		Set<Path> before = files();
		LmdbQueryMemoryManager manager = LmdbQueryMemoryManager.createForTesting(4L << 20, 1L << 20);
		var pool = java.util.concurrent.Executors.newFixedThreadPool(4);
		List<java.util.concurrent.Future<Long>> tasks = new ArrayList<>();
		try {
			for (int worker = 0; worker < 4; worker++)
				tasks.add(pool.submit(() -> {
					try (var q = manager.openQuery();
							var store = new NativeCountGroupStore(1, new boolean[] { false, true }, x -> x, null, q,
									4096)) {
						for (int i = 0; i < 4000; i++)
							store.add(new long[] { i % 61 }, new long[] { 0, i }, 1);
						store.finish();
						long[] row = new long[3];
						long sum = 0;
						while (store.next(row)) {
							eq(row[1], row[2]);
							sum += row[1];
						}
						return sum;
					}
				}));
			for (var f : tasks)
				eq(4000L, f.get());
		} finally {
			pool.shutdownNow();
		}
		check(manager.peakUsedBytes() <= manager.maxBytes(), "shared ceiling exceeded");
		eq(0L, manager.usedBytes());
		eq(before, files());
	}

	@Test
	public void countSpecializationChecksOverflowAndRejectsDistinctChannels() {
		try (var s = new NativeCountGroupStore(1, new boolean[] { false }, x -> x, null, null, 4096)) {
			s.addSingleCount(1, 0, Long.MAX_VALUE);
			try {
				s.addSingleCount(1, 0, 1);
				throw new AssertionError("overflow");
			} catch (ArithmeticException expected) {
			}
		}
		try (var s = new NativeCountGroupStore(1, new boolean[] { true }, x -> x, null, null, 4096)) {
			try {
				s.addSingleCount(1, 9, 1);
				throw new AssertionError("wrong capability");
			} catch (IllegalStateException expected) {
			}
		}
	}

	@Test
	public void probeRecordCeilingDoesNotCountRepeatedInputs() {
		KernelCancellation cancellation = new KernelCancellation(Long.MAX_VALUE, null, null, 16);
		LmdbQueryMemoryManager manager = manager();
		try (var q = manager.openQuery();
				var store = new NativeCountGroupStore(1, new boolean[] { false }, x -> x, cancellation, q, 4096)) {
			for (int i = 0; i < 10000; i++)
				store.addSingleCount(i % 16, 0, 1);
			check(!cancellation.cancelled(), "hot groups consumed a materialization allowance per input");
			try {
				store.addSingleCount(16, 0, 1);
				throw new AssertionError("record ceiling ignored");
			} catch (KernelCancelledException expected) {
			}
			eq(0L, manager.usedBytes());
		}
	}
}
