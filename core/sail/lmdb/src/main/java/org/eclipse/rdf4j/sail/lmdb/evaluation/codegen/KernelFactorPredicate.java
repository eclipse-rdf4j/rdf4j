/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation.codegen;

/**
 * Pure ID guards over a binding prefix and bounded factor windows. Both execution tiers use this contract; it has no
 * dependency on IR classes, Values, dictionaries, or query-plan objects. Dependencies and column positions are in the
 * compact scalar-output namespace of the caller, not in the engine-slot namespace. A null window column denotes a
 * scalar prefix value.
 *
 * <p>
 * filter intersects a caller-initialized (-1/0) selection mask; it must not mutate input columns or the prefix. Guard
 * evaluation must be total, deterministic and free of observable effects. A consumer may reorder independent guards and
 * evaluate each physical weighted member once.
 */
public interface KernelFactorPredicate {
	int guardCount();

	long dependencies(int guard);

	boolean test(int guard, long[] prefix);

	void filter(int guard, long[][] columns, long[] prefix, long[] selected, int size);

	/**
	 * Reduce one exact relation window after intersecting the selected pure guards. A compiled predicate may fuse this
	 * into a shape-specific loop without constructing a selection mask. The default deliberately retains the shared
	 * vector primitives for interpreted predicates, large guard graphs, and unsupported runtime layouts. Weights obey
	 * sumBounded's source bound; this contract does not permit unchecked multiplication or global aggregate
	 * accumulation. The selected array is reusable scratch only; its contents after this call are unspecified.
	 */
	default long sum(long guards, long[][] columns, long[] prefix, long[] weights, long[] selected, int size) {
		return sumGeneric(this, guards, columns, prefix, weights, selected, size);
	}

	/** Non-virtual fallback for generated classes; avoids relying on interface-super syntax. */
	static long sumGeneric(KernelFactorPredicate predicate, long guards, long[][] columns,
			long[] prefix, long[] weights, long[] selected, int size) {
		java.util.Arrays.fill(selected, 0, size, -1L);
		for (long rest = guards; rest != 0L; rest &= rest - 1L)
			predicate.filter(Long.numberOfTrailingZeros(rest), columns, prefix, selected, size);
		return KernelIdMasks.sumBounded(weights, selected, size);
	}
}
