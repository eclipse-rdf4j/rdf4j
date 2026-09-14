/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import static org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeAggregateCompiler.UNKNOWN;
import static org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeAggregateCompiler.memoReadMask;

import java.io.IOException;

import org.eclipse.rdf4j.sail.lmdb.factor.BorrowedFactorBatch;
import org.eclipse.rdf4j.sail.lmdb.factor.FactorEnvironment;
import org.eclipse.rdf4j.sail.lmdb.factor.FactorProductCursor;
import org.eclipse.rdf4j.sail.lmdb.factor.FactorSelection;
import org.eclipse.rdf4j.sail.lmdb.factor.SelectedFactorSource;

/** Local, dependency-driven bridges between existing slot operators and grouped relation rows. */
final class LmdbNativeFactorRows {
	private static final int READ_WINDOW = 256;

	private LmdbNativeFactorRows() {
	}

	static boolean enabled() {
		return !"false".equals(System.getProperty("rdf4j.lmdb.factor.transport.enabled"));
	}

	static LmdbNativeFactorCursor filter(FilterPlan plan, RowState row) throws IOException {
		return filter(plan, row, 0L);
	}

	/** Retain groups only when the immediate consumer does not already require their scalar values. */
	private static LmdbNativeFactorCursor openDemanded(SlotPlan plan, RowState row, long scalarDemand)
			throws IOException {
		return plan instanceof FilterPlan filter ? filter(filter, row, scalarDemand)
				: plan.openFactors(row, scalarDemand);
	}

	private static LmdbNativeFactorCursor filter(FilterPlan plan, RowState row, long scalarDemand) throws IOException {
		if (!enabled() || row.encounterOrderRequired || plan.filterMask < 0L)
			return null;
		boolean retainSelection = !"false".equals(System.getProperty("rdf4j.lmdb.factor.selection.enabled"))
				&& (plan.filterMask & ~scalarDemand) != 0L;
		LmdbNativeFactorCursor input = openDemanded(plan.arg, row, scalarDemand);
		if (input == null)
			return null;
		try {
			if (retainSelection && input.mayHaveFactors())
				return new SelectingFilter(input, plan.filter, row, plan.filterMask, scalarDemand);
			return new Filter(expand(input, row, plan.filterMask), plan.filter, row);
		} catch (RuntimeException | Error problem) {
			closeSuppressing(input, problem);
			throw problem;
		}
	}

	/** Known deterministic restriction over any grouped producer, including existing physical joins. */
	static LmdbNativeFactorCursor restrict(LmdbNativeFactorCursor input, NativeBooleanFilter filter,
			RowState row, long reads) {
		if (reads < 0L)
			throw new IllegalArgumentException("opaque grouped filter");
		return new Filter(expand(input, row, reads), filter, row);
	}

	static LmdbNativeFactorCursor join(JoinPlan plan, RowState row) throws IOException {
		if (!enabled() || row.encounterOrderRequired)
			return null;
		long reads = memoReadMask(plan.right);
		// A known dependency set alone does not grant replay permission to unknown future plan kinds.
		if (reads < 0L || !SlotPlan.encounterOrderReplaySafe(plan.right))
			return null;
		LmdbNativeFactorCursor left = openDemanded(plan.left, row, reads | plan.right.producedMask());
		if (left == null)
			return null;
		try {
			return new Joined(expand(left, row, reads | plan.right.producedMask()), plan.right, row);
		} catch (RuntimeException | Error problem) {
			closeSuppressing(left, problem);
			throw problem;
		}
	}

	static FactorizedRowCursor openProjected(SlotPlan plan, RowState row, int[] slots) throws IOException {
		if (!enabled() || row.encounterOrderRequired)
			return null;
		long demanded = 0L;
		for (int slot : slots) {
			java.util.Objects.checkIndex(slot, row.slots.length);
			demanded |= 1L << slot;
		}
		// Keep intermediate scalar dependencies on a private trail. Only requested bindings escape.
		RowState scratch = row.fork();
		LmdbNativeFactorCursor input = openDemanded(plan, scratch, demanded);
		if (input == null) {
			// OPTIONAL/computed BIND do not yet export arbitrary borrowed subtrees, but the
			// existing wildcard pipeline can transport their exact projected multiplicities.
			// Retain that shared physical path instead of falling straight back to scalar rows.
			RowCursor weighted = LmdbWildcardPredicateBatch.openWeightedProjection(plan, scratch, slots, READ_WINDOW);
			if (weighted == null)
				return null;
			try {
				input = scalar(weighted, scratch.slots.length);
			} catch (RuntimeException | Error problem) {
				closeSuppressing(weighted, problem);
				throw problem;
			}
		}
		try {
			return new Projected(expand(input, scratch, demanded), scratch, row, slots);
		} catch (RuntimeException | Error problem) {
			closeSuppressing(input, problem);
			throw problem;
		}
	}

	static FactorizedRowCursor asRows(LmdbNativeFactorCursor input, RowState row) {
		return new Flattened(expand(input, row, -1L), row);
	}

	private static LmdbNativeFactorCursor expand(LmdbNativeFactorCursor input, RowState row, long demanded) {
		return input.mayHaveFactors() ? new Expanded(input, row, demanded) : input;
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
		try {
			resource.close();
		} catch (Throwable failure) {
			if (failure != original)
				original.addSuppressed(failure);
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

		@Override
		public boolean next() throws IOException {
			if (closed)
				return false;
			try {
				while (true) {
					if (cancelled(row, ++tick)) {
						close();
						return false;
					}
					if (active) {
						row.rollback(mark);
						if (product.next()) {
							boolean compatible = true;
							for (long mask = product.expandedMask(); mask != 0L; mask &= mask - 1L) {
								int slot = Long.numberOfTrailingZeros(mask);
								compatible &= row.bindOrCheckTerm(slot, product.value(slot));
							}
							if (compatible)
								return true;
							continue;
						}
						active = false;
					}
					remaining.clear();
					if (!input.next()) {
						close();
						return false;
					}
					mark = row.mark();
					FactorEnvironment factors = input.factors();
					if ((row.boundMask() & factors.mask()) != 0L)
						throw new IllegalStateException("deferred factor installed as a scalar binding");
					product.bind(factors, demanded, input.multiplicity());
					remaining.append(factors, ~product.expandedMask());
					active = true;
				}
			} catch (IOException | RuntimeException | Error failure) {
				closeSuppressing(this, failure);
				throw failure;
			}
		}

		@Override
		public long multiplicity() {
			return product.multiplicity();
		}

		@Override
		public FactorEnvironment factors() {
			return remaining;
		}

		@Override
		public void close() {
			if (closed)
				return;
			closed = true;
			remaining.clear();
			try {
				product.close();
			} finally {
				if (mark >= 0)
					row.rollback(mark);
				input.close();
			}
		}
	}

	private static final class Scalar implements LmdbNativeFactorCursor {
		final RowCursor input;
		final FactorEnvironment empty;
		long weight;

		Scalar(RowCursor input, int slots) {
			this.input = input;
			empty = new FactorEnvironment(slots);
		}

		@Override
		public boolean mayHaveFactors() {
			return false;
		}

		@Override
		public boolean next() throws IOException {
			if (!input.next())
				return false;
			weight = input instanceof FactorizedRowCursor factor ? factor.multiplicity() : 1L;
			if (weight <= 0L)
				throw new IllegalStateException("invalid scalar bag weight");
			return true;
		}

		@Override
		public long multiplicity() {
			return weight;
		}

		@Override
		public FactorEnvironment factors() {
			return empty;
		}

		@Override
		public void close() {
			input.close();
		}
	}

	/**
	 * A unary restriction stays a relation. Each emitted selection is exact, but covers at most one input decode
	 * window. Consequently an early consumer never forces a full degree-sized selection or predicate scan. A
	 * cross-factor condition still opens its dependent product; it is not incorrectly represented as independent unary
	 * selections.
	 */
	private static final class SelectingFilter implements LmdbNativeFactorCursor {
		private static final int NONE = 0, UNARY = 1, PRODUCT = 2, TUPLE = 3;
		final LmdbNativeFactorCursor input;
		final NativeBooleanFilter filter;
		final RowState row;
		final long reads, scalarDemand;
		final FactorEnvironment result;
		TupleFactorFilter tupleFilter;
		SelectedFactorSource selected;
		FactorSelection selection;
		FactorProductCursor product;
		BorrowedFactorBatch boundBatch;
		BorrowedFactorBatch.Cursor reader;
		FactorEnvironment factors;
		int mark = -1, mode, slot, tick, windowAt, windowEnd;
		boolean firstPart;
		long inputWeight, weight, offset, demand;
		long firstValue, firstWeight;
		boolean closed;

		SelectingFilter(LmdbNativeFactorCursor input, NativeBooleanFilter filter, RowState row, long reads,
				long scalarDemand) {
			this.input = input;
			this.filter = filter;
			this.row = row;
			this.reads = reads;
			this.scalarDemand = scalarDemand;
			result = new FactorEnvironment(row.slots.length);
		}

		@Override
		public boolean next() throws IOException {
			if (closed)
				return false;
			try {
				result.clear();
				if (selected != null)
					selected.clear();
				if (tupleFilter != null)
					tupleFilter.clearOutput();
				while (true) {
					if (cancelled(row, ++tick)) {
						close();
						return false;
					}
					if (mark >= 0)
						row.rollback(mark);
					if (mode == UNARY) {
						if (windowAt == windowEnd) {
							int n = reader.nextWindow();
							if (n == 0) {
								mode = NONE;
								continue;
							}
							windowAt = reader.windowStart();
							windowEnd = windowAt + n;
						}
						int acceptedMembers = scanWindow();
						if (acceptedMembers < 0) {
							close();
							return false;
						}
						if (acceptedMembers == 0)
							continue;
						long bit = 1L << slot;
						result.append(factors, ~bit);
						if (acceptedMembers == 1) {
							// A singleton is already decoded and has no product to defer. Inline it instead of
							// allocating a selection/reader merely to retrieve the same value again.
							if (!row.bindOrCheckTerm(slot, firstValue))
								throw new IllegalStateException("inconsistent filter binding");
							weight = Math.multiplyExact(inputWeight, firstWeight);
							return true;
						}
						long dependencies = factors.dependencies(slot) | (reads & ~bit);
						// The whole factor passed: retain its original representation, including tiny spans.
						if (selection.count() == factors.count(slot))
							result.bind(slot, factors.batch(slot), factors.lane(slot), dependencies);
						else {
							if (selected == null)
								selected = new SelectedFactorSource(8);
							result.bind(slot, selected.select(factors.batch(slot), factors.lane(slot), selection), 0,
									dependencies);
						}
						weight = inputWeight;
						return true;
					}
					if (mode == TUPLE) {
						if (tupleFilter.next(result)) {
							weight = tupleFilter.multiplicity();
							return true;
						}
						mode = NONE;
						if (row.cancellation.isCancellationRequested()) {
							close();
							return false;
						}
					}
					if (mode == PRODUCT) {
						while (product.next()) {
							if (cancelled(row, ++tick)) {
								close();
								return false;
							}
							row.rollback(mark);
							boolean compatible = true;
							for (long rest = product.expandedMask(); rest != 0L; rest &= rest - 1L) {
								int variable = Long.numberOfTrailingZeros(rest);
								compatible &= row.bindOrCheckTerm(variable, product.value(variable));
							}
							if (compatible && filter.accept(row)) {
								result.append(factors, ~demand);
								weight = product.multiplicity();
								return true;
							}
						}
						row.rollback(mark);
						mode = NONE;
					}
					if (!input.next()) {
						close();
						return false;
					}
					mark = row.mark();
					factors = input.factors();
					if ((row.boundMask() & factors.mask()) != 0L)
						throw new IllegalStateException("deferred factor installed as a scalar binding");
					inputWeight = input.multiplicity();
					if (inputWeight < 0L)
						throw new IllegalStateException("negative input multiplicity");
					boolean empty = inputWeight == 0L;
					for (long rest = factors.groupLeaders(); rest != 0L; rest &= rest - 1L)
						empty |= factors.count(Long.numberOfTrailingZeros(rest)) == 0L;
					if (empty)
						continue;
					demand = factors.expansionClosure(reads);
					if (demand == 0L) {
						if (filter.accept(row)) {
							result.append(factors, -1L);
							weight = inputWeight;
							return true;
						}
						continue;
					}
					int leader = Long.numberOfTrailingZeros(demand);
					boolean neededNow = (factors.expansionClosure(scalarDemand) & demand) != 0L;
					if (!neededNow && factors.isTuple(leader) && factors.groupMask(leader) == demand) {
						if (tupleFilter == null)
							tupleFilter = new TupleFactorFilter(row, filter, reads);
						tupleFilter.bind(factors, leader, inputWeight, mark);
						mode = TUPLE;
						continue;
					}
					if (neededNow || Long.bitCount(demand) != 1 || factors.isTuple(leader)) {
						if (product == null)
							product = new FactorProductCursor(row.slots.length, READ_WINDOW);
						product.bind(factors, reads, inputWeight);
						mode = PRODUCT;
						continue;
					}
					slot = Long.numberOfTrailingZeros(demand);
					BorrowedFactorBatch batch = factors.batch(slot);
					if (boundBatch != batch) {
						if (reader != null)
							reader.close();
						reader = null;
						boundBatch = null;
						reader = batch.cursor(READ_WINDOW);
						boundBatch = batch;
					}
					reader.bind(factors.lane(slot));
					offset = 0L;
					mode = UNARY;
					windowAt = windowEnd = 0;
					firstPart = true;
				}
			} catch (IOException | RuntimeException | Error failure) {
				closeSuppressing(this, failure);
				throw failure;
			}
		}

		/** The hot unary kernel is separate from relation publication and product-state dispatch. */
		private int scanWindow() {
			FactorSelection matches = selection;
			if (matches != null)
				matches.clear();
			int acceptedMembers = 0, at = windowAt, end = windowEnd, checks = tick;
			long position = offset, firstOffset = 0L, value = 0L, multiplicity = 0L;
			boolean witness = firstPart;
			long[] values = reader.windowValues(), weights = reader.windowWeights();
			RowState state = row;
			NativeBooleanFilter predicate = filter;
			int variable = slot, restore = mark;
			while (at < end) {
				int i = at++;
				if (cancelled(state, ++checks))
					return -1;
				boolean accepted;
				try {
					accepted = state.bindOrCheckTerm(variable, values[i]) && predicate.accept(state);
				} finally {
					state.rollback(restore);
				}
				long count = weights[i];
				if (accepted) {
					if (acceptedMembers++ == 0) {
						firstOffset = position;
						value = values[i];
						multiplicity = count;
					} else {
						if (matches == null)
							matches = new FactorSelection(8);
						if (acceptedMembers == 2)
							matches.add(firstOffset, multiplicity);
						matches.add(position, count);
					}
				}
				position = Math.addExact(position, count);
				if (accepted && witness) {
					witness = false;
					break;
				}
			}
			windowAt = at;
			offset = position;
			tick = checks;
			firstPart = witness;
			firstValue = value;
			firstWeight = multiplicity;
			selection = matches;
			return acceptedMembers;
		}

		@Override
		public long multiplicity() {
			return weight;
		}

		@Override
		public FactorEnvironment factors() {
			return result;
		}

		@Override
		public void close() {
			if (closed)
				return;
			closed = true;
			result.clear();
			factors = null;
			try {
				if (reader != null)
					reader.close();
			} finally {
				reader = null;
				boundBatch = null;
				try {
					if (product != null)
						product.close();
				} finally {
					try {
						if (tupleFilter != null)
							tupleFilter.close();
					} finally {
						try {
							if (selected != null)
								selected.close();
						} finally {
							try {
								if (mark >= 0)
									row.rollback(mark);
								input.close();
							} finally {
								filter.close();
							}
						}
					}
				}
			}
		}
	}

	private static final class Filter implements LmdbNativeFactorCursor {
		final LmdbNativeFactorCursor input;
		final NativeBooleanFilter filter;
		final RowState row;
		boolean closed;
		int tick;

		Filter(LmdbNativeFactorCursor input, NativeBooleanFilter filter, RowState row) {
			this.input = input;
			this.filter = filter;
			this.row = row;
		}

		@Override
		public boolean next() throws IOException {
			if (closed)
				return false;
			try {
				while (input.next()) {
					if (cancelled(row, ++tick)) {
						close();
						return false;
					}
					if (filter.accept(row))
						return true;
				}
				close();
				return false;
			} catch (IOException | RuntimeException | Error failure) {
				closeSuppressing(this, failure);
				throw failure;
			}
		}

		@Override
		public long multiplicity() {
			return input.multiplicity();
		}

		@Override
		public FactorEnvironment factors() {
			return input.factors();
		}

		@Override
		public boolean mayHaveFactors() {
			return input.mayHaveFactors();
		}

		@Override
		public void close() {
			if (closed)
				return;
			closed = true;
			try {
				input.close();
			} finally {
				filter.close();
			}
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
			this.left = left;
			this.rightPlan = rightPlan;
			this.row = row;
			result = new FactorEnvironment(row.slots.length);
		}

		@Override
		public boolean next() throws IOException {
			if (closed)
				return false;
			try {
				while (true) {
					if (cancelled(row, ++tick)) {
						close();
						return false;
					}
					result.clear();
					if (right != null) {
						if (right.next()) {
							result.append(left.factors(), -1L);
							result.append(right.factors(), -1L);
							weight = Math.multiplyExact(leftWeight, right.multiplicity());
							return true;
						}
						right.close();
						right = null;
					}
					if (!left.next()) {
						close();
						return false;
					}
					leftWeight = left.multiplicity(); // capture once, never consume a left bag repeatedly
					right = rightPlan.openFactors(row);
					if (right == null)
						right = scalar(rightPlan.open(row), row.slots.length);
				}
			} catch (IOException | RuntimeException | Error failure) {
				closeSuppressing(this, failure);
				throw failure;
			}
		}

		@Override
		public long multiplicity() {
			return weight;
		}

		@Override
		public FactorEnvironment factors() {
			return result;
		}

		@Override
		public void close() {
			if (closed)
				return;
			closed = true;
			result.clear();
			try {
				if (right != null)
					right.close();
			} finally {
				right = null;
				left.close();
			}
		}
	}

	/** Ordinary cursor semantics include replay unless a weighted consumer explicitly takes it. */
	private static final class Flattened implements FactorizedRowCursor {
		final LmdbNativeFactorCursor input;
		long repeat;
		boolean closed;
		int tick;
		final RowState row;

		Flattened(LmdbNativeFactorCursor input, RowState row) {
			this.input = input;
			this.row = row;
		}

		@Override
		public boolean next() throws IOException {
			if (closed)
				return false;
			try {
				if (cancelled(row, ++tick)) {
					close();
					return false;
				}
				if (repeat > 0L) {
					repeat--;
					return true;
				}
				if (!input.next()) {
					close();
					return false;
				}
				if (input.factors().mask() != 0L)
					throw new IllegalStateException("unexpanded factor at scalar consumer");
				long count = input.multiplicity();
				if (count <= 0L)
					throw new IllegalStateException("invalid scalar prefix weight");
				repeat = count - 1L;
				return true;
			} catch (IOException | RuntimeException | Error failure) {
				closeSuppressing(this, failure);
				throw failure;
			}
		}

		@Override
		public long multiplicity() {
			long weight = repeat + 1L;
			repeat = 0L;
			return weight;
		}

		@Override
		public void close() {
			if (!closed) {
				closed = true;
				input.close();
			}
		}
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
			this.input = input;
			this.scratch = scratch;
			this.target = target;
			this.slots = slots.clone();
			mark = target.mark();
		}

		@Override
		public boolean next() throws IOException {
			if (closed)
				return false;
			try {
				if (cancelled(target, ++tick)) {
					close();
					return false;
				}
				target.rollback(mark);
				if (repeat > 0L) {
					repeat--;
					install();
					return true;
				}
				while (input.next()) {
					long count = input.factors().countProduct(-1L, input.multiplicity());
					if (count == 0L)
						continue;
					repeat = count - 1L;
					install();
					return true;
				}
				close();
				return false;
			} catch (IOException | RuntimeException | Error failure) {
				closeSuppressing(this, failure);
				throw failure;
			}
		}

		private void install() {
			long deferred = input.factors().mask();
			for (int slot : slots) {
				if ((deferred & (1L << slot)) != 0L)
					throw new IllegalStateException("unexpanded factor at projected scalar consumer");
				long value = scratch.slots[slot];
				if (value != UNKNOWN && !target.bindOrCheckTerm(slot, value))
					throw new IllegalStateException("factor projection changed an entry binding");
			}
		}

		@Override
		public long multiplicity() {
			long weight = repeat + 1L;
			repeat = 0L;
			return weight;
		}

		@Override
		public void close() {
			if (closed)
				return;
			closed = true;
			try {
				input.close();
			} finally {
				target.rollback(mark);
			}
		}
	}
}
