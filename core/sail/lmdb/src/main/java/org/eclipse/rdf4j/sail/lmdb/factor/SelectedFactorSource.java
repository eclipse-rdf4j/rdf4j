/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.factor;

import java.util.Objects;

/**
 * Query-local, exact selected view of a borrowed relation. Values remain in the original source;
 * selection runs use logical quad positions and therefore preserve weights, including split
 * fibers. Repeated selections are composed into ultimate-source coordinates, not stacked readers.
 *
 * <p>One producer owns this reusable one-lane descriptor. Its selection may be a bounded part of
 * a much larger relation. Publishing successive disjoint parts preserves the bag, but does not
 * assert that the parent keys of those parts are distinct. All consumers must finish with a part
 * before select/clear or the upstream producer advances. This source borrows upstream ownership;
 * closing it never closes the upstream source. Readers and the upstream lease must outlive reads.
 */
public final class SelectedFactorSource extends BorrowedFactorBatch.Source {
	private final BorrowedFactorBatch batch = new BorrowedFactorBatch(this, 1);
	private final FactorSelection ranges;
	private final byte readAheadMode;
	private BorrowedFactorBatch original;
	private long originalGeneration;
	private int originalLane;
	private BorrowedFactorBatch base;
	private int baseLane;
	private long baseGeneration;

	/** Use bounded dense read-ahead by default only for encoded physical sources. */
	public SelectedFactorSource(int initialRanges) { this(initialRanges, (byte) 2); }

	/** Dense selections may read ahead within a bounded window; false retains interval-only fills. */
	public SelectedFactorSource(int initialRanges, boolean denseReadAhead) {
		this(initialRanges, (byte) (denseReadAhead ? 1 : 0));
	}

	private SelectedFactorSource(int initialRanges, byte readAheadMode) {
		super(new Object(), true); // selection references are local to this producer, never cross-source handles
		ranges = new FactorSelection(initialRanges);
		this.readAheadMode = readAheadMode;
		batch.reset(1);
	}

	public BorrowedFactorBatch batch() { checkOpen(); return batch; }
	public int ranges() { checkOpen(); return ranges.ranges(); }
	public long retainedSelectionBytes() { return ranges.retainedBytes(); }

	/** Invalidates published descriptors, but can be called after the upstream producer advances. */
	public void clear() {
		checkNotClosed();
		original = base = null;
		ranges.clear();
		batch.reset(1);
	}

	/** Copies only interval metadata. The caller's builder is reusable immediately on return. */
	public BorrowedFactorBatch select(BorrowedFactorBatch input, int lane, FactorSelection selection) {
		checkNotClosed();
		Objects.requireNonNull(input, "input");
		Objects.requireNonNull(selection, "selection");
		for (BorrowedFactorBatch.Source dependency = input.source(); dependency instanceof SelectedFactorSource selected;
				dependency = selected.original == null ? null : selected.original.source()) {
			if (dependency == this) throw new IllegalArgumentException("cyclic selection dependency");
		}
		long sourceCount = input.count(lane);
		selection.validateBounds(sourceCount);
		clear();
		if (input.source() instanceof SelectedFactorSource selected) {
			// Empty selections can have no base relation. Their count is still exactly zero.
			ranges.compose(selected.ranges, selection);
			base = selected.base;
			baseLane = selected.baseLane;
			baseGeneration = selected.baseGeneration;
		} else {
			ranges.copyFrom(selection);
			base = input;
			baseLane = lane;
			baseGeneration = input.generation();
		}
		original = input;
		originalLane = lane;
		originalGeneration = input.generation();
		batch.bindSelected(0, ranges.count());
		return batch;
	}

	@Override protected void validate() {
		if (original != null) {
			if (original.generation() != originalGeneration)
				throw new IllegalStateException("selection producer advanced while a selected view was live");
			original.count(originalLane); // includes copied descriptors' selected-source version stamps
		}
		if (base != null) {
			if (base.generation() != baseGeneration)
				throw new IllegalStateException("borrowed base advanced while a selected view was live");
		}
	}

	@Override protected void validateDescriptor(byte kind, long reference, int coordinate) {
		if ((kind == BorrowedFactorBatch.SELECTED || (kind == BorrowedFactorBatch.EMPTY && reference != 0L))
				&& reference != batch.generation())
			throw new IllegalStateException("copied selected descriptor was invalidated by its producer");
	}

	@Override public BorrowedFactorBatch.Reader openReader() {
		checkOpen();
		return new SelectionReader();
	}

	private final class SelectionReader implements BorrowedFactorBatch.Reader {
		private BorrowedFactorBatch.Source boundSource;
		private BorrowedFactorBatch.Reader reader;
		private long generation;
		private long count;
		private int range;
		private long[] decodedValues = new long[0];
		private long[] decodedWeights = new long[0];
		private long bufferStart, bufferEnd, bufferPosition;
		private int bufferAt;
		private long baseCount;
		private boolean readAhead;
		private boolean bound;
		private boolean closed;

		@Override public void bind(BorrowedFactorBatch input, int lane) {
			checkReaderOpen();
			if (input.source() != SelectedFactorSource.this || input.kind(lane) != BorrowedFactorBatch.SELECTED)
				throw new IllegalArgumentException("incompatible selected factor");
			bound = false;
			if (base == null) throw new IllegalStateException("nonempty selection without a source");
			if (boundSource != base.source()) {
				if (reader != null) reader.close();
				reader = null;
				boundSource = null;
				reader = base.source().openReader();
				boundSource = base.source();
			}
			reader.bind(base, baseLane);
			generation = batch.generation();
			count = ranges.count();
			baseCount = base.count(baseLane);
			byte kind = base.kind(baseLane);
			long span = ranges.end(ranges.ranges() - 1) - ranges.start(0);
			readAhead = (readAheadMode == 1 || (readAheadMode == 2
					&& (kind == BorrowedFactorBatch.CSF_PAGE || kind == BorrowedFactorBatch.ENCODED_RUN)))
					&& count >= (span >>> 1) + (span & 1L);
			bufferStart = bufferEnd = bufferPosition = 0L;
			bufferAt = 0;
			range = 0;
			bound = true;
		}

		@Override public int copyFibers(long offset, int maximum, long[] values, long[] weights) {
			checkReaderOpen();
			if (!bound || generation != batch.generation())
				throw new IllegalStateException("selected reader is unbound or invalidated");
			if (offset < 0L || offset > count || maximum < 0 || maximum > values.length || maximum > weights.length)
				throw new IllegalArgumentException("invalid selected read");
			if (offset == count || maximum == 0) return 0;
			if (range == ranges.ranges() || offset < ranges.prefixStart(range) || offset >= ranges.prefixEnd(range))
				range = ranges.rangeAt(offset);
			int written = 0;
			long at = offset;
			while (written < maximum && at < count) {
				long physical = ranges.start(range) + (at - ranges.prefixStart(range));
				long remaining = ranges.prefixEnd(range) - at;
				if (physical < bufferStart || physical >= bufferEnd) {
					long readSpan = remaining;
					if (readAhead) {
						long span = ranges.end(ranges.ranges() - 1) - physical;
						// Half-dense suffixes amortize decoding across gaps. The source and overall
						// density gate are resolved at bind, so sparse/native defaults skip this work.
						if (count - at >= (span >>> 1) + (span & 1L)) readSpan = span;
					}
					int request = (int) Math.min(maximum - written, readSpan);
					if (decodedValues.length < request) {
						decodedValues = new long[maximum]; decodedWeights = new long[maximum];
					}
					int n = reader.copyFibers(physical, request, decodedValues, decodedWeights);
					if (n <= 0 || n > request) throw new IllegalStateException("selected source made no progress");
					bufferStart = bufferPosition = physical;
					bufferAt = 0;
					long end = physical;
					for (int i = 0; i < n; i++) {
						long weight = decodedWeights[i];
						if (weight <= 0L || weight > baseCount - end)
							throw new IllegalStateException("invalid selected source multiplicity");
						end += weight;
					}
					bufferEnd = end;
				} else if (physical < bufferPosition) {
					bufferPosition = bufferStart; bufferAt = 0;
				}
				while (bufferPosition + decodedWeights[bufferAt] <= physical)
					bufferPosition += decodedWeights[bufferAt++];
				// A selected interval may end inside a decoded fiber. Keep the untouched decode
				// window for the next interval instead of re-decoding the same prefix/fiber.
				long take = Math.min(remaining, bufferPosition + decodedWeights[bufferAt] - physical);
				values[written] = decodedValues[bufferAt];
				weights[written++] = take;
				at += take;
				if (take == remaining) range++;
			}
			return written;
		}

		private void checkReaderOpen() {
			if (closed) throw new IllegalStateException("selected reader closed");
			SelectedFactorSource.this.checkOpen();
		}

		@Override public void close() {
			if (closed) return;
			closed = true;
			try { if (reader != null) reader.close(); }
			finally { reader = null; boundSource = null; }
		}
	}

	@Override protected void release() { original = base = null; ranges.clear(); }
}
