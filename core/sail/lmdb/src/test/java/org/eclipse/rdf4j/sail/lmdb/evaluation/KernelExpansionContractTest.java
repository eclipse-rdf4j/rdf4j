/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import static org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeKernelIr.*;

import java.util.*;

import org.eclipse.rdf4j.sail.lmdb.evaluation.NativeLmdbQuerySource.NativeAdjacency;
import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.*;
import org.junit.jupiter.api.Test;

/** Independent bag/reachability oracles plus bounded-work contracts for real shared algorithms and both IR tiers. */
public class KernelExpansionContractTest {
	static void check(boolean ok, String message) {
		if (!ok)
			throw new AssertionError(message);
	}

	static final class Repeated implements NativeAdjacency {
		final long[] values, ends;
		long reads, finds;
		long budget = Long.MAX_VALUE;

		Repeated(long[] values, long[] lengths) {
			this.values = values.clone();
			ends = new long[lengths.length];
			long end = 0;
			for (int i = 0; i < lengths.length; i++) {
				check(lengths[i] > 0, "positive run");
				ends[i] = end = Math.addExact(end, lengths[i]);
			}
		}

		@Override
		public long find(long key) {
			finds++;
			return values.length == 0 ? -1 : 1;
		}

		@Override
		public long size(long h) {
			return ends.length == 0 ? 0 : ends[ends.length - 1];
		}

		@Override
		public long neighborAt(long h, long i) {
			if (++reads > budget)
				throw new AssertionError("unbounded payload traversal: " + reads);
			if (i < 0 || i >= size(h))
				throw new AssertionError("out-of-run read " + i);
			int at = Arrays.binarySearch(ends, i + 1);
			if (at < 0)
				at = -at - 1;
			return values[at];
		}

		@Override
		public long contextAt(long h, long i) {
			return 0;
		}

		@Override
		public boolean runsNeighborOrdered() {
			return true;
		}
	}

	static final class ArrayView implements NativeAdjacency {
		final long[] values;

		ArrayView(long... values) {
			this.values = values;
		}

		@Override
		public long find(long key) {
			return values.length == 0 ? -1 : 1;
		}

		@Override
		public long size(long h) {
			return values.length;
		}

		@Override
		public long neighborAt(long h, long i) {
			return values[Math.toIntExact(i)];
		}

		@Override
		public long contextAt(long h, long i) {
			return 0;
		}

		@Override
		public boolean runsNeighborOrdered() {
			return true;
		}
	}

	static KernelContext context(NativeAdjacency... views) {
		return new KernelContext(views, new long[] { 7 }, new long[0], null);
	}

	static Intersect intersection(int arity) {
		int[] indices = new int[arity];
		Operand[] keys = new Operand[arity];
		for (int i = 0; i < arity; i++) {
			indices[i] = i;
			keys[i] = Operand.constant(0);
		}
		return new Intersect(indices, keys, 0);
	}

	static List<String> execute(Kernel ir, KernelContext ctx, boolean compiled, int batch) throws Exception {
		JaninoKernel k = compiled ? KernelCompilationTestSupport.compile(ir)
				: ir.terminal instanceof Emit ? LmdbNativeKernelInterpreter.forRows(ir)
						: LmdbNativeKernelInterpreter.forAggregate(ir);
		List<String> out = new ArrayList<>();
		try (AutoCloseable c = k::close) {
			k.bind(ctx);
			long[] b = new long[Math.max(1, batch * ir.stride())];
			for (int fills = 0;; fills++) {
				if (fills > 10000)
					throw new AssertionError("nontermination");
				int n = k.fill(b, batch);
				check(n >= 0 && n <= batch, "batch bounds");
				if (n == 0)
					break;
				for (int i = 0; i < n; i++)
					out.add(Arrays.toString(Arrays.copyOfRange(b, i * ir.stride(), (i + 1) * ir.stride())));
			}
		}
		Collections.sort(out);
		return out;
	}

	static KernelContext withIdentity(KernelContext ctx) {
		return KernelMixedDemandTest.append(ctx, new KernelMixedDemandTest.Boundary(false));
	}

	@Test
	public void intersectionRandomUnsignedBagsAndCountsMatchIndependentOracle() throws Exception {
		Random random = new Random(0x14ACCE55);
		long[] universe = { 0, 1, 7, 8, Long.MAX_VALUE, Long.MIN_VALUE, Long.MIN_VALUE + 1, -2 };
		Kernel rowIR = new Kernel(1, List.of(intersection(3), new PlanRows(0, new int[0])),
				new Emit(new int[] { 0 }, false, OutputMods.none()));
		Kernel countIR = new Kernel(1, List.of(intersection(3)),
				new Aggregate(new int[] { 0 },
						new AggregateOutput[] { AggregateOutput.countStar(), AggregateOutput.count(0) }, null,
						OutputMods.none()));
		JaninoKernel rows = KernelCompilationTestSupport.compile(rowIR),
				counts = KernelCompilationTestSupport.compile(countIR);
		// Fresh generated objects are needed between evaluations (bind is not a reset contract).
		Class<?> rowClass = rows.getClass(), countClass = counts.getClass();
		rows.close();
		counts.close();
		for (int trial = 0; trial < 100; trial++) {
			int[][] multiplicities = new int[3][universe.length];
			NativeAdjacency[] views = new NativeAdjacency[3];
			for (int a = 0; a < 3; a++) {
				List<Long> values = new ArrayList<>();
				for (int u = 0; u < universe.length; u++) {
					int m = multiplicities[a][u] = random.nextInt(4);
					for (int j = 0; j < m; j++)
						values.add(universe[u]);
				}
				values.sort(Long::compareUnsigned);
				views[a] = new ArrayView(values.stream().mapToLong(Long::longValue).toArray());
			}
			List<String> expectedRows = new ArrayList<>(), expectedCounts = new ArrayList<>();
			for (int u = 0; u < universe.length; u++) {
				long count = (long) multiplicities[0][u] * multiplicities[1][u] * multiplicities[2][u];
				for (int j = 0; j < count; j++)
					expectedRows.add(Arrays.toString(new long[] { universe[u] }));
				if (count > 0)
					expectedCounts.add(Arrays.toString(new long[] { universe[u], count, count }));
			}
			Collections.sort(expectedRows);
			Collections.sort(expectedCounts);
			for (boolean compiled : new boolean[] { false, true }) {
				check(runExisting(rowIR, withIdentity(context(views)),
						compiled ? newInstance(rowClass) : LmdbNativeKernelInterpreter.forRows(rowIR), 1 + trial % 7)
								.equals(expectedRows),
						"row bag trial " + trial);
				check(runExisting(countIR, context(views),
						compiled ? newInstance(countClass) : LmdbNativeKernelInterpreter.forAggregate(countIR), 2)
								.equals(expectedCounts),
						"count bag trial " + trial);
			}
		}
	}

	static JaninoKernel newInstance(Class<?> c) throws Exception {
		return (JaninoKernel) c.getDeclaredConstructor().newInstance();
	}

	static List<String> runExisting(Kernel ir, KernelContext ctx, JaninoKernel k, int batch) throws Exception {
		List<String> rows = new ArrayList<>();
		try (AutoCloseable owner = k::close) {
			k.bind(ctx);
			long[] b = new long[batch * ir.stride()];
			for (int calls = 0;; calls++) {
				if (calls > 10000)
					throw new AssertionError("nontermination");
				int n = k.fill(b, batch);
				if (n == 0)
					break;
				for (int i = 0; i < n; i++)
					rows.add(Arrays.toString(Arrays.copyOfRange(b, i * ir.stride(), (i + 1) * ir.stride())));
			}
		}
		Collections.sort(rows);
		return rows;
	}

	@Test
	public void hugeDuplicateProductCountsWithoutEnumeratingIt() throws Exception {
		Kernel ir = new Kernel(1, List.of(intersection(2)), new Aggregate(new int[0],
				new AggregateOutput[] { AggregateOutput.countStar() }, null, OutputMods.none()));
		for (boolean compiled : new boolean[] { false, true }) {
			Repeated a = new Repeated(new long[] { Long.MIN_VALUE }, new long[] { 1L << 20 }),
					b = new Repeated(new long[] { Long.MIN_VALUE }, new long[] { 1L << 18 });
			a.budget = b.budget = 300;
			check(execute(ir, context(a, b), compiled, 1).equals(List.of("[274877906944]")), "wrong product");
			check(a.finds == 1 && b.finds == 1, "searched run twice");
		}
	}

	@Test
	public void overflowProductDoesNotPreventFirstRowButExactCountThrows() throws Exception {
		for (boolean compiled : new boolean[] { false, true }) {
			Repeated a = new Repeated(new long[] { 5 }, new long[] { 1L << 32 }),
					b = new Repeated(new long[] { 5 }, new long[] { 1L << 32 });
			a.budget = b.budget = 500;
			Kernel row = new Kernel(1, List.of(intersection(2), new PlanRows(0, new int[0])),
					new Emit(new int[] { 0 }, false, OutputMods.none()));
			JaninoKernel k = compiled ? KernelCompilationTestSupport.compile(row)
					: LmdbNativeKernelInterpreter.forRows(row);
			try (AutoCloseable owner = k::close) {
				k.bind(withIdentity(context(a, b)));
				long[] first = new long[1];
				check(k.fill(first, 1) == 1 && first[0] == 5, "first row");
			}
			a.reads = b.reads = 0;
			Kernel count = new Kernel(1, List.of(intersection(2)), new Aggregate(new int[0],
					new AggregateOutput[] { AggregateOutput.countStar() }, null, OutputMods.none()));
			try {
				execute(count, context(a, b), compiled, 1);
				throw new AssertionError("overflow lost");
			} catch (ArithmeticException expected) {
			}
		}
	}

	@Test
	public void unboundCountPreservesGroupWithoutMultiplyingHugeProduct() throws Exception {
		Kernel ir = new Kernel(2, List.of(intersection(2)), new Aggregate(new int[] { 0 },
				new AggregateOutput[] { AggregateOutput.count(1) }, null, OutputMods.none()));
		for (boolean compiled : new boolean[] { false, true }) {
			Repeated a = new Repeated(new long[] { 6 }, new long[] { 1L << 32 }),
					b = new Repeated(new long[] { 6 }, new long[] { 1L << 32 });
			a.budget = b.budget = 500;
			check(execute(ir, context(a, b), compiled, 1).equals(List.of("[6, 0]")), "lost zero group");
		}
	}

	@Test
	public void seekNearSignedLongMaximumNeverWrapsItsPosition() {
		Repeated a = new Repeated(new long[] { 9 }, new long[] { Long.MAX_VALUE });
		a.budget = 200;
		try (var c = new KernelExpansionCursors.Intersection(new NativeAdjacency[] { a }, new long[] { 7 }, null)) {
			check(c.nextGroup() && c.value() == 9 && c.groupMultiplicity() == Long.MAX_VALUE, "long extent");
			check(!c.nextGroup(), "did not exhaust");
		}
	}

	@Test
	public void intersectionCancellationAndClosureDoNotReadPayload() {
		Repeated a = new Repeated(new long[] { 1 }, new long[] { 10 });
		KernelCancellation token = new KernelCancellation(System.nanoTime() + 60_000_000_000L);
		try (var c = new KernelExpansionCursors.Intersection(new NativeAdjacency[] { a }, new long[] { 7 }, token)) {
			token.cancel();
			try {
				c.next();
				throw new AssertionError("cancel lost");
			} catch (KernelCancelledException expected) {
			}
			check(a.reads == 0, "payload after cancellation");
			c.close();
			check(!c.next(), "read after close");
		}
	}

	@Test
	public void zeroLengthPathPublishesStartBeforeAnyLookup() throws Exception {
		for (boolean compiled : new boolean[] { false, true }) {
			NativeAdjacency noRead = new NativeAdjacency() {
				@Override
				public long find(long k) {
					throw new AssertionError("read before p* witness");
				}

				@Override
				public long size(long h) {
					throw new AssertionError();
				}

				@Override
				public long neighborAt(long h, long i) {
					throw new AssertionError();
				}

				@Override
				public long contextAt(long h, long i) {
					throw new AssertionError();
				}
			};
			Kernel ir = new Kernel(1,
					List.of(new PathExpand(0, Operand.constant(0), 0, 0), new PlanRows(0, new int[0])),
					new Emit(new int[] { 0 }, false, OutputMods.none()));
			JaninoKernel k = compiled ? KernelCompilationTestSupport.compile(ir)
					: LmdbNativeKernelInterpreter.forRows(ir);
			try (AutoCloseable owner = k::close) {
				k.bind(withIdentity(context(noRead)));
				long[] b = new long[1];
				check(k.fill(b, 1) == 1 && b[0] == 7, "p* start");
			}
		}
	}

	@Test
	public void pathReachabilityMatchesBfsWithDuplicatesCyclesAndContexts() {
		Random random = new Random(142);
		for (int trial = 0; trial < 100; trial++) {
			List<long[]> edges = new ArrayList<>();
			for (int i = 0; i < 80; i++)
				edges.add(new long[] { random.nextInt(12), random.nextInt(12), random.nextInt(3) });
			NativeAdjacency graph = new NativeAdjacency() {
				List<long[]> row(long h) {
					return edges.stream().filter(e -> e[0] == h - 1).toList();
				}

				@Override
				public long find(long key) {
					return key + 1;
				}

				@Override
				public long size(long h) {
					return row(h).size();
				}

				@Override
				public long neighborAt(long h, long i) {
					return row(h).get((int) i)[1];
				}

				@Override
				public long contextAt(long h, long i) {
					return row(h).get((int) i)[2];
				}
			};
			for (int min : new int[] { 0, 1 })
				for (long[] contexts : new long[][] { new long[0], { 1 } }) {
					Set<Long> expected = new HashSet<>(), queued = new HashSet<>();
					Deque<Long> q = new ArrayDeque<>();
					q.add(0L);
					queued.add(0L);
					if (min == 0)
						expected.add(0L);
					while (!q.isEmpty()) {
						long s = q.removeFirst();
						for (long[] edge : edges)
							if (edge[0] == s && (contexts.length == 0 || edge[2] == 1)) {
								expected.add(edge[1]);
								if (queued.add(edge[1]))
									q.addLast(edge[1]);
							}
					}
					Set<Long> actual = new HashSet<>();
					try (var c = new KernelExpansionCursors.Path(graph, 0, min, contexts, null)) {
						while (c.next())
							check(actual.add(c.value()), "duplicate reachable term");
					}
					check(actual.equals(expected), "wrong reachable set " + trial);
				}
		}
	}

	@Test
	public void termCursorReadsOnlyItsFirstBoundedWindowForFirstTerm() {
		int[] calls = new int[3];
		KernelScanner scanner = (i, s, p, o, c) -> {
			calls[0]++;
			return new KernelQuadCursor() {
				@Override
				public int fill(long[] b, int n) {
					if (++calls[1] > 1)
						throw new AssertionError("eager full scan");
					b[0] = 4;
					b[2] = 5;
					return 1;
				}

				@Override
				public void close() {
					calls[2]++;
				}
			};
		};
		try (var c = new KernelExpansionCursors.Terms(scanner, 0, null)) {
			check(calls[0] == 0, "opened early");
			check(c.next() && c.value() == 4, "term");
		}
		check(Arrays.equals(calls, new int[] { 1, 1, 1 }), "ownership/demand " + Arrays.toString(calls));
	}

	@Test
	public void reusableIntersectionRebindsWithoutLeakingPreviousKeysOrPositions() {
		NativeAdjacency source = new NativeAdjacency() {
			@Override
			public long find(long k) {
				return k == 2 ? -1 : k;
			}

			@Override
			public long size(long h) {
				return 2;
			}

			@Override
			public long neighborAt(long h, long i) {
				return h * 10 + i;
			}

			@Override
			public long contextAt(long h, long i) {
				return 0;
			}
		};
		var cursor = new KernelExpansionCursors.Intersection(new NativeAdjacency[] { source, source }, null);
		for (int cycle = 0; cycle < 100; cycle++) {
			cursor.key(0, 1);
			cursor.key(1, 1);
			cursor.bind();
			check(cursor.nextGroup() && cursor.value() == 10, "initial");
			cursor.key(0, 2);
			cursor.bind();
			check(!cursor.nextGroup(), "stale failed key");
			cursor.bind(new long[] { 3, 3 });
			check(cursor.next() && cursor.value() == 30, "rebind");
			check(cursor.next() && cursor.value() == 31, "rebound tail");
			check(!cursor.next(), "exhaustion");
		}
		cursor.close();
		try {
			cursor.bind(new long[] { 1, 1 });
			throw new AssertionError("reused closed cursor");
		} catch (IllegalStateException expected) {
		}
	}

}
