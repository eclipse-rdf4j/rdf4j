/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import static org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeKernelIr.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.*;
import org.eclipse.rdf4j.sail.lmdb.factor.*;
import org.eclipse.rdf4j.sail.lmdb.factor.BorrowedTupleContractTest.WeightedTuples;

/** Actual IR, emitter, runtime and interpreter. Physical input plans are explicit test fixtures. */
public class FactorPlanKernelContractTest {
	static void equal(long expected, long actual) {
		if (expected != actual) throw new AssertionError(expected + " != " + actual);
	}
	static void check(boolean condition) { if (!condition) throw new AssertionError(); }
	static void fails(Class<? extends Throwable> type, Runnable action) {
		try { action.run(); }
		catch (Throwable failure) { if (type.isInstance(failure)) return; throw new AssertionError(failure); }
		throw new AssertionError("expected " + type);
	}

	static final class Groups implements KernelPlan {
		final FactorEnvironment[] environments;
		final long[][] scalars;
		final long[] weights;
		final int[] slots;
		boolean closed, cursorClosed, scalarOpen;
		int[] demanded;
		Groups(FactorEnvironment[] environments, long[][] scalars, long[] weights, int... slots) {
			this.environments = environments; this.scalars = scalars; this.weights = weights; this.slots = slots;
		}
		@Override public Cursor open() { scalarOpen = true; throw new AssertionError("grouped producer flattened"); }
		@Override public FactorCursor openFactors(int[] demand) { demanded = demand.clone(); return openFactors(); }
		@Override public FactorCursor openFactors() {
			return new FactorCursor() {
				int at = -1;
				@Override public boolean next() { return ++at < environments.length; }
				@Override public int slot(int column) { return slots[column]; }
				@Override public long scalar(int column) {
					if ((environments[at].mask() & (1L << slots[column])) != 0L)
						throw new AssertionError("deferred binding read as scalar");
					return scalars[at][column];
				}
				@Override public FactorEnvironment factors() { return environments[at]; }
				@Override public long multiplicity() { return weights[at]; }
				@Override public void close() { cursorClosed = true; }
			};
		}
		@Override public void close() { closed = true; }
	}

	static BorrowedFactorBatch heap(HeapFactorSource source, long... values) {
		BorrowedFactorBatch b = new BorrowedFactorBatch(source, 1);
		b.reset(1); b.bindHeap(0, values, 0, values.length); return b;
	}

	/** Metadata-only fixture. Its deliberately unreadable descriptor must never reach a decoder. */
	static final class Metadata extends BorrowedFactorBatch.Source {
		int opens;
		Metadata() { super(new Object()); }
		BorrowedFactorBatch batch(long count) {
			BorrowedFactorBatch b = new BorrowedFactorBatch(this, 1); b.reset(1);
			if (count != 0) b.bindNative(0, BorrowedFactorBatch.ENCODED_RUN, 1L, 0L, 0, count);
			return b;
		}
		@Override public BorrowedFactorBatch.Reader openReader() {
			opens++; throw new AssertionError("hidden factor payload touched");
		}
	}

	static Kernel shape(List<Node> suffix, int columns, int[] outCols, int[] groups, AggregateOutput... outputs) {
		List<Node> pipeline = new ArrayList<>(); pipeline.add(new PlanRows(0, outCols)); pipeline.addAll(suffix);
		return new Kernel(columns, pipeline, new Aggregate(groups, outputs, null, OutputMods.none()));
	}
	static List<Node> equalTo(int col, int constant) {
		return List.of(new FilterCompareId(false, Operand.col(col), Operand.constant(constant)));
	}

	static long[][] drain(JaninoKernel kernel, Kernel shape, KernelPlan plan, long[] constants,
			KernelCancellation cancellation) {
		KernelContext context = new KernelContext(null, constants, new long[0], null, null, null, new KernelPlan[]{plan}, 16);
		context.cancellation = cancellation;
		try {
			kernel.bind(context);
			int stride = shape.stride(); long[] buffer = new long[stride * 3]; List<long[]> result = new ArrayList<>(); int n;
			while ((n = kernel.fill(buffer, 3)) != 0)
				for (int i = 0; i < n; i++) result.add(Arrays.copyOfRange(buffer, i * stride, (i + 1) * stride));
			result.sort((a, b) -> {
				for (int i = 0; i < a.length; i++) { int c = Long.compareUnsigned(a[i], b[i]); if (c != 0) return c; }
				return 0;
			});
			return result.toArray(long[][]::new);
		} finally { kernel.close(); }
	}

	static void both(Kernel shape, Supplier<? extends KernelPlan> plans, long[] constants, long[][] expected) throws Exception {
		Path dir = Files.createTempDirectory("factor-ir-");
		try {
			JaninoKernel compiled = GeneratedBorrowedFtreeContractTest.compile(shape.className(), LmdbNativeKernelEmitter.emit(shape), dir);
			JaninoKernel interpreted = LmdbNativeKernelInterpreter.forAggregate(shape);
			check(interpreted != null);
			for (JaninoKernel kernel : new JaninoKernel[]{compiled, interpreted}) {
				KernelPlan plan = plans.get();
				long[][] actual = drain(kernel, shape, plan, constants, null);
				if (!Arrays.deepEquals(expected, actual))
					throw new AssertionError(Arrays.deepToString(expected) + " != " + Arrays.deepToString(actual));
				if (plan instanceof Groups grouped) check(grouped.closed && grouped.cursorClosed && !grouped.scalarOpen);
			}
		} finally { GeneratedBorrowedFtreeContractTest.remove(dir); }
	}

	@Test public void compiledAndInterpretedGuardsNeverOpenHiddenBillionRowFactors() throws Exception {
		try (HeapFactorSource source = new HeapFactorSource(this); Metadata hidden = new Metadata()) {
			FactorEnvironment env = new FactorEnvironment(3);
			env.bind(1, heap(source, 7, 7, 8), 0, 1);
			env.bind(2, hidden.batch(3_000_000_000L), 0, 1);
			Kernel k = shape(equalTo(1, 0), 3, new int[]{0, 1, 2}, new int[0], AggregateOutput.countStar());
			check(factorPlan(k) != null); check(Arrays.equals(new int[]{1}, factorPlan(k).scalarOutputs));
			both(k, () -> new Groups(new FactorEnvironment[]{env}, new long[][]{{11, 0, 0}}, new long[]{5}, 0, 1, 2),
					new long[]{7}, new long[][]{{30_000_000_000L}});
			equal(0, hidden.opens);
		}
	}

	@Test public void correlatedTupleMappingAndHighBitSlotsStayZipped() throws Exception {
		try (WeightedTuples tuples = new WeightedTuples(2, new long[][]{{7, 101}, {8, 202}, {7, 303}}, new long[]{2, 5, 3});
				Metadata hidden = new Metadata()) {
			FactorEnvironment env = new FactorEnvironment(64);
			env.bindTuple(new int[]{63, 2}, tuples.batch(), 0, 1L << 5);
			env.bind(17, hidden.batch(11), 0, 1L << 5);
			Kernel k = shape(equalTo(3, 0), 5, new int[]{3, 0, 4, 1}, new int[]{4}, AggregateOutput.countStar(), AggregateOutput.count(3));
			both(k, () -> new Groups(new FactorEnvironment[]{env}, new long[][]{{0, 47, 0, 0}}, new long[]{2}, 63, 5, 2, 17),
					new long[]{7}, new long[][]{{101, 44, 44}, {303, 66, 66}});
			equal(0, hidden.opens);
		}
	}

	@Test public void duplicateParentsNullableScalarCountsAndAliasesKeepBagSemantics() throws Exception {
		try (HeapFactorSource source = new HeapFactorSource(this)) {
			FactorEnvironment env = new FactorEnvironment(4); env.bind(2, heap(source, 9, 9, 10), 0, 3);
			Kernel k = shape(List.of(new BindAlias(Operand.col(0), 3),
					new FilterCompareId(false, Operand.col(3), Operand.constant(0))), 4, new int[]{0, 1, 2},
					new int[]{3}, AggregateOutput.countStar(), AggregateOutput.count(1));
			both(k, () -> new Groups(new FactorEnvironment[]{env, env, env}, new long[][]{{11, -1, 0}, {11, 17, 0}, {12, 17, 0}},
					new long[]{2, 3, 8}, 0, 1, 2), new long[]{11}, new long[][]{{11, 15, 9}});
			check(Arrays.equals(new int[]{0, 1}, factorPlan(k).scalarOutputs));
		}
	}

	@Test public void rejectedHiddenProductsDoNotOverflowAndEmptyGroupsDoNotAppear() throws Exception {
		try (Metadata source = new Metadata()) {
			FactorEnvironment env = new FactorEnvironment(2); env.bind(1, source.batch(2), 0, 1);
			Kernel k = shape(equalTo(0, 0), 2, new int[]{0, 1}, new int[0], AggregateOutput.countStar());
			both(k, () -> new Groups(new FactorEnvironment[]{env}, new long[][]{{7, 0}}, new long[]{Long.MAX_VALUE}, 0, 1),
					new long[]{8}, new long[][]{{0}});
			FactorEnvironment empty = new FactorEnvironment(3); empty.bind(1, source.batch(Long.MAX_VALUE), 0, 1); empty.bind(2, source.batch(0), 0, 1);
			Kernel grouped = shape(equalTo(0, 0), 3, new int[]{0, 1, 2}, new int[]{0}, AggregateOutput.countStar());
			both(grouped, () -> new Groups(new FactorEnvironment[]{empty}, new long[][]{{7, 0, 0}}, new long[]{Long.MAX_VALUE}, 0, 1, 2),
					new long[]{7}, new long[0][]);
		}
	}

	@Test public void acceptedOverflowClosesBothExecutionTiers() throws Exception {
		try (Metadata source = new Metadata()) {
			FactorEnvironment env = new FactorEnvironment(2); env.bind(1, source.batch(2), 0, 1);
			Kernel k = shape(equalTo(0, 0), 2, new int[]{0, 1}, new int[0], AggregateOutput.countStar());
			Path dir = Files.createTempDirectory("factor-overflow-");
			try {
				for (JaninoKernel kernel : new JaninoKernel[]{LmdbNativeKernelInterpreter.forAggregate(k),
						GeneratedBorrowedFtreeContractTest.compile(k.className(), LmdbNativeKernelEmitter.emit(k), dir)}) {
					Groups plan = new Groups(new FactorEnvironment[]{env}, new long[][]{{7, 0}}, new long[]{Long.MAX_VALUE}, 0, 1);
					fails(ArithmeticException.class, () -> drain(kernel, k, plan, new long[]{7}, null));
					check(plan.cursorClosed && plan.closed);
				}
			} finally { GeneratedBorrowedFtreeContractTest.remove(dir); }
		}
	}

	@Test public void exactWeightedFallbackAndUnitWeightDefaultApplyGuards() throws Exception {
		Kernel k = shape(equalTo(0, 0), 1, new int[]{0}, new int[0], AggregateOutput.countStar());
		both(k, () -> new KernelPlan() {
			@Override public Cursor open() { return new Cursor() {
				boolean done;
				@Override public int fill(long[] out, int max) { throw new AssertionError("exact weighted fallback lost"); }
				@Override public int fillWeighted(long[] out, long[] weights, int max) {
					if (done) return 0; done = true; out[0] = 7; out[1] = 8; weights[0] = 3_000_000_000L; weights[1] = 5; return 2;
				}
				@Override public void close() { }
			}; }
			@Override public void close() { }
		}, new long[]{7}, new long[][]{{3_000_000_000L}});
		both(k, () -> new KernelPlan() {
			@Override public Cursor open() { return new Cursor() {
				boolean done;
				@Override public int fill(long[] out, int max) { if (done) return 0; done = true; out[0] = 7; out[1] = 8; return 2; }
				@Override public void close() { }
			}; }
			@Override public void close() { }
		}, new long[]{7}, new long[][]{{1}});
	}

	@Test public void randomSmallProductsMatchFlatOracleAcrossBothTiers() throws Exception {
		Kernel k = shape(List.of(new FilterRangeUnsigned(Operand.col(1), 0, 1)), 3, new int[]{0, 1, 2}, new int[]{0}, AggregateOutput.countStar());
		Path dir = Files.createTempDirectory("factor-random-");
		try (HeapFactorSource source = new HeapFactorSource(this)) {
			JaninoKernel compiled = GeneratedBorrowedFtreeContractTest.compile(k.className(), LmdbNativeKernelEmitter.emit(k), dir);
			Random random = new Random(69117);
			for (int trial = 0; trial < 120; trial++) {
				int roots = 1 + random.nextInt(8); FactorEnvironment[] envs = new FactorEnvironment[roots];
				long[][] scalars = new long[roots][3]; long[] weights = new long[roots];
				Map<Long, Long> reference = new TreeMap<>(Long::compareUnsigned);
				for (int root = 0; root < roots; root++) {
					long[] a = new long[random.nextInt(15)], b = new long[random.nextInt(10)];
					for (int i = 0; i < a.length; i++) a[i] = Long.MIN_VALUE + random.nextInt(8);
					for (int i = 0; i < b.length; i++) b[i] = i / 2;
					envs[root] = new FactorEnvironment(3); envs[root].bind(1, heap(source, a), 0, 1); envs[root].bind(2, heap(source, b), 0, 1);
					scalars[root][0] = random.nextInt(3); weights[root] = 1 + random.nextInt(5);
					for (long value : a) if (Long.compareUnsigned(value, Long.MIN_VALUE + 2) >= 0 && Long.compareUnsigned(value, Long.MIN_VALUE + 5) <= 0)
						for (long ignored : b) reference.merge(scalars[root][0], weights[root], Math::addExact);
				}
				long[][] expected = reference.entrySet().stream().map(e -> new long[]{e.getKey(), e.getValue()}).toArray(long[][]::new);
				for (JaninoKernel kernel : new JaninoKernel[]{compiled.getClass().getConstructor().newInstance(), LmdbNativeKernelInterpreter.forAggregate(k)}) {
					long[][] actual = drain(kernel, k, new Groups(envs, scalars, weights, 0, 1, 2), new long[]{Long.MIN_VALUE + 2, Long.MIN_VALUE + 5}, null);
					if (!Arrays.deepEquals(expected, actual)) throw new AssertionError("bag mismatch, trial=" + trial + ", " + Arrays.deepToString(actual));
				}
			}
		} finally { GeneratedBorrowedFtreeContractTest.remove(dir); }
	}

	@Test public void selectionClipsDuplicateWeightsWithoutOpeningSibling() throws Exception {
		try (HeapFactorSource source = new HeapFactorSource(this); Metadata hidden = new Metadata(); SelectedFactorSource selected = new SelectedFactorSource(2)) {
			BorrowedFactorBatch b = heap(source, 7, 7, 7, 8, 9, 9);
			FactorSelection ranges = new FactorSelection(2); ranges.add(1, 5);
			FactorEnvironment env = new FactorEnvironment(3); env.bind(1, selected.select(b, 0, ranges), 0, 1); env.bind(2, hidden.batch(13), 0, 1);
			Kernel k = shape(equalTo(1, 0), 3, new int[]{0, 1, 2}, new int[0], AggregateOutput.countStar());
			both(k, () -> new Groups(new FactorEnvironment[]{env}, new long[][]{{11, 0, 0}}, new long[]{3}, 0, 1, 2), new long[]{7}, new long[][]{{78}});
		}
	}

	@Test public void cancellationInterruptsLargeRejectingFactor() throws Exception {
		try (HeapFactorSource source = new HeapFactorSource(this)) {
			long[] values = new long[100_000]; for (int i = 0; i < values.length; i++) values[i] = i + 1;
			FactorEnvironment env = new FactorEnvironment(2); env.bind(1, heap(source, values), 0, 1);
			Kernel k = shape(equalTo(1, 0), 2, new int[]{0, 1}, new int[0], AggregateOutput.countStar());
			Path dir = Files.createTempDirectory("factor-cancel-");
			try {
				for (JaninoKernel kernel : new JaninoKernel[]{LmdbNativeKernelInterpreter.forAggregate(k),
						GeneratedBorrowedFtreeContractTest.compile(k.className(), LmdbNativeKernelEmitter.emit(k), dir)}) {
					int[] polls = {0}; KernelCancellation cancellation = new KernelCancellation(System.nanoTime() + 60_000_000_000L, () -> ++polls[0] >= 8);
					Groups plan = new Groups(new FactorEnvironment[]{env}, new long[][]{{7, 0}}, new long[]{1}, 0, 1);
					fails(KernelCancelledException.class, () -> drain(kernel, k, plan, new long[]{-7}, cancellation));
					check(plan.cursorClosed && plan.closed); check(polls[0] < 20);
				}
			} finally { GeneratedBorrowedFtreeContractTest.remove(dir); }
		}
	}

	@Test public void demandVerifierAndShapeKeyDistinguishScalarFromDeferredBindings() {
		Kernel on = shape(equalTo(0, 0), 2, new int[]{0, 1}, new int[0], AggregateOutput.countStar());
		check(on.shapeKey().contains("PF[")); check(!weightedPlanCount(on));
		String saved = System.getProperty(FACTOR_PLAN_PROPERTY);
		try {
			System.setProperty(FACTOR_PLAN_PROPERTY, "false");
			Kernel off = shape(equalTo(0, 0), 2, new int[]{0, 1}, new int[0], AggregateOutput.countStar());
			check(factorPlan(off) == null); check(!on.shapeKey().equals(off.shapeKey()));
		} finally { if (saved == null) System.clearProperty(FACTOR_PLAN_PROPERTY); else System.setProperty(FACTOR_PLAN_PROPERTY, saved); }
		fails(IllegalArgumentException.class, () -> new Kernel(2, List.of(new PlanFactors(0, new int[]{0, 1}, new Operand[0], new int[0])),
				new Aggregate(new int[0], new AggregateOutput[]{AggregateOutput.count(1)}, null, OutputMods.none())));
		fails(IllegalArgumentException.class, () -> new PlanFactors(0, new int[]{0}, new Operand[0], new int[]{0, 0}));
		Kernel distinct = shape(equalTo(0, 0), 2, new int[]{0, 1}, new int[0], AggregateOutput.countDistinct(1));
		check(factorPlan(distinct) == null);
		Kernel oldShortcut = shape(List.of(), 2, new int[]{0, 1}, new int[0], AggregateOutput.countStar());
		check(weightedPlanCount(oldShortcut)); check(factorPlan(oldShortcut) == null);
	}

	@Test public void explicitZeroColumnGroupedCountNeedsNoPayloadAndSupportsLongWeights() throws Exception {
		try (Metadata source = new Metadata()) {
			FactorEnvironment env = new FactorEnvironment(2); env.bind(1, source.batch(1L << 34), 0, 1);
			Kernel k = new Kernel(0, List.of(new PlanFactors(0, new int[0], new Operand[0], new int[0])),
					new Aggregate(new int[0], new AggregateOutput[]{AggregateOutput.countStar()}, null, OutputMods.none()));
			both(k, () -> new Groups(new FactorEnvironment[]{env}, new long[][]{{}}, new long[]{3}), new long[0], new long[][]{{3L << 34}});
			equal(0, source.opens);
		}
	}

	@Test public void cachedMultiplicityStillRejectsInvalidationAndBadSchemasClose() {
		try (HeapFactorSource source = new HeapFactorSource(this)) {
			BorrowedFactorBatch batch = heap(source, 1, 2); FactorEnvironment env = new FactorEnvironment(2); env.bind(1, batch, 0, 1);
			Groups plan = new Groups(new FactorEnvironment[]{env}, new long[][]{{7}}, new long[]{3}, 0);
			try (KernelFactorCursor cursor = KernelFactorCursor.open(plan, 1, new int[]{0}, null)) {
				check(cursor.next()); equal(6, cursor.multiplicity()); equal(6, cursor.multiplicity());
				batch.reset(1); fails(IllegalStateException.class, cursor::multiplicity);
			}
			check(plan.cursorClosed);
			Groups invalid = new Groups(new FactorEnvironment[0], new long[0][], new long[0], 64);
			fails(IndexOutOfBoundsException.class, () -> KernelFactorCursor.open(invalid, 1, new int[]{0}, null));
			check(invalid.cursorClosed);
			fails(IllegalArgumentException.class, () -> KernelFactorCursor.open(invalid, 1, new int[]{0, 0}, null));
		}
	}

	@Test public void allUnboundCountPreservesZeroGroupsWithoutMultiplyingUnobservedProducts() throws Exception {
		try (Metadata source = new Metadata()) {
			FactorEnvironment env = new FactorEnvironment(3); env.bind(2, source.batch(2), 0, 3);
			Kernel k = shape(equalTo(0, 0), 3, new int[]{0, 1, 2}, new int[]{0}, AggregateOutput.count(1));
			both(k, () -> new Groups(new FactorEnvironment[]{env}, new long[][]{{11, -1, 0}}, new long[]{Long.MAX_VALUE}, 0, 1, 2),
					new long[]{11}, new long[][]{{11, 0}});
		}
	}

	@Test public void groupedProducerCannotHideInsideAnUnsupportedAlgebraContainer() {
		PlanFactors factors = new PlanFactors(0, new int[]{0}, new Operand[0], new int[]{0});
		fails(IllegalArgumentException.class, () -> new Kernel(1, List.of(new Exists(false, List.of(factors))),
				new Aggregate(new int[0], new AggregateOutput[]{AggregateOutput.countStar()}, null, OutputMods.none())));
		fails(IllegalArgumentException.class, () -> new PlanFactors(-1, new int[0], new Operand[0], new int[0]));
		fails(IllegalArgumentException.class, () -> new PlanFactors(0, new int[]{64}, new Operand[0], new int[0]));
	}

	@Test public void prefixFoldRetainsMultipleCountStarChannelsAndSourceOptimizerSemantics() throws Exception {
		try (HeapFactorSource source = new HeapFactorSource(this); Metadata hidden = new Metadata()) {
			FactorEnvironment env = new FactorEnvironment(3);
			env.bind(1, heap(source, 7, 7, 8), 0, 1); env.bind(2, hidden.batch(3_000_000_000L), 0, 1);
			Kernel k = shape(equalTo(1, 0), 3, new int[]{0, 1, 2}, new int[0], AggregateOutput.countStar(), AggregateOutput.countStar());
			check(foldFactorCounts(k));
			both(k, () -> new Groups(new FactorEnvironment[]{env}, new long[][]{{11, 0, 0}}, new long[]{5}, 0, 1, 2),
					new long[]{7}, new long[][]{{30_000_000_000L, 30_000_000_000L}});
			String optimized = LmdbNativeGeneratedSourceOptimizer.optimize(LmdbNativeKernelEmitter.emit(k), false);
			Path dir = Files.createTempDirectory("factor-source-optimizer-");
			try {
				JaninoKernel compiled = GeneratedBorrowedFtreeContractTest.compile(k.className(), optimized, dir);
				long[][] result = drain(compiled, k, new Groups(new FactorEnvironment[]{env}, new long[][]{{11, 0, 0}}, new long[]{5}, 0, 1, 2), new long[]{7}, null);
				check(Arrays.deepEquals(result, new long[][]{{30_000_000_000L, 30_000_000_000L}}));
			} finally { GeneratedBorrowedFtreeContractTest.remove(dir); }
		}
	}

	@Test public void structuredConsumptionExposesPrefixBoundariesAndIndependentReaders() {
		try (HeapFactorSource source = new HeapFactorSource(this)) {
			FactorEnvironment env = new FactorEnvironment(3);
			env.bind(1, heap(source, 7, 7, 8), 0, 1); env.bind(2, heap(source, 10, 11), 0, 1);
			Groups plan = new Groups(new FactorEnvironment[]{env, env}, new long[][]{{11, 0, 0}, {12, 0, 0}}, new long[]{3, 5}, 0, 1, 2);
			try (KernelFactorCursor a = KernelFactorCursor.open(plan, 3, new int[]{0, 1}, null);
					KernelFactorCursor b = KernelFactorCursor.open(plan, 3, new int[]{0, 1}, null)) {
				check(a.nextPrefix()); check(b.nextPrefix()); check(a.nextBinding()); check(b.nextBinding());
				equal(7, a.values()[1]); equal(7, b.values()[1]); equal(2, a.openedMultiplicity());
				check(a.nextBinding()); equal(8, a.values()[1]); equal(7, b.values()[1]);
				check(!a.nextBinding()); equal(6, a.remainderMultiplicity());
				fails(IllegalStateException.class, a::openedMultiplicity);
				check(a.nextPrefix()); check(a.nextBinding()); equal(12, a.values()[0]); equal(10, a.remainderMultiplicity());
				check(a.nextBinding()); check(!a.nextBinding()); check(!a.nextPrefix());
				fails(IllegalStateException.class, a::remainderMultiplicity);
			}
		}
	}

	@Test public void bulkFallbackWindowsValidateBoundsWeightsAndDoNotMixWithScalarSteps() {
		int[] closed = {0};
		KernelPlan plan = new KernelPlan() {
			@Override public Cursor open() { return new Cursor() {
				@Override public int fill(long[] rows, int maximum) { rows[0] = 7; rows[1] = 8; return 2; }
				@Override public void close() { closed[0]++; }
			}; }
			@Override public void close() { }
		};
		try (KernelFactorCursor cursor = KernelFactorCursor.open(plan, 1, new int[]{0}, null)) {
			check(!cursor.grouped()); equal(2, cursor.nextRowWindow()); equal(7, cursor.rowValues()[0]); equal(1, cursor.rowWeights()[1]);
			fails(IllegalStateException.class, cursor::next);
		}
		equal(1, closed[0]);
		KernelPlan invalid = new KernelPlan() {
			@Override public Cursor open() { return new Cursor() {
				@Override public int fill(long[] rows, int maximum) { throw new AssertionError(); }
				@Override public int fillWeighted(long[] rows, long[] weights, int maximum) { rows[0] = 7; weights[0] = 0; return 1; }
				@Override public void close() { closed[0]++; }
			}; }
			@Override public void close() { }
		};
		try (KernelFactorCursor cursor = KernelFactorCursor.open(invalid, 1, new int[]{0}, null)) {
			fails(IllegalStateException.class, cursor::nextRowWindow);
		}
		equal(2, closed[0]);
	}

	@Test public void cancellationPollsContinueInsideRejectedScalarWindows() throws Exception {
		Kernel k = shape(equalTo(0, 0), 1, new int[]{0}, new int[0], AggregateOutput.countStar());
		Path dir = Files.createTempDirectory("factor-row-cancel-");
		try {
			for (JaninoKernel kernel : new JaninoKernel[]{LmdbNativeKernelInterpreter.forAggregate(k),
					GeneratedBorrowedFtreeContractTest.compile(k.className(), LmdbNativeKernelEmitter.emit(k), dir)}) {
				int[] closed = {0}, polls = {0}, windows = {0};
				KernelPlan plan = new KernelPlan() {
					@Override public Cursor open() { return new Cursor() {
						@Override public int fill(long[] rows, int maximum) { windows[0]++; Arrays.fill(rows, 7); return maximum; }
						@Override public void close() { closed[0]++; }
					}; }
					@Override public void close() { }
				};
				KernelCancellation cancellation = new KernelCancellation(System.nanoTime() + 60_000_000_000L, () -> ++polls[0] >= 8);
				fails(KernelCancelledException.class, () -> drain(kernel, k, plan, new long[]{8}, cancellation));
				equal(1, closed[0]); check(windows[0] <= 8);
			}
		} finally { GeneratedBorrowedFtreeContractTest.remove(dir); }
	}

	@Test public void fallbackFailureRemainsPrimaryWhenCloseAlsoFails() {
		IllegalStateException primary = new IllegalStateException("read"); IllegalArgumentException cleanup = new IllegalArgumentException("close");
		KernelPlan plan = new KernelPlan() {
			@Override public Cursor open() { return new Cursor() {
				@Override public int fill(long[] target, int maximum) { throw primary; }
				@Override public void close() { throw cleanup; }
			}; }
			@Override public void close() { }
		};
		KernelFactorCursor cursor = KernelFactorCursor.open(plan, 0, new int[0], null);
		try { cursor.next(); throw new AssertionError("failure lost"); }
		catch (IllegalStateException failure) { check(failure == primary); check(Arrays.equals(new Throwable[]{cleanup}, failure.getSuppressed())); }
		cursor.close();
	}
}
