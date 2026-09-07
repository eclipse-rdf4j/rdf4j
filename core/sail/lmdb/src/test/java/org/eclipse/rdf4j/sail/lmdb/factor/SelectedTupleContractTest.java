/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.factor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Real selected tuple sources/cursors and factor environments, no storage or factor doubles. */
public class SelectedTupleContractTest {
	private static void eq(long expected, long actual) {
		if (expected != actual) throw new AssertionError(expected + " != " + actual);
	}
	private static void yes(boolean condition) { if (!condition) throw new AssertionError(); }
	private static void fails(Class<? extends Throwable> type, Runnable action) {
		try { action.run(); } catch (Throwable failure) {
			if (type.isInstance(failure)) return;
			throw new AssertionError("wrong exception", failure);
		}
		throw new AssertionError("expected " + type.getName());
	}

	/** Immutable weighted row source with explicit instrumentation; its arrays are test-owned. */
	public static final class DirectTuples extends BorrowedTupleBatch.Source {
		public final long[] payload, weights;
		public final long count;
		public int navigations, copies, readers;
		public DirectTuples(int columns, long[] payload, long[] weights) {
			super(columns);
			if (payload.length != weights.length * columns) throw new IllegalArgumentException();
			this.payload = payload.clone(); this.weights = weights.clone();
			long n = 0; for (long weight : weights) { if (weight <= 0) throw new IllegalArgumentException(); n = Math.addExact(n, weight); }
			count = n;
		}
		public BorrowedTupleBatch batch() {
			BorrowedTupleBatch b = new BorrowedTupleBatch(this, 1); b.reset(1); b.bind(0, 0, count); return b;
		}
		@Override protected void validate(long reference, long expected) {
			checkOpen(); if (reference != 0L || expected != count) throw new IllegalArgumentException();
		}
		@Override public BorrowedTupleBatch.Reader openReader() {
			checkOpen(); readers++;
			return new BorrowedTupleBatch.DirectReader() {
				int index = -1; boolean closed;
				public void bind(long reference, long expected) { validate(reference, expected); index = -1; }
				public boolean nextTuple() { checkOpen(); if (closed) throw new IllegalStateException(); navigations++; return ++index < weights.length; }
				public long[] rowArray() { return payload; }
				public int rowOffset() { return index * columns(); }
				public long tupleWeight() { return weights[index]; }
				public int copyRows(long offset, int maximum, long[] rows, long[] outWeights) {
					checkOpen(); if (closed) throw new IllegalStateException(); copies++;
					long position = 0, left = Math.min(maximum, count - offset); int written = 0;
					for (int i = 0; i < weights.length && left != 0L; i++) {
						long end = position + weights[i];
						if (end > offset) {
							long take = Math.min(left, end - Math.max(position, offset));
							System.arraycopy(payload, i * columns(), rows, written * columns(), columns());
							outWeights[written++] = take; left -= take;
						}
						position = end;
					}
					return written;
				}
				public void close() { closed = true; }
			};
		}
	}

	private static Map<String, Long> bag(BorrowedTupleBatch batch, int window) {
		Map<String, Long> result = new HashMap<>();
		try (BorrowedTupleBatch.Cursor cursor = batch.cursor(window)) {
			cursor.bind(0); long position = 0;
			while (cursor.next()) {
				eq(position, cursor.position()); position += cursor.weight();
				String key = ""; for (int c = 0; c < batch.columns(); c++) key += cursor.value(c) + ":";
				result.merge(key, cursor.weight(), Math::addExact);
			}
			eq(batch.count(0), position);
		}
		return result;
	}

	@Test public void directSelectionsBorrowTheLocatedRowsWithoutRewalkingTheSource() {
		try (DirectTuples source = new DirectTuples(2, new long[] {1, 10, 2, 20, 3, 30, 4, 40}, new long[] {2, 3, 5, 7});
				SelectedTupleSource selected = new SelectedTupleSource(2, 0, true)) {
			BorrowedTupleBatch input = source.batch(); TupleSelection builder = new TupleSelection(0);
			try (BorrowedTupleBatch.Cursor cursor = input.cursor(3)) {
				cursor.bind(0); builder.begin(cursor);
				while (cursor.next()) if ((cursor.value(0) & 1) == 0) builder.add(cursor);
			}
			BorrowedTupleBatch view = selected.select(builder); builder.clear();
			int before = source.navigations; eq(10, view.count(0));
			Map<String, Long> expected = Map.of("2:20:", 3L, "4:40:", 7L);
			yes(expected.equals(bag(view, 1))); yes(expected.equals(bag(view, 17)));
			eq(before, source.navigations); eq(0, source.copies);
			try (BorrowedTupleBatch.Cursor c = view.cursor(1)) { c.bind(0); yes(c.directRowArray() == source.payload); }
		}
	}

	@Test public void genericRangesClipWeightedTuplesAndComposeIntoBaseCoordinates() {
		try (BorrowedTupleContractTest.WeightedTuples source = new BorrowedTupleContractTest.WeightedTuples(2,
				new long[][] {{10, 100}, {20, 200}, {30, 300}}, new long[] {5, 7, 3});
				SelectedTupleSource first = new SelectedTupleSource(2, 2);
				SelectedTupleSource second = new SelectedTupleSource(2, 2)) {
			FactorSelection a = new FactorSelection(2); a.add(2, 6); a.add(10, 4);
			BorrowedTupleBatch b = first.select(source.batch(), 0, a);
			yes(Map.of("10:100:", 3L, "20:200:", 5L, "30:300:", 2L).equals(bag(b, 3)));
			FactorSelection cut = new FactorSelection(2); cut.add(1, 3); cut.add(7, 2);
			BorrowedTupleBatch c = second.select(b, 0, cut); cut.clear(); a.clear();
			yes(Map.of("10:100:", 2L, "20:200:", 2L, "30:300:", 1L).equals(bag(c, 1)));
			first.clear(); fails(IllegalStateException.class, () -> c.count(0));
		}
	}

	@Test public void directSelectionsOfSelectionsRetainTheUltimateBackingAndProvenance() {
		try (DirectTuples source = new DirectTuples(2, new long[] {1, 10, 2, 20, 3, 30, 4, 40}, new long[] {2, 3, 5, 7});
				SelectedTupleSource a = new SelectedTupleSource(2, 2, true);
				SelectedTupleSource b = new SelectedTupleSource(2, 2, true)) {
			TupleSelection selection = new TupleSelection(2);
			try (BorrowedTupleBatch.Cursor c = source.batch().cursor(2)) {
				c.bind(0); selection.begin(c); while (c.next()) if (c.value(0) > 1) selection.add(c);
			}
			BorrowedTupleBatch first = a.select(selection);
			try (BorrowedTupleBatch.Cursor c = first.cursor(1)) {
				c.bind(0); selection.begin(c); while (c.next()) if (c.value(0) != 3) selection.add(c);
			}
			BorrowedTupleBatch second = b.select(selection);
			try (BorrowedTupleBatch.Cursor c = second.cursor(2)) {
				c.bind(0); yes(c.directRowArray() == source.payload); yes(c.next()); eq(2, c.value(0)); eq(3, c.weight());
				a.clear(); fails(IllegalStateException.class, c::next);
			}
		}
	}

	@Test public void copiedNonemptyAndEmptyDescriptorsCannotOutlivePublication() {
		for (boolean accept : new boolean[] {false, true}) {
			try (DirectTuples source = new DirectTuples(2, new long[] {1, 10}, new long[] {2});
					SelectedTupleSource selected = new SelectedTupleSource(2, 0, true)) {
				TupleSelection builder = new TupleSelection(0);
				try (BorrowedTupleBatch.Cursor c = source.batch().cursor(1)) { c.bind(0); builder.begin(c); yes(c.next()); if (accept) builder.add(c); }
				BorrowedTupleBatch view = selected.select(builder), copy = new BorrowedTupleBatch(selected, 1);
				copy.reset(1); copy.copyLaneFrom(0, view, 0); eq(accept ? 2 : 0, copy.count(0));
				FactorEnvironment env = new FactorEnvironment(3); env.bindTuple(new int[] {1, 2}, copy, 0, 1);
				selected.clear(); fails(IllegalStateException.class, () -> copy.count(0));
				fails(IllegalStateException.class, env::checkValid);
				fails(IllegalStateException.class, () -> new FactorEnvironment(3).append(env, -1));
			}
		}
	}

	@Test public void emptyDirectAndGenericSelectionsRemainExactWithoutOpeningAReader() {
		try (DirectTuples source = new DirectTuples(2, new long[0], new long[0]);
				SelectedTupleSource direct = new SelectedTupleSource(2, 0, true);
				SelectedTupleSource generic = new SelectedTupleSource(2, 0)) {
			BorrowedTupleBatch b = source.batch(); TupleSelection selection = new TupleSelection(0);
			try (BorrowedTupleBatch.Cursor c = b.cursor(1)) { c.bind(0); selection.begin(c); yes(!c.next()); }
			eq(0, direct.select(selection).count(0));
			eq(0, generic.select(b, 0, new FactorSelection(0)).count(0));
			yes(bag(direct.batch(), 1).isEmpty()); yes(bag(generic.batch(), 1).isEmpty());
		}
	}

	@Test public void directRangeReadsSupportBackwardAndPartialLongWeights() {
		long huge = 1L << 35;
		try (DirectTuples source = new DirectTuples(2, new long[] {Long.MIN_VALUE, 10, 2, 20}, new long[] {huge, 9});
				SelectedTupleSource selected = new SelectedTupleSource(2, 2, true)) {
			TupleSelection selection = new TupleSelection(2);
			try (BorrowedTupleBatch.Cursor c = source.batch().cursor(1)) { c.bind(0); selection.begin(c); while (c.next()) selection.add(c); }
			BorrowedTupleBatch b = selected.select(selection);
			try (BorrowedTupleBatch.Reader r = selected.openReader()) {
				r.bind(b.reference(0), b.count(0)); long[] rows = new long[12], weights = new long[6];
				eq(2, r.copyRows(huge - 3, 6, rows, weights)); eq(3, weights[0]); eq(3, weights[1]); eq(20, rows[3]);
				eq(1, r.copyRows(1, 4, rows, weights)); eq(4, weights[0]); eq(Long.MIN_VALUE, rows[0]);
				BorrowedTupleBatch.DirectReader d = (BorrowedTupleBatch.DirectReader) r;
				yes(d.nextTuple()); eq(huge - 5, d.tupleWeight()); yes(d.nextTuple()); eq(9, d.tupleWeight()); yes(!d.nextTuple());
			}
			FactorEnvironment e = new FactorEnvironment(64); e.bindTuple(new int[] {63, 1}, b, 0, 1);
			eq((huge + 9) * 2, e.countProduct(-1, 2));
			fails(ArithmeticException.class, () -> e.countProduct(-1, Long.MAX_VALUE));
		}
	}

	@Test public void independentSelectionReadersAndClosingViewDoNotCloseTheirOwner() {
		try (DirectTuples source = new DirectTuples(2, new long[] {1, 11, 2, 22}, new long[] {3, 4})) {
			SelectedTupleSource selected = new SelectedTupleSource(2, 2, true); TupleSelection s = new TupleSelection(2);
			BorrowedTupleBatch input = source.batch();
			try (BorrowedTupleBatch.Cursor c = input.cursor(2)) { c.bind(0); s.begin(c); while (c.next()) s.add(c); }
			BorrowedTupleBatch b = selected.select(s);
			try (BorrowedTupleBatch.Cursor a = b.cursor(1); BorrowedTupleBatch.Cursor c = b.cursor(3)) {
				a.bind(0); c.bind(0); yes(a.next()); yes(a.next()); yes(c.next()); eq(1, c.value(0)); eq(2, a.value(0));
				selected.close(); fails(IllegalStateException.class, c::next);
			}
			eq(7, input.count(0)); yes(bag(input, 3).size() == 2);
		}
	}

	@Test public void builderRejectsRepeatReorderAndChangedSources() {
		try (DirectTuples source = new DirectTuples(2, new long[] {1, 10, 2, 20}, new long[] {3, 4});
				SelectedTupleSource selected = new SelectedTupleSource(2, 0, true)) {
			BorrowedTupleBatch b = source.batch(); TupleSelection s = new TupleSelection(0);
			try (BorrowedTupleBatch.Cursor c = b.cursor(2)) {
				c.bind(0); s.begin(c); yes(c.next()); s.add(c);
				fails(IllegalArgumentException.class, () -> s.add(c));
				c.bind(0); yes(c.next()); fails(IllegalArgumentException.class, () -> s.add(c));
				b.reset(1); b.bind(0, 0, source.count);
				fails(IllegalStateException.class, () -> selected.select(s));
			}
		}
	}

	@Test public void cyclesAreRejectedWithoutDestroyingThePublishedSelection() {
		try (DirectTuples source = new DirectTuples(2, new long[] {1, 10}, new long[] {3});
				SelectedTupleSource selected = new SelectedTupleSource(2, 1, true)) {
			TupleSelection s = new TupleSelection(1);
			try (BorrowedTupleBatch.Cursor c = source.batch().cursor(1)) { c.bind(0); s.begin(c); yes(c.next()); s.add(c); }
			BorrowedTupleBatch b = selected.select(s);
			try (BorrowedTupleBatch.Cursor c = b.cursor(1)) { c.bind(0); s.begin(c); yes(c.next()); s.add(c); }
			fails(IllegalArgumentException.class, () -> selected.select(s)); eq(3, b.count(0));
		}
	}

	@Test public void randomizedWeightedSelectionsKeepCorrelationAndDuplicateMultiplicity() {
		Random random = new Random(0x51e7ed);
		for (int trial = 0; trial < 200; trial++) {
			int width = 1 + random.nextInt(4), size = random.nextInt(60);
			long[] payload = new long[width * size], weights = new long[size]; long[][] rows = new long[size][width];
			Map<String, Long> expected = new HashMap<>();
			for (int i = 0; i < size; i++) {
				String key = ""; weights[i] = 1 + random.nextInt(12);
				for (int c = 0; c < width; c++) { long v = random.nextInt(9) + (c == 0 ? Long.MIN_VALUE : 0L); payload[i * width + c] = rows[i][c] = v; key += v + ":"; }
				if ((rows[i][0] & 3) != 0) expected.merge(key, weights[i], Math::addExact);
			}
			try (DirectTuples direct = new DirectTuples(width, payload, weights);
					BorrowedTupleContractTest.WeightedTuples generic = new BorrowedTupleContractTest.WeightedTuples(width, rows, weights)) {
				for (BorrowedTupleBatch b : new BorrowedTupleBatch[] {direct.batch(), generic.batch()}) {
					TupleSelection selection = new TupleSelection(0); boolean isDirect;
					try (BorrowedTupleBatch.Cursor c = b.cursor(1 + random.nextInt(17))) {
						c.bind(0); selection.begin(c); isDirect = c.hasDirectRows();
						while (c.next()) if ((c.value(0) & 3) != 0) selection.add(c);
					}
					try (SelectedTupleSource view = new SelectedTupleSource(width, 0, isDirect)) {
						BorrowedTupleBatch selected = view.select(selection);
						Map<String, Long> actual = bag(selected, 1 + random.nextInt(19));
						if (!expected.equals(actual)) throw new AssertionError("trial " + trial + " direct=" + isDirect);
					}
				}
			}
		}
	}

	@Test public void arbitraryRangesOverDirectSelectionsPreservePartialWeights() {
		try (DirectTuples input = new DirectTuples(2, new long[] {1, 11, 2, 22, 3, 33}, new long[] {8, 9, 10});
				SelectedTupleSource direct = new SelectedTupleSource(2, 3, true);
				SelectedTupleSource ranged = new SelectedTupleSource(2, 3)) {
			TupleSelection s = new TupleSelection(3);
			try (BorrowedTupleBatch.Cursor c = input.batch().cursor(4)) { c.bind(0); s.begin(c); while (c.next()) s.add(c); }
			FactorSelection r = new FactorSelection(3); r.add(3, 8); r.add(16, 5);
			yes(Map.of("1:11:", 5L, "2:22:", 4L, "3:33:", 4L).equals(bag(ranged.select(direct.select(s), 0, r), 7)));
		}
	}

	@Test public void malformedGenericWeightsAreRejectedBeforePublishingPayload() {
		try (BorrowedTupleBatch.Source input = new BorrowedTupleBatch.Source(2) {
			protected void validate(long r, long n) { }
			public BorrowedTupleBatch.Reader openReader() { return new BorrowedTupleBatch.Reader() {
				public void bind(long r, long n) { }
				public int copyRows(long o, int m, long[] rows, long[] w) { w[0] = m + 1L; return 1; }
			}; }
		}; SelectedTupleSource selected = new SelectedTupleSource(2, 1)) {
			BorrowedTupleBatch b = new BorrowedTupleBatch(input, 1); b.reset(1); b.bind(0, 0, 8);
			FactorSelection ranges = new FactorSelection(1); ranges.add(2, 4);
			try (BorrowedTupleBatch.Cursor c = selected.select(b, 0, ranges).cursor(2)) { c.bind(0); fails(IllegalStateException.class, c::next); }
		}
	}

	@Test public void nestedProvenanceVisitsTheRootOnlyOncePerCount() {
		int[] visits = {0};
		try (BorrowedTupleBatch.Source root = new BorrowedTupleBatch.Source(1, true) {
			protected void validateState() { visits[0]++; }
			protected void validate(long reference, long count) {
				if (reference != 1 || count != 100) throw new IllegalArgumentException();
			}
			public BorrowedTupleBatch.Reader openReader() { throw new AssertionError("metadata-only test opened values"); }
		}) {
			BorrowedTupleBatch b = new BorrowedTupleBatch(root, 1); b.reset(1); b.bind(0, 1, 100);
			SelectedTupleSource[] chain = new SelectedTupleSource[40];
			try {
				FactorSelection all = new FactorSelection(1); all.add(0, 100);
				for (int i = 0; i < chain.length; i++) {
					chain[i] = new SelectedTupleSource(1, 1); b = chain[i].select(b, 0, all);
				}
				visits[0] = 0; eq(100, b.count(0)); eq(1, visits[0]);
				chain[5].clear(); BorrowedTupleBatch last = b;
				fails(IllegalStateException.class, () -> last.count(0));
			} finally { for (int i = chain.length - 1; i >= 0; i--) if (chain[i] != null) chain[i].close(); }
		}
	}
}
