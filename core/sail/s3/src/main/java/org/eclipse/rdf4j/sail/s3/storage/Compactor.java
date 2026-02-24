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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.sail.s3.cache.TieredCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performs merge compaction on Parquet files within a predicate partition. Merges files at a source level into one set
 * of files at the target level, per sort order.
 *
 * <ul>
 * <li>L0→L1: merge all L0 files per sort order, tombstones preserved</li>
 * <li>L1→L2: merge all L1 files per sort order, tombstones suppressed (L2 = highest level)</li>
 * </ul>
 */
public class Compactor {

	private static final Logger logger = LoggerFactory.getLogger(Compactor.class);
	private static final String[] SORT_ORDERS = { "soc", "osc", "cso" };

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
	 * @param predicateId the predicate partition being compacted
	 * @param sourceFiles all files at the source level in this partition
	 * @param sourceLevel the source level (0 or 1)
	 * @param targetLevel the target level (1 or 2)
	 * @param epoch       the epoch for the new compacted files
	 * @param catalog     the catalog to update
	 * @return result containing new files created and old files removed
	 */
	public CompactionResult compact(long predicateId, List<Catalog.ParquetFileInfo> sourceFiles,
			int sourceLevel, int targetLevel, long epoch, Catalog catalog) {

		boolean suppressTombstones = (targetLevel == 2);
		List<Catalog.ParquetFileInfo> newFiles = new ArrayList<>();
		Set<String> oldKeys = new HashSet<>();

		for (String sortOrder : SORT_ORDERS) {
			// Collect source files for this sort order, ordered newest-first (highest epoch first)
			List<Catalog.ParquetFileInfo> sortOrderFiles = sourceFiles.stream()
					.filter(f -> sortOrder.equals(f.getSortOrder()))
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
				sources.add(new ParquetQuadSource(fileData, sortOrder));
			}

			if (sources.isEmpty()) {
				continue;
			}

			// Merge and collect entries
			List<MemTable.QuadEntry> merged = mergeEntries(sources, suppressTombstones);

			if (merged.isEmpty()) {
				continue;
			}

			// Convert to ParquetFileBuilder.QuadEntry
			List<ParquetFileBuilder.QuadEntry> parquetEntries = new ArrayList<>();
			for (MemTable.QuadEntry e : merged) {
				parquetEntries.add(new ParquetFileBuilder.QuadEntry(e.subject, e.object, e.context, e.flag));
			}

			// Write merged Parquet file
			ParquetSchemas.SortOrder parsedSortOrder = ParquetSchemas.SortOrder.valueOf(sortOrder.toUpperCase());
			String s3Key = "data/predicates/" + predicateId + "/L" + targetLevel + "-"
					+ String.format("%05d", epoch) + "-" + sortOrder + ".parquet";

			byte[] parquetData = ParquetFileBuilder.build(parquetEntries, ParquetSchemas.PARTITIONED_SCHEMA,
					parsedSortOrder, predicateId, rowGroupSize, pageSize);

			objectStore.put(s3Key, parquetData);
			if (cache != null) {
				cache.writeThrough(s3Key, parquetData);
			}

			// Compute stats from sorted entries
			long minSubject = Long.MAX_VALUE, maxSubject = Long.MIN_VALUE;
			long minObject = Long.MAX_VALUE, maxObject = Long.MIN_VALUE;
			long minContext = Long.MAX_VALUE, maxContext = Long.MIN_VALUE;
			for (MemTable.QuadEntry e : merged) {
				minSubject = Math.min(minSubject, e.subject);
				maxSubject = Math.max(maxSubject, e.subject);
				minObject = Math.min(minObject, e.object);
				maxObject = Math.max(maxObject, e.object);
				minContext = Math.min(minContext, e.context);
				maxContext = Math.max(maxContext, e.context);
			}

			newFiles.add(new Catalog.ParquetFileInfo(s3Key, targetLevel, sortOrder, merged.size(),
					epoch, parquetData.length,
					minSubject, maxSubject, minObject, maxObject, minContext, maxContext));
		}

		// Update catalog: remove old files, add new ones
		catalog.removeFiles(predicateId, oldKeys);
		for (Catalog.ParquetFileInfo newFile : newFiles) {
			catalog.addFile(predicateId, newFile);
		}

		// Delete old S3 files and invalidate cache
		for (String key : oldKeys) {
			objectStore.delete(key);
			if (cache != null) {
				cache.invalidate(key);
			}
		}

		logger.info("Compacted predicate {} L{}→L{}: {} files merged into {} files",
				predicateId, sourceLevel, targetLevel, oldKeys.size(), newFiles.size());

		return new CompactionResult(newFiles, oldKeys);
	}

	private List<MemTable.QuadEntry> mergeEntries(List<RawEntrySource> sources, boolean suppressTombstones) {
		List<MemTable.QuadEntry> result = new ArrayList<>();

		// Simple K-way merge: use a priority queue approach
		// Each source is already sorted. We merge them, dedup by key, newest wins.
		// For simplicity, read all into one list then dedup.
		// Since compaction is a background operation, this is acceptable.

		// Use ParquetQuadSource entries directly
		// Sources are ordered newest-first, so for dedup, first occurrence wins
		java.util.TreeMap<CompactKey, MemTable.QuadEntry> deduped = new java.util.TreeMap<>();
		for (RawEntrySource source : sources) {
			while (source.hasNext()) {
				byte[] key = source.peekKey();
				byte flag = source.peekFlag();
				// Only insert if not already present (first = newest wins)
				CompactKey ck = new CompactKey(key);
				if (!deduped.containsKey(ck)) {
					// Decode the key to get quad values
					// The key format from ParquetQuadSource encodes (subject, object, context) as varints
					java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(key);
					long v1 = Varint.readUnsigned(bb);
					long v2 = Varint.readUnsigned(bb);
					long v3 = Varint.readUnsigned(bb);
					if (!suppressTombstones || flag != MemTable.FLAG_TOMBSTONE) {
						deduped.put(ck, new MemTable.QuadEntry(v1, v2, v3, flag));
					}
				}
				source.advance();
			}
		}

		if (suppressTombstones) {
			for (MemTable.QuadEntry e : deduped.values()) {
				if (e.flag != MemTable.FLAG_TOMBSTONE) {
					result.add(e);
				}
			}
		} else {
			result.addAll(deduped.values());
		}

		return result;
	}

	private static class CompactKey implements Comparable<CompactKey> {
		final byte[] key;

		CompactKey(byte[] key) {
			this.key = key.clone();
		}

		@Override
		public int compareTo(CompactKey other) {
			return java.util.Arrays.compareUnsigned(this.key, other.key);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof CompactKey)) {
				return false;
			}
			return java.util.Arrays.equals(key, ((CompactKey) o).key);
		}

		@Override
		public int hashCode() {
			return java.util.Arrays.hashCode(key);
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
