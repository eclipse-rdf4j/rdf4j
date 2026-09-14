/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.factor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

/** Exact bag contracts for correlated tuple groups alongside existing unary adjacency factors. */
public class BorrowedTupleContractTest {
	private static void eq(long expected, long actual) {
		if (expected != actual)
			throw new AssertionError(expected + " != " + actual);
	}

	private static void yes(boolean value) {
		if (!value)
			throw new AssertionError();
	}

	private static void fails(Class<? extends Throwable> type, Runnable action) {
		try {
			action.run();
		} catch (Throwable failure) {
			if (type.isInstance(failure))
				return;
			throw new AssertionError(failure);
		}
		throw new AssertionError("expected " + type);
	}

	/** Test-owned immutable tuples, including weights larger than a decode window. */
	public static final class WeightedTuples extends BorrowedTupleBatch.Source {
		private final long[][] rows;
		private final long[] weights;
		public final long count;
		public int reads;

		public WeightedTuples(int width, long[][] rows, long[] weights) {
			super(width);
			if (rows.length != weights.length)
				throw new IllegalArgumentException();
			this.rows = new long[rows.length][];
			this.weights = weights.clone();
			long n = 0L;
			for (int i = 0; i < rows.length; i++) {
				if (rows[i].length != width || weights[i] <= 0)
					throw new IllegalArgumentException();
				this.rows[i] = rows[i].clone();
				n = Math.addExact(n, weights[i]);
			}
			count = n;
		}

		public BorrowedTupleBatch batch() {
			BorrowedTupleBatch batch = new BorrowedTupleBatch(this, 1);
			batch.reset(1);
			batch.bind(0, 0, count);
			return batch;
		}

		@Override
		protected void validate(long reference, long count) {
			checkOpen();
			if (reference != 0 || count != this.count)
				throw new IllegalArgumentException();
		}

		@Override
		public BorrowedTupleBatch.Reader openReader() {
			checkOpen();
			return new BorrowedTupleBatch.Reader() {
				@Override
				public void bind(long reference, long count) {
					validate(reference, count);
				}

				@Override
				public int copyRows(long offset, int maximum, long[] target, long[] outWeights) {
					checkOpen();
					reads++;
					int n = 0;
					long start = 0, left = maximum;
					for (int i = 0; i < rows.length && left != 0; i++) {
						long end = start + weights[i];
						if (end > offset) {
							long take = Math.min(left, end - Math.max(start, offset));
							System.arraycopy(rows[i], 0, target, n * columns(), columns());
							outWeights[n++] = take;
							left -= take;
						}
						start = end;
					}
					return n;
				}
			};
		}
	}

	@Test
	public void tupleColumnsAreZippedAndSiblingFactorsMultiply() {
		try (WeightedTuples source = new WeightedTuples(2, new long[][] { { 11, 101 }, { 12, 202 } },
				new long[] { 2, 5 });
				HeapFactorSource heap = new HeapFactorSource(this)) {
			BorrowedTupleBatch tuple = source.batch();
			BorrowedFactorBatch sibling = new BorrowedFactorBatch(heap, 1);
			sibling.reset(1);
			sibling.bindHeap(0, new long[] { 7, 8, 9 }, 0, 3);
			FactorEnvironment env = new FactorEnvironment(4);
			env.bindTuple(new int[] { 1, 2 }, tuple, 0, 1);
			env.bind(3, sibling, 0, 1);
			eq(42, env.countProduct(-1, 2));
			eq(7, env.countProduct(2, 1));
			eq(7, env.countProduct(4, 1));
			eq(10, env.groupLeaders());
			eq(6, env.expansionClosure(2));
			try (FactorProductCursor product = new FactorProductCursor(4, 3)) {
				product.bind(env, 2, 2);
				eq(6, product.expandedMask());
				long sum = 0;
				while (product.next()) {
					yes(product.value(1) == 11 ? product.value(2) == 101 : product.value(2) == 202);
					sum += product.multiplicity();
				}
				eq(14, sum);
				product.bind(env, -1, 2);
				sum = 0;
				while (product.next()) {
					yes(product.value(1) == 11 ? product.value(2) == 101 : product.value(2) == 202);
					sum += product.multiplicity();
				}
				eq(42, sum);
			}
		}
	}

	@Test
	public void randomizedWeightedTupleProductsMatchFlatBag() {
		Random random = new Random(721498);
		for (int trial = 0; trial < 100; trial++) {
			int n = random.nextInt(8), m = random.nextInt(8);
			long[][] a = new long[n][2], b = new long[m][2];
			long[] wa = new long[n], wb = new long[m];
			for (int i = 0; i < n; i++) {
				a[i][0] = Long.MIN_VALUE + random.nextInt(4);
				a[i][1] = random.nextInt(4);
				wa[i] = 1 + random.nextInt(8);
			}
			for (int i = 0; i < m; i++) {
				b[i][0] = random.nextInt(4);
				b[i][1] = random.nextInt(4);
				wb[i] = 1 + random.nextInt(8);
			}
			try (WeightedTuples sa = new WeightedTuples(2, a, wa); WeightedTuples sb = new WeightedTuples(2, b, wb)) {
				FactorEnvironment env = new FactorEnvironment(5);
				env.bindTuple(new int[] { 3, 1 }, sa.batch(), 0, 1);
				env.bindTuple(new int[] { 2, 4 }, sb.batch(), 0, 1);
				Map<String, Long> expected = new HashMap<>(), actual = new HashMap<>();
				for (int i = 0; i < n; i++)
					for (int j = 0; j < m; j++)
						expected.merge(a[i][0] + ":" + a[i][1] + ":" + b[j][0] + ":" + b[j][1], 3 * wa[i] * wb[j],
								Long::sum);
				try (FactorProductCursor c = new FactorProductCursor(5, 1 + random.nextInt(5))) {
					c.bind(env, -1, 3);
					long count = 0;
					while (c.next()) {
						actual.merge(c.value(3) + ":" + c.value(1) + ":" + c.value(2) + ":" + c.value(4),
								c.multiplicity(), Long::sum);
						count += c.multiplicity();
					}
					if (!expected.equals(actual))
						throw new AssertionError("tuple bag differs in trial " + trial);
					eq(env.countProduct(-1, 3), count);
				}
			}
		}
	}

	@Test
	public void nonMonotoneSlotMappingAndHighBitArePreserved() {
		try (WeightedTuples source = new WeightedTuples(3, new long[][] { { Long.MIN_VALUE, 9, -8 } },
				new long[] { 3 })) {
			FactorEnvironment env = new FactorEnvironment(64);
			env.bindTuple(new int[] { 63, 2, 17 }, source.batch(), 0, 1);
			long bits = (1L << 63) | (1L << 2) | (1L << 17);
			eq(bits, env.expansionClosure(1L << 63));
			eq(4, env.groupLeaders());
			try (FactorProductCursor c = new FactorProductCursor(64, 8)) {
				c.bind(env, 1L << 63, 2);
				yes(c.next());
				eq(Long.MIN_VALUE, c.value(63));
				eq(9, c.value(2));
				eq(-8, c.value(17));
				eq(6, c.multiplicity());
				yes(!c.next());
			}
		}
	}

	@Test
	public void exactCountsNeedNoReaderAndCanExceedIntegerRange() {
		try (WeightedTuples source = new WeightedTuples(2, new long[][] { { 1, 2 } }, new long[] { 1L << 34 })) {
			FactorEnvironment env = new FactorEnvironment(3);
			env.bindTuple(new int[] { 1, 2 }, source.batch(), 0, 1);
			eq(1L << 35, env.countProduct(-1, 2));
			eq(0, source.reads);
			fails(ArithmeticException.class, () -> env.countProduct(-1, Long.MAX_VALUE));
			try (FactorProductCursor c = new FactorProductCursor(3, 7)) {
				c.bind(env, 0, 2);
				yes(c.next());
				eq(2, c.multiplicity());
				yes(!c.next());
				eq(0, source.reads);
			}
		}
	}

	@Test
	public void emptyTupleAnnihilatesBeforeOverflow() {
		try (WeightedTuples source = new WeightedTuples(2, new long[0][2], new long[0]);
				HeapFactorSource heap = new HeapFactorSource(this)) {
			BorrowedFactorBatch b = new BorrowedFactorBatch(heap, 1);
			b.reset(1);
			b.bindHeap(0, new long[] { 1, 2 }, 0, 2);
			FactorEnvironment env = new FactorEnvironment(4);
			env.bind(1, b, 0, 1);
			env.bindTuple(new int[] { 2, 3 }, source.batch(), 0, 1);
			eq(0, env.countProduct(-1, Long.MAX_VALUE));
			try (FactorProductCursor c = new FactorProductCursor(4, 1)) {
				c.bind(env, 0, Long.MAX_VALUE);
				yes(!c.next());
			}
		}
	}

	@Test
	public void independentReadersAndCopiedDescriptorsRetainCorrelation() {
		try (WeightedTuples source = new WeightedTuples(2, new long[][] { { 1, 10 }, { 2, 20 } },
				new long[] { 5, 2 })) {
			BorrowedTupleBatch original = source.batch(), copy = new BorrowedTupleBatch(source, 1);
			copy.reset(1);
			copy.copyLaneFrom(0, original, 0);
			FactorEnvironment env = new FactorEnvironment(3), kept = new FactorEnvironment(3);
			env.bindTuple(new int[] { 1, 2 }, copy, 0, 1);
			kept.append(env, -1);
			env.clear();
			try (BorrowedTupleBatch.Cursor a = original.cursor(2); BorrowedTupleBatch.Cursor b = copy.cursor(3)) {
				a.bind(0);
				b.bind(0);
				yes(a.next());
				eq(1, a.value(0));
				eq(2, a.weight());
				yes(b.next());
				eq(3, b.weight());
				original.reset(1);
				fails(IllegalStateException.class, a::next);
				long total = 3;
				while (b.next())
					total += b.weight();
				eq(7, total);
				eq(7, kept.countProduct(-1, 1));
				copy.reset(1);
				fails(IllegalStateException.class, kept::checkValid);
			}
		}
	}

	@Test
	public void ownerCloseAndEnvironmentChangesInvalidateConsumption() {
		WeightedTuples source = new WeightedTuples(2, new long[][] { { 1, 2 } }, new long[] { 3 });
		BorrowedTupleBatch b = source.batch();
		FactorEnvironment env = new FactorEnvironment(3);
		env.bindTuple(new int[] { 1, 2 }, b, 0, 1);
		try (FactorProductCursor c = new FactorProductCursor(3, 1)) {
			c.bind(env, 2, 1);
			yes(c.next());
			source.close();
			fails(IllegalStateException.class, c::next);
		}
		try (WeightedTuples other = new WeightedTuples(2, new long[][] { { 1, 2 } }, new long[] { 3 });
				FactorProductCursor c = new FactorProductCursor(3, 1)) {
			env.clear();
			env.bindTuple(new int[] { 1, 2 }, other.batch(), 0, 1);
			c.bind(env, 2, 1);
			env.clear();
			fails(IllegalStateException.class, c::next);
		}
	}

	@Test
	public void partialTupleCopiesAndInvalidDependenciesAreRejectedAtomically() {
		try (WeightedTuples source = new WeightedTuples(2, new long[][] { { 1, 2 } }, new long[] { 1 })) {
			BorrowedTupleBatch b = source.batch();
			FactorEnvironment e = new FactorEnvironment(5), copy = new FactorEnvironment(5);
			e.bindTuple(new int[] { 1, 3 }, b, 0, 1);
			fails(IllegalArgumentException.class, () -> copy.append(e, 2));
			eq(0, copy.mask());
			fails(IllegalArgumentException.class, () -> copy.bindTuple(new int[] { 1, 1 }, b, 0, 1));
			eq(0, copy.mask());
			fails(IllegalArgumentException.class, () -> copy.bindTuple(new int[] { 1, 2 }, b, 0, 2));
			eq(0, copy.mask());
			fails(IllegalArgumentException.class, () -> copy.bindTuple(new int[] { 0, 4 }, b, 0, 0x100));
			eq(0, copy.mask());
			copy.append(e, -1);
			eq(10, copy.mask());
			fails(IllegalStateException.class, () -> copy.batch(1));
			FactorEnvironment small = new FactorEnvironment(2);
			fails(IllegalArgumentException.class, () -> small.append(e, -1));
			eq(0, small.mask());
		}
	}

	@Test
	public void tupleAndUnaryReadersCanReuseTheSameSlotAcrossPrefixes() {
		try (WeightedTuples source = new WeightedTuples(2, new long[][] { { 11, 12 }, { 21, 22 } },
				new long[] { 1, 1 });
				HeapFactorSource heap = new HeapFactorSource(this);
				FactorProductCursor c = new FactorProductCursor(4, 4)) {
			BorrowedFactorBatch b = new BorrowedFactorBatch(heap, 1);
			b.reset(1);
			b.bindHeap(0, new long[] { 1, 2 }, 0, 2);
			FactorEnvironment e = new FactorEnvironment(4);
			for (int pass = 0; pass < 6; pass++) {
				e.clear();
				if ((pass & 1) == 0)
					e.bindTuple(new int[] { 1, 2 }, source.batch(), 0, 1);
				else
					e.bind(1, b, 0, 1);
				c.bind(e, 2, 1);
				long total = 0;
				while (c.next()) {
					total += c.multiplicity();
					if ((pass & 1) == 0)
						eq(c.value(1) + 1, c.value(2));
				}
				eq(2, total);
			}
		}
	}

	@Test
	public void malformedReaderWeightsCannotEscapeAsExactCounts() {
		for (int mode = 0; mode < 3; mode++) {
			int failureMode = mode;
			try (BorrowedTupleBatch.Source s = new BorrowedTupleBatch.Source(2) {
				protected void validate(long ref, long count) {
				}

				public BorrowedTupleBatch.Reader openReader() {
					return new BorrowedTupleBatch.Reader() {
						public void bind(long ref, long count) {
						}

						public int copyRows(long off, int max, long[] rows, long[] w) {
							w[0] = failureMode == 1 ? 0 : max + 1;
							return failureMode == 0 ? 0 : 1;
						}
					};
				}
			}) {
				BorrowedTupleBatch b = new BorrowedTupleBatch(s, 1);
				b.reset(1);
				b.bind(0, 0, 4);
				try (BorrowedTupleBatch.Cursor c = b.cursor(2)) {
					c.bind(0);
					fails(IllegalStateException.class, c::next);
				}
			}
		}
	}

	@Test
	public void directRowsBorrowBackingArrayOncePerBindAndNeverCopy() {
		int[] accesses = { 0 };
		long[] payload = { 10, 100, 20, 200 };
		try (BorrowedTupleBatch.Source source = new BorrowedTupleBatch.Source(2) {
			protected void validate(long reference, long count) {
				if (reference != 0 || count != 5)
					throw new IllegalArgumentException();
			}

			public BorrowedTupleBatch.Reader openReader() {
				return new BorrowedTupleBatch.DirectReader() {
					int index = -1;

					public void bind(long reference, long count) {
						index = -1;
					}

					public int copyRows(long offset, int maximum, long[] rows, long[] weights) {
						throw new AssertionError("direct source copied");
					}

					public boolean nextTuple() {
						return ++index < 2;
					}

					public long[] rowArray() {
						accesses[0]++;
						return payload;
					}

					public int rowOffset() {
						return index * 2;
					}

					public long tupleWeight() {
						return index == 0 ? 2 : 3;
					}
				};
			}
		}) {
			BorrowedTupleBatch b = new BorrowedTupleBatch(source, 1);
			b.reset(1);
			b.bind(0, 0, 5);
			try (BorrowedTupleBatch.Cursor c = b.cursor(1)) {
				for (int pass = 0; pass < 2; pass++) {
					c.bind(0);
					fails(IllegalStateException.class, () -> c.value(0));
					yes(c.next());
					eq(10, c.value(0));
					eq(100, c.value(1));
					eq(2, c.weight());
					yes(c.next());
					eq(200, c.value(1));
					eq(3, c.weight());
					yes(!c.next());
					fails(IllegalStateException.class, c::weight);
				}
				eq(2, accesses[0]);
			}
		}
	}

	@Test
	public void malformedDirectRowsRejectBoundsWeightsAndTruncation() {
		for (int mode = 0; mode < 3; mode++) {
			int failureMode = mode;
			try (BorrowedTupleBatch.Source source = new BorrowedTupleBatch.Source(2) {
				protected void validate(long ref, long count) {
				}

				public BorrowedTupleBatch.Reader openReader() {
					return new BorrowedTupleBatch.DirectReader() {
						public void bind(long ref, long count) {
						}

						public int copyRows(long o, int m, long[] r, long[] w) {
							throw new AssertionError();
						}

						public boolean nextTuple() {
							return failureMode != 0;
						}

						public long[] rowArray() {
							return new long[2];
						}

						public int rowOffset() {
							return failureMode == 2 ? 1 : 0;
						}

						public long tupleWeight() {
							return failureMode == 1 ? 0 : 1;
						}
					};
				}
			}) {
				BorrowedTupleBatch b = new BorrowedTupleBatch(source, 1);
				b.reset(1);
				b.bind(0, 0, 1);
				try (BorrowedTupleBatch.Cursor c = b.cursor(2)) {
					c.bind(0);
					fails(failureMode == 2 ? IndexOutOfBoundsException.class : IllegalStateException.class, c::next);
				}
			}
		}
	}
}
