/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import static org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeAggregateCompiler.UNKNOWN;
import static org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeAggregateCompiler.memoReadMask;

import java.io.IOException;
import org.eclipse.rdf4j.sail.lmdb.factor.FactorEnvironment;
import org.eclipse.rdf4j.sail.lmdb.factor.FactorProductCursor;

/** Local, dependency-driven bridges between existing slot operators and grouped relation rows. */
final class LmdbNativeFactorRows {
	private static final int READ_WINDOW = 256;
	private LmdbNativeFactorRows() { }

	static boolean enabled() {
		return !"false".equals(System.getProperty("rdf4j.lmdb.factor.transport.enabled"));
	}

	static LmdbNativeFactorCursor filter(FilterPlan plan, RowState row) throws IOException {
		if (!enabled() || row.encounterOrderRequired || plan.filterMask < 0L) return null;
		LmdbNativeFactorCursor input = plan.arg.openFactors(row);
		if (input == null) return null;
		try { return new Filter(new Expanded(input, row, plan.filterMask), plan.filter, row); }
		catch (RuntimeException | Error problem) { closeSuppressing(input, problem); throw problem; }
	}

	static LmdbNativeFactorCursor join(JoinPlan plan, RowState row) throws IOException {
		if (!enabled() || row.encounterOrderRequired) return null;
		long reads = memoReadMask(plan.right);
		// A known dependency set alone does not grant replay permission to unknown future plan kinds.
		if (reads < 0L || !SlotPlan.encounterOrderReplaySafe(plan.right)) return null;
		LmdbNativeFactorCursor left = plan.left.openFactors(row);
		if (left == null) return null;
		try { return new Joined(new Expanded(left, row, reads | plan.right.producedMask()), plan.right, row); }
		catch (RuntimeException | Error problem) { closeSuppressing(left, problem); throw problem; }
	}

	static FactorizedRowCursor openProjected(SlotPlan plan, RowState row, int[] slots) throws IOException {
		if (!enabled() || row.encounterOrderRequired) return null;
		long demanded = 0L;
		for (int slot : slots) {
			java.util.Objects.checkIndex(slot, row.slots.length);
			demanded |= 1L << slot;
		}
		// Keep intermediate scalar dependencies on a private trail. Only requested bindings escape.
		RowState scratch = row.fork();
		LmdbNativeFactorCursor input = plan.openFactors(scratch);
		if (input == null) return null;
		try { return new Projected(new Expanded(input, scratch, demanded), scratch, row, slots); }
		catch (RuntimeException | Error problem) { closeSuppressing(input, problem); throw problem; }
	}

	static FactorizedRowCursor asRows(LmdbNativeFactorCursor input, RowState row) {
		return new Flattened(new Expanded(input, row, -1L));
	}

	static LmdbNativeFactorCursor scalar(RowCursor input, int slots) {
		return new Scalar(input, slots);
	}

	/** A query cancellation is exhaustion, not a probe decline or a recoverable plan failure. */
	static boolean cancelled(RowState row, int tick) {
		LmdbNativeProbeDeadline.poll(tick);
		return (tick == 1 || (tick & 1023) == 0) && row.cancellation.isCancellationRequested();
	}

	private static void closeSuppressing(AutoCloseable resource, Throwable original) {
		try { resource.close(); } catch (Throwable failure) {
			if (failure != original) original.addSuppressed(failure);
		}
	}

	/** Expands only the consumer's dependency set, leaving all other descriptors attached. */
	static final class Expanded implements LmdbNativeFactorCursor {
		final LmdbNativeFactorCursor input;
		final RowState row;
		final long demanded;
		final FactorEnvironment remaining;
		final FactorProductCursor product;
		int mark = -1;
		boolean active;
		boolean closed;
		int tick;

		Expanded(LmdbNativeFactorCursor input, RowState row, long demanded) {
			this.input = input;
			this.row = row;
			this.demanded = demanded;
			remaining = new FactorEnvironment(row.slots.length);
			product = new FactorProductCursor(row.slots.length, READ_WINDOW);
		}

		@Override public boolean next() throws IOException {
			if (closed) return false;
			try {
				while (true) {
					if (cancelled(row, ++tick)) { close(); return false; }
					if (active) {
						row.rollback(mark);
						if (product.next()) {
							boolean compatible = true;
							for (long mask = product.expandedMask(); mask != 0L; mask &= mask - 1L) {
								int slot = Long.numberOfTrailingZeros(mask);
								compatible &= row.bindOrCheckTerm(slot, product.value(slot));
							}
							if (compatible) return true;
							continue;
						}
						active = false;
					}
					remaining.clear();
					if (!input.next()) { close(); return false; }
					mark = row.mark();
					FactorEnvironment factors = input.factors();
					if ((row.boundMask() & factors.mask()) != 0L)
						throw new IllegalStateException("deferred factor installed as a scalar binding");
					remaining.append(factors, ~demanded);
					product.bind(factors, demanded, input.multiplicity());
					active = true;
				}
			} catch (IOException | RuntimeException | Error failure) {
				closeSuppressing(this, failure);
				throw failure;
			}
		}

		@Override public long multiplicity() { return product.multiplicity(); }
		@Override public FactorEnvironment factors() { return remaining; }
		@Override public void close() {
			if (closed) return;
			closed = true;
			remaining.clear();
			try { product.close(); }
			finally {
				if (mark >= 0) row.rollback(mark);
				input.close();
			}
		}
	}

	private static final class Scalar implements LmdbNativeFactorCursor {
		final RowCursor input;
		final FactorEnvironment empty;
		long weight;
		Scalar(RowCursor input, int slots) { this.input = input; empty = new FactorEnvironment(slots); }
		@Override public boolean next() throws IOException {
			if (!input.next()) return false;
			weight = input instanceof FactorizedRowCursor factor ? factor.multiplicity() : 1L;
			if (weight <= 0L) throw new IllegalStateException("invalid scalar bag weight");
			return true;
		}
		@Override public long multiplicity() { return weight; }
		@Override public FactorEnvironment factors() { return empty; }
		@Override public void close() { input.close(); }
	}

	private static final class Filter implements LmdbNativeFactorCursor {
		final LmdbNativeFactorCursor input;
		final NativeBooleanFilter filter;
		final RowState row;
		boolean closed;
		int tick;
		Filter(LmdbNativeFactorCursor input, NativeBooleanFilter filter, RowState row) {
			this.input = input; this.filter = filter; this.row = row;
		}
		@Override public boolean next() throws IOException {
			if (closed) return false;
			try {
				while (input.next()) {
					if (cancelled(row, ++tick)) { close(); return false; }
					if (filter.accept(row)) return true;
				}
				close(); return false;
			} catch (IOException | RuntimeException | Error failure) {
				closeSuppressing(this, failure); throw failure;
			}
		}
		@Override public long multiplicity() { return input.multiplicity(); }
		@Override public FactorEnvironment factors() { return input.factors(); }
		@Override public void close() {
			if (closed) return; closed = true;
			try { input.close(); } finally { filter.close(); }
		}
	}

	private static final class Joined implements LmdbNativeFactorCursor {
		final LmdbNativeFactorCursor left;
		final SlotPlan rightPlan;
		final RowState row;
		final FactorEnvironment result;
		LmdbNativeFactorCursor right;
		long leftWeight;
		long weight;
		boolean closed;
		int tick;
		Joined(LmdbNativeFactorCursor left, SlotPlan rightPlan, RowState row) {
			this.left = left; this.rightPlan = rightPlan; this.row = row;
			result = new FactorEnvironment(row.slots.length);
		}
		@Override public boolean next() throws IOException {
			if (closed) return false;
			try {
				while (true) {
					if (cancelled(row, ++tick)) { close(); return false; }
					result.clear();
					if (right != null) {
						if (right.next()) {
							result.append(left.factors(), -1L);
							result.append(right.factors(), -1L);
							weight = Math.multiplyExact(leftWeight, right.multiplicity());
							return true;
						}
						right.close(); right = null;
					}
					if (!left.next()) { close(); return false; }
					leftWeight = left.multiplicity(); // capture once, never consume a left bag repeatedly
					right = rightPlan.openFactors(row);
					if (right == null) right = scalar(rightPlan.open(row), row.slots.length);
				}
			} catch (IOException | RuntimeException | Error failure) {
				closeSuppressing(this, failure); throw failure;
			}
		}
		@Override public long multiplicity() { return weight; }
		@Override public FactorEnvironment factors() { return result; }
		@Override public void close() {
			if (closed) return; closed = true; result.clear();
			try { if (right != null) right.close(); } finally { right = null; left.close(); }
		}
	}

	/** Ordinary cursor semantics include replay unless a weighted consumer explicitly takes it. */
	private static final class Flattened implements FactorizedRowCursor {
		final Expanded input;
		long repeat;
		boolean closed;
		int tick;
		Flattened(Expanded input) { this.input = input; }
		@Override public boolean next() throws IOException {
			if (closed) return false;
			try {
				if (cancelled(input.row, ++tick)) { close(); return false; }
				if (repeat > 0L) { repeat--; return true; }
				if (!input.next()) { close(); return false; }
				repeat = input.multiplicity() - 1L;
				return true;
			} catch (IOException | RuntimeException | Error failure) {
				closeSuppressing(this, failure); throw failure;
			}
		}
		@Override public long multiplicity() { long weight = repeat + 1L; repeat = 0L; return weight; }
		@Override public void close() { if (!closed) { closed = true; input.close(); } }
	}

	private static final class Projected implements FactorizedRowCursor {
		final LmdbNativeFactorCursor input;
		final RowState scratch;
		final RowState target;
		final int[] slots;
		final int mark;
		long repeat;
		boolean closed;
		int tick;
		Projected(LmdbNativeFactorCursor input, RowState scratch, RowState target, int[] slots) {
			this.input = input; this.scratch = scratch; this.target = target; this.slots = slots.clone();
			mark = target.mark();
		}
		@Override public boolean next() throws IOException {
			if (closed) return false;
			try {
				if (cancelled(target, ++tick)) { close(); return false; }
				target.rollback(mark);
				if (repeat > 0L) { repeat--; install(); return true; }
				while (input.next()) {
					long count = input.factors().countProduct(-1L, input.multiplicity());
					if (count == 0L) continue;
					repeat = count - 1L;
					install(); return true;
				}
				close(); return false;
			} catch (IOException | RuntimeException | Error failure) {
				closeSuppressing(this, failure); throw failure;
			}
		}
		private void install() {
			for (int slot : slots) {
				long value = scratch.slots[slot];
				if (value != UNKNOWN && !target.bindOrCheckTerm(slot, value))
					throw new IllegalStateException("factor projection changed an entry binding");
			}
		}
		@Override public long multiplicity() { long weight = repeat + 1L; repeat = 0L; return weight; }
		@Override public void close() {
			if (closed) return; closed = true;
			try { input.close(); } finally { target.rollback(mark); }
		}
	}
}
