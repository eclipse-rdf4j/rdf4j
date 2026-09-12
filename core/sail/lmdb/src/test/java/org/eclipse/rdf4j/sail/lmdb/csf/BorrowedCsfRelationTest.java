/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.csf;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.eclipse.rdf4j.sail.lmdb.factor.BorrowedFactorBatch;
import org.eclipse.rdf4j.sail.lmdb.factor.FactorSelection;
import org.eclipse.rdf4j.sail.lmdb.factor.SelectedFactorSource;
import org.junit.jupiter.api.Test;

/** Real CSF allocation/codec tests, including continuation seams and cursor reuse. */
public class BorrowedCsfRelationTest {
	private record Row(long root, long[] values, long[] contexts) {
	}

	private static long id(long x) {
		return (x << 7) | 2L;
	}

	private static void equal(long a, long b) {
		if (a != b)
			throw new AssertionError(a + " != " + b);
	}

	private static void feed(ImmutablePagedQuadCsfIndex.Builder builder, List<Row> rows) {
		for (Row row : rows) {
			builder.beginRow(0, 0, row.root);
			for (int i = 0; i < row.values.length; i++)
				builder.pair(row.values[i], row.contexts[i]);
			builder.endRow();
		}
	}

	private static ImmutablePagedQuadCsfIndex build(List<Row> rows) {
		ImmutablePagedQuadCsfIndex.BuildPlan plan;
		try (var sizing = ImmutablePagedQuadCsfIndex.sizingBuilder(1)) {
			feed(sizing, rows);
			plan = sizing.finishPlan();
		}
		try (var builder = ImmutablePagedQuadCsfIndex.materializingBuilder(plan)) {
			feed(builder, rows);
			return builder.finishIndex();
		}
	}

	private static final class Source extends BorrowedFactorBatch.Source {
		final ImmutablePagedQuadCsfIndex index;

		Source(ImmutablePagedQuadCsfIndex index) {
			super(index);
			this.index = index;
			index.retain();
		}

		@Override
		protected void release() {
			index.close();
		}

		void export(BorrowedFactorBatch batch, int lane, ImmutablePagedQuadCsfIndex.RowCursor row, long ref) {
			batch.bindNative(lane, row.singlePageRow() ? BorrowedFactorBatch.CSF_PAGE : BorrowedFactorBatch.ENCODED_RUN,
					row.firstPageAddress(), ref, row.firstLocalRow(), row.edgeCount());
		}

		@Override
		public BorrowedFactorBatch.Reader openReader() {
			checkOpen();
			return new BorrowedFactorBatch.Reader() {
				final ImmutablePagedQuadCsfIndex.BorrowedPageReader page = new ImmutablePagedQuadCsfIndex.BorrowedPageReader();
				final ImmutablePagedQuadCsfIndex.RowCursor row = new ImmutablePagedQuadCsfIndex.RowCursor();
				boolean single;

				public void bind(BorrowedFactorBatch b, int lane) {
					checkOpen();
					single = b.kind(lane) == BorrowedFactorBatch.CSF_PAGE;
					if (single)
						page.bind(b.address(lane), b.coordinate(lane));
					else
						index.resolve(b.reference(lane), row);
				}

				public int copyFibers(long offset, int maximum, long[] values, long[] weights) {
					checkOpen();
					return single ? page.copyFibers(offset, maximum, values, weights)
							: row.copyFibers(offset, maximum, values, weights);
				}
			};
		}
	}

	@Test
	public void positionedDescriptorsSurviveLookupCursorRebindAndIndexOwnerClose() {
		List<Row> rows = new ArrayList<>();
		Random r = new Random(752);
		for (int root = 0; root < 2200; root++) {
			int degree = 1 + r.nextInt(12);
			long[] v = new long[degree], c = new long[degree];
			long n = (root % 5 == 0 ? Long.MIN_VALUE : 0L) + id(5000);
			for (int i = 0; i < degree; i++) {
				if (i > 0 && r.nextBoolean())
					n += 128;
				v[i] = n;
				c[i] = id(i + 1);
			}
			rows.add(new Row(id(100 + 3L * root), v, c));
		}
		ImmutablePagedQuadCsfIndex index = build(rows);
		try (Source source = new Source(index)) {
			BorrowedFactorBatch batch = new BorrowedFactorBatch(source, rows.size());
			batch.reset(rows.size());
			var lookup = new ImmutablePagedQuadCsfIndex.RowCursor();
			for (int i = 0; i < rows.size(); i++) {
				long ref = index.findLocalReference(0, 0, rows.get(i).root);
				index.resolve(ref, lookup);
				source.export(batch, i, lookup, ref);
			}
			index.close(); // The source's retained lease alone now keeps the pages alive.
			try (var a = batch.cursor(1); var b = batch.cursor(7)) {
				for (int i = rows.size() - 1; i >= 0; i--) {
					a.bind(i);
					b.bind(i);
					int at = 0;
					while (a.next()) {
						if (!b.next())
							throw new AssertionError("second consumer stopped");
						equal(a.value(), b.value());
						equal(a.weight(), b.weight());
						for (long k = 0; k < a.weight(); k++)
							equal(rows.get(i).values[at++], a.value());
					}
					equal(rows.get(i).values.length, at);
					if (b.next())
						throw new AssertionError("second consumer overran");
				}
			}
		}
	}

	@Test
	public void keyCursorExportKeepsRowPositionAcrossPages() {
		List<Row> rows = new ArrayList<>();
		for (int i = 0; i < 2300; i++)
			rows.add(new Row(id(11 + 2L * i), new long[] { id(i + 10000) }, new long[] { 0 }));
		try (ImmutablePagedQuadCsfIndex index = build(rows); Source source = new Source(index)) {
			BorrowedFactorBatch batch = new BorrowedFactorBatch(source, rows.size());
			batch.reset(rows.size());
			var cursor = index.keyDomain(0, 0).cursor();
			int at = 0;
			while (cursor.advance()) {
				equal(rows.get(at).root, cursor.key());
				if (!cursor.singlePageRow())
					throw new AssertionError("unexpected continuation");
				batch.bindNative(at++, BorrowedFactorBatch.CSF_PAGE, cursor.firstPageAddress(), cursor.localReference(),
						cursor.firstLocalRow(), cursor.edgeCount());
			}
			equal(rows.size(), at);
			try (var c = batch.cursor(1)) {
				for (int i = 0; i < at; i++) {
					c.bind(i);
					if (!c.next())
						throw new AssertionError();
					equal(rows.get(i).values[0], c.value());
					if (c.next())
						throw new AssertionError();
				}
			}
		}
	}

	@Test
	public void highDegreeAndSplitContextFibersKeepExactMultisets() {
		for (boolean sameNeighbor : new boolean[] { false, true }) {
			int n = 180_000;
			long[] v = new long[n], c = new long[n];
			for (int i = 0; i < n; i++) {
				v[i] = Long.MIN_VALUE + id(sameNeighbor ? 500 : i / 3 + 500);
				c[i] = sameNeighbor ? id(i + 1) : id(i % 3 + 1);
			}
			Row row = new Row(id(47), v, c);
			try (ImmutablePagedQuadCsfIndex index = build(List.of(row)); Source source = new Source(index)) {
				var lookup = new ImmutablePagedQuadCsfIndex.RowCursor();
				long ref = index.findLocalReference(0, 0, row.root);
				index.resolve(ref, lookup);
				if (lookup.singlePageRow())
					throw new AssertionError("test did not span pages");
				BorrowedFactorBatch batch = new BorrowedFactorBatch(source, 1);
				batch.reset(1);
				source.export(batch, 0, lookup, ref);
				equal(n, batch.count(0));
				for (int window : new int[] { 1, 17, 4096 })
					try (var cursor = batch.cursor(window)) {
						cursor.bind(0);
						int at = 0;
						while (cursor.next())
							for (long k = 0; k < cursor.weight(); k++)
								equal(v[at++], cursor.value());
						equal(n, at);
					}
			}
		}
	}

	@Test
	public void extentReaderSupportsBackwardSeeksAndPartialContextFibers() {
		for (int n : new int[] { 91, 150_001 }) {
			long[] values = new long[n], contexts = new long[n];
			for (int i = 0; i < n; i++) {
				values[i] = id(i / 7 + 1000);
				contexts[i] = id(i % 7 + 1);
			}
			Row input = new Row(id(47), values, contexts);
			try (ImmutablePagedQuadCsfIndex index = build(List.of(input))) {
				var row = new ImmutablePagedQuadCsfIndex.RowCursor();
				index.resolve(index.findLocalReference(0, 0, input.root), row);
				long[] out = new long[17], weights = new long[17];
				for (int start : new int[] { n - 1, 5, n / 2 + 1, 0, n - 17, 6 }) {
					int at = start;
					for (int window = 0; window < 5 && at < n; window++) {
						int got = row.copyFibers(at, out.length, out, weights);
						if (got <= 0)
							throw new AssertionError("no seek progress");
						for (int i = 0; i < got; i++)
							for (long k = 0; k < weights[i]; k++)
								equal(values[at++], out[i]);
					}
				}
			}
		}
	}

	@Test
	public void selectedCsfExtentsClipContextFibersAndComposeWithoutCopyingValues() {
		for (int n : new int[] { 91, 150_001 }) {
			long[] values = new long[n], contexts = new long[n];
			for (int i = 0; i < n; i++) {
				values[i] = Long.MIN_VALUE + id(i / 7 + 1000);
				contexts[i] = id(i % 7 + 1);
			}
			Row input = new Row(id(47), values, contexts);
			try (ImmutablePagedQuadCsfIndex index = build(List.of(input));
					Source source = new Source(index);
					SelectedFactorSource one = new SelectedFactorSource(0);
					SelectedFactorSource two = new SelectedFactorSource(0)) {
				var row = new ImmutablePagedQuadCsfIndex.RowCursor();
				long ref = index.findLocalReference(0, 0, input.root);
				index.resolve(ref, row);
				var batch = new BorrowedFactorBatch(source, 1);
				batch.reset(1);
				source.export(batch, 0, row, ref);
				FactorSelection selection = new FactorSelection(0);
				List<Long> expected = new ArrayList<>();
				for (int i = 2; i < n; i += 41) {
					int length = Math.min(17, n - i);
					selection.add(i, length);
					for (int j = i; j < i + length; j++)
						expected.add(values[j]);
				}
				var selected = one.select(batch, 0, selection);
				equal(expected.size(), selected.count(0));
				FactorSelection sub = new FactorSelection(0);
				List<Long> reduced = new ArrayList<>();
				for (int i = 1; i < expected.size(); i += 3) {
					sub.add(i, 1);
					reduced.add(expected.get(i));
				}
				var result = two.select(selected, 0, sub);
				for (int window : new int[] { 1, 17, 256 })
					try (var c = result.cursor(window)) {
						c.bind(0);
						int at = 0;
						while (c.next())
							for (long k = 0; k < c.weight(); k++)
								equal(reduced.get(at++), c.value());
						equal(reduced.size(), at);
					}
			}
		}
	}

	@Test
	public void pageAndExtentReadersAcceptRandomAndMonotonePartialSeeks() {
		Random random = new Random(761213);
		for (int n : new int[] { 513, 150_019 })
			for (boolean unit : new boolean[] { false, true }) {
				long[] values = new long[n], contexts = new long[n];
				int at = 0;
				long neighbor = Long.MIN_VALUE + id(1000);
				while (at < n) {
					int length = unit ? 1 : 1 + random.nextInt(23);
					for (int c = 0; c < length && at < n; c++, at++) {
						values[at] = neighbor;
						contexts[at] = id(c + 1);
					}
					neighbor += id(1);
				}
				Row input = new Row(id(47), values, contexts);
				try (ImmutablePagedQuadCsfIndex index = build(List.of(input)); Source source = new Source(index)) {
					var row = new ImmutablePagedQuadCsfIndex.RowCursor();
					long ref = index.findLocalReference(0, 0, input.root);
					index.resolve(ref, row);
					var batch = new BorrowedFactorBatch(source, 1);
					batch.reset(1);
					source.export(batch, 0, row, ref);
					long[] out = new long[17], weights = new long[17];
					try (var reader = source.openReader()) {
						reader.bind(batch, 0);
						int position = 0;
						for (int iteration = 0; iteration < 800; iteration++) {
							if ((iteration & 7) == 0)
								position = random.nextInt(n);
							else
								position = Math.min(n - 1, position + random.nextInt(79));
							int maximum = 1 + random.nextInt(out.length);
							int got = reader.copyFibers(position, maximum, out, weights), logical = position;
							if (got <= 0 || got > maximum)
								throw new AssertionError("seek did not make bounded progress");
							for (int i = 0; i < got; i++) {
								if (weights[i] <= 0)
									throw new AssertionError("empty fiber");
								for (long j = 0; j < weights[i]; j++)
									equal(values[logical++], out[i]);
							}
						}
					}
				}
			}
	}

	@Test
	public void independentCountWindowsUseRealCsfPagesAndContinuationFibers() {
		for (int size : new int[] { 91, 180001 }) {
			long[] values = new long[size], contexts = new long[size];
			long expected = 0;
			long low = Long.MIN_VALUE + id(1011), high = Long.MIN_VALUE + id(1111);
			for (int i = 0; i < size; i++) {
				values[i] = Long.MIN_VALUE + id(i / 7 + 1000);
				contexts[i] = id(i % 7 + 1);
				if (Long.compareUnsigned(values[i], low) >= 0 && Long.compareUnsigned(values[i], high) <= 0)
					expected++;
			}
			Row input = new Row(id(47), values, contexts);
			try (var index = build(List.of(input));
					var source = new Source(index);
					var counter = new org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.KernelFactorCount(new int[] { 1 },
							null)) {
				var row = new ImmutablePagedQuadCsfIndex.RowCursor();
				long ref = index.findLocalReference(0, 0, input.root);
				index.resolve(ref, row);
				var batch = new BorrowedFactorBatch(source, 1);
				batch.reset(1);
				source.export(batch, 0, row, ref);
				var env = new org.eclipse.rdf4j.sail.lmdb.factor.FactorEnvironment(2);
				env.bind(1, batch, 0, 1);
				var predicate = new org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.KernelFactorPredicate() {
					public int guardCount() {
						return 1;
					}

					public long dependencies(int guard) {
						return 1;
					}

					public boolean test(int guard, long[] prefix) {
						throw new AssertionError("factor not scalar");
					}

					public void filter(int guard, long[][] columns, long[] prefix, long[] mask, int n) {
						org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.KernelIdMasks.range(columns[0], 0, low, high,
								mask, n);
					}
				};
				equal(expected * 3, counter.count(env, new long[1], 3, predicate));
			}
		}
	}

}
