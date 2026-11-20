/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.order.StatementOrder;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.sail.lmdb.LmdbPrefixRunCursor;
import org.eclipse.rdf4j.sail.lmdb.LmdbPrefixRunPlan;
import org.eclipse.rdf4j.sail.lmdb.LmdbPrefixRunPlanTestAccess;
import org.eclipse.rdf4j.sail.lmdb.RecordIterator;
import org.eclipse.rdf4j.sail.lmdb.TripleIndex;
import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.KernelQueryCancelledException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

/**
 * A query cancellation (timeout, client disconnect, close) must stop a running non-IR type-matrix scan at its next
 * poll, on the evaluating thread and in parallel workers. The fixtures request cancellation from inside the scan after
 * a fixed number of rows so the test is deterministic: before the fix the scan simply runs to completion.
 */
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class LmdbNativeTypeMatrixCancellationTest {

	private static final long SUBJECT_TYPE_PREDICATE = 2L;
	private static final long EDGE_PREDICATE = 3L;
	private static final int ROWS = 1 << 14;
	private static final int CANCEL_AFTER = 100;
	private static final Duration BOUND = Duration.ofSeconds(60);

	@Test
	void serialPredicatePrecomputeStopsOnQueryCancellation() {
		NativeCancellationToken token = new NativeCancellationToken();
		SerialSource source = new SerialSource(token);
		NativeSlotLayout layout = new NativeSlotLayout(Map.of("predicate", 0), null);
		layout.freeze(List.of("predicate"));
		LmdbNativeTypeMatrix matrix = new LmdbNativeTypeMatrix(source, SUBJECT_TYPE_PREDICATE,
				NativeLmdbQuerySource.UNKNOWN_ID, 0, new String[] { "type", "predicate" }, new String[] { "count" },
				new MaskedFilter[] { new MaskedFilter(row -> true, 1L) }, 0, layout, null, null, null);

		Throwable failure = assertTimeoutPreemptively(BOUND,
				() -> catchThrowable(() -> evaluate(matrix, source, token)));

		assertThat(failure).isInstanceOf(KernelQueryCancelledException.class);
		assertThat(token.owns((KernelQueryCancelledException) failure)).isTrue();
		assertThat(source.prefixCursor.closed).as("prefix cursor must be closed on cancellation").isTrue();
		assertThat(source.prefixCursor.index).as("precompute must stop at its next poll").isLessThan(ROWS);
	}

	@Test
	void serialCoScanStopsOnQueryCancellation() {
		NativeCancellationToken token = new NativeCancellationToken();
		SerialSource source = new SerialSource(token);
		LmdbNativeTypeMatrix matrix = new LmdbNativeTypeMatrix(source, SUBJECT_TYPE_PREDICATE,
				NativeLmdbQuerySource.UNKNOWN_ID, 0, new String[] { "type", "predicate" }, new String[] { "count" },
				new MaskedFilter[0], 0, NativeSlotLayout.empty(), null, null, null);

		Throwable failure = assertTimeoutPreemptively(BOUND,
				() -> catchThrowable(() -> evaluate(matrix, source, token)));

		assertThat(failure).isInstanceOf(KernelQueryCancelledException.class);
		assertThat(token.owns((KernelQueryCancelledException) failure)).isTrue();
		assertThat(source.statementsRead.get()).as("co-scan must stop at its next poll").isLessThan(ROWS);
	}

	@Test
	void parallelCoScanWorkersStopOnQueryCancellation() {
		String previousThreads = System.getProperty("rdf4j.lmdb.parallel.threads");
		String previousMaxTasks = System.getProperty("rdf4j.lmdb.parallel.maxTasks");
		String previousThreshold = System.getProperty(LmdbNativeParallelPrefixRuns.PARALLEL_MIN_ESTIMATE_PROPERTY);
		try {
			System.setProperty("rdf4j.lmdb.parallel.threads", "2");
			System.setProperty("rdf4j.lmdb.parallel.maxTasks", "2");
			System.setProperty(LmdbNativeParallelPrefixRuns.PARALLEL_MIN_ESTIMATE_PROPERTY, "0");

			NativeCancellationToken token = new NativeCancellationToken();
			ParallelFixture source = new ParallelFixture(new AtomicInteger(), token);
			LmdbNativeTypeMatrix matrix = new LmdbNativeTypeMatrix(source, SUBJECT_TYPE_PREDICATE,
					NativeLmdbQuerySource.UNKNOWN_ID, 0, new String[] { "type", "predicate" },
					new String[] { "count" }, new MaskedFilter[0], 0, NativeSlotLayout.empty(), null, null, null);

			Throwable failure = assertTimeoutPreemptively(BOUND,
					() -> catchThrowable(() -> evaluate(matrix, source, token)));

			assertThat(failure).isInstanceOf(KernelQueryCancelledException.class);
			assertThat(token.owns((KernelQueryCancelledException) failure)).isTrue();
			assertThat(source.workers).isNotEmpty().allSatisfy(worker -> assertThat(worker.closed).isTrue());
			assertThat(source.read.get()).as("workers must stop at their next poll").isLessThan(2 * ROWS);
		} finally {
			restoreProperty("rdf4j.lmdb.parallel.threads", previousThreads);
			restoreProperty("rdf4j.lmdb.parallel.maxTasks", previousMaxTasks);
			restoreProperty(LmdbNativeParallelPrefixRuns.PARALLEL_MIN_ESTIMATE_PROPERTY, previousThreshold);
		}
	}

	private static void evaluate(LmdbNativeTypeMatrix matrix, NativeLmdbQuerySource source,
			NativeCancellationToken token) {
		try (CloseableIteration<BindingSet> result = matrix.evaluate(EmptyBindingSet.getInstance(), source, token)) {
			while (result.hasNext()) {
				result.next();
			}
		}
	}

	private static void restoreProperty(String property, String value) {
		if (value == null) {
			System.clearProperty(property);
		} else {
			System.setProperty(property, value);
		}
	}

	private static class SerialSource implements NativeLmdbQuerySource {
		final NativeCancellationToken token;
		final AtomicInteger statementsRead = new AtomicInteger();
		PrefixCursor prefixCursor;

		SerialSource(NativeCancellationToken token) {
			this.token = token;
		}

		@Override
		public long idOf(Value value) {
			return NativeLmdbQuerySource.UNKNOWN_ID;
		}

		@Override
		public Value lazyValue(long id) {
			return SimpleValueFactory.getInstance().createIRI("urn:type-matrix:id:" + id);
		}

		@Override
		public Object idSpace() {
			return this;
		}

		@Override
		public RecordIterator statements(long subj, long pred, long obj, long context) {
			return new RecordIterator() {
				private int index;

				@Override
				public long[] next() {
					if (index == ROWS) {
						return null;
					}
					if (statementsRead.incrementAndGet() == CANCEL_AFTER) {
						token.requestCancellation();
					}
					int row = index++;
					return row == 0 ? new long[] { 1L, SUBJECT_TYPE_PREDICATE, 10L, 0L }
							: new long[] { 1L, EDGE_PREDICATE + row, 11L, 0L };
				}

				@Override
				public void close() {
				}
			};
		}

		@Override
		public RecordIterator statements(StatementOrder order, long subj, long pred, long obj, long context) {
			return statements(subj, pred, obj, context);
		}

		@Override
		public LmdbPrefixRunPlan prefixRunPlan(int[] prefixFields, long subj, long pred, long obj, long context) {
			return prefixFields.length == 1 && prefixFields[0] == TripleIndex.PRED_IDX
					? LmdbPrefixRunPlanTestAccess.adjacencyPredicate(prefixFields)
					: null;
		}

		@Override
		public LmdbPrefixRunCursor prefixRuns(LmdbPrefixRunPlan plan, long subj, long pred, long obj, long context,
				boolean countRunRows) {
			prefixCursor = new PrefixCursor(token);
			return prefixCursor;
		}

		@Override
		public long count(long subj, long pred, long obj, long context) {
			return ROWS;
		}

		@Override
		public boolean has(long subj, long pred, long obj, long context) {
			return true;
		}

		@Override
		public double estimate(long subj, long pred, long obj, long context) {
			return ROWS;
		}

		@Override
		public boolean hasStatementsInSource() {
			return true;
		}
	}

	private static final class ParallelFixture extends SerialSource implements NativeLmdbQuerySource.ParallelSource {
		private static final Object ID_SPACE = new Object();
		final AtomicInteger read;
		volatile List<ParallelFixture> workers = List.of();
		volatile boolean closed;

		ParallelFixture(AtomicInteger read, NativeCancellationToken token) {
			super(token);
			this.read = read;
		}

		@Override
		public Object idSpace() {
			return ID_SPACE;
		}

		@Override
		public LmdbPrefixRunPlan prefixRunPlan(int[] prefixFields, long subj, long pred, long obj, long context) {
			if (prefixFields.length != 1) {
				return null;
			}
			return switch (prefixFields[0]) {
			case TripleIndex.PRED_IDX -> LmdbPrefixRunPlanTestAccess.adjacencyPredicate(prefixFields);
			case TripleIndex.SUBJ_IDX -> LmdbPrefixRunPlanTestAccess.adjacencySubject(prefixFields);
			default -> null;
			};
		}

		@Override
		public LmdbPrefixRunCursor prefixRuns(LmdbPrefixRunPlan plan, long subj, long pred, long obj, long context,
				boolean countRunRows) {
			return new ParallelPrefixCursor();
		}

		@Override
		public long[][] prefixRunSplitValues(LmdbPrefixRunPlan plan, long subj, long pred, long obj, long context,
				int targetPartitions, int tupleLength) {
			return new long[][] { { 2L } };
		}

		@Override
		public NativeLmdbQuerySource.ParallelSource[] openParallelSources(int count) {
			ParallelFixture[] opened = new ParallelFixture[count];
			for (int i = 0; i < count; i++) {
				opened[i] = new ParallelFixture(read, token);
			}
			workers = List.of(opened);
			return opened;
		}

		@Override
		public RecordIterator statements(long subj, long pred, long obj, long context) {
			return new RecordIterator() {
				private int index;

				@Override
				public long[] next() {
					if (index == ROWS) {
						return null;
					}
					if (read.incrementAndGet() == CANCEL_AFTER) {
						token.requestCancellation();
					}
					int row = index++;
					return row == 0 ? new long[] { 1L, SUBJECT_TYPE_PREDICATE, 10L, 0L }
							: new long[] { 1L, EDGE_PREDICATE, 11L, 0L };
				}

				@Override
				public boolean seekForward(long targetSubject, long ignoredPredicate, long ignoredObject,
						long ignoredContext) {
					index = targetSubject <= 1L ? index : ROWS;
					return true;
				}

				@Override
				public void close() {
				}
			};
		}

		@Override
		public double estimate(long subj, long pred, long obj, long context) {
			return ROWS;
		}

		@Override
		public void close() {
			closed = true;
		}
	}

	private static final class ParallelPrefixCursor implements LmdbPrefixRunCursor {
		private int index;

		@Override
		public boolean next() {
			return index++ < 2;
		}

		@Override
		public long prefixValue(int field) {
			return field == TripleIndex.PRED_IDX ? (index == 1 ? SUBJECT_TYPE_PREDICATE : EDGE_PREDICATE) : 0L;
		}

		@Override
		public boolean seekTo(long value) {
			return false;
		}

		@Override
		public boolean seekTo(long[] prefixValues) {
			return false;
		}

		@Override
		public void stopBefore(long[] prefixValues) {
		}

		@Override
		public long[] quad() {
			return new long[] { 1L, prefixValue(TripleIndex.PRED_IDX), 0L, 0L };
		}

		@Override
		public long runRowCount() {
			return 1L;
		}

		@Override
		public long getSourceRowsScannedActual() {
			return index;
		}

		@Override
		public long getSourceRowsMatchedActual() {
			return index;
		}

		@Override
		public long getSourceRowsFilteredActual() {
			return 0L;
		}

		@Override
		public String getIndexName() {
			return "type-matrix-cancellation-test";
		}

		@Override
		public void close() {
		}
	}

	private static final class PrefixCursor implements LmdbPrefixRunCursor {
		private final NativeCancellationToken token;
		private int index;
		private long[] current;
		private boolean closed;

		private PrefixCursor(NativeCancellationToken token) {
			this.token = token;
		}

		@Override
		public boolean next() {
			if (index >= ROWS) {
				return false;
			}
			if (index == CANCEL_AFTER) {
				token.requestCancellation();
			}
			current = new long[] { 1L, SUBJECT_TYPE_PREDICATE + index, 10L, 0L };
			index++;
			return true;
		}

		@Override
		public boolean seekTo(long value) {
			return false;
		}

		@Override
		public boolean seekTo(long[] prefixValues) {
			return false;
		}

		@Override
		public void stopBefore(long[] prefixValues) {
		}

		@Override
		public long[] quad() {
			return current;
		}

		@Override
		public long runRowCount() {
			return 1L;
		}

		@Override
		public long getSourceRowsScannedActual() {
			return index;
		}

		@Override
		public long getSourceRowsMatchedActual() {
			return index;
		}

		@Override
		public long getSourceRowsFilteredActual() {
			return 0L;
		}

		@Override
		public String getIndexName() {
			return "type-matrix-cancellation-test";
		}

		@Override
		public void close() {
			closed = true;
		}
	}
}
