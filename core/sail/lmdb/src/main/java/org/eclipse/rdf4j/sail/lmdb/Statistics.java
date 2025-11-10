/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import java.util.Arrays;

/**
 * Helper class for computing cardinalities within triple store.
 */
class Statistics {

	/**
	 * Number of buckets used to sample average distances of keys.
	 */
	static final int MAX_BUCKETS = 3;

	/**
	 * Number of samples for each bucket.
	 */
	static final int MAX_SAMPLES_PER_BUCKET = 100;

	final long[][] startValues = new long[MAX_BUCKETS + 1][4];
	final long[][] lastValues = new long[MAX_BUCKETS][4];
	final long[] values = new long[4];
	final long[] minValues = new long[4];
	final long[] maxValues = new long[4];
	final double[] avgRowsPerValue = new double[4];
	final long[] avgRowsPerValueCounts = new long[4];
	final long[] counts = new long[4];

	void reset() {
		for (long[] startValue : startValues) {
			Arrays.fill(startValue, 0);
		}
		for (long[] lastValue : lastValues) {
			Arrays.fill(lastValue, 0);
		}
		Arrays.fill(values, 0);
		Arrays.fill(minValues, 0);
		Arrays.fill(maxValues, 0);
		Arrays.fill(avgRowsPerValue, 1.0);
		Arrays.fill(avgRowsPerValueCounts, 0);
		Arrays.fill(counts, 0);
	}
}
