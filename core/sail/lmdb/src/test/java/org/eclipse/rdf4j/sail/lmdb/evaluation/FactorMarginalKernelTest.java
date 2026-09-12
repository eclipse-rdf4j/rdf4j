/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import static org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeKernelIr.*;

import java.math.BigInteger;
import java.util.*;

import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.*;
import org.eclipse.rdf4j.sail.lmdb.factor.*;
import org.eclipse.rdf4j.sail.lmdb.factor.BorrowedTupleContractTest.WeightedTuples;
import org.junit.jupiter.api.Test;

/** Actual emitter/interpreter and factor runtime. Input plans are explicit immutable fixtures. */
public class FactorMarginalKernelTest {
	static void yes(boolean b) {
		if (!b)
			throw new AssertionError();
	}

	static void eq(Object a, Object b) {
		if (!Objects.equals(a, b))
			throw new AssertionError(a + " != " + b);
	}

	static void fails(Class<? extends Throwable> type, Runnable f) {
		try {
			f.run();
		} catch (Throwable t) {
			if (type.isInstance(t))
				return;
			throw new AssertionError(t);
		}
		throw new AssertionError("missing " + type);
	}

	static Kernel shape(int width, int[] groups, AggregateOutput... outputs) {
		int[] c = new int[width];
		for (int i = 0; i < width; i++)
			c[i] = i;
		return new Kernel(width, List.of(new PlanRows(0, c)), new Aggregate(groups, outputs, null, OutputMods.none()));
	}

	static class Hooks implements KernelHooks {
		final Map<String, BigInteger> sums = new HashMap<>();
		final Map<String, Long> counts = new HashMap<>();
		long calls;

		public boolean testFilter(int id, long a, long b, long c) {
			throw new AssertionError("semantic guard not expected");
		}

		public long computeBind(int id, long a, long b) {
			throw new AssertionError("BIND not expected");
		}

		public int compareValues(long a, long b) {
			return Long.compare(a, b);
		}

		public boolean isNumeric(long id) {
			return true;
		}

		public double doubleValue(long id) {
			return id;
		}

		public void accumulateNumeric(int a, int g, long id) {
			accumulateNumericWeighted(a, g, id, 1);
		}

		public boolean supportsWeightedNumericAggregates() {
			return true;
		}

		public void accumulateNumericWeighted(int a, int g, long id, long w) {
			calls++;
			String k = a + ":" + g;
			sums.merge(k, BigInteger.valueOf(id).multiply(BigInteger.valueOf(w)), BigInteger::add);
			counts.merge(k, w, Math::addExact);
		}

		public boolean supportsCanonicalTermKeys() {
			return true;
		}

		public long canonicalTermKey(long id) {
			return id;
		}

		public long termHashKey(long id) {
			return id;
		}

		public boolean sameRdfTerm(long a, long b) {
			return a == b;
		}
	}

	static final class Plan implements KernelPlan {
		final FactorEnvironment[] env;
		final long[][] scalar;
		final long[] weight;
		final int[] slots;
		int opens, pulls, closes;
		boolean ended;

		Plan(FactorEnvironment[] e, long[][] s, long[] w, int... slots) {
			env = e;
			scalar = s;
			weight = w;
			this.slots = slots;
		}

		public Cursor open() {
			throw new AssertionError("scalar product requested");
		}

		public FactorCursor openFactors(int[] demanded) {
			eq(0, demanded.length);
			opens++;
			return new FactorCursor() {
				int at = -1;
				boolean closed;

				public boolean next() {
					pulls++;
					return ++at < env.length;
				}

				public int slot(int c) {
					return slots[c];
				}

				public long scalar(int c) {
					if ((env[at].mask() & (1L << slots[c])) != 0)
						throw new AssertionError("deferred scalar");
					return scalar[at][c];
				}

				public long multiplicity() {
					return weight[at];
				}

				public FactorEnvironment factors() {
					return env[at];
				}

				public void close() {
					if (!closed) {
						closed = true;
						closes++;
					}
				}
			};
		}

		public void close() {
			ended = true;
		}
	}

	static BorrowedFactorBatch heap(HeapFactorSource s, long... ids) {
		BorrowedFactorBatch b = new BorrowedFactorBatch(s, 1);
		b.reset(1);
		b.bindHeap(0, ids, 0, ids.length);
		return b;
	}

	static final class Metadata extends BorrowedFactorBatch.Source {
		int opens;

		Metadata() {
			super(new Object());
		}

		BorrowedFactorBatch batch(long count) {
			BorrowedFactorBatch b = new BorrowedFactorBatch(this, 1);
			b.reset(1);
			if (count != 0)
				b.bindNative(0, BorrowedFactorBatch.ENCODED_RUN, 1, 0, 0, count);
			return b;
		}

		public BorrowedFactorBatch.Reader openReader() {
			opens++;
			throw new AssertionError("hidden source decoded");
		}
	}

	static List<long[]> run(Kernel ir, KernelPlan plan, boolean compiled, Hooks hooks) throws Exception {
		JaninoKernel k = compiled ? KernelCompilationTestSupport.compile(ir)
				: LmdbNativeKernelInterpreter.forAggregate(ir);
		KernelContext context = new KernelContext(new NativeLmdbQuerySource.NativeAdjacency[0], new long[0],
				new long[0], new long[0][], new int[0], new int[0], hooks, null, new KernelPlan[] { plan }, 16, null,
				null);
		try {
			k.bind(context);
			List<long[]> result = new ArrayList<>();
			long[] out = new long[ir.stride() * 3];
			int n;
			while ((n = k.fill(out, 3)) != 0)
				for (int i = 0; i < n; i++)
					result.add(Arrays.copyOfRange(out, i * ir.stride(), (i + 1) * ir.stride()));
			result.sort((a, b) -> {
				for (int i = 0; i < a.length; i++) {
					int c = Long.compare(a[i], b[i]);
					if (c != 0)
						return c;
				}
				return 0;
			});
			return result;
		} finally {
			k.close();
		}
	}

	static String show(List<long[]> r) {
		return Arrays.deepToString(r.toArray(long[][]::new));
	}

	@Test
	public void layoutSharesArgumentTraversalsAndKeepsPresenceSeparate() {
		FactorProjectionLayout l = new FactorProjectionLayout(new int[] { 0 }, new int[] { 1, 1, 2, 2, -1, 0 },
				new boolean[] { false, true, true, false, true, false });
		eq(4, l.size());
		eq("[0]", Arrays.toString(l.columns(0)));
		eq("[5]", Arrays.toString(l.channels(0)));
		eq("[0, 1]", Arrays.toString(l.channels(1)));
		yes(l.exactWeights()[1]);
		eq("[2, 3]", Arrays.toString(l.channels(2)));
		int[] copy = l.columns(1);
		copy[0] = 63;
		eq("[0, 1]", Arrays.toString(l.columns(1)));
		fails(IllegalArgumentException.class,
				() -> new FactorProjectionLayout(new int[] { 0, 0 }, new int[] { 1 }, new boolean[] { true }));
	}

	@Test
	public void countDistinctSumAvgBundleUsesMarginalsInsteadOfSiblingProduct() throws Exception {
		try (HeapFactorSource s = new HeapFactorSource(this); Metadata hidden = new Metadata()) {
			FactorEnvironment e = new FactorEnvironment(4);
			e.bind(1, heap(s, 2, 2, 3), 0, 1);
			e.bind(2, heap(s, 5, 7), 0, 1);
			e.bind(3, hidden.batch(1000), 0, 1);
			Kernel ir = shape(4, new int[] { 0 }, AggregateOutput.countStar(), AggregateOutput.countDistinct(1),
					AggregateOutput.countDistinct(2), AggregateOutput.sum(1), AggregateOutput.avg(2));
			yes(ir.aggregateProjections != null);
			eq(4, ir.aggregateProjections.layout.size());
			for (boolean compiled : new boolean[] { false, true }) {
				Plan p = new Plan(new FactorEnvironment[] { e, e }, new long[][] { { 11, 0, 0, 0 }, { 11, 0, 0, 0 } },
						new long[] { 2, 3 }, 0, 1, 2, 3);
				Hooks h = new Hooks();
				eq("[[11, 30000, 2, 2, 0, 0]]", show(run(ir, p, compiled, h)));
				eq(BigInteger.valueOf(70000), h.sums.get("3:0"));
				eq(BigInteger.valueOf(180000), h.sums.get("4:0"));
				eq(30000L, h.counts.get("4:0"));
				yes(h.calls <= 12);
				eq(1, p.opens);
				eq(3, p.pulls);
				eq(1, p.closes);
				yes(p.ended);
			}
			eq(0, hidden.opens);
		}
	}

	@Test
	public void tupleArgumentsRemainAlignedAndGroupCardinalityIsUsedOnce() throws Exception {
		try (WeightedTuples t = new WeightedTuples(2, new long[][] { { 7, 101 }, { 8, 202 }, { 7, 303 } },
				new long[] { 2, 5, 3 }); Metadata hidden = new Metadata()) {
			FactorEnvironment e = new FactorEnvironment(64);
			e.bindTuple(new int[] { 63, 2 }, t.batch(), 0, 1L << 5);
			e.bind(17, hidden.batch(11), 0, 1L << 5);
			Kernel ir = shape(4, new int[] { 2 }, AggregateOutput.countStar(), AggregateOutput.countDistinct(1),
					AggregateOutput.sum(1));
			for (boolean compiled : new boolean[] { false, true }) {
				Hooks h = new Hooks();
				Plan p = new Plan(new FactorEnvironment[] { e }, new long[][] { { 47, 0, 0, 0 } }, new long[] { 2 }, 5,
						63, 2, 17);
				eq("[[101, 44, 1, 0], [202, 110, 1, 1], [303, 66, 1, 2]]", show(run(ir, p, compiled, h)));
				eq(BigInteger.valueOf(308), h.sums.get("2:0"));
				eq(BigInteger.valueOf(880), h.sums.get("2:1"));
				eq(BigInteger.valueOf(462), h.sums.get("2:2"));
			}
			eq(0, hidden.opens);
		}
	}

	@Test
	public void nullCountAndDistinctDoNotMultiplyUnobservedHugeFactors() throws Exception {
		try (HeapFactorSource s = new HeapFactorSource(this); Metadata hidden = new Metadata()) {
			FactorEnvironment e = new FactorEnvironment(4);
			e.bind(1, heap(s, 9, 10), 0, 1);
			e.bind(2, hidden.batch(Long.MAX_VALUE), 0, 1);
			Kernel ir = shape(4, new int[] { 0 }, AggregateOutput.countDistinct(1), AggregateOutput.count(3));
			for (boolean c : new boolean[] { false, true })
				eq("[[7, 2, 0]]", show(run(ir, new Plan(new FactorEnvironment[] { e }, new long[][] { { 7, 0, 0, -1 } },
						new long[] { Long.MAX_VALUE }, 0, 1, 2, 3), c, new Hooks())));
			eq(0, hidden.opens);
		}
	}

	@Test
	public void exactObservedOverflowStillFailsAndCloses() throws Exception {
		try (HeapFactorSource s = new HeapFactorSource(this); Metadata hidden = new Metadata()) {
			FactorEnvironment e = new FactorEnvironment(3);
			e.bind(1, heap(s, 9, 10), 0, 1);
			e.bind(2, hidden.batch(Long.MAX_VALUE), 0, 1);
			Kernel ir = shape(3, new int[0], AggregateOutput.countStar(), AggregateOutput.countDistinct(1));
			for (boolean c : new boolean[] { false, true }) {
				Plan p = new Plan(new FactorEnvironment[] { e }, new long[][] { { 7, 0, 0 } }, new long[] { 2 }, 0, 1,
						2);
				try {
					run(ir, p, c, new Hooks());
					throw new AssertionError("overflow ignored");
				} catch (ArithmeticException good) {
				}
				eq(1, p.closes);
			}
		}
	}

	@Test
	public void emptyHiddenRelationAnnihilatesAllMarginalsBeforeOverflow() throws Exception {
		try (HeapFactorSource s = new HeapFactorSource(this); Metadata hidden = new Metadata()) {
			FactorEnvironment e = new FactorEnvironment(4);
			e.bind(1, heap(s, 9), 0, 1);
			e.bind(2, hidden.batch(Long.MAX_VALUE), 0, 1);
			e.bind(3, hidden.batch(0), 0, 1);
			Kernel ir = shape(4, new int[] { 0 }, AggregateOutput.countStar(), AggregateOutput.countDistinct(1));
			for (boolean c : new boolean[] { false, true })
				eq("[]", show(run(ir, new Plan(new FactorEnvironment[] { e }, new long[][] { { 7, 0, 0, 0 } },
						new long[] { Long.MAX_VALUE }, 0, 1, 2, 3), c, new Hooks())));
		}
	}

	@Test
	public void weightedRowFallbackReadsInputOnceAndNeverDropsOtherCountChannels() throws Exception {
		Kernel ir = shape(3, new int[] { 0 }, AggregateOutput.countStar(), AggregateOutput.count(1),
				AggregateOutput.countDistinct(2));
		for (boolean c : new boolean[] { false, true }) {
			BoundedGroupKernelTest.Plan p = new BoundedGroupKernelTest.Plan(
					new long[][] { { 7, 3, 9 }, { 7, -1, 9 }, { 8, 1, -1 } }, new long[] { 5, 11, 13 });
			eq("[[7, 16, 5, 1], [8, 13, 13, 0]]", show(run(ir, p, c, new Hooks())));
			eq(1, p.opens);
			eq(1, p.closes);
		}
	}

	@Test
	public void separateNativeBatchProjectionsHaveFirstPriority() throws Exception {
		Kernel ir = shape(3, new int[] { 0 }, AggregateOutput.countStar(), AggregateOutput.countDistinct(1),
				AggregateOutput.countDistinct(2));
		for (boolean c : new boolean[] { false, true }) {
			int[] counts = new int[4];
			KernelPlan p = new KernelPlan() {
				public Cursor open() {
					throw new AssertionError();
				}

				public FactorCursor openFactors(int[] d) {
					throw new AssertionError();
				}

				public ProjectionCursor openProjections(int[][] columns, boolean[] exact) {
					counts[0]++;
					return new ProjectionCursor() {
						boolean batch;
						int last = -1, at;

						public boolean nextBatch() {
							counts[1]++;
							if (batch)
								return false;
							batch = true;
							return true;
						}

						public boolean next(int request) {
							if (request != last) {
								last = request;
								at = 0;
							}
							return at++ < 1;
						}

						public long value(int col) {
							return col == 0 ? 7 : col == 1 ? 9 : 11;
						}

						public long multiplicity() {
							counts[2]++;
							return 3000000000L;
						}

						public void close() {
							counts[3]++;
						}
					};
				}

				public void close() {
				}
			};
			eq("[[7, 3000000000, 1, 1]]", show(run(ir, p, c, new Hooks())));
			eq(1, counts[0]);
			eq(2, counts[1]);
			eq(1, counts[2]);
			eq(1, counts[3]);
		}
	}

	@Test
	public void randomizedIndependentBagsMatchFlatOracleInBothTiers() throws Exception {
		Random random = new Random(0x20FAC70);
		Kernel ir = shape(5, new int[] { 0 }, AggregateOutput.countStar(), AggregateOutput.count(4),
				AggregateOutput.countDistinct(1), AggregateOutput.countDistinct(2), AggregateOutput.sum(1),
				AggregateOutput.avg(2),
				AggregateOutput.sumDistinct(2), AggregateOutput.minId(1), AggregateOutput.maxId(2));
		for (int trial = 0; trial < 80; trial++)
			try (HeapFactorSource source = new HeapFactorSource(this); Metadata hidden = new Metadata()) {
				int n = 1 + random.nextInt(5);
				FactorEnvironment[] env = new FactorEnvironment[n];
				long[][] scalar = new long[n][5];
				long[] prefix = new long[n];
				Map<Long, long[]> expected = new TreeMap<>();
				Map<Long, Set<Long>> da = new HashMap<>(), db = new HashMap<>();
				for (int i = 0; i < n; i++) {
					long group = random.nextInt(3) == 0 ? Long.MIN_VALUE : 11 + random.nextInt(2);
					long nullable = random.nextBoolean() ? -1 : 17;
					long[] a = new long[random.nextInt(5)], b = new long[random.nextInt(5)];
					for (int j = 0; j < a.length; j++)
						a[j] = 2 + random.nextInt(4);
					for (int j = 0; j < b.length; j++)
						b[j] = 7 + random.nextInt(4);
					Arrays.sort(a);
					Arrays.sort(b);
					int hn = random.nextInt(5);
					prefix[i] = 1 + random.nextInt(3);
					scalar[i] = new long[] { group, 0, 0, 0, nullable };
					env[i] = new FactorEnvironment(5);
					env[i].bind(1, heap(source, a), 0, 1);
					env[i].bind(2, heap(source, b), 0, 1);
					env[i].bind(3, hidden.batch(hn), 0, 1);
					for (long x : a)
						for (long y : b)
							for (int j = 0; j < hn; j++) {
								long[] e = expected.computeIfAbsent(group,
										g -> new long[] { 0, 0, 0, 0, Long.MAX_VALUE, Long.MIN_VALUE });
								e[0] += prefix[i];
								if (nullable != -1)
									e[1] += prefix[i];
								e[2] += x * prefix[i];
								e[3] += y * prefix[i];
								e[4] = Math.min(e[4], x);
								e[5] = Math.max(e[5], y);
								da.computeIfAbsent(group, g -> new HashSet<>()).add(x);
								db.computeIfAbsent(group, g -> new HashSet<>()).add(y);
							}
				}
				for (boolean compiled : new boolean[] { false, true }) {
					Hooks h = new Hooks();
					Plan plan = new Plan(env, scalar, prefix, 0, 1, 2, 3, 4);
					List<long[]> actual = run(ir, plan, compiled, h);
					eq(expected.size(), actual.size());
					for (long[] row : actual) {
						long[] e = expected.get(row[0]);
						yes(e != null);
						eq(e[0], row[1]);
						eq(e[1], row[2]);
						eq((long) da.get(row[0]).size(), row[3]);
						eq((long) db.get(row[0]).size(), row[4]);
						eq(BigInteger.valueOf(e[2]), h.sums.get("4:" + row[5]));
						eq(BigInteger.valueOf(e[3]), h.sums.get("5:" + row[6]));
						eq(e[0], h.counts.get("5:" + row[6]));
						eq(BigInteger.valueOf(db.get(row[0]).stream().mapToLong(Long::longValue).sum()),
								h.sums.get("6:" + row[7]));
						eq(e[4], row[8]);
						eq(e[5], row[9]);
					}
					eq(1, plan.opens);
					eq(n + 1, plan.pulls);
					eq(1, plan.closes);
				}
				eq(0, hidden.opens);
			}
	}

	@Test
	public void runtimeProjectionOrderEpochAndClosureAreChecked() {
		try (HeapFactorSource source = new HeapFactorSource(this)) {
			FactorEnvironment env = new FactorEnvironment(2);
			env.bind(1, heap(source, 2, 3), 0, 1);
			Plan plan = new Plan(new FactorEnvironment[] { env }, new long[][] { { 9, 0 } }, new long[] { 1 }, 0, 1);
			try (KernelMarginalCursor m = KernelMarginalCursor.open(plan, 2, new int[][] { { 0 }, { 0, 1 } },
					new boolean[] { false, true }, null)) {
				yes(m.nextBatch());
				yes(m.next(0));
				eq(9L, m.value(0));
				yes(!m.next(0));
				yes(m.next(1));
				env.clear();
				fails(IllegalStateException.class, () -> m.value(1));
			}
			eq(1, plan.closes);
			Plan bad = new Plan(new FactorEnvironment[] { env }, new long[][] { { 9, 0 } }, new long[] { 1 }, 0, 1);
			try (KernelMarginalCursor m = KernelMarginalCursor.open(bad, 2, new int[][] { { 0 }, { 1 } },
					new boolean[] { false, true }, null)) {
				yes(m.nextBatch());
				fails(IllegalStateException.class, () -> m.next(1));
			}
			eq(1, bad.closes);
		}
	}

	@Test
	public void cancellationBetweenProjectionPassesClosesSource() {
		try (HeapFactorSource source = new HeapFactorSource(this)) {
			FactorEnvironment env = new FactorEnvironment(2);
			env.bind(1, heap(source, 2, 3), 0, 1);
			Plan plan = new Plan(new FactorEnvironment[] { env }, new long[][] { { 9, 0 } }, new long[] { 1 }, 0, 1);
			KernelCancellation cancel = new KernelCancellation(System.nanoTime() + 1000000000000L);
			try (KernelMarginalCursor m = KernelMarginalCursor.open(plan, 2, new int[][] { { 0 }, { 1 } },
					new boolean[] { false, true }, cancel)) {
				yes(m.nextBatch());
				while (m.next(0)) {
				}
				cancel.cancel();
				// This is a projection boundary, not a large per-row loop: cancellation must be immediate.
				fails(KernelCancelledException.class, () -> m.next(1));
			}
			eq(1, plan.closes);
		}
	}

	@Test
	public void numericCapabilityRefusalHappensBeforeOpeningFactorSources() throws Exception {
		Kernel ir = shape(3, new int[] { 0 }, AggregateOutput.sum(1), AggregateOutput.avg(2));
		yes(ir.aggregateProjections != null);
		for (boolean compiled : new boolean[] { false, true }) {
			BoundedGroupKernelTest.Plan plan = new BoundedGroupKernelTest.Plan(
					new long[][] { { 7, 2, 5 }, { 7, 3, 9 } }, null) {
				public FactorCursor openFactors(int[] demand) {
					throw new AssertionError("unsupported numeric marginal opened");
				}
			};
			Hooks hooks = new Hooks() {
				public boolean supportsWeightedNumericAggregates() {
					return false;
				}
			};
			eq("[[7, 0, 0]]", show(run(ir, plan, compiled, hooks)));
			eq(BigInteger.valueOf(5), hooks.sums.get("0:0"));
			eq(BigInteger.valueOf(14), hooks.sums.get("1:0"));
			eq(2L, hooks.counts.get("1:0"));
		}
	}

	@Test
	public void spillChannelsMergeIndependentlyWithoutAddingDistinctCardinalities() throws Exception {
		var before = NativeCountGroupStoreTest.files();
		var manager = NativeCountGroupStoreTest.manager();
		try (var ledger = manager.openQuery();
				var store = new NativeCountGroupStore(1, new boolean[] { false, true, false }, x -> x, null, ledger,
						4096)) {
			for (int g = 0; g < 700; g++)
				store.addChannel(new long[] { g }, -1, -1, 1);
			for (int pass = 0; pass < 3; pass++)
				for (int g = 0; g < 700; g++) {
					store.addChannel(new long[] { g }, 0, 0, 5);
					store.addChannel(new long[] { g }, 1, pass % 2, Long.MAX_VALUE);
					store.addChannel(new long[] { g }, 2, g % 2 == 0 ? -1 : 0, 7);
				}
			List<String> expected = new ArrayList<>();
			for (int g = 0; g < 700; g++)
				expected.add("[" + g + ", 15, 2, " + (g % 2 == 0 ? 0 : 21) + "]");
			eq(expected, NativeCountGroupStoreTest.drain(store, 4));
			yes(store.spilledRecords() > 0);
		}
		eq(0L, manager.usedBytes());
		eq(before, NativeCountGroupStoreTest.files());
	}

	@Test
	public void projectionShapeAndScalarCountShortcutRemainDistinct() {
		Kernel plain = shape(3, new int[] { 0 }, AggregateOutput.countStar());
		yes(plain.aggregateProjections == null);
		Kernel multi = shape(3, new int[] { 0 }, AggregateOutput.countDistinct(1), AggregateOutput.countDistinct(2));
		yes(multi.aggregateProjections != null);
		String old = System.getProperty(FACTOR_MARGINALS_PROPERTY);
		try {
			System.setProperty(FACTOR_MARGINALS_PROPERTY, "false");
			Kernel off = shape(3, new int[] { 0 }, AggregateOutput.countDistinct(1), AggregateOutput.countDistinct(2));
			yes(off.aggregateProjections == null);
			yes(!off.shapeKey().equals(multi.shapeKey()));
		} finally {
			if (old == null)
				System.clearProperty(FACTOR_MARGINALS_PROPERTY);
			else
				System.setProperty(FACTOR_MARGINALS_PROPERTY, old);
		}
	}

	@Test
	public void singletonAndScalarWindowsUseOneAllChannelPass() throws Exception {
		Kernel ir = shape(3, new int[] { 0 }, AggregateOutput.countStar(), AggregateOutput.countDistinct(1),
				AggregateOutput.sum(2));
		try (HeapFactorSource owner = new HeapFactorSource(this)) {
			FactorEnvironment env = new FactorEnvironment(3);
			env.bind(1, heap(owner, 9), 0, 1);
			env.bind(2, heap(owner, 11), 0, 1);
			for (boolean compiled : new boolean[] { false, true }) {
				Hooks h = new Hooks();
				Plan p = new Plan(new FactorEnvironment[] { env }, new long[][] { { 7, 0, 0 } }, new long[] { 13 }, 0,
						1, 2);
				eq("[[7, 13, 1, 0]]", show(run(ir, p, compiled, h)));
				eq(BigInteger.valueOf(143), h.sums.get("2:0"));
				eq(1L, h.calls);
			}
			Plan p = new Plan(new FactorEnvironment[] { env }, new long[][] { { 7, 0, 0 } }, new long[] { 13 }, 0, 1,
					2);
			try (KernelMarginalCursor c = KernelMarginalCursor.open(p, 3, new int[][] { { 0 }, { 0, 1 }, { 0, 2 } },
					new boolean[] { false, false, true }, null)) {
				yes(c.nextBatch());
				eq(1, c.flatRowCount());
				eq("[7, 9, 11]", Arrays.toString(c.flatValues()));
				eq(13L, c.flatWeights()[0]);
				c.finishFlat();
				yes(!c.nextBatch());
			}
		}
	}

	@Test
	public void factorReadersGrowWhenOneBatchChangesFromSingletonToLargeLanes() {
		try (HeapFactorSource owner = new HeapFactorSource(this)) {
			BorrowedFactorBatch b = new BorrowedFactorBatch(owner, 2);
			b.reset(2);
			b.bindHeap(0, new long[] { 9 }, 0, 1);
			long[] many = new long[517];
			Arrays.setAll(many, i -> i + 17);
			b.bindHeap(1, many, 0, many.length);
			FactorEnvironment env = new FactorEnvironment(1);
			try (FactorProductCursor c = new FactorProductCursor(1, 256)) {
				env.bind(0, b, 0, 0);
				c.bind(env, 1, 1, false);
				yes(c.next());
				eq(9L, c.value(0));
				yes(!c.next());
				env.clear();
				env.bind(0, b, 1, 0);
				c.bind(env, 1, 1, false);
				int count = 0;
				while (c.next()) {
					eq(many[count++], c.value(0));
					eq(1L, c.exactMultiplicity());
				}
				eq(517, count);
				env.clear();
				env.bind(0, b, 0, 0);
				c.bind(env, 1, 1, false);
				yes(c.next());
				eq(9L, c.value(0));
				yes(!c.next());
			}
		}
	}

}
