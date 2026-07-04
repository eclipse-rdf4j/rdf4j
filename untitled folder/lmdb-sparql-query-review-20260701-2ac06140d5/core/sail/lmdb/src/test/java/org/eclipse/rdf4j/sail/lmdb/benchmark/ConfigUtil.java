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

package org.eclipse.rdf4j.sail.lmdb.benchmark;

import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;

/**
 * Creates LMDB store configurations for benchmarking.
 */
final class ConfigUtil {
	private static final String DEFAULT_TRIPLE_INDEXES = "spoc,ospc,psoc,posc";
	private static final String ALL_TRIPLE_INDEXES = "spoc,psoc,sopc,opsc,posc,ospc";
	private static final int THEME_SUBJECT_BUCKET_COUNT = 4096;
	private static final int THEME_PREDICATE_BUCKET_COUNT = 64;
	private static final int THEME_OBJECT_BUCKET_COUNT = 4096;
	private static final int THEME_CONTEXT_BUCKET_COUNT = 16;

	static LmdbStoreConfig createConfig() {
		return createConfig(DEFAULT_TRIPLE_INDEXES);
	}

	static LmdbStoreConfig createAllIndexesConfig() {
		return createConfig(ALL_TRIPLE_INDEXES);
	}

	static LmdbStoreConfig createConfig(String tripleIndexes) {
		LmdbStoreConfig config = new LmdbStoreConfig(tripleIndexes);
		config.setForceSync(false);
		config.setValueDBSize(1_073_741_824L); // 1 GiB
		config.setTripleDBSize(config.getValueDBSize());
		config.setSketchEstimatorSubjectBucketCount(THEME_SUBJECT_BUCKET_COUNT);
		config.setSketchEstimatorPredicateBucketCount(THEME_PREDICATE_BUCKET_COUNT);
		config.setSketchEstimatorObjectBucketCount(THEME_OBJECT_BUCKET_COUNT);
		config.setSketchEstimatorContextBucketCount(THEME_CONTEXT_BUCKET_COUNT);
		config.setSketchEstimatorContextPairSketchesEnabled(false);
		return config;
	}
}
