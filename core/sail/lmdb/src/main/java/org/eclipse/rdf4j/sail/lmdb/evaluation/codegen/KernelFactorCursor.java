/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation.codegen;

import java.util.Objects;
import org.eclipse.rdf4j.sail.lmdb.factor.FactorEnvironment;
import org.eclipse.rdf4j.sail.lmdb.factor.FactorProductCursor;

/**
 * Degree-bounded grouped-plan reader shared by generated and interpreted kernels. Requested
 * bindings become scalar IDs, correlated tuple columns open together, and unrelated groups stay
 * borrowed. Exact hidden cardinalities are multiplied only when the continuation accepts a row.
 *
 * <p>Unsupported borrowing falls back to exact weighted rows, never to an empty relation. The plan
 * owns the snapshot; this reader closes its child readers before its producing cursor. Instances
 * are thread-confined and cannot outlive or advance independently of the current producer prefix.
 */
public final class KernelFactorCursor implements AutoCloseable {
	private final KernelPlan.FactorCursor factors;
	private final KernelPlan.Cursor rows;
	private final int[] outputs;
	private final int[] slots;
	private final long[] values;
	private final long[] rowBuffer;
	private final long[] weights;
	private final int width;
	private final KernelCancellation cancellation;
	private final long demand;
	private FactorProductCursor product;
	private KernelFactorCount counter;
	private boolean productBound;
	private boolean singletonDemand;
	private boolean bindingsStarted;
	private boolean reduced;
	private FactorEnvironment environment;
	private long epoch;
	private long expanded;
	private long deferredOutputs;
	private long prefixWeight;
	private long residualWeight;
	private long currentWeight;
	private boolean residualKnown;
	private boolean prefixActive;
	private boolean scalarPending;
	private int groupedMode;
	private boolean positioned;
	private boolean closed;
	private int at;
	private int end;
	private int pollTick;
	private boolean bulkRows;

	public static KernelFactorCursor open(KernelPlan plan, int outputWidth, int[] scalarOutputs,
			KernelCancellation cancellation) {
		Objects.requireNonNull(plan, "plan");
		Objects.requireNonNull(scalarOutputs, "scalarOutputs");
		if (outputWidth < 0) throw new IllegalArgumentException("negative output width");
		boolean[] seen = new boolean[outputWidth];
		for (int output : scalarOutputs) {
			Objects.checkIndex(output, outputWidth);
			if (seen[output]) throw new IllegalArgumentException("repeated scalar output");
			seen[output] = true;
		}
		KernelRuntime.checkCancelled(cancellation);
		KernelPlan.FactorCursor factors = plan.openFactors(scalarOutputs);
		KernelPlan.Cursor rows = null;
		try {
			if (factors == null) rows = Objects.requireNonNull(plan.open(), "scalar fallback cursor");
			return new KernelFactorCursor(factors, rows, outputWidth, scalarOutputs, cancellation);
		} catch (RuntimeException | Error failure) {
			try { if (factors != null) factors.close(); else if (rows != null) rows.close(); }
			catch (RuntimeException | Error closeFailure) { if (failure != closeFailure) failure.addSuppressed(closeFailure); }
			throw failure;
		}
	}

	private KernelFactorCursor(KernelPlan.FactorCursor factors, KernelPlan.Cursor rows, int width,
			int[] outputs, KernelCancellation cancellation) {
		this.factors = factors;
		this.rows = rows;
		this.width = width;
		this.outputs = outputs.clone();
		this.cancellation = cancellation;
		values = new long[outputs.length];
		slots = factors == null ? null : new int[outputs.length];
		long requested = 0L;
		if (factors != null) {
			for (int i = 0; i < outputs.length; i++) {
				int slot = factors.slot(outputs[i]);
				Objects.checkIndex(slot, Long.SIZE);
				slots[i] = slot;
				requested |= 1L << slot;
			}
		}
		demand = requested;
		rowBuffer = rows == null ? null : new long[Math.multiplyExact(Math.max(1, width), KernelRuntime.SCAN_BATCH_ROWS)];
		weights = rows == null ? null : new long[KernelRuntime.SCAN_BATCH_ROWS];
	}

	/** Reused array in scalarOutputs order. Hoist once; values become valid on successful next(). */
	public long[] values() { return values; }

	public boolean next() {
		if (closed) return false;
		positioned = false;
		try {
			if (rows != null) return nextRows();
			if (groupedMode == 2) throw new IllegalStateException("mixed grouped consumption");
			groupedMode = 1;
			while (true) {
				if (prefixActive && nextBindingInternal()) return true;
				if (!nextPrefixInternal()) return false;
			}
		} catch (RuntimeException | Error failure) {
			closeOnFailure(failure);
			throw failure;
		}
	}

	/** Open one binding prefix. Used by fused kernels to reduce a group before observing its siblings. */
	public boolean nextPrefix() {
		if (factors == null || groupedMode == 1) throw new IllegalStateException("not structured grouped consumption");
		if (closed) return false;
		groupedMode = 2;
		try { return nextPrefixInternal(); }
		catch (RuntimeException | Error failure) { closeOnFailure(failure); throw failure; }
	}

	/** Enumerate just the demanded bindings of the current prefix, never its unopened siblings. */
	public boolean nextBinding() {
		if (groupedMode != 2 || !prefixActive || closed) throw new IllegalStateException("no active grouped prefix");
		try { return nextBindingInternal(); }
		catch (RuntimeException | Error failure) { closeOnFailure(failure); throw failure; }
	}

	private boolean nextBindingInternal() {
		if (reduced) throw new IllegalStateException("prefix already reduced");
		bindingsStarted = true;
		positioned = false;
		if ((++pollTick & 255) == 0) KernelRuntime.checkCancelled(cancellation);
		if (expanded == 0L) {
			if (!scalarPending) return false;
			scalarPending = false;
		} else {
			if (!productBound) {
				if (product == null) product = new FactorProductCursor(Long.SIZE, 256);
				product.bind(environment, demand, 1L);
				productBound = true;
			}
			if (!product.next()) return false;
			copyExpanded();
		}
		positioned = true;
		return true;
	}

	private boolean nextPrefixInternal() {
		positioned = false;
		prefixActive = false;
		while (true) {
			KernelRuntime.checkCancelled(cancellation);
			if (!factors.next()) { close(); return false; }
			environment = Objects.requireNonNull(factors.factors(), "factor environment");
			epoch = environment.epoch();
			prefixWeight = factors.multiplicity();
			if (prefixWeight <= 0L) throw new IllegalStateException("nonpositive factor prefix weight");
			expanded = environment.expansionClosure(demand);
			residualKnown = false;
			boolean empty = false;
			singletonDemand = expanded != 0L;
			for (long rest = environment.groupLeaders(); rest != 0L; rest &= rest - 1L) {
				int leader = Long.numberOfTrailingZeros(rest);
				long count = environment.count(leader);
				if (count == 0L) { empty = true; break; }
				if ((expanded & (1L << leader)) != 0L && count != 1L) singletonDemand = false;
			}
			if (empty) continue;
			deferredOutputs = 0L;
			for (int i = 0; i < outputs.length; i++) {
				if ((expanded & (1L << slots[i])) == 0L) values[i] = factors.scalar(outputs[i]);
				else deferredOutputs |= 1L << i;
			}
			productBound = bindingsStarted = reduced = false;
			prefixActive = true;
			scalarPending = true;
			return true;
		}
	}

	/**
	 * Reduce pure guards independently where every guard touches at most one grouped relation.
	 * Returns -1 without consuming the prefix when a guard couples distinct groups. A nonnegative
	 * result consumes this prefix, including its hidden multiplicities; advance with nextPrefix().
	 */
	public long reduceIndependent(KernelFactorPredicate predicate) {
		return reduceIndependent(predicate, 0L, true);
	}

	/**
	 * terminalOutputs must be scalar in this prefix. When exactMultiplicity is false the caller
	 * observes only existence (e.g. all COUNT operands unbound); a successful relation contributes
	 * a witness weight of one, without multiplying an unobserved, possibly overflowing product.
	 */
	public long reduceIndependent(KernelFactorPredicate predicate, long terminalOutputs, boolean exactMultiplicity) {
		if (groupedMode != 2 || !prefixActive || closed || bindingsStarted || reduced)
			throw new IllegalStateException("prefix not available for independent reduction");
		try {
			if (environment.epoch() != epoch) throw new IllegalStateException("factor prefix advanced");
			long schema = slots.length == Long.SIZE ? -1L : (1L << slots.length) - 1L;
			if ((terminalOutputs & ~schema) != 0L) throw new IllegalArgumentException("terminal output outside schema");
			// Pure scalar guards can reject even when a later guard or grouping key requires
			// the general product path. Classify before allocating any selection workspace.
			boolean independent = (terminalOutputs & deferredOutputs) == 0L;
			int guards = predicate.guardCount();
			if (guards < 0 || guards > Long.SIZE) throw new IllegalArgumentException("invalid guard count");
			for (int i = 0; i < guards; i++) {
				long dependencies = predicate.dependencies(i);
				if ((dependencies & ~schema) != 0L) throw new IllegalArgumentException("guard outside schema");
				long related = dependencies & deferredOutputs;
				if (related == 0L) {
					if (!predicate.test(i, values)) {
						if (environment.epoch() != epoch) throw new IllegalStateException("prefix advanced during reduction");
						environment.checkValid(); reduced = true; return 0L;
					}
				} else if ((related & (related - 1L)) != 0L) {
					long group = environment.groupMask(slots[Long.numberOfTrailingZeros(related)]);
					for (long rest = related & (related - 1L); rest != 0L; rest &= rest - 1L)
						if ((group & (1L << slots[Long.numberOfTrailingZeros(rest)])) == 0L) independent = false;
				}
			}
			if (environment.epoch() != epoch) throw new IllegalStateException("prefix advanced during reduction");
			environment.checkValid();
			if (!independent) return KernelFactorCount.UNSUPPORTED;
			if (expanded == 0L) {
				reduced = true;
				return exactMultiplicity ? remainderMultiplicity() : 1L;
			}
			// One demanded logical tuple has no Cartesian growth to eliminate. Reuse the existing
			// scalar continuation instead of allocating selection windows and invoking mask kernels.
			if (singletonDemand && exactMultiplicity) return KernelFactorCount.UNSUPPORTED;
			if (counter == null) counter = new KernelFactorCount(slots, cancellation);
			long count = counter.count(environment, values, prefixWeight, predicate, exactMultiplicity);
			reduced = count != KernelFactorCount.UNSUPPORTED;
			return count;
		} catch (RuntimeException | Error failure) { closeOnFailure(failure); throw failure; }
	}

	/** Weight already carried by the opened binding; no hidden cardinality is evaluated. */
	public long openedMultiplicity() {
		if (!positioned || closed || environment.epoch() != epoch) throw new IllegalStateException("no current binding");
		return expanded == 0L ? 1L : product.multiplicity();
	}

	/** Exact prefix/hidden weight, still available after the last demanded member of this prefix. */
	public long remainderMultiplicity() {
		if (!prefixActive || closed || environment.epoch() != epoch) throw new IllegalStateException("no current prefix");
		environment.checkValid();
		if (!residualKnown) {
			residualWeight = environment.countProduct(environment.mask() & ~expanded, prefixWeight);
			residualKnown = true;
		}
		return residualWeight;
	}

	private void copyExpanded() {
		for (int i = 0; i < outputs.length; i++)
			if ((expanded & (1L << slots[i])) != 0L) values[i] = product.value(slots[i]);
	}

	/** Representation dispatch once per producer activation, not once per scalar value. */
	public boolean grouped() { return factors != null; }

	/** Bounded row-major fallback window, valid until the next read. */
	public long[] rowValues() {
		if (rows == null) throw new IllegalStateException("grouped source has no row window");
		return rowBuffer;
	}

	/** Positive, exact weights aligned with the current fallback row window. */
	public long[] rowWeights() {
		if (rows == null) throw new IllegalStateException("grouped source has no weight window");
		return weights;
	}

	/**
	 * Bulk fallback for fused kernels. Every returned row is owned by the consumer; next() must not
	 * be mixed with this interface. Lifetime and cancellation checks occur once per bounded window.
	 */
	public int nextRowWindow() {
		if (rows == null) throw new IllegalStateException("not a row fallback");
		if (closed) return 0;
		if (!bulkRows && (at != 0 || end != 0)) throw new IllegalStateException("mixed row consumption");
		bulkRows = true;
		positioned = false;
		try { return readRows(); }
		catch (RuntimeException | Error failure) { closeOnFailure(failure); throw failure; }
	}

	private int readRows() {
		KernelRuntime.checkCancelled(cancellation);
		end = rows.fillWeighted(rowBuffer, weights, KernelRuntime.SCAN_BATCH_ROWS);
		if (end < 0 || end > KernelRuntime.SCAN_BATCH_ROWS) throw new IllegalStateException("invalid row batch size");
		at = 0;
		for (int i = 0; i < end; i++) if (weights[i] <= 0L) throw new IllegalStateException("nonpositive row multiplicity");
		if (end == 0) close();
		return end;
	}

	private boolean nextRows() {
		if (bulkRows) throw new IllegalStateException("mixed row consumption");
		if (at == end && readRows() == 0) return false;
		int base = at * width;
		for (int i = 0; i < outputs.length; i++) values[i] = rowBuffer[base + outputs[i]];
		currentWeight = weights[at++];
		positioned = true;
		return true;
	}

	/** Exact weight of this accepted projection. Each unopened independent group contributes once. */
	public long multiplicity() {
		if (!positioned || closed) throw new IllegalStateException("cursor not positioned");
		if (rows != null) return currentWeight;
		return Math.multiplyExact(remainderMultiplicity(), openedMultiplicity());
	}

	/** Retain a primary execution failure when reader cleanup fails too. */
	public void closeOnFailure(Throwable failure) {
		try { close(); }
		catch (RuntimeException | Error closeFailure) { if (failure != closeFailure) failure.addSuppressed(closeFailure); }
	}

	@Override public void close() {
		if (closed) return;
		closed = true;
		positioned = false;
		prefixActive = false;
		Throwable failure = null;
		try { if (product != null) product.close(); }
		catch (RuntimeException | Error problem) { failure = problem; }
		try { if (counter != null) counter.close(); }
		catch (RuntimeException | Error problem) {
			if (failure == null) failure = problem; else if (failure != problem) failure.addSuppressed(problem);
		}
		try { if (factors != null) factors.close(); else rows.close(); }
		catch (RuntimeException | Error problem) {
			if (failure == null) failure = problem; else if (failure != problem) failure.addSuppressed(problem);
		}
		environment = null;
		if (failure instanceof RuntimeException problem) throw problem;
		if (failure instanceof Error problem) throw problem;
	}
}
