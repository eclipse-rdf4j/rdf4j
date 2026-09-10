/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import static org.eclipse.rdf4j.sail.lmdb.evaluation.FactorMarginalKernelTest.*;
import static org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeKernelIr.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.math.BigInteger;
import java.util.*;

import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.*;
import org.eclipse.rdf4j.sail.lmdb.factor.*;
import org.junit.jupiter.api.Test;

/** Real cursor, emitter and interpreter; immutable sources and numeric authority are explicit fixtures. */
public class MarginalWindowKernelTest {
	static long[] ids(int count, long start) {
		long[] result = new long[count];
		Arrays.setAll(result, i -> start + i);
		return result;
	}

	static Plan plan(FactorEnvironment e, long weight) {
		return new Plan(new FactorEnvironment[] { e }, new long[][] { { 7, 0, 0 } }, new long[] { weight }, 0, 1, 2);
	}

	static KernelMarginalCursor cursor(Plan p) {
		return KernelMarginalCursor.open(p, 3, new int[][] { { 0 }, { 0, 1 }, { 0, 2 } },
				new boolean[] { false, true, true }, null);
	}

	static void presence(KernelMarginalCursor c) {
		yes(c.next(0));
		eq(7L, c.value(0));
		yes(!c.next(0));
	}

	@Test
	public void groupingKeysAreHashedOncePerProjectionWithoutSkippingNumericUpdates() throws Exception {
		try (HeapFactorSource owner = new HeapFactorSource(this)) {
			FactorEnvironment e = new FactorEnvironment(3);
			e.bind(1, heap(owner, ids(2048, 2)), 0, 1);
			e.bind(2, heap(owner, ids(2048, 4099)), 0, 1);
			Kernel ir = shape(3, new int[] { 0 }, AggregateOutput.sum(1), AggregateOutput.avg(2));
			for (boolean compiled : new boolean[] { false, true }) {
				long[] hashes = { 0 };
				Hooks h = new Hooks() {
					@Override
					public long termHashKey(long id) {
						hashes[0]++;
						return id;
					}
				};
				eq("[[7, 0, 0]]", show(run(ir, plan(e, 1), compiled, h)));
				eq(4096L, h.calls);
				eq(3L, hashes[0]);
				long a = (2L + 2049L) * 2048 / 2, b = (4099L + 6146L) * 2048 / 2;
				eq(BigInteger.valueOf(a).multiply(BigInteger.valueOf(2048)), h.sums.get("0:0"));
				eq(BigInteger.valueOf(b).multiply(BigInteger.valueOf(2048)), h.sums.get("1:0"));
				System.out.println("GROUP_HASH_WORK tier=" + (compiled ? "compiled" : "interpreted")
						+ " hashes=" + hashes[0] + " numericUpdates=" + h.calls);
			}
		}
	}

	@Test
	public void windowsKeepLogicalWeightsAndDoNotExpandHiddenSiblings() {
		try (HeapFactorSource owner = new HeapFactorSource(this); Metadata hidden = new Metadata()) {
			long[] values = ids(517, 11);
			FactorEnvironment e = new FactorEnvironment(3);
			e.bind(1, heap(owner, values), 0, 1);
			e.bind(2, hidden.batch(1000000), 0, 1);
			Plan p = plan(e, 3);
			// Only the first branch is demanded. The second never needs a reader.
			try (KernelMarginalCursor c = KernelMarginalCursor.open(p, 3, new int[][] { { 0 }, { 0, 1 } },
					new boolean[] { false, true }, null)) {
				yes(c.nextBatch());
				eq(1L, c.constantColumns());
				presence(c);
				eq(1, c.windowColumn(1));
				int n, position = 0, windows = 0;
				while ((n = c.nextWindow(1)) != 0) {
					windows++;
					eq(7L, c.value(0));
					eq(3000000L, c.windowScale());
					for (int i = c.windowStart(), end = i + n; i < end; i++) {
						eq(values[position++], c.windowValues()[i]);
						eq(1L, c.windowWeights()[i]);
					}
				}
				eq(517, position);
				eq(3, windows);
				yes(!c.nextBatch());
			}
			eq(0, hidden.opens);
			eq(1, p.opens);
			eq(1, p.closes);
		}
	}

	@Test
	public void selectedLogicalRangesRetainClippedFiberWeights() throws Exception {
		try (HeapFactorSource owner = new HeapFactorSource(this);
				SelectedFactorSource selected = new SelectedFactorSource(4)) {
			FactorSelection selection = new FactorSelection(4);
			selection.add(1, 1);
			selection.add(4, 1);
			// Source [2,2,2,5,7,7]; selected positions [1,2), [4,5).
			BorrowedFactorBatch b = selected.select(heap(owner, 2, 2, 2, 5, 7, 7), 0, selection);
			FactorEnvironment e = new FactorEnvironment(3);
			e.bind(1, b, 0, 1);
			e.bind(2, heap(owner, 11, 13), 0, 1);
			Kernel ir = shape(3, new int[] { 0 }, AggregateOutput.countStar(), AggregateOutput.sum(1),
					AggregateOutput.avg(2));
			for (boolean compiled : new boolean[] { false, true }) {
				Hooks h = new Hooks();
				eq("[[7, 12, 0, 0]]", show(run(ir, plan(e, 3), compiled, h)));
				eq(BigInteger.valueOf(54), h.sums.get("1:0"));
				eq(BigInteger.valueOf(144), h.sums.get("2:0"));
			}
		}
	}

	@Test
	public void allNullArgumentDoesNotObserveAnOverflowingHiddenProduct() throws Exception {
		try (HeapFactorSource owner = new HeapFactorSource(this); Metadata hidden = new Metadata()) {
			FactorEnvironment e = new FactorEnvironment(3);
			e.bind(1, heap(owner, -1, -1), 0, 1);
			e.bind(2, hidden.batch(Long.MAX_VALUE), 0, 1);
			Kernel ir = shape(3, new int[] { 0 }, AggregateOutput.sum(1));
			for (boolean compiled : new boolean[] { false, true }) {
				Hooks h = new Hooks();
				eq("[[7, 0]]", show(run(ir, plan(e, 2), compiled, h)));
				eq(0L, h.calls);
			}
			eq(0, hidden.opens);
		}
	}

	@Test
	public void aNonNullArgumentStillChecksHiddenWeightOverflow() throws Exception {
		try (HeapFactorSource owner = new HeapFactorSource(this); Metadata hidden = new Metadata()) {
			FactorEnvironment e = new FactorEnvironment(3);
			e.bind(1, heap(owner, -1, 2), 0, 1);
			e.bind(2, hidden.batch(Long.MAX_VALUE), 0, 1);
			Kernel ir = shape(3, new int[] { 0 }, AggregateOutput.sum(1));
			for (boolean compiled : new boolean[] { false, true }) {
				Plan p = plan(e, 2);
				try {
					run(ir, p, compiled, new Hooks());
					throw new AssertionError("overflow accepted");
				} catch (ArithmeticException expected) {
					eq(1, p.closes);
				}
			}
			eq(0, hidden.opens);
		}
	}

	@Test
	public void windowModeCannotBeMixedWithScalarProductState() {
		try (HeapFactorSource owner = new HeapFactorSource(this)) {
			FactorEnvironment e = new FactorEnvironment(3);
			e.bind(1, heap(owner, 2, 3), 0, 1);
			e.bind(2, heap(owner, 5, 7), 0, 1);
			Plan p = plan(e, 1);
			try (KernelMarginalCursor c = cursor(p)) {
				yes(c.nextBatch());
				presence(c);
				yes(c.nextWindow(1) > 0);
				fails(IllegalStateException.class, () -> c.value(1));
				fails(IllegalStateException.class, c::multiplicity);
				fails(IllegalStateException.class, () -> c.next(1));
			}
			eq(1, p.closes);
		}
	}

	@Test
	public void cancellationAtNextWindowClosesTheProducer() {
		try (HeapFactorSource owner = new HeapFactorSource(this)) {
			FactorEnvironment e = new FactorEnvironment(3);
			e.bind(1, heap(owner, ids(1025, 3)), 0, 1);
			e.bind(2, heap(owner, 11, 12), 0, 1);
			KernelCancellation cancel = new KernelCancellation(System.nanoTime() + 1000000000000L);
			Plan p = plan(e, 1);
			try (KernelMarginalCursor c = KernelMarginalCursor.open(p, 3, new int[][] { { 0 }, { 1 } },
					new boolean[] { false, true }, cancel)) {
				yes(c.nextBatch());
				presence(c);
				yes(c.nextWindow(1) > 0);
				cancel.cancel();
				fails(KernelCancelledException.class, () -> c.nextWindow(1));
			}
			eq(1, p.closes);
		}
	}

	@Test
	public void hiddenSourceInvalidationIsDetectedEvenAfterTheLastPayloadWindow() {
		try (HeapFactorSource owner = new HeapFactorSource(this)) {
			BorrowedFactorBatch a = heap(owner, 2, 3), b = heap(owner, 5, 7);
			FactorEnvironment e = new FactorEnvironment(3);
			e.bind(1, a, 0, 1);
			e.bind(2, b, 0, 1);
			Plan p = plan(e, 1);
			try (KernelMarginalCursor c = cursor(p)) {
				yes(c.nextBatch());
				presence(c);
				eq(2, c.nextWindow(1));
				b.reset(1);
				fails(IllegalStateException.class, () -> c.nextWindow(1));
			}
			eq(1, p.closes);
		}
	}

	@Test
	public void readerRebindIsOwnerSpecificNotMerelySnapshotSpecific() {
		Object snapshot = new Object();
		try (HeapFactorSource a = new HeapFactorSource(snapshot); HeapFactorSource b = new HeapFactorSource(snapshot)) {
			BorrowedFactorBatch first = heap(a, 1, 2), second = heap(a, 8, 9), foreign = heap(b, 10, 11);
			try (BorrowedFactorBatch.Cursor c = first.cursor(2)) {
				c.bind(0);
				yes(c.next());
				eq(1L, c.value());
				c.bind(second, 0);
				yes(c.next());
				eq(8L, c.value());
				yes(c.next());
				eq(9L, c.value());
				yes(!c.next());
				fails(IllegalArgumentException.class, () -> c.bind(foreign, 0));
				second.reset(1);
				fails(IllegalStateException.class, c::next);
			}
		}
	}

	@Test
	public void separateDescriptorBatchesReuseOneSourceReader() {
		final int[] opens = { 0 }, closes = { 0 };
		try (BorrowedFactorBatch.Source owner = new BorrowedFactorBatch.Source(new Object()) {
			public BorrowedFactorBatch.Reader openReader() {
				opens[0]++;
				return new BorrowedFactorBatch.Reader() {
					long[] a;
					int start, n;

					public void bind(BorrowedFactorBatch b, int lane) {
						a = b.array(lane);
						start = b.coordinate(lane);
						n = (int) b.count(lane);
					}

					public int copyFibers(long off, int max, long[] out, long[] weights) {
						int size = Math.min(max, n - (int) off);
						System.arraycopy(a, start + (int) off, out, 0, size);
						Arrays.fill(weights, 0, size, 1);
						return size;
					}

					public void close() {
						closes[0]++;
					}
				};
			}
		}) {
			FactorEnvironment[] env = new FactorEnvironment[40];
			long[][] scalar = new long[40][2];
			long[] weight = new long[40];
			Arrays.fill(weight, 1);
			for (int i = 0; i < env.length; i++) {
				BorrowedFactorBatch b = new BorrowedFactorBatch(owner, 1);
				b.reset(1);
				b.bindHeap(0, new long[] { 3, 5 }, 0, 2);
				env[i] = new FactorEnvironment(2);
				env[i].bind(1, b, 0, 1);
				scalar[i][0] = i;
			}
			Plan p = new Plan(env, scalar, weight, 0, 1);
			try (KernelMarginalCursor c = KernelMarginalCursor.open(p, 2, new int[][] { { 1 } }, new boolean[] { true },
					null)) {
				int count = 0;
				while (c.nextBatch()) {
					while (c.nextWindow(0) != 0)
						count += c.windowValues().length;
				}
				eq(80, count);
			}
			eq(1, opens[0]);
			eq(1, closes[0]);
			eq(1, p.closes);
		}
	}

	@Test
	public void oneSourceCanMoveBetweenSingletonLargeAndSingletonBatches() throws Exception {
		try (HeapFactorSource owner = new HeapFactorSource(this)) {
			FactorEnvironment[] e = new FactorEnvironment[3];
			long[][] scalar = { { 7, 0, 0 }, { 7, 0, 0 }, { 7, 0, 0 } };
			for (int i = 0; i < 3; i++) {
				e[i] = new FactorEnvironment(3);
				e[i].bind(1, heap(owner, ids(i == 1 ? 517 : 1, 3)), 0, 1);
				e[i].bind(2, heap(owner, 11), 0, 1);
			}
			Kernel ir = shape(3, new int[] { 0 }, AggregateOutput.countStar(), AggregateOutput.sum(1),
					AggregateOutput.avg(2));
			for (boolean compiled : new boolean[] { false, true }) {
				Hooks h = new Hooks();
				Plan p = new Plan(e, scalar, new long[] { 1, 1, 1 }, 0, 1, 2);
				eq("[[7, 519, 0, 0]]", show(run(ir, p, compiled, h)));
				eq(BigInteger.valueOf((3L + 519) * 517 / 2 + 6), h.sums.get("1:0"));
				eq(BigInteger.valueOf(519L * 11), h.sums.get("2:0"));
			}
		}
	}

	@Test
	public void deferredGroupingKeysNeverReuseThePreviousGroup() throws Exception {
		try (HeapFactorSource owner = new HeapFactorSource(this)) {
			FactorEnvironment e = new FactorEnvironment(3);
			e.bind(1, heap(owner, 2, 2, 3), 0, 1);
			e.bind(2, heap(owner, 7, 11), 0, 1);
			Kernel ir = shape(3, new int[] { 1 }, AggregateOutput.sum(2), AggregateOutput.countStar());
			for (boolean compiled : new boolean[] { false, true }) {
				Hooks h = new Hooks();
				eq("[[2, 0, 4], [3, 1, 2]]", show(run(ir, plan(e, 1), compiled, h)));
				eq(BigInteger.valueOf(36), h.sums.get("0:0"));
				eq(BigInteger.valueOf(18), h.sums.get("0:1"));
			}
		}
	}

	@Test
	public void rawNativeWindowsKeepLeaseBoundsAndVectorTails() throws Exception {
		Arena arena = Arena.ofConfined();
		MemorySegment memory = arena.allocate(517L * 8, 8);
		long[] ids = ids(517, 3);
		MemorySegment.copy(MemorySegment.ofArray(ids), 0, memory, 0, 517L * 8);
		try (NativeLongFactorSource owner = new NativeLongFactorSource(memory, arena);
				HeapFactorSource other = new HeapFactorSource(this)) {
			BorrowedFactorBatch b = new BorrowedFactorBatch(owner, 1);
			b.reset(1);
			owner.bind(b, 0, 0, 517);
			FactorEnvironment e = new FactorEnvironment(3);
			e.bind(1, b, 0, 1);
			e.bind(2, heap(other, 11, 13), 0, 1);
			Kernel ir = shape(3, new int[] { 0 }, AggregateOutput.countStar(), AggregateOutput.sum(1));
			for (boolean compiled : new boolean[] { false, true }) {
				Hooks h = new Hooks();
				eq("[[7, 3102, 0]]", show(run(ir, plan(e, 3), compiled, h)));
				eq(BigInteger.valueOf((3L + 519) * 517 / 2 * 6), h.sums.get("1:0"));
			}
		}
		yes(!arena.scope().isAlive());
	}

	@Test
	public void compressedCsfPagesAndContinuationsFeedTheSameMarginals() throws Exception {
		for (int count : new int[] { 517, 60001 })
			try (var view = new KernelIntersectionSpecializationTest.CsfView(count, 3, false);
					BorrowedFactorBatch.Source owner = view.openFactorSource();
					HeapFactorSource other = new HeapFactorSource(this)) {
				BorrowedFactorBatch b = new BorrowedFactorBatch(owner, 1);
				b.reset(1);
				view.borrowRun(view.reference, b, 0);
				FactorEnvironment e = new FactorEnvironment(3);
				e.bind(1, b, 0, 1);
				e.bind(2, heap(other, 11, 13), 0, 1);
				Kernel ir = shape(3, new int[] { 0 }, AggregateOutput.countDistinct(1), AggregateOutput.sum(1));
				BigInteger expected = BigInteger.ZERO;
				for (int i = 0; i < count; i++)
					expected = expected.add(
							BigInteger.valueOf(Long.MIN_VALUE + KernelIntersectionSpecializationTest.id(i + 1000)));
				expected = expected.multiply(BigInteger.valueOf(18));
				for (boolean compiled : new boolean[] { false, true }) {
					Hooks h = new Hooks();
					eq("[[7, " + count + ", 0]]", show(run(ir, plan(e, 3), compiled, h)));
					eq(expected, h.sums.get("1:0"));
				}
				eq(0L, view.reads);
				yes(view.fills > 0);
			}
	}

	@Test
	public void aSiteMaySwitchBetweenUnaryWindowsAndCorrelatedTuples() throws Exception {
		try (HeapFactorSource owner = new HeapFactorSource(this);
				var tuples = new BorrowedTupleContractTest.WeightedTuples(2, new long[][] { { 5, 11 }, { 7, 13 } },
						new long[] { 2, 3 })) {
			FactorEnvironment unary = new FactorEnvironment(3), zipped = new FactorEnvironment(3);
			unary.bind(1, heap(owner, 2, 3), 0, 1);
			unary.bind(2, heap(owner, 17, 19), 0, 1);
			zipped.bindTuple(new int[] { 1, 2 }, tuples.batch(), 0, 1);
			Kernel ir = shape(3, new int[] { 0 }, AggregateOutput.countStar(), AggregateOutput.sum(1),
					AggregateOutput.avg(2));
			for (boolean compiled : new boolean[] { false, true }) {
				Plan p = new Plan(new FactorEnvironment[] { unary, zipped, unary },
						new long[][] { { 7, 0, 0 }, { 7, 0, 0 }, { 7, 0, 0 } }, new long[] { 1, 1, 2 }, 0, 1, 2);
				Hooks h = new Hooks();
				eq("[[7, 17, 0, 0]]", show(run(ir, p, compiled, h)));
				eq(BigInteger.valueOf(61), h.sums.get("1:0"));
				eq(BigInteger.valueOf(277), h.sums.get("2:0"));
				eq(17L, h.counts.get("2:0"));
			}
		}
	}

	@Test
	public void unfinishedWindowProjectionCannotAdvanceTheInput() {
		try (HeapFactorSource owner = new HeapFactorSource(this)) {
			FactorEnvironment e = new FactorEnvironment(3);
			e.bind(1, heap(owner, ids(517, 2)), 0, 1);
			e.bind(2, heap(owner, 11, 12), 0, 1);
			Plan p = plan(e, 1);
			try (KernelMarginalCursor c = cursor(p)) {
				yes(c.nextBatch());
				presence(c);
				yes(c.nextWindow(1) > 0);
				fails(IllegalStateException.class, c::nextBatch);
			}
			eq(1, p.closes);
		}
	}

	@Test
	public void failedDescriptorRebindDoesNotLeaveTheOldPositionUsable() {
		try (HeapFactorSource owner = new HeapFactorSource(this)) {
			BorrowedFactorBatch a = heap(owner, 2, 3), b = heap(owner, 5, 7);
			try (BorrowedFactorBatch.Cursor c = a.cursor(2)) {
				c.bind(0);
				yes(c.next());
				fails(IndexOutOfBoundsException.class, () -> c.bind(b, 19));
				fails(IllegalStateException.class, c::next);
				c.bind(b, 0);
				yes(c.next());
				eq(5L, c.value());
			}
		}
	}

}
