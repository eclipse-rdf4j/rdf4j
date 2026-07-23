/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.optimizer.cascades.packed;

import org.eclipse.rdf4j.model.IRI;

/**
 * Backend-neutral source of stored predicate-object range guarantees for packed planning. A provider translates its
 * store-specific domain facts into the reusable {@link PackedPredicateRange} slot; returning {@code false}, or filling
 * the slot with {@link PackedPredicateRange#STATE_UNKNOWN} or {@link PackedPredicateRange#STATE_UNRESTRICTED}, all mean
 * that no proof is available and planning must not assume anything about the predicate's objects.
 */
public interface PackedPredicateRangeProvider {

	/**
	 * Describes the proven object range of {@code predicate} into {@code output}. The slot has been reset by the caller
	 * before this call.
	 *
	 * @return {@code true} when the slot now carries a usable proof
	 */
	boolean describeObjectRange(IRI predicate, PackedPredicateRange output);

	/**
	 * Version stamp covering the encoding and semantics of the ranges this provider produces, including any store-side
	 * configuration (disabled/excluded predicates) that changes which proofs exist. Cache keys must change whenever
	 * this value changes.
	 */
	long predicateRangeVersion();
}
