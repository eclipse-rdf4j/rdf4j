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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.junit.jupiter.api.Test;

/**
 * Numeric regression contract for packed cardinality math, ported from the superseded hardening plan's log-domain
 * cases: the packed planner uses raw doubles, so its guarantees are saturation to {@code Double.MAX_VALUE} instead of
 * overflow, positive floors instead of underflow to zero, and fail-fast rejection of non-finite or negative boundary
 * costs.
 */
class PackedCostNumericContractTest {

	@Test
	void hugeJoinProductsSaturateInsteadOfOverflowing() {
		assertEquals(Double.MAX_VALUE, PackedJoinEnumerator.joinRows(1e300d, 1e300d, false));
		// Saturation applies before the connected-join selectivity discount, so a saturated product stays
		// saturated rather than silently shrinking back into the representable range.
		assertEquals(Double.MAX_VALUE, PackedJoinEnumerator.joinRows(1e300d, 1e300d, true));
	}

	@Test
	void mixedMagnitudeProductsStayExact() {
		assertEquals(1e64d, PackedJoinEnumerator.joinRows(1e40d, 1e24d, false));
		assertEquals(81.0d, PackedJoinEnumerator.joinRows(9.0d, 9.0d, false), 1e-9d);
		// Sub-row factors are floored at one row, never allowed to shrink the other side.
		assertEquals(1e100d, PackedJoinEnumerator.joinRows(1e100d, 1e-36d, false));
	}

	@Test
	void tinyPositiveInputsKeepPositiveFloors() {
		// Positive underflow must never produce a zero or negative row estimate.
		double connected = PackedJoinEnumerator.joinRows(Double.MIN_VALUE, Double.MIN_VALUE, true);
		double disconnected = PackedJoinEnumerator.joinRows(Double.MIN_VALUE, Double.MIN_VALUE, false);
		assertTrue(connected >= 1.0d, "connected join rows must keep the positive floor: " + connected);
		assertTrue(disconnected >= 1.0d, "disconnected join rows must keep the positive floor: " + disconnected);
	}

	@Test
	void planningResultAcceptsExactZeroRows() {
		PackedPlanningResult result = new PackedPlanningResult(new SingletonSet(), 0.0d, 0.0d, null, false, false,
				0L);
		assertEquals(0.0d, result.outputRows());
		assertEquals(0.0d, result.totalCost());
	}

	@Test
	void planningResultRejectsNonFiniteOrNegativeCosts() {
		SingletonSet plan = new SingletonSet();
		assertThrows(IllegalArgumentException.class,
				() -> new PackedPlanningResult(plan, Double.NaN, 1.0d, null, false, false, 0L));
		assertThrows(IllegalArgumentException.class,
				() -> new PackedPlanningResult(plan, Double.POSITIVE_INFINITY, 1.0d, null, false, false, 0L));
		assertThrows(IllegalArgumentException.class,
				() -> new PackedPlanningResult(plan, -1.0d, 1.0d, null, false, false, 0L));
		assertThrows(IllegalArgumentException.class,
				() -> new PackedPlanningResult(plan, 1.0d, Double.NaN, null, false, false, 0L));
		assertThrows(IllegalArgumentException.class,
				() -> new PackedPlanningResult(plan, 1.0d, -0.5d, null, false, false, 0L));
	}
}
