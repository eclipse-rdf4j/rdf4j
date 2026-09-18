/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.factor;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/** Contract tests also runnable by the dependency-isolated validation harness. */
public class BorrowedFactorContractTest {
	private static void equal(long expected, long actual) {
		if (expected != actual)
			throw new AssertionError(expected + " != " + actual);
	}

	private static void expect(Class<? extends Throwable> type, Runnable operation) {
		try {
			operation.run();
		} catch (Throwable e) {
			if (type.isInstance(e))
				return;
			throw new AssertionError("expected " + type + " but got " + e, e);
		}
		throw new AssertionError("expected " + type);
	}

	@Test
	public void independentReadersKeepWeightsAndUnsignedIds() {
		long[] values = { 2, 2, 3, Long.MIN_VALUE, Long.MIN_VALUE, -1L };
		try (HeapFactorSource source = new HeapFactorSource(values)) {
			BorrowedFactorBatch batch = new BorrowedFactorBatch(source, 1);
			batch.reset(1);
			batch.bindHeap(0, values, 0, values.length);
			try (BorrowedFactorBatch.Cursor a = batch.cursor(1); BorrowedFactorBatch.Cursor b = batch.cursor(3)) {
				a.bind(0);
				b.bind(0);
				long count = 0;
				for (long expected : new long[] { 2, 3, Long.MIN_VALUE, -1L }) {
					if (!a.next() || !b.next())
						throw new AssertionError("short result");
					equal(expected, a.value());
					equal(expected, b.value());
					equal(a.weight(), b.weight());
					count += a.weight();
				}
				equal(6, count);
				if (a.next() || b.next())
					throw new AssertionError("long result");
				a.bind(0);
				if (!a.next())
					throw new AssertionError();
				equal(2, a.value());
			}
		}
	}

	@Test
	public void staleAndUnboundReadersFailBeforeAccess() {
		HeapFactorSource source = new HeapFactorSource(this);
		BorrowedFactorBatch batch = new BorrowedFactorBatch(source, 1);
		batch.reset(1);
		batch.bindHeap(0, new long[] { 11 }, 0, 1);
		try (BorrowedFactorBatch.Cursor cursor = batch.cursor(1)) {
			expect(IllegalStateException.class, cursor::next);
			cursor.bind(0);
			batch.reset(1);
			expect(IllegalStateException.class, cursor::next);
			batch.bindHeap(0, new long[] { 47 }, 0, 1);
			cursor.bind(0);
			source.close();
			expect(IllegalStateException.class, cursor::next);
		}
		source.close();
	}

	@Test
	public void copyDescriptorsRetainsAlignmentWithoutCopyingPayload() {
		Object snapshot = new Object();
		try (HeapFactorSource one = new HeapFactorSource(snapshot);
				HeapFactorSource two = new HeapFactorSource(snapshot);
				HeapFactorSource other = new HeapFactorSource(new Object())) {
			BorrowedFactorBatch original = new BorrowedFactorBatch(one, 0);
			BorrowedFactorBatch copy = new BorrowedFactorBatch(two, 0);
			BorrowedFactorBatch wrong = new BorrowedFactorBatch(other, 1);
			long[] array = { 11, 17, 47, 53 };
			original.reset(3);
			copy.reset(2);
			wrong.reset(1);
			original.bindHeap(0, array, 0, 1);
			original.bindHeap(1, array, 1, 2);
			original.bindHeap(2, array, 3, 1);
			copy.copyLaneFrom(0, original, 2);
			copy.copyLaneFrom(1, original, 0);
			if (copy.array(0) != array)
				throw new AssertionError("payload copied");
			try (BorrowedFactorBatch.Cursor c = copy.cursor(1)) {
				c.bind(0);
				if (!c.next())
					throw new AssertionError();
				equal(53, c.value());
				c.bind(1);
				if (!c.next())
					throw new AssertionError();
				equal(11, c.value());
			}
			expect(IllegalArgumentException.class, () -> wrong.copyLaneFrom(0, original, 0));
		}
	}

	@Test
	public void nativeAddressAndAllocationTailAreExact() {
		Arena arena = Arena.ofConfined();
		MemorySegment memory = arena.allocate(41, 1);
		ValueLayout.OfLong id = ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
		long[] expected = { 1, 2, 2, Long.MIN_VALUE, -1L };
		for (int i = 0; i < expected.length; i++)
			memory.set(id, 1L + 8L * i, expected[i]);
		try (NativeLongFactorSource source = new NativeLongFactorSource(memory, arena)) {
			BorrowedFactorBatch batch = new BorrowedFactorBatch(source, 1);
			batch.reset(1);
			source.bind(batch, 0, 1, 5);
			equal(memory.address() + 1, batch.address(0));
			equal(5, batch.count(0));
			try (BorrowedFactorBatch.Cursor cursor = batch.cursor(2)) {
				cursor.bind(0);
				int position = 0;
				while (cursor.next())
					for (long k = 0; k < cursor.weight(); k++)
						equal(expected[position++], cursor.value());
				equal(expected.length, position);
			}
			expect(IndexOutOfBoundsException.class, () -> source.bind(batch, 0, 2, 5));
			expect(ArithmeticException.class, () -> source.bind(batch, 0, 0, Long.MAX_VALUE));
		}
		if (memory.scope().isAlive())
			throw new AssertionError("lease not released");
	}

	@Test
	public void exactCountDoesNotOpenOrDecodeAndSupportsLongPositions() {
		AtomicInteger binds = new AtomicInteger();
		AtomicInteger reads = new AtomicInteger();
		long cardinality = (long) Integer.MAX_VALUE + 1000;
		try (BorrowedFactorBatch.Source source = new BorrowedFactorBatch.Source(this) {
			@Override
			public BorrowedFactorBatch.Reader openReader() {
				return new BorrowedFactorBatch.Reader() {
					public void bind(BorrowedFactorBatch b, int l) {
						binds.incrementAndGet();
					}

					public int copyFibers(long from, int maximum, long[] values, long[] weights) {
						reads.incrementAndGet();
						values[0] = 17;
						weights[0] = cardinality;
						return 1;
					}
				};
			}
		}) {
			BorrowedFactorBatch batch = new BorrowedFactorBatch(source, 1);
			batch.reset(1);
			batch.bindNative(0, BorrowedFactorBatch.ENCODED_RUN, 8, 1, 0, cardinality);
			equal(cardinality, batch.count(0));
			equal(0, reads.get());
			equal(0, binds.get());
			try (BorrowedFactorBatch.Cursor c = batch.cursor(2)) {
				c.bind(0);
				if (!c.next())
					throw new AssertionError();
				equal(cardinality, c.weight());
				if (c.next())
					throw new AssertionError();
			}
			equal(1, reads.get());
			equal(1, binds.get());
		}
	}

	@Test
	public void randomizedWeightedReplayMatchesFlatRelation() {
		Random random = new Random(0xFACADE);
		for (int trial = 0; trial < 200; trial++) {
			long[] values = new long[random.nextInt(2000)];
			long next = Long.MAX_VALUE - 20;
			for (int i = 0; i < values.length; i++) {
				next += random.nextInt(3);
				values[i] = next;
			}
			try (HeapFactorSource source = new HeapFactorSource(values)) {
				BorrowedFactorBatch batch = new BorrowedFactorBatch(source, 1);
				batch.reset(1);
				batch.bindHeap(0, values, 0, values.length);
				try (BorrowedFactorBatch.Cursor c = batch.cursor(1 + random.nextInt(50))) {
					c.bind(0);
					int at = 0;
					while (c.next())
						for (long n = 0; n < c.weight(); n++)
							equal(values[at++], c.value());
					equal(values.length, at);
				}
			}
		}
	}

	@Test
	public void hugeDuplicateGroupsYieldBoundedWeightedFragments() {
		int n = 50_001;
		long[] values = new long[n];
		java.util.Arrays.fill(values, Long.MIN_VALUE);
		try (HeapFactorSource source = new HeapFactorSource(values)) {
			BorrowedFactorBatch batch = new BorrowedFactorBatch(source, 1);
			batch.reset(1);
			batch.bindHeap(0, values, 0, n);
			boundedFragments(batch, n);
		}
		Arena arena = Arena.ofConfined();
		MemorySegment memory = arena.allocate((long) n * 8, 8);
		for (int i = 0; i < n; i++)
			memory.set(ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN), (long) i * 8,
					Long.MIN_VALUE);
		try (NativeLongFactorSource source = new NativeLongFactorSource(memory, arena)) {
			BorrowedFactorBatch batch = new BorrowedFactorBatch(source, 1);
			batch.reset(1);
			source.bind(batch, 0, 0, n);
			boundedFragments(batch, n);
		}
	}

	private static void boundedFragments(BorrowedFactorBatch batch, int expected) {
		try (BorrowedFactorBatch.Cursor cursor = batch.cursor(1)) {
			cursor.bind(0);
			long count = 0;
			int fragments = 0;
			while (cursor.next()) {
				equal(Long.MIN_VALUE, cursor.value());
				if (cursor.weight() > 4096)
					throw new AssertionError("unbounded payload scan");
				count += cursor.weight();
				fragments++;
			}
			equal(expected, count);
			if (fragments <= 1)
				throw new AssertionError("no bounded yielding");
		}
	}

}
