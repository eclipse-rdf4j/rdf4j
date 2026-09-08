/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import static org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeKernelIr.*;
import static org.eclipse.rdf4j.sail.lmdb.evaluation.FactorPlanKernelContractTest.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.*;
import org.eclipse.rdf4j.sail.lmdb.factor.*;
import org.eclipse.rdf4j.sail.lmdb.factor.BorrowedTupleContractTest.WeightedTuples;

/** Actual emitter and strict compiler backend. No interpreter is substituted for a compiled failure. */
public class CompiledCountSpecializationTest {
	private static final long[] EXTREMES = {0, 1, -1, -2, Long.MIN_VALUE, Long.MIN_VALUE + 1, Long.MAX_VALUE};

	static final class Rows implements KernelPlan {
		final long[][] rows;
		final long[] weights;
		int closes, cursorCloses;
		Rows(long[][] rows, long[] weights) { this.rows = rows; this.weights = weights; }
		@Override public Cursor open() {
			return new Cursor() {
				int at;
				@Override public int fill(long[] target, int max) { throw new AssertionError("weighted transfer required"); }
				@Override public int fillWeighted(long[] target, long[] outputWeights, int max) {
					int n = Math.min(max, rows.length - at);
					for (int i = 0; i < n; i++) {
						System.arraycopy(rows[at], 0, target, i * rows[at].length, rows[at].length);
						outputWeights[i] = weights[at++];
					}
					return n;
				}
				@Override public void close() { cursorCloses++; }
			};
		}
		@Override public void close() { closes++; }
	}

	private static Kernel make(List<Node> guards, int width, boolean enabled) {
		String old = System.getProperty(COUNT_SPECIALIZATION_PROPERTY);
		try {
			System.setProperty(COUNT_SPECIALIZATION_PROPERTY, Boolean.toString(enabled));
			return shape(guards, width, java.util.stream.IntStream.range(0, width).toArray(), new int[0], AggregateOutput.countStar());
		} finally {
			if (old == null) System.clearProperty(COUNT_SPECIALIZATION_PROPERTY); else System.setProperty(COUNT_SPECIALIZATION_PROPERTY, old);
		}
	}

	private record Bound(JaninoKernel kernel) implements AutoCloseable {
		@Override public void close() { kernel.close(); }
	}
	private static Bound compiled(Kernel k, long[] constants) throws Exception {
		JaninoKernel result = KernelCompilationTestSupport.compile(k);
		result.bind(new KernelContext(null, constants, new long[0], null, null, null,
				new KernelPlan[] {new Rows(new long[0][], new long[0])}, 16));
		return new Bound(result);
	}

	@Test public void specializationIsCapturedInShapeWithoutCapturingValues() {
		List<Node> guards = List.of(new FilterRangeUnsigned(Operand.col(1), 0, 1));
		Kernel on = make(guards, 2, true), off = make(guards, 2, false);
		assertTrue(on.compiledCountSpecialization);
		assertFalse(off.compiledCountSpecialization);
		assertNotEquals(on.shapeKey(), off.shapeKey());
		String first = LmdbNativeKernelEmitter.emit(on);
		assertTrue(first.contains("private long factorSum"));
		assertTrue(first.contains("long acceptedWeight = 0L"));
		assertFalse(LmdbNativeKernelEmitter.emit(off).contains("private long factorSum"));
		String old = System.getProperty(COUNT_SPECIALIZATION_PROPERTY);
		try {
			System.setProperty(COUNT_SPECIALIZATION_PROPERTY, "false");
			assertEquals(first, LmdbNativeKernelEmitter.emit(on));
		} finally { if (old == null) System.clearProperty(COUNT_SPECIALIZATION_PROPERTY); else System.setProperty(COUNT_SPECIALIZATION_PROPERTY, old); }
	}

	@Test public void allGuardFamiliesAndColumnLayoutsMatchIndependentOracle() throws Exception {
		List<List<Node>> shapes = List.of(
				List.of(new FilterRangeUnsigned(Operand.col(0), 0, 1)),
				List.of(new FilterCompareId(false, Operand.col(0), Operand.col(1))),
				List.of(new FilterCompareId(true, Operand.col(0), Operand.constant(2))),
				List.of(new FilterEntryCompatible(Operand.col(0), 2)),
				List.of(new FilterInConstants(Operand.col(0), new int[] {0, 1, 2, 3})),
				List.of(new FilterRangeUnsigned(Operand.col(0), 0, 1), new FilterCompareId(true, Operand.col(0), Operand.col(1))),
				List.of(new FilterRangeUnsigned(Operand.col(0), 0, 1), new FilterEntryCompatible(Operand.col(0), 2),
						new FilterCompareId(false, Operand.col(0), Operand.col(1)), new FilterInConstants(Operand.col(1), new int[] {0, 3})),
				List.of(new FilterRangeUnsigned(Operand.col(0), 0, 1), new FilterCompareId(true, Operand.col(0), Operand.constant(2)),
						new FilterInConstants(Operand.col(0), new int[] {0, 2, 3})),
				List.of(new FilterInConstants(Operand.col(0), new int[] {2})),
				List.of(new FilterRangeUnsigned(Operand.col(0), 0, 1), new FilterRangeUnsigned(Operand.col(0), 2, 3)));
		Random random = new Random(921204);
		for (List<Node> guards : shapes) {
			Kernel k = make(guards, 2, true);
			for (int trial = 0; trial < 7; trial++) {
				long[] constants = {EXTREMES[trial], EXTREMES[6 - trial], EXTREMES[(trial + 2) % 7], random.nextLong()};
				try (Bound bound = compiled(k, constants)) {
					JaninoKernel kernel = bound.kernel();
					KernelFactorPredicate predicate = (KernelFactorPredicate) kernel;
					int width = factorPlan(k).scalarOutputs.length;
					for (int n : new int[] {0, 1, 7, 8, 31, 255, 256, 257}) {
						long[][] all = new long[width][n + 3];
						long[] prefix = new long[width], weights = new long[n + 3];
						for (int c = 0; c < width; c++) {
							prefix[c] = EXTREMES[(trial + c) % 7];
							for (int i = 0; i < all[c].length; i++) all[c][i] = i % 3 == 0 ? random.nextLong() : EXTREMES[(i + c) % 7];
						}
						for (int i = 0; i < weights.length; i++) weights[i] = 1 + random.nextInt(32);
						for (int layout = 0; layout < (1 << width); layout++) {
							long[][] columns = new long[width][];
							for (int c = 0; c < width; c++) if ((layout & (1 << c)) != 0) columns[c] = all[c];
							for (long mask = 0; mask < (1L << guards.size()); mask++) {
								long expected = 0;
								for (int i = 0; i < n; i++) {
									long[] row = new long[2];
									for (int c = 0; c < width; c++) row[factorPlan(k).scalarOutputs[c]] = columns[c] == null ? prefix[c] : columns[c][i];
									boolean accepted = true;
									for (int g = 0; g < guards.size(); g++) if ((mask & (1L << g)) != 0) accepted &= accepts(guards.get(g), row, constants);
									if (accepted) expected = Math.addExact(expected, weights[i]);
								}
								assertEquals(expected, predicate.sum(mask, columns, prefix, weights, new long[n], n));
							}
						}
					}
				}
			}
		}
	}

	private static long operand(Operand o, long[] row, long[] constants) {
		return o.kind == Operand.COL ? row[o.index] : constants[o.index];
	}
	private static boolean accepts(Node node, long[] row, long[] constants) {
		if (node instanceof FilterCompareId f) return (operand(f.left, row, constants) == operand(f.right, row, constants)) != f.negated;
		if (node instanceof FilterRangeUnsigned f) {
			long v = operand(f.value, row, constants);
			return Long.compareUnsigned(v, constants[f.lowConstant]) >= 0 && Long.compareUnsigned(v, constants[f.highConstant]) <= 0;
		}
		if (node instanceof FilterEntryCompatible f) { long v = operand(f.value, row, constants); return v == -1 || v == constants[f.constant]; }
		FilterInConstants f = (FilterInConstants) node;
		for (int c : f.constantIndices) if (operand(f.value, row, constants) == constants[c]) return true;
		return false;
	}

	@Test public void finiteDomainIsCheckedBeforeTheElementLoopWithoutChangingBagWeights() throws Exception {
		List<Node> guards = List.of(new FilterInConstants(Operand.col(0), new int[] {0, 1, 2, 3}),
				new FilterRangeUnsigned(Operand.col(0), 4, 5), new FilterCompareId(true, Operand.col(0), Operand.constant(2)));
		Kernel k = make(guards, 1, true);
		String source = LmdbNativeKernelEmitter.emit(k);
		assertTrue(source.contains("long pick0 ="));
		int method = source.indexOf("private long factorSum0_1");
		String loop = source.substring(source.indexOf("for (int i = 0;", method), source.indexOf("return total;", method));
		assertFalse(loop.contains("belowMask"));
		assertTrue(loop.contains("pick0"));
		for (long[] constants : new long[][] {{3, 3, 4, 8, 3, 8}, {3, 3, 3, 8, 3, 8},
				{Long.MIN_VALUE, -1, 0, 0, Long.MIN_VALUE, -1}, {3, 3, 4, 8, 8, 3}}) {
			try (Bound bound = compiled(k, constants)) {
				long[] values = {3, 3, 4, 8, 9, Long.MIN_VALUE, -1, 0};
				long[] weights = {2, 5, 11, 13, 17, 19, 23, 29};
				long expected = 0;
				for (int i = 0; i < values.length; i++) {
					boolean accepted = true;
					for (Node guard : guards) accepted &= accepts(guard, new long[] {values[i]}, constants);
					if (accepted) expected += weights[i];
				}
				assertEquals(expected, ((KernelFactorPredicate) bound.kernel()).sum(7, new long[][] {values}, new long[1], weights, new long[8], 8));
			}
		}
	}

	@Test public void unsignedIntervalWidthIdentityHoldsOnRandomFullWidthIds() throws Exception {
		Kernel k = make(List.of(new FilterRangeUnsigned(Operand.col(0), 0, 1)), 1, true);
		Random r = new Random(187302);
		for (int t = 0; t < 80; t++) {
			long low = r.nextLong(), high = r.nextLong();
			try (Bound bound = compiled(k, new long[] {low, high})) {
				JaninoKernel c = bound.kernel();
				long[] values = new long[259], weights = new long[259]; Arrays.fill(weights, 1);
				for (int i = 0; i < values.length; i++) values[i] = r.nextLong();
				values[0] = low; values[1] = high; values[2] = low - 1; values[3] = high + 1;
				long expected = Arrays.stream(values).filter(v -> Long.compareUnsigned(v, low) >= 0 && Long.compareUnsigned(v, high) <= 0).count();
				assertEquals(expected, ((KernelFactorPredicate) c).sum(1, new long[][] {values}, new long[1], weights, new long[259], 259));
			}
		}
	}

	@Test public void boundedSumSupportsMaximumCardinalityAndValidatesTails() throws Exception {
		Kernel k = make(List.of(new FilterRangeUnsigned(Operand.col(0), 0, 1)), 1, true);
		try (Bound bound = compiled(k, new long[] {0, -1})) {
			JaninoKernel c = bound.kernel();
			KernelFactorPredicate predicate = (KernelFactorPredicate) c;
			long[][] input = {{0, -1, Long.MIN_VALUE}}; long[] weights = {Long.MAX_VALUE - 2, 1, 1};
			long[] untouched = {71, 72, 73};
			assertEquals(Long.MAX_VALUE, predicate.sum(1, input, new long[1], weights, untouched, 3));
			assertArrayEquals(new long[] {71, 72, 73}, untouched);
			assertThrows(IndexOutOfBoundsException.class, () -> predicate.sum(1, input, new long[1], weights, untouched, 4));
			assertThrows(IndexOutOfBoundsException.class, () -> predicate.sum(1, new long[][] {{1}}, new long[1], weights, untouched, 3));
			assertThrows(IndexOutOfBoundsException.class, () -> predicate.sum(1, input, new long[1], weights, untouched, -1));
		}
	}

	@Test public void codeGrowthIsBoundedAndLargeShapesRetainGenericVectorCode() {
		List<Node> guards = new ArrayList<>();
		for (int i = 0; i < 64; i++) guards.add(new FilterCompareId(false, Operand.col(0), Operand.constant(0)));
		Kernel k = make(guards, 1, true);
		assertNotNull(k.factorCountGuards); assertFalse(k.compiledCountSpecialization);
		assertFalse(LmdbNativeKernelEmitter.emit(k).contains("private long factorSum"));
	}

	@Test public void unboundedBroadcastConjunctionKeepsVectorPassesButFiniteDomainStillFuses() {
		List<Node> guards = List.of(new FilterRangeUnsigned(Operand.col(0), 0, 1),
				new FilterCompareId(true, Operand.col(0), Operand.col(1)));
		String source = LmdbNativeKernelEmitter.emit(make(guards, 2, true));
		assertTrue(source.contains("private long factorSum0_3"));
		assertFalse(source.contains("private long factorSum0_2"));
		assertFalse(source.contains("private long factorSum0_1"));
		List<Node> finite = new ArrayList<>(guards);
		finite.add(new FilterInConstants(Operand.col(0), new int[] {0, 2}));
		String specialized = LmdbNativeKernelEmitter.emit(make(finite, 2, true));
		assertTrue(specialized.contains("private long factorSum0_1"));
		assertTrue(specialized.contains("long pick0 ="));
	}

	@Test public void fullQueryUsesBothUnaryAndTupleLayoutsWithoutLosingHiddenFactors() throws Exception {
		try (HeapFactorSource heap = new HeapFactorSource(this); Metadata hidden = new Metadata();
				WeightedTuples tuple = new WeightedTuples(2, new long[][] {{3, 3}, {4, 9}, {8, 8}}, new long[] {2, 5, 3})) {
			FactorEnvironment a = new FactorEnvironment(4), b = new FactorEnvironment(4);
			a.bind(1, FactorPlanKernelContractTest.heap(heap, 3, 3, 5, 9), 0, 1); a.bind(3, hidden.batch(17), 0, 1);
			b.bindTuple(new int[] {1, 2}, tuple.batch(), 0, 1); b.bind(3, hidden.batch(19), 0, 1);
			List<Node> guards = List.of(new FilterRangeUnsigned(Operand.col(1), 0, 1), new FilterCompareId(false, Operand.col(1), Operand.col(2)));
			for (boolean enabled : new boolean[] {false, true}) {
				Kernel k = make(guards, 4, enabled);
				for (boolean generated : new boolean[] {false, true}) {
					JaninoKernel exec = generated ? KernelCompilationTestSupport.compile(k) : LmdbNativeKernelInterpreter.forAggregate(k);
					long[][] actual = drain(exec, k, new Groups(new FactorEnvironment[] {a, b}, new long[][] {{0, 0, 3, 0}, {1, 0, 0, 0}}, new long[] {2, 3}, 0, 1, 2, 3), new long[] {3, 8}, null);
					assertEquals(2 * 2 * 17 + 3 * 5 * 19, actual[0][0]);
				}
			}
			assertEquals(0, hidden.opens);
		}
	}

	@Test public void entryBindingsRemainEvaluationLocalInBothScalarAndWindowSpecializations() throws Exception {
		List<Node> guards = List.of(new FilterCompareId(false, Operand.col(0), Operand.entry(0)),
				new FilterRangeUnsigned(Operand.col(0), 0, 1));
		long[] values = {0, Long.MIN_VALUE, -1, Long.MIN_VALUE};
		long[] weights = {2, 3, 5, 7};
		for (boolean enabled : new boolean[] {false, true}) {
			Kernel k = make(guards, 1, enabled);
			for (long entry : new long[] {Long.MIN_VALUE, 0, -1}) {
				long expected = entry == 0 ? 0 : entry == -1 ? 5 : 10;
				for (boolean generated : new boolean[] {false, true}) {
					Rows source = new Rows(new long[][] {{0}, {Long.MIN_VALUE}, {-1}, {Long.MIN_VALUE}}, weights);
					JaninoKernel exec = generated ? KernelCompilationTestSupport.compile(k) : LmdbNativeKernelInterpreter.forAggregate(k);
					try {
						exec.bind(new KernelContext(null, new long[] {Long.MIN_VALUE, -1}, new long[] {entry}, null, null, null,
								new KernelPlan[] {source}, 16));
						if (generated) assertEquals(expected, ((KernelFactorPredicate) exec).sum(3, new long[][] {values},
								new long[1], weights, new long[4], 4));
						long[] out = new long[1];
						assertEquals(1, exec.fill(out, 1)); assertEquals(expected, out[0]);
						assertEquals(0, exec.fill(out, 1));
					} finally { exec.close(); }
					assertEquals(1, source.closes); assertEquals(1, source.cursorCloses);
				}
			}
		}
	}

	@Test public void scalarFallbackFusesAliasesAndRetainsCheckedTotals() throws Exception {
		List<Node> guards = List.of(new BindAlias(Operand.col(1), 2), new FilterRangeUnsigned(Operand.col(2), 0, 1), new FilterInConstants(Operand.col(0), new int[] {2, 3}));
		long[][] rows = new long[1031][3]; long[] weights = new long[1031]; long expected = 0;
		for (int i = 0; i < rows.length; i++) { rows[i] = new long[] {i % 7, i % 16, -1}; weights[i] = i % 5 + 1; if (i % 16 >= 3 && i % 16 <= 8 && (i % 7 == 1 || i % 7 == 5)) expected += weights[i]; }
		for (boolean enabled : new boolean[] {false, true}) {
			Kernel k = make(guards, 3, enabled);
			for (boolean generated : new boolean[] {false, true}) {
				Rows input = new Rows(rows, weights);
				JaninoKernel exec = generated ? KernelCompilationTestSupport.compile(k) : LmdbNativeKernelInterpreter.forAggregate(k);
				assertEquals(expected, drain(exec, k, input, new long[] {3, 8, 1, 5}, null)[0][0]);
				assertEquals(1, input.closes); assertEquals(1, input.cursorCloses);
			}
		}
	}

	@Test public void scalarFallbackOverflowMatchesInterpreterAndUnfusedCode() throws Exception {
		List<Node> guards = List.of(new FilterCompareId(false, Operand.col(0), Operand.constant(0)));
		for (boolean enabled : new boolean[] {false, true}) {
			Kernel k = make(guards, 1, enabled);
			for (boolean generated : new boolean[] {false, true}) {
				JaninoKernel exec = generated ? KernelCompilationTestSupport.compile(k) : LmdbNativeKernelInterpreter.forAggregate(k);
				Rows input = new Rows(new long[][] {{7}, {7}}, new long[] {Long.MAX_VALUE, 1});
				assertThrows(ArithmeticException.class, () -> drain(exec, k, input, new long[] {7}, null));
				assertEquals(1, input.cursorCloses); assertEquals(1, input.closes);
			}
		}
	}

	@Test public void scalarRejectedHugeWeightsDoNotOverflowAndEmptyInputStillCountsZero() throws Exception {
		Kernel k = make(List.of(new FilterCompareId(false, Operand.col(0), Operand.constant(0))), 1, true);
		for (boolean generated : new boolean[] {false, true}) {
			JaninoKernel exec = generated ? KernelCompilationTestSupport.compile(k) : LmdbNativeKernelInterpreter.forAggregate(k);
			assertEquals(1, drain(exec, k, new Rows(new long[][] {{8}, {8}, {7}}, new long[] {Long.MAX_VALUE, Long.MAX_VALUE, 1}), new long[] {7}, null)[0][0]);
			exec = generated ? KernelCompilationTestSupport.compile(k) : LmdbNativeKernelInterpreter.forAggregate(k);
			assertEquals(0, drain(exec, k, new Rows(new long[0][], new long[0]), new long[] {7}, null)[0][0]);
		}
	}
}
