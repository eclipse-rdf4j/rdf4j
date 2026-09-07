/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import static org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeAggregateCompiler.UNKNOWN;

import java.io.IOException;
import org.eclipse.rdf4j.sail.lmdb.factor.BorrowedTupleBatch;
import org.eclipse.rdf4j.sail.lmdb.factor.FactorEnvironment;

/** Group-valued output of the existing admitted build/probe cursor, not a separate join algorithm. */
final class HashJoinFactorCursor implements LmdbNativeFactorCursor {
	private final HashJoinBatchCursor core;
	private final RowState row;
	private final FactorEnvironment result;
	private final long dependencies;
	private final boolean inlinePayload;
	private final int mark;
	private PrimitiveHashJoinFactorSource source;
	private BorrowedTupleBatch groups;
	private LmdbNativeFactorCursor fallback;
	private long weight, probeWeight;
	private int pendingPayload = -1;
	private int tick, probeMark;
	private boolean initialized, closed;

	HashJoinFactorCursor(HashJoinBatchCursor core) { this(core, 0L); }

	HashJoinFactorCursor(HashJoinBatchCursor core, long scalarDemand) {
		this.core = core;
		inlinePayload = (scalarDemand & HashJoinBatchCursor.maskOf(core.payloadSlots)) != 0L;
		row = core.row;
		mark = row.mark();
		dependencies = HashJoinBatchCursor.maskOf(core.keySlots) | row.boundMask();
		result = new FactorEnvironment(row.slots.length);
	}

	@Override public boolean next() throws IOException {
		if (closed) return false;
		try {
			result.clear();
			if (groups != null) groups.reset(1);
			if (LmdbNativeFactorRows.cancelled(row, ++tick)) { close(); return false; }
			// A fallback owns its active scalar trail; clearing it here would destroy a nested-loop prefix.
			if (fallback == null) row.rollback(pendingPayload >= 0 ? probeMark : mark);
			if (!initialized) {
				initialized = core.initialized = true;
				if (!core.build()) {
					if (core.buildCancelled) { close(); return false; }
					// The core's existing unfiltered fallback cannot recursively reopen this producer.
					fallback = LmdbNativeFactorRows.scalar(core.fallbackPlan.open(row), row.slots.length);
				} else if (core.payloadSlots.length != 0 && !core.table.uniqueKeys && !inlinePayload) {
					source = new PrimitiveHashJoinFactorSource(core.table);
					groups = new BorrowedTupleBatch(source, 1);
					groups.reset(1);
				}
			}
			if (fallback != null) {
				if (!fallback.next()) { close(); return false; }
				weight = fallback.multiplicity();
				return true;
			}
			if (core.table.payloadCount == 0) { close(); return false; }
			while (true) {
				if (LmdbNativeFactorRows.cancelled(row, ++tick)) { close(); return false; }
				row.rollback(pendingPayload >= 0 ? probeMark : mark);
				if (pendingPayload >= 0) {
					int payload = pendingPayload;
					pendingPayload = core.table.next[payload];
					if (!installPayload(payload)) continue;
					weight = probeWeight;
					return true;
				}
				int bucket = core.nextProbeBucket();
				if (bucket == HashJoinBatchCursor.END_OF_PROBE) { close(); return false; }
				if (bucket < 0) continue;
				row.rollback(mark);
				if (!installProbe()) continue;
				probeWeight = core.probeCursor instanceof WeightedBatchCursor weighted
						? weighted.weight(core.currentProbeRow) : 1L;
				if (probeWeight <= 0L) throw new IllegalStateException("invalid probe bag weight");
				weight = probeWeight;
				if (inlinePayload) {
					probeMark = row.mark();
					int payload = core.table.heads[bucket];
					pendingPayload = core.table.next[payload];
					if (!installPayload(payload)) continue;
					return true;
				}
				int count = core.table.chainCounts[bucket];
				if (core.payloadSlots.length == 0) {
					weight = Math.multiplyExact(weight, count);
				} else if (count == 1) {
					// Preserve the existing unique-key/singleton fast path: no tuple window or descriptor.
					if (!installPayload(core.table.heads[bucket])) continue;
				} else {
					source.export(groups, 0, bucket);
					result.bindTuple(core.payloadSlots, groups, 0, dependencies);
				}
				return true;
			}
		} catch (IOException | RuntimeException | Error failure) {
			try { close(); } catch (Throwable closing) { if (closing != failure) failure.addSuppressed(closing); }
			throw failure;
		}
	}

	private boolean installProbe() {
		boolean compatible = true;
		for (int slot = 0; slot < row.slots.length; slot++) {
			long id = core.probeBatch.get(slot, core.currentProbeRow);
			if (id != UNKNOWN) compatible &= row.bindOrCheckTerm(slot, id);
		}
		return compatible;
	}

	private boolean installPayload(int payload) {
		boolean compatible = true;
		for (int column = 0; column < core.payloadSlots.length; column++)
			compatible &= row.bindOrCheckTerm(core.payloadSlots[column], core.table.payload(payload, column));
		return compatible;
	}

	@Override public boolean mayHaveFactors() { return !inlinePayload && core.payloadSlots.length != 0; }
	@Override public long multiplicity() { return weight; }
	@Override public FactorEnvironment factors() { return result; }
	@Override public void close() {
		if (closed) return;
		closed = true; pendingPayload = -1;
		result.clear();
		try { if (fallback != null) fallback.close(); }
		finally {
			try { if (source != null) source.close(); }
			finally { try { core.close(); } finally { row.rollback(mark); } }
		}
	}
}
