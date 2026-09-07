/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.factor;

import java.util.Objects;

/**
 * Opens only a requested subset of independent factors. Unopened siblings remain relations.
 * Bounded readers are reused between prefixes; no allocation depends on degree. Each cursor owns
 * its own iterator state, so another consumer can traverse the same environment independently.
 */
public final class FactorProductCursor implements AutoCloseable {
	private final BorrowedFactorBatch[] boundBatches;
	private final BorrowedFactorBatch.Cursor[] readers;
	private final int[] order;
	private final long[] values;
	private final long[] prefixWeights;
	private final int window;
	private FactorEnvironment environment;
	private long epoch;
	private long selected;
	private int width;
	private boolean first;
	private boolean exhausted;
	private boolean closed;

	public FactorProductCursor(int slots, int window) {
		if (slots < 0 || slots > Long.SIZE || window <= 0) throw new IllegalArgumentException("invalid cursor size");
		boundBatches = new BorrowedFactorBatch[slots];
		readers = new BorrowedFactorBatch.Cursor[slots];
		order = new int[slots];
		values = new long[slots];
		prefixWeights = new long[slots + 1];
		this.window = window;
	}

	public void bind(FactorEnvironment environment, long demandedSlots, long multiplicity) {
		checkOpen();
		Objects.requireNonNull(environment, "environment").checkValid();
		if (environment.slots() > readers.length || multiplicity < 0)
			throw new IllegalArgumentException("incompatible environment or multiplicity");
		this.environment = environment;
		epoch = environment.epoch();
		selected = environment.mask() & demandedSlots;
		width = 0;
		exhausted = multiplicity == 0;
		for (long rest = environment.mask(); rest != 0L; rest &= rest - 1L) {
			int slot = Long.numberOfTrailingZeros(rest);
			if (environment.count(slot) == 0L) exhausted = true;
			if ((selected & (1L << slot)) != 0L) order[width++] = slot;
		}
		prefixWeights[0] = multiplicity;
		first = true;
	}

	public long expandedMask() { return selected; }
	public long value(int slot) {
		Objects.checkIndex(slot, values.length);
		if ((selected & (1L << slot)) == 0L) throw new IllegalArgumentException("slot not expanded");
		return values[slot];
	}
	/** Multiplicity of the emitted scalar prefix; excludes all unopened factors. */
	public long multiplicity() { return prefixWeights[width]; }

	public boolean next() {
		checkOpen();
		if (environment == null) throw new IllegalStateException("cursor not bound");
		if (environment.epoch() != epoch) throw new IllegalStateException("factor environment changed during expansion");
		if (exhausted) return false;
		if (first) {
			environment.checkValid(); // includes zero-width consumers that never open a reader
			first = false;
			for (int i = 0; i < width; i++) {
				if (!restart(i)) { exhausted = true; return false; }
			}
			return true;
		}
		for (int i = width - 1; i >= 0; i--) {
			int slot = order[i];
			if (!readers[slot].next()) continue;
			capture(i);
			for (int j = i + 1; j < width; j++)
				if (!restart(j)) throw new IllegalStateException("exact immutable factor changed during replay");
			return true;
		}
		exhausted = true;
		return false;
	}

	private boolean restart(int index) {
		int slot = order[index];
		BorrowedFactorBatch batch = environment.batch(slot);
		if (boundBatches[slot] != batch) {
			if (readers[slot] != null) readers[slot].close();
			readers[slot] = null;
			boundBatches[slot] = null;
			readers[slot] = batch.cursor(window);
			boundBatches[slot] = batch;
		}
		readers[slot].bind(environment.lane(slot));
		if (!readers[slot].next()) return false;
		capture(index);
		return true;
	}

	private void capture(int index) {
		int slot = order[index];
		values[slot] = readers[slot].value();
		prefixWeights[index + 1] = Math.multiplyExact(prefixWeights[index], readers[slot].weight());
	}
	private void checkOpen() { if (closed) throw new IllegalStateException("factor product cursor closed"); }

	@Override public void close() {
		if (closed) return;
		closed = true;
		Throwable failure = null;
		for (BorrowedFactorBatch.Cursor reader : readers) if (reader != null) {
			try { reader.close(); }
			catch (RuntimeException | Error problem) {
				if (failure == null) failure = problem; else failure.addSuppressed(problem);
			}
		}
		environment = null;
		java.util.Arrays.fill(boundBatches, null);
		java.util.Arrays.fill(readers, null);
		if (failure instanceof RuntimeException problem) throw problem;
		if (failure instanceof Error problem) throw problem;
	}
}
