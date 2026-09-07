/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.factor;

import java.util.Objects;

/**
 * A row-local sidecar of independent unary relations, never dictionary IDs masquerading as pointers.
 * Every dependency is in the accompanying scalar prefix. Tuple-valued or mutually dependent factors
 * must not be split into entries here. The producer owns the backing sources and may advance only
 * after downstream readers have finished with the current prefix.
 *
 * <p>Storage is bounded by the slot schema, not degree or product size. Copying copies descriptors,
 * not payload, ownership, or mutable readers. This class and its cursors are thread-confined.
 */
public final class FactorEnvironment {
	private final BorrowedFactorBatch[] batches;
	private final int[] lanes;
	private final long[] generations;
	private final long[] dependencies;
	private long mask;
	private long epoch;

	public FactorEnvironment(int slots) {
		if (slots < 0 || slots > Long.SIZE) throw new IllegalArgumentException("slot count must be in [0,64]");
		batches = new BorrowedFactorBatch[slots];
		lanes = new int[slots];
		generations = new long[slots];
		dependencies = new long[slots];
	}

	public int slots() { return batches.length; }
	public long mask() { return mask; }
	public long epoch() { return epoch; }

	/** Invalidates this row view without resetting any shared source batch. */
	public void clear() {
		for (long remaining = mask; remaining != 0L; remaining &= remaining - 1L)
			batches[Long.numberOfTrailingZeros(remaining)] = null;
		mask = 0L;
		epoch++;
	}

	public void bind(int slot, BorrowedFactorBatch batch, int lane, long dependencyMask) {
		Objects.checkIndex(slot, batches.length);
		Objects.requireNonNull(batch, "batch").count(lane); // validate the exact descriptor and owner once
		long bit = 1L << slot;
		long schemaMask = batches.length == Long.SIZE ? -1L : (1L << batches.length) - 1L;
		if ((dependencyMask & ~schemaMask) != 0L)
			throw new IllegalArgumentException("factor dependency outside the slot schema");
		if ((mask & bit) != 0L) throw new IllegalArgumentException("factor slot already bound");
		if ((dependencyMask & (mask | bit)) != 0L)
			throw new IllegalArgumentException("a factor dependency must be scalar");
		for (long rest = mask; rest != 0L; rest &= rest - 1L)
			if ((dependencies[Long.numberOfTrailingZeros(rest)] & bit) != 0L)
				throw new IllegalArgumentException("cannot defer another factor's scalar dependency");
		batches[slot] = batch;
		lanes[slot] = lane;
		generations[slot] = batch.generation();
		dependencies[slot] = dependencyMask;
		mask |= bit;
		epoch++;
	}

	/** Append a disjoint subset. Each referenced batch still belongs to its original producer. */
	public void append(FactorEnvironment other, long selectedMask) {
		if (other == this) throw new IllegalArgumentException("self append");
		long selected = other.mask & selectedMask;
		if ((mask & selected) != 0L) throw new IllegalArgumentException("overlapping factor variables require a join");
		for (long rest = selected; rest != 0L; rest &= rest - 1L) {
			int slot = Long.numberOfTrailingZeros(rest);
			other.checkSlot(slot);
			bind(slot, other.batches[slot], other.lanes[slot], other.dependencies[slot]);
		}
	}

	public BorrowedFactorBatch batch(int slot) { checkSlot(slot); return batches[slot]; }
	public int lane(int slot) { checkSlot(slot); return lanes[slot]; }
	public long dependencies(int slot) { checkSlot(slot); return dependencies[slot]; }
	public long count(int slot) { checkSlot(slot); return batches[slot].count(lanes[slot]); }

	/** Exact only: overflow throws. Used when the consumer explicitly requests bag cardinality. */
	public long countProduct(long selectedMask, long prefixMultiplicity) {
		if (prefixMultiplicity < 0L) throw new IllegalArgumentException("negative multiplicity");
		long selected = mask & selectedMask;
		// A zero factor annihilates the product even when an earlier partial product would overflow.
		for (long rest = selected; rest != 0L; rest &= rest - 1L)
			if (count(Long.numberOfTrailingZeros(rest)) == 0L) return 0L;
		long result = prefixMultiplicity;
		for (long rest = selected; rest != 0L; rest &= rest - 1L)
			result = Math.multiplyExact(result, count(Long.numberOfTrailingZeros(rest)));
		return result;
	}

	public void checkValid() {
		for (long rest = mask; rest != 0L; rest &= rest - 1L) checkSlot(Long.numberOfTrailingZeros(rest));
	}

	private void checkSlot(int slot) {
		Objects.checkIndex(slot, batches.length);
		if ((mask & (1L << slot)) == 0L) throw new IllegalStateException("slot is not a factor");
		BorrowedFactorBatch batch = batches[slot];
		batch.source().checkOpen();
		if (batch.generation() != generations[slot])
			throw new IllegalStateException("factor producer advanced while the row view was live");
	}
}
