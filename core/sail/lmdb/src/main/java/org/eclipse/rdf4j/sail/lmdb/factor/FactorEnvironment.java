/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.factor;

import java.util.Objects;

/**
 * A row-local sidecar of independent relations, never dictionary IDs masquerading as pointers.
 * Every dependency is in the accompanying scalar prefix. Columns in a tuple group are explicitly
 * zipped; mutually dependent groups must not be split into independent entries here. The producer
 * owns backing sources and may advance only after consumers finish the current prefix.
 *
 * <p>Storage is bounded by the slot schema, not degree or product size. Copying copies descriptors,
 * not payload, ownership, or mutable readers. This class and its cursors are thread-confined.
 */
public final class FactorEnvironment {
	private final BorrowedFactorBatch[] batches;
	private final int[] lanes;
	private final long[] generations;
	private final long[] dependencies;
	// Tuple-only state is lazy: adjacency-only environments allocate no tuple-specific backing arrays.
	private BorrowedTupleBatch[] tuples;
	private int[] columns;
	private long[] groups;
	private long tupleMask;
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
		for (long remaining = mask; remaining != 0L; remaining &= remaining - 1L) {
			int slot = Long.numberOfTrailingZeros(remaining);
			batches[slot] = null;
			if (tuples != null) tuples[slot] = null;
		}
		mask = tupleMask = 0L;
		epoch++;
	}

	public void bind(int slot, BorrowedFactorBatch batch, int lane, long dependencyMask) {
		Objects.checkIndex(slot, batches.length);
		Objects.requireNonNull(batch, "batch").count(lane);
		long bit = 1L << slot;
		validateNewGroup(bit, dependencyMask);
		batches[slot] = batch;
		lanes[slot] = lane;
		generations[slot] = batch.generation();
		dependencies[slot] = dependencyMask;
		mask |= bit;
		epoch++;
	}

	/** Installs a zipped relation; slots[column] maps every source column to a distinct engine slot. */
	public void bindTuple(int[] slots, BorrowedTupleBatch batch, int lane, long dependencyMask) {
		Objects.requireNonNull(slots, "slots");
		Objects.requireNonNull(batch, "batch").count(lane);
		if (slots.length != batch.columns()) throw new IllegalArgumentException("tuple arity mismatch");
		long group = 0L;
		for (int slot : slots) {
			Objects.checkIndex(slot, batches.length);
			long bit = 1L << slot;
			if ((group & bit) != 0L) throw new IllegalArgumentException("repeated tuple slot");
			group |= bit;
		}
		validateNewGroup(group, dependencyMask);
		ensureTupleState();
		for (int column = 0; column < slots.length; column++) {
			int slot = slots[column];
			tuples[slot] = batch;
			columns[slot] = column;
			lanes[slot] = lane;
			generations[slot] = batch.generation();
			dependencies[slot] = dependencyMask;
			groups[slot] = group;
		}
		mask |= group;
		tupleMask |= group;
		epoch++;
	}

	private void ensureTupleState() {
		if (tuples == null) {
			tuples = new BorrowedTupleBatch[batches.length];
			columns = new int[batches.length];
			groups = new long[batches.length];
		}
	}

	private void validateNewGroup(long group, long dependencyMask) {
		long schema = batches.length == Long.SIZE ? -1L : (1L << batches.length) - 1L;
		if ((group & ~schema) != 0L) throw new IllegalArgumentException("factor slot outside schema");
		if ((group & mask) != 0L) throw new IllegalArgumentException("overlapping factor variables require a join");
		if ((dependencyMask & ~schema) != 0L || (dependencyMask & (mask | group)) != 0L)
			throw new IllegalArgumentException("factor dependency must be a scalar in the slot schema");
		for (long rest = mask; rest != 0L; rest &= rest - 1L)
			if ((dependencies[Long.numberOfTrailingZeros(rest)] & group) != 0L)
				throw new IllegalArgumentException("cannot defer another factor's scalar dependency");
	}

	/** Smallest set containing requested deferred slots and every correlated tuple column. */
	public long expansionClosure(long requested) {
		long selected = mask & requested;
		for (long rest = tupleMask & selected; rest != 0L;) {
			long group = groups[Long.numberOfTrailingZeros(rest)];
			selected |= group;
			rest &= ~group;
		}
		return selected;
	}

	/** One representative slot per independent factor; tuple columns contribute just one. */
	public long groupLeaders() {
		long leaders = mask & ~tupleMask;
		for (long rest = tupleMask; rest != 0L;) {
			long bit = Long.lowestOneBit(rest);
			leaders |= bit;
			rest &= ~groups[Long.numberOfTrailingZeros(bit)];
		}
		return leaders;
	}

	/** Append whole, disjoint groups. A partial tuple would change the represented relation. */
	public void append(FactorEnvironment other, long selectedMask) {
		if (other == this) throw new IllegalArgumentException("self append");
		long selected = other.mask & selectedMask;
		if (other.expansionClosure(selected) != selected)
			throw new IllegalArgumentException("cannot split a tuple factor");
		for (long rest = other.groupLeaders() & selected; rest != 0L; rest &= rest - 1L) {
			int slot = Long.numberOfTrailingZeros(rest);
			other.count(slot); // includes selected-descriptor provenance, not only owner liveness
			validateNewGroup(other.groupMaskUnchecked(slot), other.dependencies[slot]);
		}
		if ((other.tupleMask & selected) != 0L) ensureTupleState();
		for (long rest = selected; rest != 0L; rest &= rest - 1L) {
			int slot = Long.numberOfTrailingZeros(rest);
			batches[slot] = other.batches[slot];
			lanes[slot] = other.lanes[slot];
			generations[slot] = other.generations[slot];
			dependencies[slot] = other.dependencies[slot];
			if ((other.tupleMask & (1L << slot)) != 0L) {
				tuples[slot] = other.tuples[slot];
				columns[slot] = other.columns[slot];
				groups[slot] = other.groups[slot];
			}
		}
		mask |= selected;
		tupleMask |= other.tupleMask & selected;
		if (selected != 0L) epoch++;
	}

	public boolean isTuple(int slot) { checkSlot(slot); return (tupleMask & (1L << slot)) != 0L; }
	public long groupMask(int slot) { checkSlot(slot); return groupMaskUnchecked(slot); }
	private long groupMaskUnchecked(int slot) {
		long bit = 1L << slot;
		return (tupleMask & bit) == 0L ? bit : groups[slot];
	}
	public BorrowedTupleBatch tupleBatch(int slot) {
		if (!isTuple(slot)) throw new IllegalStateException("not a tuple factor");
		return tuples[slot];
	}
	public int tupleColumn(int slot) {
		if (!isTuple(slot)) throw new IllegalStateException("not a tuple factor");
		return columns[slot];
	}
	public BorrowedFactorBatch batch(int slot) {
		checkSlot(slot);
		if ((tupleMask & (1L << slot)) != 0L) throw new IllegalStateException("tuple group is not a unary span");
		return batches[slot];
	}
	public int lane(int slot) { checkSlot(slot); return lanes[slot]; }
	public long dependencies(int slot) { checkSlot(slot); return dependencies[slot]; }
	public long count(int slot) {
		checkSlot(slot);
		return (tupleMask & (1L << slot)) == 0L ? batches[slot].count(lanes[slot]) : tuples[slot].count(lanes[slot]);
	}

	/** Exact cardinality. Each group contributes once, even when several of its columns are selected. */
	public long countProduct(long selectedMask, long prefixMultiplicity) {
		if (prefixMultiplicity < 0L) throw new IllegalArgumentException("negative multiplicity");
		if (tupleMask == 0L) return unaryCountProduct(mask & selectedMask, prefixMultiplicity);
		long selected = groupLeaders() & expansionClosure(selectedMask);
		// A zero factor annihilates the product even when an earlier partial product would overflow.
		for (long rest = selected; rest != 0L; rest &= rest - 1L)
			if (count(Long.numberOfTrailingZeros(rest)) == 0L) return 0L;
		long result = prefixMultiplicity;
		for (long rest = selected; rest != 0L; rest &= rest - 1L)
			result = Math.multiplyExact(result, count(Long.numberOfTrailingZeros(rest)));
		return result;
	}

	/** No tuple metadata or public slot validation is needed when the loop itself supplies known unary slots. */
	private long unaryCountProduct(long selected, long prefixMultiplicity) {
		for (long rest = selected; rest != 0L; rest &= rest - 1L)
			if (unaryCount(Long.numberOfTrailingZeros(rest)) == 0L) return 0L;
		long result = prefixMultiplicity;
		for (long rest = selected; rest != 0L; rest &= rest - 1L)
			result = Math.multiplyExact(result, unaryCount(Long.numberOfTrailingZeros(rest)));
		return result;
	}

	private long unaryCount(int slot) {
		BorrowedFactorBatch batch = batches[slot];
		if (batch.generation() != generations[slot])
			throw new IllegalStateException("factor producer advanced while the row view was live");
		return batch.count(lanes[slot]); // owner and copied selected-descriptor checks remain mandatory
	}

	public void checkValid() {
		for (long rest = groupLeaders(); rest != 0L; rest &= rest - 1L)
			count(Long.numberOfTrailingZeros(rest));
	}

	private void checkSlot(int slot) {
		Objects.checkIndex(slot, batches.length);
		if ((mask & (1L << slot)) == 0L) throw new IllegalStateException("slot is not a factor");
		long generation;
		if ((tupleMask & (1L << slot)) == 0L) {
			batches[slot].source().checkOpen();
			generation = batches[slot].generation();
		} else {
			tuples[slot].source().checkOpen();
			generation = tuples[slot].generation();
		}
		if (generation != generations[slot])
			throw new IllegalStateException("factor producer advanced while the row view was live");
	}
}
