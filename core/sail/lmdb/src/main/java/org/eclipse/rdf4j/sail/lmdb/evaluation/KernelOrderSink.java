/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.KernelCancellation;
import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.KernelHooks;
import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.KernelRuntime;

/**
 * Incremental, stable ordering of authority-local packed ID rows. Both IR tiers use the
 * existing native top-K and external sorter through this sink; no complete IR output array
 * is retained. Source Values and borrowed snapshots remain owned by the evaluation.
 *
 * The budget is a local native-sort workspace budget, NOT a query-wide heap limit. It does
 * not account for the authority dictionary, DISTINCT/group state, comparator caches or I/O
 * buffering. External merging bounds simultaneously open readers and temporary-path state.
 */
public final class KernelOrderSink implements AutoCloseable {
	private final int stride;
	private final KernelCancellation cancellation;
	private final long offset, limit;
	private final PackedRowComparator comparator;
	private final long maxBytes;
	private NativeTopKBuffer top;
	private NativeSpillSort spill;
	private NativeSortedRows result;
	private long[] scratch;
	private long seen, skipped, emitted;
	private long peakPayloadBytes;
	private int comparisonTick;
	private boolean finished, closed;

	public KernelOrderSink(int stride, int[] keys, boolean[] descending, KernelHooks hooks,
			KernelCancellation cancellation, long offset, long limit) {
		if (stride < 0 || offset < 0 || limit < -1) throw new IllegalArgumentException("Invalid ordered slice");
		int[] columns = Objects.requireNonNull(keys, "keys").clone();
		boolean[] reverse = descending == null ? null : descending.clone();
		if (reverse != null && reverse.length != columns.length) throw new IllegalArgumentException("Order arity mismatch");
		for (int key : columns) if (key < 0 || key >= stride) throw new IllegalArgumentException("Order key outside row");
		this.stride = stride; this.offset = offset; this.limit = limit; this.cancellation = cancellation;
		this.maxBytes = NativeSpillSort.configuredMaxBytes();
		this.comparator = (left, lo, right, ro) -> {
			if (cancellation != null && (++comparisonTick & 1023) == 0) KernelRuntime.checkCancelled(cancellation);
			for (int k = 0; k < columns.length; k++) {
				long a = left[lo + columns[k]], b = right[ro + columns[k]];
				int cmp = hooks == null ? Long.compareUnsigned(a, b) : hooks.compareValues(a, b);
				// Reverse operands' ordering sign, not -cmp (a legal comparator may return MIN_VALUE).
				if (cmp != 0) return reverse != null && reverse[k] ? (cmp < 0 ? 1 : -1) : (cmp < 0 ? -1 : 1);
			}
			return 0;
		};
	}

	/** Copies only an admitted row. Rejected top-K candidates never enter the payload arena. */
	public void add(long[] row) { addWithOrdinal(row, seen); }

	/** A reordered grouped producer retains its original, unique nonnegative input ordinal for stable ties. */
	public void addWithOrdinal(long[] row, long ordinal) {
		if (ordinal < 0L) throw new IllegalArgumentException("Negative stable order ordinal");
		if (closed || finished) throw new IllegalStateException("Ordering sink is not accepting rows");
		Objects.requireNonNull(row, "row");
		if (row.length < stride) throw new IllegalArgumentException("Short order row");
		if (limit == 0) return;
		try {
			if ((seen & 1023L) == 0L) KernelRuntime.checkCancelled(cancellation);
			// Retain the probe's complete-consumption ceiling, including rows written to disk.
			KernelRuntime.checkMaterializationCapacity(cancellation, (int) Math.min(seen, Integer.MAX_VALUE));
			if (top == null && spill == null) initialize();
			seen = Math.addExact(seen, 1L);
			if (top != null) {
				if (top.admit(row, ordinal) == NativeTopKBuffer.REJECTED) return;
			} else spill.add(row, ordinal);
			NativeSortBuffer buffer = top != null ? top.buffer : spill.buffer;
			long bytes = 8L * (buffer.rows.length + (long) buffer.ordinals.length)
					+ (top == null ? 0L : 4L * top.heap.length);
			peakPayloadBytes = Math.max(peakPayloadBytes, bytes);
		} catch (IOException failure) { fail(new UncheckedIOException(failure)); }
		catch (RuntimeException | Error failure) { fail(failure); }
	}

	private void initialize() {
		long cap = limit < 0 ? Long.MAX_VALUE : NativeSliceMath.limitPlusOffset(limit, offset);
		// Includes arena growth, heap and final sort scratch; cap arithmetic must never wrap.
		long bytesPerRow = 2L * (stride + 1L) * Long.BYTES + 3L * Integer.BYTES;
		long maxTop = Math.min((Integer.MAX_VALUE - 8L) / Math.max(1, stride), maxBytes / bytesPerRow);
		if (limit >= 0 && cap <= maxTop) {
			top = new NativeTopKBuffer(stride, (int) cap, comparator);
		} else {
			int retain = limit >= 0 && cap <= Integer.MAX_VALUE ? (int) cap : -1;
			spill = new NativeSpillSort(stride, stride, comparator, retain,
					LmdbNativeAttemptMetrics.direct(), maxBytes,
					cancellation == null ? null : () -> KernelRuntime.checkCancelled(cancellation));
		}
	}

	public void finish() {
		if (closed || finished) return;
		try {
			KernelRuntime.checkCancelled(cancellation);
			finished = true;
			if (top != null) result = top.buffer.sortedRows(comparator);
			else if (spill != null) result = spill.sortedRows();
			scratch = new long[stride];
		} catch (IOException failure) { fail(new UncheckedIOException(failure)); }
		catch (RuntimeException | Error failure) { fail(failure); }
	}

	/** Applies OFFSET/LIMIT while draining, with long positions and no full-result copy. */
	public int fill(long[] target, int maxRows) {
		if (maxRows <= 0 || closed) return 0;
		if (target == null || (long) maxRows * stride > target.length) throw new IllegalArgumentException("Short row buffer");
		if (!finished) throw new IllegalStateException("Ordering input has not finished");
		try {
			KernelRuntime.checkCancelled(cancellation);
			if (result == null || limit == 0 || limit >= 0 && emitted >= limit) { close(); return 0; }
			while (skipped < offset) {
				if ((skipped & 1023L) == 0L) KernelRuntime.checkCancelled(cancellation);
				if (!result.next(scratch)) { close(); return 0; }
				skipped++;
			}
			int n = 0;
			while (n < maxRows && (limit < 0 || emitted < limit)) {
				if ((emitted & 1023L) == 0L) KernelRuntime.checkCancelled(cancellation);
				if (!result.next(scratch)) { close(); break; }
				System.arraycopy(scratch, 0, target, n * stride, stride);
				n++; emitted++;
			}
			if (limit >= 0 && emitted >= limit) close();
			return n;
		} catch (IOException failure) { fail(new UncheckedIOException(failure)); return 0; }
		catch (RuntimeException | Error failure) { fail(failure); return 0; }
	}

	public long inputRows() { return seen; }
	/** High-water payload/ordinal/heap arrays only; excludes sort scratch and I/O buffers. */
	public long peakPayloadBytes() { return peakPayloadBytes; }
	public boolean usedTopK() { return top != null; }
	public long spilledRows() { return spill == null ? 0 : spill.spilledRows; }
	public int peakRunCount() { return spill == null ? 0 : spill.peakRunCount; }

	private void fail(Throwable failure) {
		KernelRuntime.closeResource(this, failure);
		KernelRuntime.rethrowCloseFailure(failure);
	}

	@Override public void close() {
		if (closed) return;
		closed = true;
		Throwable failure = KernelRuntime.closeResource(result, null); result = null;
		if (top != null) {
			try { top.close(); } catch (RuntimeException | Error problem) {
				if (failure == null) failure = problem; else failure.addSuppressed(problem);
			}
		}
		failure = KernelRuntime.closeResource(spill, failure);
		scratch = null;
		KernelRuntime.rethrowCloseFailure(failure);
	}
}
