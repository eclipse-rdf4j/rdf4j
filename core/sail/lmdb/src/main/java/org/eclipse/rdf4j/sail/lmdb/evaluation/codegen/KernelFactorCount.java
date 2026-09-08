/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation.codegen;

import java.util.Arrays;
import java.util.Objects;
import org.eclipse.rdf4j.sail.lmdb.factor.BorrowedFactorBatch;
import org.eclipse.rdf4j.sail.lmdb.factor.BorrowedTupleBatch;
import org.eclipse.rdf4j.sail.lmdb.factor.FactorEnvironment;

/**
 * Local variable elimination for exact independent grouped relations. Each guard may depend on
 * scalars and at most one relation (a zipped tuple is one relation). Qualifying weights are reduced
 * separately, then multiplied once. A guard joining distinct relations returns UNSUPPORTED before
 * invoking predicates or opening readers; the caller retains its general exact join path.
 *
 * <p>This is a reusable kernel, not a physical join strategy. Sources remain borrowed, unary
 * windows are consumed in place, and correlated tuple columns are staged only in bounded windows.
 * No state scales with degree or the represented Cartesian product. The owner remains the caller.
 */
public final class KernelFactorCount implements AutoCloseable {
	public static final long UNSUPPORTED = -1L;
	private static final int WINDOW = 256;
	private final int[] outputSlots;
	private final long[][] columns;
	private final long[][] tupleColumns;
	private final long[] selection = new long[WINDOW];
	private long[] tupleWeights;
	private final long[] guardGroups = new long[Long.SIZE];
	private final long[] counts = new long[Long.SIZE];
	private final BorrowedFactorBatch[] batches = new BorrowedFactorBatch[Long.SIZE];
	private final BorrowedFactorBatch.Cursor[] readers = new BorrowedFactorBatch.Cursor[Long.SIZE];
	private BorrowedTupleBatch[] tupleBatches;
	private BorrowedTupleBatch.Cursor[] tupleReaders;
	private final int[] tupleSourceColumns;
	private final KernelCancellation cancellation;
	private boolean closed;

	public KernelFactorCount(int[] outputSlots, KernelCancellation cancellation) {
		this.outputSlots = outputSlots.clone();
		if (outputSlots.length > Long.SIZE) throw new IllegalArgumentException("too many output columns");
		for (int slot : outputSlots) Objects.checkIndex(slot, Long.SIZE);
		columns = new long[outputSlots.length][];
		tupleColumns = new long[outputSlots.length][];
		tupleSourceColumns = new int[outputSlots.length];
		this.cancellation = cancellation;
	}

	/**
	 * Returns an exact count, or UNSUPPORTED without consuming any factor. Independent zero results
	 * are resolved before multiplying any summaries, so a later zero annihilates even a huge prefix.
	 */
	public long count(FactorEnvironment environment, long[] prefix, long prefixWeight, KernelFactorPredicate predicate) {
		return count(environment, prefix, prefixWeight, predicate, true);
	}

	/** Existence demand avoids exact enumeration and multiplication after a witness in each group. */
	public long count(FactorEnvironment environment, long[] prefix, long prefixWeight, KernelFactorPredicate predicate,
			boolean exactMultiplicity) {
		if (closed) throw new IllegalStateException("factor counter closed");
		Objects.requireNonNull(environment); Objects.requireNonNull(predicate);
		if (prefix.length != outputSlots.length || prefixWeight <= 0L) throw new IllegalArgumentException("invalid prefix");
		int guards = predicate.guardCount();
		if (guards < 0 || guards > Long.SIZE) throw new IllegalArgumentException("invalid guard count");
		long epoch = environment.epoch();
		environment.checkValid();
		Arrays.fill(guardGroups, 0L);
		long scalarGuards = 0L;
		long selectedGroups = 0L;
		long schema = outputSlots.length == Long.SIZE ? -1L : (1L << outputSlots.length) - 1L;
		for (int guard = 0; guard < guards; guard++) {
			long dependencies = predicate.dependencies(guard);
			if ((dependencies & ~schema) != 0L) throw new IllegalArgumentException("guard dependency outside output schema");
			long related = 0L;
			for (long rest = dependencies; rest != 0L; rest &= rest - 1L) {
				int slot = outputSlots[Long.numberOfTrailingZeros(rest)];
				if ((environment.mask() & (1L << slot)) != 0L) related |= environment.groupMask(slot);
			}
			long leaders = environment.groupLeaders() & related;
			if (Long.bitCount(leaders) > 1) return UNSUPPORTED;
			if (leaders == 0L) scalarGuards |= 1L << guard;
			else {
				int leader = Long.numberOfTrailingZeros(leaders);
				guardGroups[leader] |= 1L << guard;
				selectedGroups |= leaders;
			}
		}
		KernelRuntime.checkCancelled(cancellation);
		for (long rest = scalarGuards; rest != 0L; rest &= rest - 1L)
			if (!predicate.test(Long.numberOfTrailingZeros(rest), prefix)) {
				checkEpoch(environment, epoch); environment.checkValid(); return 0L;
			}
		// Empty mandatory groups annihilate without opening any values or multiplying cardinalities.
		for (long rest = environment.groupLeaders(); rest != 0L; rest &= rest - 1L)
			if (environment.count(Long.numberOfTrailingZeros(rest)) == 0L) return 0L;
		// Establish one witness in every independently filtered branch before completing any of
		// them. Keep each private cursor positioned: the second phase resumes, never rescans.
		// This avoids fully reading a high-degree branch when a sibling rejects the parent.
		boolean witnessesFirst = !exactMultiplicity || Long.bitCount(selectedGroups) > 1;
		for (long rest = selectedGroups; rest != 0L; rest &= rest - 1L) {
			int leader = Long.numberOfTrailingZeros(rest);
			long accepted = reduceGroup(environment, epoch, leader, prefix, predicate, true, witnessesFirst);
			environment.checkValid();
			if (accepted == 0L) return 0L;
			counts[leader] = accepted;
		}
		checkEpoch(environment, epoch);
		environment.checkValid();
		if (!exactMultiplicity) return 1L;
		if (witnessesFirst) {
			for (long rest = selectedGroups; rest != 0L; rest &= rest - 1L) {
				int leader = Long.numberOfTrailingZeros(rest);
				// Disjoint windows of the same exact relation: their sum is source-bounded.
				counts[leader] += reduceGroup(environment, epoch, leader, prefix, predicate, false, false);
			}
			environment.checkValid();
		}
		long result = prefixWeight;
		for (long rest = environment.groupLeaders(); rest != 0L; rest &= rest - 1L) {
			int leader = Long.numberOfTrailingZeros(rest);
			long count = (selectedGroups & (1L << leader)) == 0L ? environment.count(leader) : counts[leader];
			result = Math.multiplyExact(result, count);
		}
		return result;
	}

	private long reduceGroup(FactorEnvironment environment, long epoch, int leader, long[] prefix,
			KernelFactorPredicate predicate, boolean bind, boolean untilWitness) {
		Arrays.fill(columns, null);
		long group = environment.groupMask(leader);
		long total = 0L;
		if (!environment.isTuple(leader)) {
			BorrowedFactorBatch batch = environment.batch(leader);
			if (batches[leader] != batch) {
				if (readers[leader] != null) readers[leader].close();
				readers[leader] = null; batches[leader] = null;
				readers[leader] = batch.cursor(WINDOW); batches[leader] = batch;
			}
			BorrowedFactorBatch.Cursor reader = readers[leader];
			if (bind) reader.bind(environment.lane(leader));
			for (int i = 0; i < outputSlots.length; i++)
				if ((group & (1L << outputSlots[i])) != 0L) columns[i] = reader.windowValues();
			long[] weights = reader.windowWeights();
			int size;
			while (true) {
				checkEpoch(environment, epoch);
				KernelRuntime.checkCancelled(cancellation);
				size = reader.nextWindow();
				if (size == 0) break;
				// Whole-window consumption from a freshly bound private reader always starts at zero.
				if (reader.windowStart() != 0) throw new IllegalStateException("partial private factor window");
				total += selectedWeight(leader, prefix, predicate, weights, size);
				if (untilWitness && total != 0L) break;
			}
		} else {
			if (tupleReaders == null) {
				tupleWeights = new long[WINDOW];
				tupleReaders = new BorrowedTupleBatch.Cursor[Long.SIZE];
				tupleBatches = new BorrowedTupleBatch[Long.SIZE];
			}
			BorrowedTupleBatch batch = environment.tupleBatch(leader);
			if (tupleBatches[leader] != batch) {
				if (tupleReaders[leader] != null) tupleReaders[leader].close();
				tupleReaders[leader] = null; tupleBatches[leader] = null;
				tupleReaders[leader] = batch.cursor(WINDOW); tupleBatches[leader] = batch;
			}
			BorrowedTupleBatch.Cursor reader = tupleReaders[leader];
			if (bind) reader.bind(environment.lane(leader));
			long outputs = 0L;
			for (int i = 0; i < outputSlots.length; i++) {
				if ((group & (1L << outputSlots[i])) == 0L) continue;
				outputs |= 1L << i;
				if (tupleColumns[i] == null) tupleColumns[i] = new long[WINDOW];
				columns[i] = tupleColumns[i];
				tupleSourceColumns[i] = environment.tupleColumn(outputSlots[i]);
			}
			while (true) {
				checkEpoch(environment, epoch);
				KernelRuntime.checkCancelled(cancellation);
				int size = 0;
				while (size < WINDOW && reader.next()) {
					for (long rest = outputs; rest != 0L; rest &= rest - 1L) {
						int output = Long.numberOfTrailingZeros(rest);
						columns[output][size] = reader.value(tupleSourceColumns[output]);
					}
					tupleWeights[size++] = reader.weight();
				}
				if (size == 0) break;
				total += selectedWeight(leader, prefix, predicate, tupleWeights, size);
				if (untilWitness && total != 0L) break;
			}
		}
		checkEpoch(environment, epoch);
		return total;
	}

	private long selectedWeight(int leader, long[] prefix, KernelFactorPredicate predicate, long[] weights, int size) {
		return predicate.sum(guardGroups[leader], columns, prefix, weights, selection, size);
	}

	private static void checkEpoch(FactorEnvironment environment, long epoch) {
		if (environment.epoch() != epoch) throw new IllegalStateException("factor prefix advanced during reduction");
	}

	@Override public void close() {
		if (closed) return;
		closed = true;
		Throwable failure = null;
		for (BorrowedFactorBatch.Cursor reader : readers) if (reader != null) {
			try { reader.close(); } catch (RuntimeException | Error problem) {
				if (failure == null) failure = problem; else if (failure != problem) failure.addSuppressed(problem);
			}
		}
		if (tupleReaders != null) for (BorrowedTupleBatch.Cursor reader : tupleReaders) if (reader != null) {
			try { reader.close(); } catch (RuntimeException | Error problem) {
				if (failure == null) failure = problem; else if (failure != problem) failure.addSuppressed(problem);
			}
		}
		Arrays.fill(columns, null); Arrays.fill(tupleColumns, null); Arrays.fill(batches, null); Arrays.fill(readers, null);
		if (tupleReaders != null) { Arrays.fill(tupleReaders, null); Arrays.fill(tupleBatches, null); }
		if (failure instanceof RuntimeException problem) throw problem;
		if (failure instanceof Error problem) throw problem;
	}
}
