/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation.codegen;

import java.util.Objects;

import org.eclipse.rdf4j.sail.lmdb.factor.BorrowedFactorBatch;
import org.eclipse.rdf4j.sail.lmdb.factor.FactorEnvironment;
import org.eclipse.rdf4j.sail.lmdb.factor.FactorProductCursor;

/**
 * Consumer-driven projections over one physical input. Native packed chunks, borrowed factors and weighted rows obey
 * the same protocol. No source is reopened for another aggregate channel. Only source-owned batches or a bounded row
 * window are retained. Each projection is consumed once, in request order, so mutation/callback failures cannot
 * silently restart an input.
 */
public final class KernelMarginalCursor implements KernelPlan.ProjectionCursor {
	private static final int WINDOW = 256;
	private final int width;
	private final int[][] columns;
	private final boolean[] exact;
	private final KernelCancellation cancellation;
	private final KernelPlan.ProjectionCursor nativeCursor;
	private final KernelPlan.FactorCursor factors;
	private final KernelPlan.Cursor rows;
	private final int[] slots;
	private final long[] rowBuffer, rowWeights;
	private final long[] masks;
	private FactorProductCursor product;
	private BorrowedFactorBatch.Cursor windowReader;
	private BorrowedFactorBatch windowBatch;
	private int windowCapacity, windowSlot = -1, windowOutput = -1, windowCount;
	private boolean windowMode, prefixChanged;
	private final long[] requestedMasks;
	private FactorEnvironment environment;
	private long epoch, prefixWeight, hiddenWeight, currentWeight;
	private int rowCount, rowIndex, active = -1, completed = -1, ticks;
	private long expanded;
	private long unionDemand;
	private int flatCount;
	private long[] singletonValues, singletonWeights;
	private long unionColumns;
	private boolean scalarPending, batch, positioned, closed, started;

	public static KernelMarginalCursor open(KernelPlan plan, int width, int[][] columns, boolean[] exact,
			KernelCancellation cancellation) {
		Objects.requireNonNull(plan, "plan");
		validate(width, columns, exact);
		KernelRuntime.checkCancelled(cancellation);
		KernelPlan.ProjectionCursor nativeCursor = plan.openProjections(columns, exact);
		KernelPlan.FactorCursor factors = null;
		KernelPlan.Cursor rows = null;
		try {
			if (nativeCursor == null) {
				factors = plan.openFactors(new int[0]);
				if (factors == null)
					rows = Objects.requireNonNull(plan.openWeighted(), "plan cursor");
			}
			return new KernelMarginalCursor(width, columns, exact, cancellation, nativeCursor, factors, rows);
		} catch (RuntimeException | Error problem) {
			Throwable failure = KernelRuntime.closeResource(nativeCursor, problem);
			failure = KernelRuntime.closeResource(factors, failure);
			KernelRuntime.closeResource(rows, failure);
			throw problem;
		}
	}

	private static void validate(int width, int[][] columns, boolean[] exact) {
		if (width < 0 || width > 64 || columns == null || exact == null || columns.length != exact.length
				|| columns.length == 0)
			throw new IllegalArgumentException("invalid projection schema");
		for (int[] projection : columns) {
			long seen = 0L;
			for (int column : Objects.requireNonNull(projection)) {
				Objects.checkIndex(column, width);
				long bit = 1L << column;
				if ((seen & bit) != 0L)
					throw new IllegalArgumentException("duplicate projected column");
				seen |= bit;
			}
		}
	}

	private KernelMarginalCursor(int width, int[][] columns, boolean[] exact, KernelCancellation cancellation,
			KernelPlan.ProjectionCursor nativeCursor, KernelPlan.FactorCursor factors, KernelPlan.Cursor rows) {
		this.width = width;
		this.exact = exact.clone();
		this.cancellation = cancellation;
		this.columns = new int[columns.length][];
		requestedMasks = new long[columns.length];
		for (int i = 0; i < columns.length; i++) {
			this.columns[i] = columns[i].clone();
			for (int column : columns[i]) {
				unionColumns |= 1L << column;
				requestedMasks[i] |= 1L << column;
			}
		}
		this.nativeCursor = nativeCursor;
		this.factors = factors;
		this.rows = rows;
		slots = factors == null ? null : new int[width];
		masks = factors == null ? null : new long[columns.length];
		if (factors != null) {
			for (int i = 0; i < width; i++)
				slots[i] = Objects.checkIndex(factors.slot(i), 64);
			for (int p = 0; p < columns.length; p++)
				for (int column : columns[p]) {
					masks[p] |= 1L << slots[column];
					unionDemand |= 1L << slots[column];
				}
		}
		rowBuffer = rows == null ? null : new long[Math.multiplyExact(WINDOW, Math.max(1, width))];
		rowWeights = rows == null ? null : new long[WINDOW];
	}

	@Override
	public void permitPrefixPartitions() {
		if (started || closed)
			throw new IllegalStateException("partition permission must precede input");
		if (nativeCursor != null)
			nativeCursor.permitPrefixPartitions();
	}

	@Override
	public boolean nextBatch() {
		if (closed)
			return false;
		started = true;
		try {
			if (batch && completed != columns.length - 1)
				throw new IllegalStateException("unfinished projections");
			KernelRuntime.checkCancelled(cancellation);
			positioned = batch = false;
			flatCount = 0;
			active = completed = -1;
			windowMode = false;
			windowCount = 0;
			boolean found;
			if (nativeCursor != null)
				found = nativeCursor.nextBatch();
			else if (rows != null) {
				rowCount = rows.fillWeighted(rowBuffer, rowWeights, WINDOW);
				if (rowCount < 0 || rowCount > WINDOW)
					throw new IllegalStateException("invalid plan window");
				for (int i = 0; i < rowCount; i++)
					if (rowWeights[i] <= 0L)
						throw new IllegalStateException("nonpositive projected weight");
				found = rowCount != 0;
				flatCount = rowCount;
			} else {
				found = false;
				while (factors.next()) {
					KernelRuntime.checkCancelled(cancellation);
					environment = Objects.requireNonNull(factors.factors());
					environment.checkValid();
					epoch = environment.epoch();
					prefixWeight = factors.multiplicity();
					if (prefixWeight <= 0L)
						throw new IllegalStateException("nonpositive prefix weight");
					boolean empty = false, singleton = true;
					for (long rest = environment.groupLeaders(); rest != 0L; rest &= rest - 1L) {
						long count = environment.count(Long.numberOfTrailingZeros(rest));
						if (count == 0L) {
							empty = true;
							break;
						}
						singleton &= count == 1L;
					}
					if (!empty) {
						found = true;
						if (singleton)
							captureSingleton();
						break;
					}
				}
			}
			if (!found) {
				close();
				return false;
			}
			batch = true;
			return true;
		} catch (RuntimeException | Error failure) {
			fail(failure);
			return false;
		}
	}

	private void captureSingleton() {
		if (singletonValues == null) {
			singletonValues = new long[Math.max(1, width)];
			singletonWeights = new long[1];
		}
		long open = environment.expansionClosure(unionDemand);
		if (open != 0L) {
			if (product == null)
				product = new FactorProductCursor(64, WINDOW);
			product.bind(environment, unionDemand, 1L, false);
			if (!product.next())
				throw new IllegalStateException("nonempty singleton factor disappeared");
		}
		for (long rest = unionColumns; rest != 0L; rest &= rest - 1L) {
			int column = Long.numberOfTrailingZeros(rest);
			singletonValues[column] = (open & (1L << slots[column])) != 0L
					? product.value(slots[column])
					: factors.scalar(column);
		}
		// Each independent logical relation has exactly one positive unit-weight member.
		// Thus the original prefix is the complete weight; no hidden product is formed.
		singletonWeights[0] = prefixWeight;
		flatCount = 1;
	}

	/** Optional row window of the same batch. Scalars and singleton products need only one pass. */
	public int flatRowCount() {
		if (closed || !batch || active != -1)
			throw new IllegalStateException("no unconsumed batch");
		return flatCount;
	}

	public long[] flatValues() {
		if (flatRowCount() == 0)
			throw new IllegalStateException("not flat");
		return rows != null ? rowBuffer : singletonValues;
	}

	public long[] flatWeights() {
		if (flatRowCount() == 0)
			throw new IllegalStateException("not flat");
		return rows != null ? rowWeights : singletonWeights;
	}

	/** Complete the all-channel row pass instead of requesting individual marginals. */
	public void finishFlat() {
		if (flatRowCount() == 0)
			throw new IllegalStateException("not flat");
		active = completed = columns.length - 1;
		positioned = false;
	}

	/** Exact output IDs invariant across all projections of this batch. */
	@Override
	public long constantColumns() {
		if (closed || !batch)
			throw new IllegalStateException("no current relation batch");
		if (nativeCursor != null)
			return nativeCursor.constantColumns();
		if (factors == null)
			return 0L;
		checkEpoch();
		long constant = 0L;
		for (long rest = unionColumns; rest != 0L; rest &= rest - 1L) {
			int column = Long.numberOfTrailingZeros(rest);
			if ((environment.mask() & (1L << slots[column])) == 0L)
				constant |= 1L << column;
		}
		return constant;
	}

	/**
	 * Returns the sole varying unary output column, or -1 without advancing input. Native packed projections can supply
	 * the same capability; zipped/multi-factor requests may decline. Admission is evaluated per batch: the same site
	 * may switch representation on the next batch.
	 */
	public int windowColumn(int projection) {
		if (closed || !batch)
			throw new IllegalStateException("no current relation batch");
		Objects.checkIndex(projection, columns.length);
		if (nativeCursor != null)
			return nativeCursor.windowColumn(projection);
		if (factors == null)
			return -1;
		checkEpoch();
		long open = environment.expansionClosure(masks[projection]);
		if (Long.bitCount(open) != 1)
			return -1;
		int slot = Long.numberOfTrailingZeros(open);
		if (environment.isTuple(slot))
			return -1;
		int output = -1;
		for (int c : columns[projection])
			if (slots[c] == slot) {
				if (output != -1)
					return -1; // aliases can use the general projection contract
				output = c;
			}
		return output;
	}

	/**
	 * Consume a bounded decoder window, validating the complete environment at each boundary. The returned arrays are
	 * read-only and valid only until the next cursor operation. No scalar product is built; hidden weights remain lazy,
	 * including for all-unbound argument windows.
	 */
	public int nextWindow(int projection) {
		if (closed || !batch)
			throw new IllegalStateException("no current relation batch");
		Objects.checkIndex(projection, columns.length);
		try {
			KernelRuntime.checkCancelled(cancellation);
			if (nativeCursor != null) {
				positioned = false;
				if (projection == completed)
					return 0;
				if (active != projection) {
					if (projection != completed + 1 || active != completed)
						throw new IllegalStateException("projection order or unfinished projection");
					windowOutput = windowColumn(projection);
					if (windowOutput < 0)
						throw new IllegalStateException("projection is not a native window");
					active = projection;
					windowMode = true;
				} else if (!windowMode)
					throw new IllegalStateException("cannot switch projection traversal mode");
				windowCount = nativeCursor.nextWindow(projection);
				if (windowCount < 0 || windowCount > WINDOW)
					throw new IllegalStateException("invalid native window");
				KernelRuntime.checkCancelled(cancellation);
				if (windowCount == 0) {
					completed = active;
					return 0;
				}
				prefixChanged = nativeCursor.windowPrefixChanged();
				positioned = true;
				return windowCount;
			}
			checkEpoch();
			prefixChanged = active != projection;
			positioned = false;
			if (projection == completed)
				return 0;
			if (active != projection) {
				if (projection != completed + 1 || active != completed)
					throw new IllegalStateException("projection order or unfinished projection");
				windowOutput = windowColumn(projection);
				if (windowOutput < 0)
					throw new IllegalStateException("projection is not a unary window");
				active = projection;
				windowMode = true;
				windowSlot = slots[windowOutput];
				expanded = 1L << windowSlot;
				hiddenWeight = 0L;
				BorrowedFactorBatch sourceBatch = environment.batch(windowSlot);
				int capacity = (int) Math.min(WINDOW, environment.count(windowSlot));
				if (windowReader == null || windowBatch.source() != sourceBatch.source()
						|| windowCapacity < capacity) {
					BorrowedFactorBatch.Cursor old = windowReader;
					windowReader = null;
					windowBatch = null;
					if (old != null)
						old.close();
					windowReader = sourceBatch.cursor(capacity);
					windowCapacity = capacity;
				}
				windowBatch = sourceBatch;
				windowReader.bind(sourceBatch, environment.lane(windowSlot));
			} else if (!windowMode)
				throw new IllegalStateException("cannot switch projection traversal mode");
			windowCount = windowReader.nextWindow();
			// A reader callback must not advance another mandatory factor while filling this window.
			checkEpoch();
			if (windowCount == 0) {
				completed = active;
				return 0;
			}
			positioned = true;
			return windowCount;
		} catch (RuntimeException | Error failure) {
			fail(failure);
			return 0;
		}
	}

	public long[] windowValues() {
		checkWindow();
		return nativeCursor != null ? nativeCursor.windowValues() : windowReader.windowValues();
	}

	public long[] windowWeights() {
		checkWindow();
		return nativeCursor != null ? nativeCursor.windowWeights() : windowReader.windowWeights();
	}

	public int windowStart() {
		checkWindow();
		return nativeCursor != null ? nativeCursor.windowStart() : windowReader.windowStart();
	}

	@Override
	public boolean windowPrefixChanged() {
		checkWindow();
		return prefixChanged;
	}

	/** Multiplier outside the opened factor. Call only after a non-null argument needs its weight. */
	public long windowScale() {
		checkWindow();
		if (!exact[active])
			return 1L;
		if (nativeCursor != null)
			return nativeCursor.windowScale();
		checkEpoch();
		if (hiddenWeight == 0L)
			hiddenWeight = environment.countProduct(environment.mask() & ~expanded, prefixWeight);
		return hiddenWeight;
	}

	private void checkWindow() {
		if (closed || !positioned || !windowMode || windowCount == 0)
			throw new IllegalStateException("no current factor window");
	}

	@Override
	public boolean next(int projection) {
		if (closed || !batch)
			throw new IllegalStateException("no current relation batch");
		Objects.checkIndex(projection, columns.length);
		try {
			if ((++ticks & 255) == 0)
				KernelRuntime.checkCancelled(cancellation);
			positioned = false;
			if (projection == completed)
				return false;
			if (active != projection) {
				KernelRuntime.checkCancelled(cancellation);
				if (projection != completed + 1 || active != completed)
					throw new IllegalStateException("projection order or unfinished projection");
				active = projection;
				rowIndex = -1;
				windowMode = false;
				if (factors != null) {
					checkEpoch();
					expanded = environment.expansionClosure(masks[projection]);
					// Do not multiply a hidden product for DISTINCT/presence demand.
					hiddenWeight = 0L;
					scalarPending = true;
					if (expanded != 0L) {
						if (product == null)
							product = new FactorProductCursor(64, WINDOW);
						product.bind(environment, masks[projection], 1L, false);
					}
				}
			}
			if (windowMode)
				throw new IllegalStateException("cannot switch projection traversal mode");
			boolean found;
			if (nativeCursor != null) {
				found = nativeCursor.next(projection);
				if (found)
					currentWeight = 0L;
			} else if (rows != null) {
				found = ++rowIndex < rowCount;
				if (found)
					currentWeight = exact[projection] ? rowWeights[rowIndex] : 1L;
			} else {
				checkEpoch();
				if (expanded == 0L) {
					found = scalarPending;
					scalarPending = false;
					currentWeight = 0L;
				} else {
					found = product.next();
					if (found)
						currentWeight = 0L;
				}
			}
			if (!found) {
				completed = active;
				return false;
			}
			positioned = true;
			return true;
		} catch (RuntimeException | Error failure) {
			fail(failure);
			return false;
		}
	}

	@Override
	public long value(int column) {
		if (closed || !positioned)
			throw new IllegalStateException("projection is not positioned");
		if (column < 0 || column >= width || (requestedMasks[active] & (1L << column)) == 0L)
			throw new IllegalArgumentException("column not in current projection");
		if (windowMode && column == windowOutput)
			throw new IllegalStateException("read this column from the current window");
		if (nativeCursor != null)
			return nativeCursor.value(column);
		if (rows != null)
			return rowBuffer[rowIndex * width + column];
		checkEpoch();
		if (windowMode && column == windowOutput)
			throw new IllegalStateException("read this column from the current window");
		return (expanded & (1L << slots[column])) == 0L ? factors.scalar(column) : product.value(slots[column]);
	}

	@Override
	public long multiplicity() {
		if (closed || !positioned)
			throw new IllegalStateException("projection is not positioned");
		if (windowMode)
			throw new IllegalStateException("window weights are per member, not one scalar weight");
		if (!exact[active])
			return 1L;
		if (factors != null)
			checkEpoch();
		if (currentWeight == 0L) {
			if (nativeCursor != null)
				currentWeight = nativeCursor.multiplicity();
			else {
				if (hiddenWeight == 0L)
					hiddenWeight = environment.countProduct(environment.mask() & ~expanded, prefixWeight);
				currentWeight = expanded == 0L ? hiddenWeight
						: Math.multiplyExact(hiddenWeight, product.exactMultiplicity());
			}
		}
		if (currentWeight <= 0L)
			throw new IllegalStateException("nonpositive projection weight");
		return currentWeight;
	}

	private void checkEpoch() {
		if (environment.epoch() != epoch)
			throw new IllegalStateException("relation advanced during projection");
		environment.checkValid();
	}

	private void fail(Throwable failure) {
		KernelRuntime.closeResource(this, failure);
		KernelRuntime.rethrowCloseFailure(failure);
	}

	@Override
	public void close() {
		if (closed)
			return;
		closed = true;
		Throwable failure = KernelRuntime.closeResource(windowReader, null);
		windowReader = null;
		windowBatch = null;
		failure = KernelRuntime.closeResource(product, failure);
		failure = KernelRuntime.closeResource(nativeCursor, failure);
		failure = KernelRuntime.closeResource(factors, failure);
		failure = KernelRuntime.closeResource(rows, failure);
		environment = null;
		KernelRuntime.rethrowCloseFailure(failure);
	}
}
