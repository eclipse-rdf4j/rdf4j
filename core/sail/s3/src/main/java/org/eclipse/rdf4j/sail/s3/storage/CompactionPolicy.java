/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.s3.storage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Determines when compaction should be triggered for a predicate partition. Counts distinct epochs at each level and
 * compares against configurable thresholds.
 */
public class CompactionPolicy {

	/** Default number of L0 epochs before triggering L0→L1 compaction. */
	public static final int DEFAULT_L0_THRESHOLD = 8;

	/** Default number of L1 epochs before triggering L1→L2 compaction. */
	public static final int DEFAULT_L1_THRESHOLD = 4;

	private final int l0Threshold;
	private final int l1Threshold;

	public CompactionPolicy() {
		this(DEFAULT_L0_THRESHOLD, DEFAULT_L1_THRESHOLD);
	}

	public CompactionPolicy(int l0Threshold, int l1Threshold) {
		this.l0Threshold = l0Threshold;
		this.l1Threshold = l1Threshold;
	}

	/**
	 * Checks if compaction should run at the given level.
	 *
	 * @param files all catalog files
	 * @param level the source level (0 or 1)
	 * @return true if the number of distinct epochs at that level >= threshold
	 */
	public boolean shouldCompact(List<Catalog.ParquetFileInfo> files, int level) {
		int threshold = level == 0 ? l0Threshold : l1Threshold;
		return countEpochsAtLevel(files, level) >= threshold;
	}

	private static int countEpochsAtLevel(List<Catalog.ParquetFileInfo> files, int level) {
		Set<Long> epochs = new HashSet<>();
		for (Catalog.ParquetFileInfo f : files) {
			if (f.getLevel() == level) {
				epochs.add(f.getEpoch());
			}
		}
		return epochs.size();
	}

	/**
	 * Returns the files at the given level for a predicate partition.
	 *
	 * @param files all files in the partition
	 * @param level the target level (0, 1, or 2)
	 * @return files at that level
	 */
	public static List<Catalog.ParquetFileInfo> filesAtLevel(List<Catalog.ParquetFileInfo> files, int level) {
		return files.stream().filter(f -> f.getLevel() == level).toList();
	}
}
