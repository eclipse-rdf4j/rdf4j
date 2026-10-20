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

/** Regression coverage for cost-admitting merge SIP instead of opening a key cursor eagerly. */
class LmdbNativeAdjacencySemijoinAdmissionTest {

	@Test
	void allHitSampleNeverPaysForAKeyCursor() {
		AdjacencyIntersectionProbe.StreamAdmission admission = new AdjacencyIntersectionProbe.StreamAdmission(4, 2, 25,
				16, 0);
		CountingAdjacency adjacency = new CountingAdjacency(10_000L);

		for (int i = 0; i < 8; i++) {
			long key = i + 1L;
			admission.observe(adjacency, key, 1);
			assertThat(admission.shouldStream(adjacency, key + 1L)).isFalse();
		}

		assertThat(adjacency.keyCursorOpens).isZero();
	}

	@Test
	void selectiveMonotoneSampleAdmitsStreamingOnlyAfterMeasuredBenefit() {
		AdjacencyIntersectionProbe.StreamAdmission admission = new AdjacencyIntersectionProbe.StreamAdmission(4, 2, 25,
				16, 0);
		CountingAdjacency adjacency = new CountingAdjacency(10_000L);

		admission.observe(adjacency, 1L, 1);
		admission.observe(adjacency, 2L, 0);
		admission.observe(adjacency, 3L, 1);
		assertThat(admission.shouldStream(adjacency, 4L)).isFalse();
		admission.observe(adjacency, 4L, 0);

		assertThat(admission.shouldStream(adjacency, 5L)).isTrue();
	}

	@Test
	void longDenseMonotoneStreamAdmitsOnlyAfterSetupCanBeAmortized() {
		AdjacencyIntersectionProbe.StreamAdmission admission = new AdjacencyIntersectionProbe.StreamAdmission(4, 2, 25,
				6, 0);
		CountingAdjacency adjacency = new CountingAdjacency(10_000L);

		for (int i = 0; i < 5; i++) {
			admission.observe(adjacency, i + 1L, 1);
			assertThat(admission.shouldStream(adjacency, i + 2L)).isFalse();
		}
		admission.observe(adjacency, 6L, 1);

		assertThat(admission.shouldStream(adjacency, 7L)).isTrue();
	}

	@Test
	void smallPartitionKeepsThePageLocalPointLookup() {
		AdjacencyIntersectionProbe.StreamAdmission admission = new AdjacencyIntersectionProbe.StreamAdmission(4, 2, 25,
				6, 32);
		CountingAdjacency adjacency = new CountingAdjacency(16L);

		for (int i = 0; i < 8; i++) {
			admission.observe(adjacency, i + 1L, 0);
		}

		assertThat(admission.shouldStream(adjacency, 9L)).isFalse();
	}

	@Test
	void backwardsOuterOrderRefusesTheForwardMerge() {
		AdjacencyIntersectionProbe.StreamAdmission admission = new AdjacencyIntersectionProbe.StreamAdmission(4, 2, 25,
				16, 0);
		CountingAdjacency adjacency = new CountingAdjacency(10_000L);

		admission.observe(adjacency, 10L, 0);
		admission.observe(adjacency, 11L, 0);
		admission.observe(adjacency, 12L, 1);
		admission.observe(adjacency, 13L, 1);
		assertThat(admission.shouldStream(adjacency, 14L)).isTrue();
		assertThat(admission.shouldStream(adjacency, 9L)).isFalse();
	}

	private static final class CountingAdjacency implements NativeLmdbQuerySource.NativeAdjacency {
		private final long keyCount;
		private int keyCursorOpens;

		private CountingAdjacency(long keyCount) {
			this.keyCount = keyCount;
		}

		@Override
		public long find(long key) {
			return key > 0 && key <= keyCount ? key : NOT_FOUND;
		}

		@Override
		public long size(long runHandle) {
			return runHandle > 0 ? 1L : 0L;
		}

		@Override
		public long neighborAt(long runHandle, long runOffset) {
			return runHandle;
		}

		@Override
		public long contextAt(long runHandle, long runOffset) {
			return 0L;
		}

		@Override
		public boolean supportsKeyEnumeration() {
			return true;
		}

		@Override
		public long keyCount() {
			return keyCount;
		}

		@Override
		public KeyRunCursor openKeyRunCursor(long fromOrdinal, long toOrdinal) {
			keyCursorOpens++;
			return null;
		}

		@Override
		public void close() {
		}
	}
}
