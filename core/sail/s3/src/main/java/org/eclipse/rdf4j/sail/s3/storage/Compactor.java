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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.rdf4j.sail.s3.cache.TieredCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performs merge compaction on Parquet files. Merges files at a source level into one set of files at the target level,
 * per sort order.
 *
 * <ul>
 * <li>L0→L1: merge all L0 files per sort order, tombstones preserved</li>
 * <li>L1→L2: merge all L1 files per sort order, tombstones suppressed (L2 = highest level)</li>
 * </ul>
 */
public class Compactor {

	private static final Logger logger = LoggerFactory.getLogger(Compactor.class);
	private static final ParquetSchemas.SortOrder[] SORT_ORDERS = ParquetSchemas.SortOrder.values();

	private final ObjectStore objectStore;
	private final TieredCache cache;
	private final int rowGroupSize;
	private final int pageSize;

	public Compactor(ObjectStore objectStore, TieredCache cache, int rowGroupSize, int pageSize) {
		this.objectStore = objectStore;
		this.cache = cache;
		this.rowGroupSize = rowGroupSize;
		this.pageSize = pageSize;
	}

	/**
	 * Compacts files at the source level into a single set of files at the target level.
	 *
	 * @param sourceFiles all files at the source level
	 * @param sourceLevel the source level (0 or 1)
	 * @param targetLevel the target level (1 or 2)
	 * @param epoch       the epoch for the new compacted files
	 * @param catalog     the catalog to update
	 * @return result containing new files created and old files removed
	 */
	public CompactionResult compact(List<Catalog.ParquetFileInfo> sourceFiles,
			int sourceLevel, int targetLevel, long epoch, Catalog catalog) {

		boolean suppressTombstones = (targetLevel == 2);
		List<Catalog.ParquetFileInfo> newFiles = new ArrayList<>();
		Set<String> oldKeys = new HashSet<>();

		for (ParquetSchemas.SortOrder sortOrder : SORT_ORDERS) {
			String suffix = sortOrder.suffix();
			QuadIndex quadIndex = new QuadIndex(suffix);

			// Collect source files for this sort order, ordered newest-first (highest epoch first)
			List<Catalog.ParquetFileInfo> sortOrderFiles = sourceFiles.stream()
					.filter(f -> suffix.equals(f.getSortOrder()))
					.sorted(Comparator.comparingLong(Catalog.ParquetFileInfo::getEpoch).reversed())
					.toList();

			if (sortOrderFiles.isEmpty()) {
				continue;
			}

			// Collect old keys for cleanup
			for (Catalog.ParquetFileInfo f : sortOrderFiles) {
				oldKeys.add(f.getS3Key());
			}

			// Build merge sources from Parquet files (newest first)
			List<RawEntrySource> sources = new ArrayList<>();
			for (Catalog.ParquetFileInfo fileInfo : sortOrderFiles) {
				byte[] fileData = cache != null ? cache.get(fileInfo.getS3Key()) : objectStore.get(fileInfo.getS3Key());
				if (fileData == null) {
					logger.warn("Missing Parquet file during compaction: {}", fileInfo.getS3Key());
					continue;
				}
				sources.add(new ParquetQuadSource(fileData, quadIndex));
			}

			if (sources.isEmpty()) {
				continue;
			}

			// Merge and collect entries
			List<QuadEntry> merged = mergeEntries(sources, quadIndex, suppressTombstones);

			if (merged.isEmpty()) {
				continue;
			}

			// Write merged Parquet file
			String s3Key = "data/L" + targetLevel + "-"
					+ String.format("%05d", epoch) + "-" + suffix + ".parquet";

			byte[] parquetData = ParquetFileBuilder.build(merged, ParquetSchemas.QUAD_SCHEMA,
					sortOrder, rowGroupSize, pageSize);

			objectStore.put(s3Key, parquetData);
			if (cache != null) {
				cache.writeThrough(s3Key, parquetData);
			}

			QuadStats stats = QuadStats.fromEntries(merged);
			newFiles.add(new Catalog.ParquetFileInfo(s3Key, targetLevel, suffix, merged.size(),
					epoch, parquetData.length, stats));
		}

		// Update catalog in memory: remove old files, add new ones.
		// Physical deletion of old files is deferred to the caller, after the catalog is saved,
		// to prevent data loss if the process crashes between deletion and catalog save.
		catalog.removeFiles(oldKeys);
		for (Catalog.ParquetFileInfo newFile : newFiles) {
			catalog.addFile(newFile);
		}

		logger.info("Compacted L{}→L{}: {} files merged into {} files",
				sourceLevel, targetLevel, oldKeys.size(), newFiles.size());

		return new CompactionResult(newFiles, oldKeys);
	}

	private List<QuadEntry> mergeEntries(List<RawEntrySource> sources, QuadIndex quadIndex,
			boolean suppressTombstones) {
		// Sources are ordered newest-first, so for dedup, first occurrence wins
		TreeMap<CompactKey, QuadEntry> deduped = new TreeMap<>();
		for (RawEntrySource source : sources) {
			while (source.hasNext()) {
				byte[] key = source.peekKey();
				byte flag = source.peekFlag();
				CompactKey ck = new CompactKey(key);
				if (!deduped.containsKey(ck)) {
					long[] quad = new long[4];
					quadIndex.keyToQuad(key, quad);
					if (!suppressTombstones || flag != MemTable.FLAG_TOMBSTONE) {
						deduped.put(ck, new QuadEntry(
								quad[QuadIndex.SUBJ_IDX], quad[QuadIndex.PRED_IDX],
								quad[QuadIndex.OBJ_IDX], quad[QuadIndex.CONTEXT_IDX], flag));
					}
				}
				source.advance();
			}
		}

		return new ArrayList<>(deduped.values());
	}

	private static class CompactKey implements Comparable<CompactKey> {
		final byte[] key;

		CompactKey(byte[] key) {
			this.key = key.clone();
		}

		@Override
		public int compareTo(CompactKey other) {
			return Arrays.compareUnsigned(this.key, other.key);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof CompactKey)) {
				return false;
			}
			return Arrays.equals(key, ((CompactKey) o).key);
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(key);
		}
	}

	/**
	 * Result of a compaction operation.
	 */
	public static class CompactionResult {
		private final List<Catalog.ParquetFileInfo> newFiles;
		private final Set<String> deletedKeys;

		public CompactionResult(List<Catalog.ParquetFileInfo> newFiles, Set<String> deletedKeys) {
			this.newFiles = newFiles;
			this.deletedKeys = deletedKeys;
		}

		public List<Catalog.ParquetFileInfo> getNewFiles() {
			return newFiles;
		}

		public Set<String> getDeletedKeys() {
			return deletedKeys;
		}
	}
}
