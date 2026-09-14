/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.factor;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/** Exact selection contracts, independent of slot plans, term dictionaries, and the IR. */
public class SelectedFactorContractTest {
	private static void eq(long a, long b) {
		if (a != b)
			throw new AssertionError(a + " != " + b);
	}

	private static void yes(boolean v) {
		if (!v)
			throw new AssertionError();
	}

	private static void fails(Class<? extends Throwable> type, Runnable operation) {
		try {
			operation.run();
		} catch (Throwable failure) {
			if (type.isInstance(failure))
				return;
			throw new AssertionError(failure);
		}
		throw new AssertionError("expected " + type);
	}

	private static BorrowedFactorBatch heap(HeapFactorSource s, long... values) {
		BorrowedFactorBatch b = new BorrowedFactorBatch(s, 1);
		b.reset(1);
		b.bindHeap(0, values, 0, values.length);
		return b;
	}

	private static List<Long> read(BorrowedFactorBatch b, int window) {
		List<Long> values = new ArrayList<>();
		try (var c = b.cursor(window)) {
			c.bind(0);
			while (c.next())
				for (long k = 0; k < c.weight(); k++)
					values.add(c.value());
		}
		return values;
	}

	private static void same(Object a, Object b) {
		if (!a.equals(b))
			throw new AssertionError(a + " != " + b);
	}

	@Test
	public void rangesCoalesceAndTranslateLogicalOffsets() {
		FactorSelection s = new FactorSelection(0);
		s.add(3, 2);
		s.add(5, 4);
		s.add(11, 2);
		eq(2, s.ranges());
		eq(8, s.count());
		eq(9, s.end(0));
		eq(6, s.prefixEnd(0));
		eq(0, s.rangeAt(0));
		eq(0, s.rangeAt(5));
		eq(1, s.rangeAt(6));
		eq(1, s.rangeAt(7));
		fails(IndexOutOfBoundsException.class, () -> s.rangeAt(8));
		fails(IllegalArgumentException.class, () -> s.add(12, 1));
		fails(IllegalArgumentException.class, () -> s.validateBounds(12));
		s.validateBounds(13);
		FactorSelection subset = new FactorSelection(0);
		subset.add(4, 3);
		FactorSelection composed = new FactorSelection(0);
		composed.compose(s, subset);
		eq(2, composed.ranges());
		eq(7, composed.start(0));
		eq(9, composed.end(0));
		eq(11, composed.start(1));
	}

	@Test
	public void positionsAndCountsRemainLongAndOverflowIsRejected() {
		FactorSelection s = new FactorSelection(0);
		long n = (long) Integer.MAX_VALUE + 93;
		s.add(n, n);
		s.add(3 * n, n);
		eq(2 * n, s.count());
		eq(1, s.rangeAt(n));
		fails(ArithmeticException.class, () -> new FactorSelection(1).add(Long.MAX_VALUE, 1));
		FactorSelection nearEnd = new FactorSelection(0);
		nearEnd.add(Long.MAX_VALUE - 1, 1);
		nearEnd.validateBounds(Long.MAX_VALUE);
		eq(Long.MAX_VALUE, nearEnd.end(0));
	}

	@Test
	public void selectedPositionsPreservePartialDuplicateFibersAndUnsignedIds() {
		long[] v = { 1, 1, 1, 2, 3, 3, Long.MIN_VALUE, Long.MIN_VALUE, -1L };
		try (HeapFactorSource source = new HeapFactorSource(this);
				SelectedFactorSource selected = new SelectedFactorSource(1)) {
			BorrowedFactorBatch b = heap(source, v);
			FactorSelection s = new FactorSelection(0);
			s.add(1, 1);
			s.add(4, 1);
			s.add(6, 3);
			BorrowedFactorBatch view = selected.select(b, 0, s);
			eq(5, view.count(0));
			eq(0, view.address(0));
			eq(BorrowedFactorBatch.SELECTED, view.kind(0));
			s.clear();
			s.add(0, 1); // published metadata cannot alias a caller's reusable builder
			for (int window : new int[] { 1, 2, 16 })
				same(List.of(1L, 3L, Long.MIN_VALUE, Long.MIN_VALUE, -1L), read(view, window));
		}
	}

	@Test
	public void nativeSelectionsEndExactlyAtUnalignedAllocationTail() {
		Arena arena = Arena.ofConfined();
		MemorySegment m = arena.allocate(1 + 8 * 19, 1);
		ValueLayout.OfLong id = ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < 19; i++)
			m.set(id, 1 + 8L * i, Long.MIN_VALUE + i);
		try (NativeLongFactorSource source = new NativeLongFactorSource(m, arena);
				SelectedFactorSource selected = new SelectedFactorSource(0)) {
			BorrowedFactorBatch b = new BorrowedFactorBatch(source, 1);
			b.reset(1);
			source.bind(b, 0, 1, 19);
			FactorSelection s = new FactorSelection(0);
			s.add(0, 1);
			s.add(17, 2);
			var result = selected.select(b, 0, s);
			same(List.of(Long.MIN_VALUE, Long.MIN_VALUE + 17, Long.MIN_VALUE + 18), read(result, 256));
			FactorSelection bad = new FactorSelection(1);
			bad.add(18, 2);
			fails(IllegalArgumentException.class, () -> selected.select(b, 0, bad));
		}
		yes(!m.scope().isAlive());
	}

	@Test
	public void randomizedChainedSelectionsEqualFlatReference() {
		Random random = new Random(9672531);
		try (HeapFactorSource source = new HeapFactorSource(this);
				SelectedFactorSource one = new SelectedFactorSource(0);
				SelectedFactorSource two = new SelectedFactorSource(0)) {
			for (int trial = 0; trial < 600; trial++) {
				int n = random.nextInt(200);
				long[] data = new long[n];
				FactorSelection a = new FactorSelection(0);
				List<Long> first = new ArrayList<>(), expected = new ArrayList<>();
				for (int i = 0; i < n; i++) {
					data[i] = Long.MIN_VALUE + i / 3;
					if (random.nextBoolean()) {
						a.add(i, 1);
						first.add(data[i]);
					}
				}
				var root = heap(source, data);
				var intermediate = one.select(root, 0, a);
				FactorSelection b = new FactorSelection(0);
				for (int i = 0; i < first.size(); i++)
					if (random.nextBoolean()) {
						b.add(i, 1);
						expected.add(first.get(i));
					}
				var result = two.select(intermediate, 0, b);
				eq(expected.size(), result.count(0));
				same(expected, read(result, 1 + random.nextInt(31)));
				same(expected, read(result, 256));
			}
		}
	}

	@Test
	public void composedReadersHaveIndependentPositionsAndSupportBackwardSeek() {
		try (HeapFactorSource source = new HeapFactorSource(this);
				SelectedFactorSource one = new SelectedFactorSource(0);
				SelectedFactorSource two = new SelectedFactorSource(0)) {
			long[] values = new long[100];
			for (int i = 0; i < values.length; i++)
				values[i] = i;
			var root = heap(source, values);
			FactorSelection a = new FactorSelection(0);
			a.add(5, 10);
			a.add(40, 20);
			var intermediate = one.select(root, 0, a);
			FactorSelection b = new FactorSelection(0);
			b.add(8, 5);
			b.add(20, 3);
			var result = two.select(intermediate, 0, b);
			eq(3, two.ranges());
			same(List.of(13L, 14L, 40L, 41L, 42L, 50L, 51L, 52L), read(result, 2));
			try (var x = result.cursor(3); var y = result.cursor(1); var raw = two.openReader()) {
				x.bind(0);
				y.bind(0);
				yes(x.next());
				eq(13, x.value());
				yes(x.next());
				eq(14, x.value());
				yes(y.next());
				eq(13, y.value());
				raw.bind(result, 0);
				long[] out = new long[4], weights = new long[4];
				int n = raw.copyFibers(6, 4, out, weights);
				eq(2, n);
				eq(51, out[0]);
				n = raw.copyFibers(1, 4, out, weights);
				eq(4, n);
				eq(14, out[0]);
				eq(42, out[3]);
				eq(0, raw.copyFibers(8, 4, out, weights));
			}
		}
	}

	@Test
	public void countsNeverDecodeAndLargeWeightsAreClippedAtSelectionSeams() {
		long cardinality = 3L * Integer.MAX_VALUE;
		AtomicInteger reads = new AtomicInteger();
		try (BorrowedFactorBatch.Source source = new BorrowedFactorBatch.Source(this) {
			public BorrowedFactorBatch.Reader openReader() {
				return new BorrowedFactorBatch.Reader() {
					public void bind(BorrowedFactorBatch batch, int lane) {
					}

					public int copyFibers(long offset, int maximum, long[] values, long[] weights) {
						reads.incrementAndGet();
						values[0] = 17;
						weights[0] = cardinality - offset;
						return 1;
					}
				};
			}
		}; SelectedFactorSource selected = new SelectedFactorSource(0)) {
			var root = new BorrowedFactorBatch(source, 1);
			root.reset(1);
			root.bindNative(0, BorrowedFactorBatch.ENCODED_RUN, 8, 1, 0, cardinality);
			FactorSelection s = new FactorSelection(0);
			s.add(7, Integer.MAX_VALUE);
			s.add(2L * Integer.MAX_VALUE, 15);
			var result = selected.select(root, 0, s);
			eq((long) Integer.MAX_VALUE + 15, result.count(0));
			eq(0, reads.get());
			try (var c = result.cursor(2)) {
				c.bind(0);
				long total = 0;
				while (c.next()) {
					eq(17, c.value());
					total += c.weight();
				}
				eq((long) Integer.MAX_VALUE + 15, total);
			}
			eq(1, reads.get());
		}
	}

	@Test
	public void invalidationIsTransitiveAndProducerReuseDoesNotOwnTheBase() {
		HeapFactorSource source = new HeapFactorSource(this);
		try (SelectedFactorSource one = new SelectedFactorSource(0);
				SelectedFactorSource two = new SelectedFactorSource(0)) {
			var root = heap(source, 1, 2, 3);
			FactorSelection s = new FactorSelection(1);
			s.add(0, 2);
			var middle = one.select(root, 0, s);
			var result = two.select(middle, 0, s);
			try (var c = result.cursor(16)) {
				c.bind(0);
				yes(c.next());
				one.clear();
				fails(IllegalStateException.class, c::next);
				fails(IllegalStateException.class, () -> two.batch().count(0));
				middle = one.select(root, 0, s);
				result = two.select(middle, 0, s);
				c.bind(0);
				root.reset(1);
				fails(IllegalStateException.class, c::next);
				root.bindHeap(0, new long[] { 8, 9 }, 0, 2);
				// The producer can publish anew after upstream reset without first reading stale state.
				middle = one.select(root, 0, s);
				result = two.select(middle, 0, s);
				c.bind(0);
				yes(c.next());
				eq(8, c.value());
				source.close();
				fails(IllegalStateException.class, c::next);
			}
		} finally {
			source.close();
		}
		try (HeapFactorSource owned = new HeapFactorSource(this)) {
			var root = heap(owned, 1);
			var selected = new SelectedFactorSource(1);
			FactorSelection s = new FactorSelection(1);
			s.add(0, 1);
			selected.select(root, 0, s);
			selected.close();
			eq(1, root.count(0));
		}
	}

	@Test
	public void emptySelectionsStayExactAndDoNotOpenTheirBase() {
		try (HeapFactorSource source = new HeapFactorSource(this);
				SelectedFactorSource selected = new SelectedFactorSource(0)) {
			var root = heap(source, 4, 5);
			var result = selected.select(root, 0, new FactorSelection(0));
			eq(0, result.count(0));
			same(List.of(), read(result, 1));
			FactorEnvironment e = new FactorEnvironment(2);
			e.bind(1, result, 0, 1);
			eq(0, e.countProduct(-1, Long.MAX_VALUE));
		}
	}

	@Test
	public void copiedDescriptorsRetainSelectionStampsAndRejectCycles() {
		try (HeapFactorSource source = new HeapFactorSource(this);
				SelectedFactorSource one = new SelectedFactorSource(0);
				SelectedFactorSource two = new SelectedFactorSource(0)) {
			var root = heap(source, 1, 2, 3, 4);
			FactorSelection s = new FactorSelection(1);
			s.add(1, 2);
			var original = one.select(root, 0, s);
			var copy = new BorrowedFactorBatch(one, 1);
			copy.reset(1);
			copy.copyLaneFrom(0, original, 0);
			same(List.of(2L, 3L), read(copy, 8));
			FactorSelection all = new FactorSelection(1);
			all.add(0, 2);
			var nested = two.select(copy, 0, all);
			FactorEnvironment environment = new FactorEnvironment(2);
			environment.bind(1, copy, 0, 1);
			fails(IllegalArgumentException.class, () -> one.select(nested, 0, all));
			try (var reader = copy.cursor(8)) {
				reader.bind(0);
				yes(reader.next());
				one.select(root, 0, s); // the copy's own batch generation has not changed
				fails(IllegalStateException.class, () -> copy.count(0));
				fails(IllegalStateException.class, environment::checkValid);
				fails(IllegalStateException.class, reader::next);
				fails(IllegalStateException.class, () -> nested.count(0));
				copy.reset(1);
				eq(0, copy.count(0));
				copy.copyLaneFrom(0, one.batch(), 0);
				reader.bind(0);
				yes(reader.next());
				eq(2, reader.value());
			}
		}
	}

	@Test
	public void denseReadAheadReducesDecoderCallsButSparseSelectionsDoNotReadLargeGaps() {
		for (int stride : new int[] { 2, 17 }) {
			int[] calls = new int[2], decoded = new int[2];
			for (int mode = 0; mode < 2; mode++) {
				final int variant = mode;
				try (BorrowedFactorBatch.Source source = new BorrowedFactorBatch.Source(this) {
					public BorrowedFactorBatch.Reader openReader() {
						return new BorrowedFactorBatch.Reader() {
							public void bind(BorrowedFactorBatch b, int lane) {
							}

							public int copyFibers(long offset, int maximum, long[] values, long[] weights) {
								int n = (int) Math.min(maximum, 128 - offset);
								calls[variant]++;
								decoded[variant] += n;
								for (int i = 0; i < n; i++) {
									values[i] = offset + i;
									weights[i] = 1;
								}
								return n;
							}
						};
					}
				}; SelectedFactorSource selected = new SelectedFactorSource(0, mode == 1)) {
					var base = new BorrowedFactorBatch(source, 1);
					base.reset(1);
					base.bindNative(0, BorrowedFactorBatch.ENCODED_RUN, 8, 1, 0, 128);
					FactorSelection positions = new FactorSelection(0);
					List<Long> expected = new ArrayList<>();
					for (int i = 0; i < 128; i += stride) {
						positions.add(i, 1);
						expected.add((long) i);
					}
					same(expected, read(selected.select(base, 0, positions), 8));
				}
			}
			if (stride == 2) {
				yes(calls[1] < calls[0]);
				yes(decoded[1] > decoded[0]);
			} else {
				eq(calls[0], calls[1]);
				eq(decoded[0], decoded[1]);
			}
		}
	}

}
