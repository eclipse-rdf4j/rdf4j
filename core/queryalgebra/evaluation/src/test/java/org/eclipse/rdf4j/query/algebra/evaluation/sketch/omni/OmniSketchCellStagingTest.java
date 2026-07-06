/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.query.algebra.evaluation.sketch.omni;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.SplittableRandom;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

/**
 * Differential tests for the staged {@link OmniSketchCell} update path against a reference model of the k smallest
 * distinct unsigned hashes.
 */
class OmniSketchCellStagingTest {

	private static TreeSet<Long> newUnsignedSet() {
		return new TreeSet<>(Long::compareUnsigned);
	}

	private static long[] expectedRetained(TreeSet<Long> seen, int nominalEntries) {
		long[] expected = new long[Math.min(nominalEntries, seen.size())];
		int offset = 0;
		for (long hash : seen) {
			if (offset == expected.length) {
				break;
			}
			expected[offset++] = hash;
		}
		return expected;
	}

	@Test
	void retainedSampleMatchesReferenceModel() {
		for (int nominalEntries : new int[] { 1, 2, 16, 100, 2048 }) {
			SplittableRandom random = new SplittableRandom(nominalEntries * 31L + 7L);
			OmniSketchCell cell = new OmniSketchCell(nominalEntries);
			TreeSet<Long> seen = newUnsignedSet();
			int updates = nominalEntries * 20 + 500;
			for (int i = 0; i < updates; i++) {
				// skewed distribution with plenty of duplicates
				long hash = random.nextLong() >>> random.nextInt(8);
				cell.update(hash);
				seen.add(hash);
				if (i % 777 == 0) {
					// interleaved reads must flush pending staged updates and agree with the model
					assertThat(cell.copyHashes()).isEqualTo(expectedRetained(seen, nominalEntries));
				}
			}
			assertThat(cell.copyHashes()).isEqualTo(expectedRetained(seen, nominalEntries));
			assertThat(cell.getRetainedEntries()).isEqualTo(Math.min(nominalEntries, seen.size()));
			assertThat(cell.getCount()).isEqualTo(updates);
		}
	}

	@Test
	void rejectedUpdatesAreNeverPartOfTheRetainedSample() {
		int nominalEntries = 64;
		SplittableRandom random = new SplittableRandom(42);
		OmniSketchCell cell = new OmniSketchCell(nominalEntries);
		TreeSet<Long> seen = newUnsignedSet();
		for (int i = 0; i < 50_000; i++) {
			long hash = random.nextLong();
			boolean retained = cell.updateAndCheckRetained(hash);
			seen.add(hash);
			if (!retained) {
				// a rejection must only happen for hashes outside the true k smallest distinct set
				long[] expected = expectedRetained(seen, nominalEntries);
				assertThat(Long.compareUnsigned(hash, expected[expected.length - 1]) > 0)
						.as("rejected hash %s must be larger than the kth smallest", hash)
						.isTrue();
			}
		}
		assertThat(cell.copyHashes()).isEqualTo(expectedRetained(seen, nominalEntries));
	}

	@Test
	void mergeWithPendingStagedUpdatesMatchesUnion() {
		int nominalEntries = 128;
		SplittableRandom random = new SplittableRandom(7);
		OmniSketchCell left = new OmniSketchCell(nominalEntries);
		OmniSketchCell right = new OmniSketchCell(nominalEntries);
		TreeSet<Long> union = newUnsignedSet();
		for (int i = 0; i < 5_000; i++) {
			long leftHash = random.nextLong();
			long rightHash = random.nextLong();
			left.update(leftHash);
			right.update(rightHash);
			union.add(leftHash);
			union.add(rightHash);
		}
		// merge without any prior read, so both cells still hold staged updates
		left.merge(right);
		assertThat(left.copyHashes()).isEqualTo(expectedRetained(union, nominalEntries));
		assertThat(left.getCount()).isEqualTo(10_000);
	}

	@Test
	void zeroNominalEntriesRetainsNothing() {
		OmniSketchCell cell = new OmniSketchCell(0);
		assertThat(cell.updateAndCheckRetained(123L)).isFalse();
		assertThat(cell.copyHashes()).isEmpty();
		assertThat(cell.getCount()).isEqualTo(1L);
	}
}
