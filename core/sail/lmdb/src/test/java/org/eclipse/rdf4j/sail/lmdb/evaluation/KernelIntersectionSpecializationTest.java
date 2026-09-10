/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import static org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeKernelIr.*;

import java.util.*;

import org.eclipse.rdf4j.sail.lmdb.csf.ImmutablePagedQuadCsfIndex;
import org.eclipse.rdf4j.sail.lmdb.evaluation.NativeLmdbQuerySource.NativeAdjacency;
import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.*;
import org.eclipse.rdf4j.sail.lmdb.factor.BorrowedFactorBatch;
import org.junit.jupiter.api.Test;

/** Independent bag/count oracles, exact read budgets and actual CSF-backed compiled/interpreted execution. */
public class KernelIntersectionSpecializationTest {
	static void check(boolean condition, String message) {
		if (!condition)
			throw new AssertionError(message);
	}

	static long id(long ordinal) {
		return (ordinal << 7) | 2L;
	}

	static Kernel countIR(int arity) {
		return new Kernel(1, List.of(KernelExpansionContractTest.intersection(arity)),
				new Aggregate(new int[0], new AggregateOutput[] { AggregateOutput.countStar() }, null,
						OutputMods.none()));
	}

	static List<String> execute(Kernel ir, boolean compiled, NativeAdjacency... views) throws Exception {
		return KernelExpansionContractTest.execute(ir, KernelExpansionContractTest.context(views), compiled, 1);
	}

	static class ReadingView implements NativeAdjacency {
		final long[] values;
		long reads, finds;

		ReadingView(long[] values) {
			this.values = values.clone();
		}

		public long find(long key) {
			finds++;
			return values.length == 0 ? -1L : 1L;
		}

		public long size(long handle) {
			return values.length;
		}

		public long neighborAt(long handle, long offset) {
			reads++;
			return values[Math.toIntExact(offset)];
		}

		public long contextAt(long handle, long offset) {
			return 0L;
		}

		public boolean runsNeighborOrdered() {
			return true;
		}
	}

	@Test
	public void alignedUniqueFrontiersReadEverySourceValueExactlyOnce() {
		long[] ids = new long[4096];
		for (int i = 0; i < ids.length; i++)
			ids[i] = Long.MIN_VALUE + i;
		for (int arity = 1; arity <= 5; arity++) {
			ReadingView[] sources = new ReadingView[arity];
			Arrays.setAll(sources, i -> new ReadingView(ids));
			try (var cursor = new KernelExpansionCursors.Intersection(sources, new long[arity], null)) {
				int at = 0;
				while (cursor.nextGroup()) {
					check(cursor.value() == ids[at++], "value order");
					check(cursor.groupMultiplicity() == 1L, "unique weight");
				}
				check(at == ids.length, "dropped frontier");
				for (ReadingView source : sources) {
					check(source.reads == ids.length, "read same frontier more than once: " + source.reads);
					check(source.finds == 1L, "repeated row lookup");
				}
			}
		}
	}

	@Test
	public void oneInputRuntimeCountIsMetadataOnlyEvenAtLongMaximum() {
		NativeAdjacency metadata = new NativeAdjacency() {
			public long find(long key) {
				return 1L;
			}

			public long size(long h) {
				return Long.MAX_VALUE;
			}

			public long neighborAt(long h, long p) {
				throw new AssertionError("COUNT read a unary payload");
			}

			public long contextAt(long h, long p) {
				throw new AssertionError();
			}
		};
		// The reusable cursor accepts arity one. IR Intersect intentionally still requires arity at least two.
		try (var cursor = new KernelExpansionCursors.Intersection(new NativeAdjacency[] { metadata }, new long[] { 1 },
				null)) {
			check(cursor.countRemainingGroups() == Long.MAX_VALUE, "unary count");
			check(!cursor.next(), "metadata count did not consume");
		}
	}

	@Test
	public void cardinalityShortcutIsNotUsedForGroupingDistinctOrPerMappingWork() {
		var intersect = KernelExpansionContractTest.intersection(2);
		check(intersectionTotalCountTail(countIR(2)), "plain count should qualify");
		Kernel grouped = new Kernel(1, List.of(intersect), new Aggregate(new int[] { 0 },
				new AggregateOutput[] { AggregateOutput.countStar() }, null, OutputMods.none()));
		Kernel distinct = new Kernel(1, List.of(intersect), new Aggregate(new int[0],
				new AggregateOutput[] { AggregateOutput.countDistinct(0) }, null, OutputMods.none()));
		Kernel guarded = new Kernel(1,
				List.of(intersect, new FilterCompareId(false, Operand.col(0), Operand.constant(0))),
				new Aggregate(new int[0], new AggregateOutput[] { AggregateOutput.countStar() }, null,
						OutputMods.none()));
		check(!intersectionTotalCountTail(grouped) && !intersectionTotalCountTail(distinct)
				&& !intersectionTotalCountTail(guarded), "crossed observable continuation");
	}

	@Test
	public void noOverlapEmptyAndRepeatedCountChannelsRemainExact() throws Exception {
		Kernel ir = new Kernel(1, List.of(KernelExpansionContractTest.intersection(2)), new Aggregate(new int[0],
				new AggregateOutput[] { AggregateOutput.countStar(), AggregateOutput.countStar() }, null,
				OutputMods.none()));
		for (boolean compiled : new boolean[] { false, true }) {
			check(execute(ir, compiled, new ReadingView(new long[] { 1, 2 }), new ReadingView(new long[] { 3, 4 }))
					.equals(List.of("[0, 0]")), "disjoint zero");
			check(execute(ir, compiled, new ReadingView(new long[0]), new ReadingView(new long[] { 1 }))
					.equals(List.of("[0, 0]")), "empty zero");
			check(execute(ir, compiled, new ReadingView(new long[] { 1, 1, 2 }),
					new ReadingView(new long[] { 1, 2, 2 }))
							.equals(List.of("[4, 4]")),
					"two count channels");
		}
	}

	@Test
	public void cachedGallopingMatchesRandomArityAndSkewedUnsignedBags() {
		Random random = new Random(0x15CA_C4EDL);
		long[] domain = new long[128];
		for (int i = 0; i < domain.length; i++)
			domain[i] = i < 64 ? i : Long.MIN_VALUE + i;
		for (int trial = 0; trial < 600; trial++) {
			int arity = 1 + random.nextInt(6);
			ReadingView[] views = new ReadingView[arity];
			long[] counts = new long[domain.length];
			Arrays.fill(counts, 1L);
			for (int v = 0; v < arity; v++) {
				ArrayList<Long> input = new ArrayList<>();
				for (int i = 0; i < domain.length; i++) {
					int duplicates = random.nextInt(v == 0 ? 3 : 9);
					counts[i] *= duplicates;
					for (int j = 0; j < duplicates; j++)
						input.add(domain[i]);
				}
				views[v] = new ReadingView(input.stream().mapToLong(Long::longValue).toArray());
			}
			long expected = Arrays.stream(counts).sum();
			try (var cursor = new KernelExpansionCursors.Intersection(views, new long[arity], null)) {
				check(cursor.countRemainingGroups() == expected, "random bag count " + trial);
				check(!cursor.next(), "count did not consume");
				cursor.bind(new long[arity]);
				for (int i = 0; i < domain.length; i++)
					if (counts[i] != 0) {
						check(cursor.nextGroup() && cursor.value() == domain[i]
								&& cursor.groupMultiplicity() == counts[i],
								"random group " + trial + ":" + i);
					}
				check(!cursor.nextGroup(), "extra group");
			}
		}
	}

	@Test
	public void skewRetainsLogarithmicAccessInsteadOfDecodingTheLargeRun() {
		var huge = new KernelExpansionContractTest.Repeated(new long[] { 0, 100, Long.MIN_VALUE },
				new long[] { 1L << 40, 1L, 1L << 40 });
		huge.budget = 180L;
		try (var cursor = new KernelExpansionCursors.Intersection(
				new NativeAdjacency[] { huge, new ReadingView(new long[] { 100 }) }, new long[] { 1, 1 }, null)) {
			check(cursor.countRemainingGroups() == 1L, "skew count");
			check(huge.reads < 180L, "linearized huge skewed run");
		}
	}

	@Test
	public void countAfterPositionedGroupSkipsOnlyThatGroup() {
		ReadingView a = new ReadingView(new long[] { 1, 1, 2, 2, 2, 3 });
		ReadingView b = new ReadingView(new long[] { 1, 1, 2, 2, 3 });
		try (var cursor = new KernelExpansionCursors.Intersection(new NativeAdjacency[] { a, b }, new long[] { 1, 1 },
				null)) {
			check(cursor.next() && cursor.value() == 1L, "first duplicate");
			check(cursor.countRemainingGroups() == 7L, "count skipped following group or replayed current group");
			cursor.bind(new long[] { 1, 1 });
			check(cursor.countRemainingGroups() == 11L, "rebind count");
		}
	}

	/** Logical weighted source with independent readers, optionally splitting a fiber into many fragments. */
	static class WeightedView implements NativeAdjacency {
		final long[] values, weights, ends;
		final boolean borrow;
		final long fragment;
		long reads, finds, exports, opens, closes, readerCloses, fills;
		boolean wrongCount, failReader, failClose;

		WeightedView(long[] values, long[] weights, boolean borrow, long fragment) {
			this.values = values.clone();
			this.weights = weights.clone();
			this.borrow = borrow;
			this.fragment = fragment;
			ends = new long[weights.length];
			long total = 0L;
			for (int i = 0; i < weights.length; i++)
				ends[i] = total = Math.addExact(total, weights[i]);
		}

		public long find(long key) {
			finds++;
			return values.length == 0 ? -1L : 1L;
		}

		public long size(long handle) {
			return ends.length == 0 ? 0L : ends[ends.length - 1];
		}

		int ordinal(long offset) {
			int at = Arrays.binarySearch(ends, offset + 1L);
			return at < 0 ? -at - 1 : at;
		}

		public long neighborAt(long handle, long offset) {
			reads++;
			return values[ordinal(offset)];
		}

		public long contextAt(long h, long offset) {
			return 0L;
		}

		public boolean runsNeighborOrdered() {
			return true;
		}

		public BorrowedFactorBatch.Source openFactorSource() {
			if (!borrow)
				return null;
			opens++;
			return new BorrowedFactorBatch.Source(this) {
				protected void release() {
					closes++;
					if (failClose)
						throw new IllegalStateException("source close");
				}

				public BorrowedFactorBatch.Reader openReader() {
					if (failReader)
						throw new IllegalStateException("reader failed");
					return new BorrowedFactorBatch.Reader() {
						public void bind(BorrowedFactorBatch batch, int lane) {
							checkOpen();
						}

						public int copyFibers(long offset, int max, long[] out, long[] multiplicities) {
							checkOpen();
							fills++;
							int n = 0;
							check(max <= 256, "unbounded window");
							while (offset < size(1L) && n < max) {
								int ordinal = ordinal(offset);
								long weight = Math.min(fragment, ends[ordinal] - offset);
								out[n] = values[ordinal];
								multiplicities[n++] = weight;
								offset += weight;
							}
							return n;
						}

						public void close() {
							readerCloses++;
						}
					};
				}
			};
		}

		public boolean borrowRun(long handle, BorrowedFactorBatch target, int lane) {
			exports++;
			if (!borrow)
				return false;
			target.bindNative(lane, BorrowedFactorBatch.ENCODED_RUN, 1L, handle, 0,
					wrongCount ? size(handle) - 1L : size(handle));
			return true;
		}
	}

	@Test
	public void borrowedFragmentsKeepWeightsAcrossWindowsAndConsumers() throws Exception {
		long[] values = { 0, 7, Long.MAX_VALUE, Long.MIN_VALUE, -2L };
		long[] weights = { 513, 7, 1027, 1, 256 };
		long expected = 0L;
		for (long weight : weights)
			expected += weight * weight;
		for (boolean compiled : new boolean[] { false, true }) {
			WeightedView a = new WeightedView(values, weights, true, 1), b = new WeightedView(values, weights, true, 3);
			check(execute(countIR(2), compiled, a, b).equals(List.of("[" + expected + "]")), "split weights");
			check(a.reads == 0L && b.reads == 0L, "fell back to scalar source reads");
			check(a.finds == 1L && b.finds == 1L && a.exports == 1L && b.exports == 1L, "lost row position");
			check(a.opens == a.closes && b.opens == b.closes && a.readerCloses == 1 && b.readerCloses == 1,
					"borrowed owner leak");
		}
	}

	@Test
	public void unsupportedBorrowingFallsBackWithoutInventingAnEmptyResult() throws Exception {
		for (boolean compiled : new boolean[] { false, true }) {
			WeightedView a = new WeightedView(new long[] { 7 }, new long[] { 1024 }, true, 1024);
			WeightedView b = new WeightedView(new long[] { 7 }, new long[] { 1024 }, false, 1024);
			check(execute(countIR(2), compiled, a, b).equals(List.of("[1048576]")), "unsupported treated as empty");
			check(a.opens == a.closes && a.fills == 0 && b.fills == 0, "partial borrow consumed or leaked");
		}
	}

	@Test
	public void malformedBorrowedCardinalityAndReaderFailureCloseEveryOwner() throws Exception {
		for (boolean wrongCount : new boolean[] { false, true })
			for (boolean compiled : new boolean[] { false, true }) {
				WeightedView a = new WeightedView(new long[] { 7 }, new long[] { 512 }, true, 512);
				WeightedView b = new WeightedView(new long[] { 7 }, new long[] { 512 }, true, 512);
				b.wrongCount = wrongCount;
				b.failReader = !wrongCount;
				try {
					execute(countIR(2), compiled, a, b);
					throw new AssertionError("malformed source accepted");
				} catch (IllegalStateException expected) {
				}
				check(a.opens == a.closes && b.opens == b.closes, "owner leaked after failed binding");
			}
	}

	@Test
	public void borrowedCountCancellationAndOverflowAreNotHidden() throws Exception {
		for (boolean compiled : new boolean[] { false, true }) {
			WeightedView a = new WeightedView(new long[] { 7 }, new long[] { 1L << 32 }, true, Long.MAX_VALUE);
			WeightedView b = new WeightedView(new long[] { 7 }, new long[] { 1L << 32 }, true, Long.MAX_VALUE);
			try {
				execute(countIR(2), compiled, a, b);
				throw new AssertionError("overflow lost");
			} catch (ArithmeticException expected) {
			}
			check(a.opens == a.closes && b.opens == b.closes, "overflow leaked lease");
		}
		KernelCancellation cancellation = new KernelCancellation(System.nanoTime() + 60_000_000_000L);
		WeightedView a = new WeightedView(new long[] { 7 }, new long[] { 4096 }, true, 1);
		try (var cursor = new KernelExpansionCursors.Intersection(new NativeAdjacency[] { a, a }, new long[] { 1, 1 },
				cancellation)) {
			cancellation.cancel();
			try {
				cursor.countRemainingGroups();
				throw new AssertionError("cancellation lost");
			} catch (KernelCancelledException expected) {
			}
			check(a.reads == 0L && a.fills == 0L, "read after cancellation");
		}
	}

	@Test
	public void borrowedCountsCheckSumOverflowAsWellAsProductOverflow() throws Exception {
		for (boolean compiled : new boolean[] { false, true }) {
			WeightedView a = new WeightedView(new long[] { 3, 7 }, new long[] { 1L << 31, 1L << 31 }, true,
					Long.MAX_VALUE);
			WeightedView b = new WeightedView(new long[] { 3, 7 }, new long[] { 1L << 31, 1L << 31 }, true,
					Long.MAX_VALUE);
			try {
				execute(countIR(2), compiled, a, b);
				throw new AssertionError("sum overflow lost");
			} catch (ArithmeticException expected) {
			}
			check(a.opens == a.closes && b.opens == b.closes, "sum overflow leaked owner");
		}
	}

	@Test
	public void borrowedReadersAndLeasesAreReusedAcrossBindingsAndClosedOnce() {
		WeightedView a = new WeightedView(new long[] { 3, 7 }, new long[] { 1024, 512 }, true, 7);
		WeightedView b = new WeightedView(new long[] { 3, 7 }, new long[] { 1024, 512 }, true, 3);
		var cursor = new KernelExpansionCursors.Intersection(new NativeAdjacency[] { a, b }, null);
		try {
			for (int i = 0; i < 17; i++) {
				cursor.bind(new long[] { i, i });
				check(cursor.countRemainingGroups() == 1310720L, "rebound borrowed count");
			}
			check(a.opens == 1L && b.opens == 1L && a.readerCloses == 0L && b.readerCloses == 0L,
					"per-binding lease or reader creation");
		} finally {
			cursor.close();
			cursor.close();
		}
		check(a.closes == 1L && b.closes == 1L && a.readerCloses == 1L && b.readerCloses == 1L, "double/leaked close");
	}

	@Test
	public void aFailedCloseDoesNotSkipTheOtherBorrowedOwner() {
		WeightedView a = new WeightedView(new long[] { 3 }, new long[] { 512 }, true, 512);
		WeightedView b = new WeightedView(new long[] { 3 }, new long[] { 512 }, true, 512);
		var cursor = new KernelExpansionCursors.Intersection(new NativeAdjacency[] { a, b }, new long[] { 1, 1 }, null);
		check(cursor.countRemainingGroups() == 262144L, "count before release");
		a.failClose = b.failClose = true;
		try {
			cursor.close();
			throw new AssertionError("release failure lost");
		} catch (IllegalStateException expected) {
			check(expected.getSuppressed().length == 1, "second release not retained");
		}
		cursor.close();
		check(a.closes == 1L && b.closes == 1L && a.readerCloses == 1L && b.readerCloses == 1L, "release sweep failed");
	}

	@Test
	public void metadataWeightsAreNotAssumedToMakeRawDuplicateScanningCheap() {
		long[] data = new long[1 << 18];
		Arrays.fill(data, 7L);
		class HeapView extends ReadingView {
			HeapView() {
				super(data);
			}

			public BorrowedFactorBatch.Source openFactorSource() {
				return new org.eclipse.rdf4j.sail.lmdb.factor.HeapFactorSource(this);
			}

			public boolean borrowRun(long handle, BorrowedFactorBatch batch, int lane) {
				batch.bindHeap(lane, values, 0, values.length);
				return true;
			}
		}
		HeapView a = new HeapView(), b = new HeapView();
		try (var cursor = new KernelExpansionCursors.Intersection(new NativeAdjacency[] { a, b }, new long[] { 1, 1 },
				null)) {
			check(cursor.countRemainingGroups() == 1L << 36, "raw duplicate count");
			check(a.reads < 80L && b.reads < 80L, "raw duplicate run was decoded instead of sought");
		}
	}

	/** Real immutable CSF fixture. Only the NativeAdjacency adapter is a fixture; allocation/codec/readers are real. */
	static final class CsfView implements NativeAdjacency, AutoCloseable {
		final ImmutablePagedQuadCsfIndex index;
		final long reference;
		final ImmutablePagedQuadCsfIndex.RowCursor row = new ImmutablePagedQuadCsfIndex.RowCursor();
		long reads, finds, fills;

		CsfView(int distinct, int duplicates, boolean sameNeighbor) {
			ImmutablePagedQuadCsfIndex.BuildPlan plan;
			try (var builder = ImmutablePagedQuadCsfIndex.sizingBuilder(1)) {
				feed(builder, distinct, duplicates, sameNeighbor);
				plan = builder.finishPlan();
			}
			try (var builder = ImmutablePagedQuadCsfIndex.materializingBuilder(plan)) {
				feed(builder, distinct, duplicates, sameNeighbor);
				index = builder.finishIndex();
			}
			reference = index.findLocalReference(0, 0, id(47));
			index.resolve(reference, row);
		}

		static void feed(ImmutablePagedQuadCsfIndex.Builder builder, int distinct, int duplicates,
				boolean sameNeighbor) {
			builder.beginRow(0, 0, id(47));
			for (int i = 0; i < distinct; i++)
				for (int j = 0; j < duplicates; j++)
					builder.pair(Long.MIN_VALUE + id(sameNeighbor ? 1000 : i + 1000),
							id(sameNeighbor ? (long) i * duplicates + j + 1 : j + 1));
			builder.endRow();
		}

		public long find(long key) {
			finds++;
			return reference;
		}

		public long size(long handle) {
			return row.edgeCount();
		}

		public long neighborAt(long handle, long at) {
			reads++;
			return row.neighborAt(at);
		}

		public long contextAt(long handle, long at) {
			return row.contextAt(at);
		}

		public boolean runsNeighborOrdered() {
			return true;
		}

		public BorrowedFactorBatch.Source openFactorSource() {
			index.retain();
			return new BorrowedFactorBatch.Source(index) {
				protected void release() {
					index.close();
				}

				public BorrowedFactorBatch.Reader openReader() {
					return new BorrowedFactorBatch.Reader() {
						final ImmutablePagedQuadCsfIndex.BorrowedPageReader page = new ImmutablePagedQuadCsfIndex.BorrowedPageReader();
						final ImmutablePagedQuadCsfIndex.RowCursor extent = new ImmutablePagedQuadCsfIndex.RowCursor();
						boolean single;

						public void bind(BorrowedFactorBatch batch, int lane) {
							single = batch.kind(lane) == BorrowedFactorBatch.CSF_PAGE;
							if (single)
								page.bind(batch.address(lane), batch.coordinate(lane));
							else
								index.resolve(batch.reference(lane), extent);
						}

						public int copyFibers(long offset, int maximum, long[] values, long[] weights) {
							checkOpen();
							fills++;
							return single ? page.copyFibers(offset, maximum, values, weights)
									: extent.copyFibers(offset, maximum, values, weights);
						}
					};
				}
			};
		}

		public boolean borrowRun(long handle, BorrowedFactorBatch batch, int lane) {
			batch.bindNative(lane, row.singlePageRow() ? BorrowedFactorBatch.CSF_PAGE : BorrowedFactorBatch.ENCODED_RUN,
					row.firstPageAddress(), reference, row.firstLocalRow(), row.edgeCount());
			return true;
		}

		public void close() {
			index.close();
		}
	}

	@Test
	public void directCsfPagesAndContinuationsUseWeightedWindowsInBothTiers() throws Exception {
		for (int size : new int[] { 1024, 60_001 })
			for (boolean same : new boolean[] { false, true }) {
				try (CsfView left = new CsfView(size, 3, same); CsfView right = new CsfView(size, 3, same)) {
					long expected = same ? (long) size * size * 9L : (long) size * 9L;
					for (boolean compiled : new boolean[] { false, true })
						check(execute(countIR(2), compiled, left, right).equals(List.of("[" + expected + "]")),
								"CSF count");
					check(left.reads == 0L && right.reads == 0L, "decoded one incidence at a time");
					check(left.fills > 0L && right.fills > 0L, "CSF fixture not read");
				}
			}
	}
}
