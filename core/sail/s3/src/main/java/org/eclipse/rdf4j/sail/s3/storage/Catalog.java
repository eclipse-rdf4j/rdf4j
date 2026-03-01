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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON-serialized catalog tracking Parquet files with per-file statistics.
 *
 * <p>
 * All quads are stored in flat files (no predicate partitioning). Each file has statistics including min/max for all
 * four quad components (subject, predicate, object, context).
 *
 * <h3>S3 Layout</h3>
 *
 * <pre>
 * catalog/current             -> plain text "v{epoch}.json"
 * catalog/v{epoch}.json       -> JSON catalog
 * </pre>
 *
 * <h3>JSON Structure</h3>
 *
 * <pre>
 * {
 *   "version": 3,
 *   "epoch": 42,
 *   "nextValueId": 12345,
 *   "files": [ { file info... } ]
 * }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Catalog {

	static final String CATALOG_POINTER_KEY = "catalog/current";
	private static final String CATALOG_DIR = "catalog/";

	@JsonProperty("version")
	private int version = 3;

	@JsonProperty("epoch")
	private long epoch;

	@JsonProperty("nextValueId")
	private long nextValueId;

	@JsonProperty("files")
	private volatile List<ParquetFileInfo> files = new ArrayList<>();

	public Catalog() {
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public long getEpoch() {
		return epoch;
	}

	public void setEpoch(long epoch) {
		this.epoch = epoch;
	}

	public long getNextValueId() {
		return nextValueId;
	}

	public void setNextValueId(long nextValueId) {
		this.nextValueId = nextValueId;
	}

	public List<ParquetFileInfo> getFiles() {
		return files;
	}

	public void setFiles(List<ParquetFileInfo> files) {
		this.files = new ArrayList<>(files);
	}

	/**
	 * Loads the catalog from the object store.
	 *
	 * <p>
	 * Reads the {@code catalog/current} pointer to find the active catalog version, then parses the corresponding JSON
	 * file. Returns an empty catalog if no pointer or catalog file exists.
	 *
	 * @param store  the object store to read from
	 * @param mapper the Jackson ObjectMapper for JSON parsing
	 * @return the loaded catalog, or an empty catalog if none exists
	 */
	public static Catalog load(ObjectStore store, ObjectMapper mapper) {
		byte[] pointer = store.get(CATALOG_POINTER_KEY);
		if (pointer == null) {
			return new Catalog();
		}
		String catalogKey = CATALOG_DIR + new String(pointer, StandardCharsets.UTF_8).trim();
		byte[] json = store.get(catalogKey);
		if (json == null) {
			return new Catalog();
		}
		try {
			return mapper.readValue(json, Catalog.class);
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to parse catalog", e);
		}
	}

	/**
	 * Saves this catalog to the object store.
	 *
	 * <p>
	 * Writes the catalog JSON to {@code catalog/v{epoch}.json} and updates the {@code catalog/current} pointer. The
	 * epoch field is set to the given value before saving.
	 *
	 * @param store  the object store to write to
	 * @param mapper the Jackson ObjectMapper for JSON serialization
	 * @param epoch  the epoch number for this catalog version
	 */
	public void save(ObjectStore store, ObjectMapper mapper, long epoch) {
		this.epoch = epoch;
		try {
			String versionedKey = "v" + epoch + ".json";
			byte[] json = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(this);
			store.put(CATALOG_DIR + versionedKey, json);
			store.put(CATALOG_POINTER_KEY, versionedKey.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to save catalog", e);
		}
	}

	/**
	 * Adds a Parquet file to the catalog. Copy-on-write for thread safety.
	 *
	 * @param info the file info to add
	 */
	public void addFile(ParquetFileInfo info) {
		List<ParquetFileInfo> updated = new ArrayList<>(files);
		updated.add(info);
		files = updated;
	}

	/**
	 * Removes Parquet files by their S3 keys. Copy-on-write for thread safety.
	 *
	 * @param s3Keys the set of S3 keys to remove
	 */
	public void removeFiles(Set<String> s3Keys) {
		List<ParquetFileInfo> updated = new ArrayList<>(files);
		updated.removeIf(f -> s3Keys.contains(f.getS3Key()));
		files = updated;
	}

	/**
	 * Returns all files for the given sort order. Reads from a volatile snapshot so it is safe to call without external
	 * synchronization.
	 *
	 * @param sortOrder the sort order suffix (e.g. "spoc", "opsc", "cspo")
	 * @return list of files matching the sort order
	 */
	public List<ParquetFileInfo> getFilesForSortOrder(String sortOrder) {
		List<ParquetFileInfo> snapshot = files;
		List<ParquetFileInfo> result = new ArrayList<>();
		for (ParquetFileInfo f : snapshot) {
			if (sortOrder.equals(f.getSortOrder())) {
				result.add(f);
			}
		}
		return result;
	}

	/**
	 * Generates the S3 key for a data file at the given level, epoch, and sort suffix.
	 */
	public static String dataKey(int level, long epoch, String sortSuffix) {
		return "data/L" + level + "-" + String.format("%05d", epoch) + "-" + sortSuffix + ".parquet";
	}

	/**
	 * Metadata about a single Parquet file in the catalog, including its location, sort order, size, and min/max
	 * statistics for subject, predicate, object, and context columns.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ParquetFileInfo {

		@JsonProperty("s3Key")
		private String s3Key;

		@JsonProperty("level")
		private int level;

		@JsonProperty("sortOrder")
		private String sortOrder;

		@JsonProperty("rowCount")
		private long rowCount;

		@JsonProperty("epoch")
		private long epoch;

		@JsonProperty("sizeBytes")
		private long sizeBytes;

		@JsonProperty("minSubject")
		private long minSubject;

		@JsonProperty("maxSubject")
		private long maxSubject;

		@JsonProperty("minPredicate")
		private long minPredicate;

		@JsonProperty("maxPredicate")
		private long maxPredicate;

		@JsonProperty("minObject")
		private long minObject;

		@JsonProperty("maxObject")
		private long maxObject;

		@JsonProperty("minContext")
		private long minContext;

		@JsonProperty("maxContext")
		private long maxContext;

		public ParquetFileInfo() {
		}

		public ParquetFileInfo(String s3Key, int level, String sortOrder, long rowCount,
				long epoch, long sizeBytes, QuadStats stats) {
			this(s3Key, level, sortOrder, rowCount, epoch, sizeBytes,
					stats.minSubject, stats.maxSubject, stats.minPredicate, stats.maxPredicate,
					stats.minObject, stats.maxObject, stats.minContext, stats.maxContext);
		}

		ParquetFileInfo(String s3Key, int level, String sortOrder, long rowCount,
				long epoch, long sizeBytes,
				long minSubject, long maxSubject,
				long minPredicate, long maxPredicate,
				long minObject, long maxObject,
				long minContext, long maxContext) {
			this.s3Key = s3Key;
			this.level = level;
			this.sortOrder = sortOrder;
			this.rowCount = rowCount;
			this.epoch = epoch;
			this.sizeBytes = sizeBytes;
			this.minSubject = minSubject;
			this.maxSubject = maxSubject;
			this.minPredicate = minPredicate;
			this.maxPredicate = maxPredicate;
			this.minObject = minObject;
			this.maxObject = maxObject;
			this.minContext = minContext;
			this.maxContext = maxContext;
		}

		public String getS3Key() {
			return s3Key;
		}

		public void setS3Key(String s3Key) {
			this.s3Key = s3Key;
		}

		public int getLevel() {
			return level;
		}

		public void setLevel(int level) {
			this.level = level;
		}

		public String getSortOrder() {
			return sortOrder;
		}

		public void setSortOrder(String sortOrder) {
			this.sortOrder = sortOrder;
		}

		public long getRowCount() {
			return rowCount;
		}

		public void setRowCount(long rowCount) {
			this.rowCount = rowCount;
		}

		public long getEpoch() {
			return epoch;
		}

		public void setEpoch(long epoch) {
			this.epoch = epoch;
		}

		public long getSizeBytes() {
			return sizeBytes;
		}

		public void setSizeBytes(long sizeBytes) {
			this.sizeBytes = sizeBytes;
		}

		public long getMinSubject() {
			return minSubject;
		}

		public void setMinSubject(long minSubject) {
			this.minSubject = minSubject;
		}

		public long getMaxSubject() {
			return maxSubject;
		}

		public void setMaxSubject(long maxSubject) {
			this.maxSubject = maxSubject;
		}

		public long getMinPredicate() {
			return minPredicate;
		}

		public void setMinPredicate(long minPredicate) {
			this.minPredicate = minPredicate;
		}

		public long getMaxPredicate() {
			return maxPredicate;
		}

		public void setMaxPredicate(long maxPredicate) {
			this.maxPredicate = maxPredicate;
		}

		public long getMinObject() {
			return minObject;
		}

		public void setMinObject(long minObject) {
			this.minObject = minObject;
		}

		public long getMaxObject() {
			return maxObject;
		}

		public void setMaxObject(long maxObject) {
			this.maxObject = maxObject;
		}

		public long getMinContext() {
			return minContext;
		}

		public void setMinContext(long minContext) {
			this.minContext = minContext;
		}

		public long getMaxContext() {
			return maxContext;
		}

		public void setMaxContext(long maxContext) {
			this.maxContext = maxContext;
		}
	}
}
