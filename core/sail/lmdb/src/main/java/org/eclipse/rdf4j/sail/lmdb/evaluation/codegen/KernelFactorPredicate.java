/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation.codegen;

/**
 * Pure ID guards over a binding prefix and bounded factor windows. Both execution tiers use this
 * contract; it has no dependency on IR classes, Values, dictionaries, or query-plan objects.
 * Dependencies and column positions are in the compact scalar-output namespace of the caller,
 * not in the engine-slot namespace. A null window column denotes a scalar prefix value.
 *
 * <p>filter intersects a caller-initialized (-1/0) selection mask; it must not mutate input columns
 * or the prefix. Guard evaluation must be total, deterministic and free of observable effects.
 * A consumer may reorder independent guards and evaluate each physical weighted member once.
 */
public interface KernelFactorPredicate {
	int guardCount();
	long dependencies(int guard);
	boolean test(int guard, long[] prefix);
	void filter(int guard, long[][] columns, long[] prefix, long[] selected, int size);
}
