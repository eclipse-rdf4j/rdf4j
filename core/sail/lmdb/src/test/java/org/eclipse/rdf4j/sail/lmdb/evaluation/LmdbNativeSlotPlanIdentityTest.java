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
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Regression coverage for unused-VALUES elimination preserving a Janino-lowerable flat join. */
class LmdbNativeSlotPlanIdentityTest {

	@Test
	void singletonIsARealRelationalIdentity() {
		SlotPlan values = SlotPlan.values(new ValuesRow[] {
				new ValuesRow(new int[] { 0 }, new long[] { 11L })
		});

		assertThat(SlotPlan.join(SlotPlan.singleton(), values)).isSameAs(values);
		assertThat(SlotPlan.join(values, SlotPlan.singleton())).isSameAs(values);
	}

	@Test
	void eliminatingUnusedValuesDoesNotLeaveANestedJoinBarrier() {
		SlotPlan left = SlotPlan.values(new ValuesRow[] {
				new ValuesRow(new int[] { 0 }, new long[] { 11L })
		});
		SlotPlan right = SlotPlan.values(new ValuesRow[] {
				new ValuesRow(new int[] { 1 }, new long[] { 22L })
		});

		SlotPlan plan = SlotPlan.join(left, SlotPlan.join(right, SlotPlan.singleton()));

		assertThat(plan).isInstanceOf(MultiJoinPlan.class);
		assertThat(((MultiJoinPlan) plan).children)
				.containsExactly(left, right)
				.doesNotContain(SingletonPlan.INSTANCE);
	}
	@Test
	void orderingAndAttemptBorrowCannotResurrectTheIdentityBarrier() {
		SlotPlan values = SlotPlan.values(new ValuesRow[] {
				new ValuesRow(new int[] { 0 }, new long[] { 11L })
		});
		SlotPlan stale = new JoinPlan(SingletonPlan.INSTANCE, values);

		NativeTupleDistinctPlan ordered = LmdbNativeOrderPlanner.tuple(stale, new int[] { 0 }, null);
		assertThat(ordered.arg).isSameAs(values);

		NativeFilterLease lease = new NativeFilterLease();
		try {
			assertThat(lease.borrow(stale)).isSameAs(values);
		} finally {
			lease.discard();
		}
	}

}
