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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON-serialized catalog tracking Parquet files with per-file statistics and predicate partitioning.
 *
 * <p>
 * Evolved from {@link Manifest} to support the Parquet-based storage format with predicate partitioning. Each predicate
 * ID maps to a list of {@link ParquetFileInfo} entries describing the Parquet files for that partition.
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
 *   "version": 2,
 *   "epoch": 42,
 *   "nextValueId": 12345,
 *   "predicatePartitions": {
 *     "7": [ { file info... } ],
 *     "42": [ { file info... } ]
 *   },
 *   "predicateLabels": { "7": "http://www.w3.org/1999/02/22-rdf-syntax-ns#type" },
 *   "unpartitionedFiles": []
 * }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Catalog {

	@JsonProperty("version")
	private int version = 2;

	@JsonProperty("epoch")
	private long epoch;

	@JsonProperty("nextValueId")
	private long nextValueId;

	@JsonProperty("predicatePartitions")
	private Map<String, List<ParquetFileInfo>> predicatePartitions = new LinkedHashMap<>();

	@JsonProperty("predicateLabels")
	private Map<String, String> predicateLabels = new LinkedHashMap<>();

	@JsonProperty("unpartitionedFiles")
	private List<ParquetFileInfo> unpartitionedFiles = new ArrayList<>();

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

	public Map<String, List<ParquetFileInfo>> getPredicatePartitions() {
		return predicatePartitions;
	}

	public void setPredicatePartitions(Map<String, List<ParquetFileInfo>> predicatePartitions) {
		this.predicatePartitions = predicatePartitions;
	}

	public Map<String, String> getPredicateLabels() {
		return predicateLabels;
	}

	public void setPredicateLabels(Map<String, String> predicateLabels) {
		this.predicateLabels = predicateLabels;
	}

	public List<ParquetFileInfo> getUnpartitionedFiles() {
		return unpartitionedFiles;
	}

	public void setUnpartitionedFiles(List<ParquetFileInfo> unpartitionedFiles) {
		this.unpartitionedFiles = unpartitionedFiles;
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
		byte[] pointer = store.get("catalog/current");
		if (pointer == null) {
			return new Catalog();
		}
		String catalogKey = "catalog/" + new String(pointer, StandardCharsets.UTF_8).trim();
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
			store.put("catalog/" + versionedKey, json);
			store.put("catalog/current", versionedKey.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to save catalog", e);
		}
	}

	/**
	 * Returns the set of predicate IDs that have partitioned files.
	 *
	 * @return set of predicate IDs parsed from the partition keys
	 */
	public Set<Long> getPredicateIds() {
		return predicatePartitions.keySet()
				.stream()
				.map(Long::parseLong)
				.collect(Collectors.toSet());
	}

	/**
	 * Returns the list of Parquet files for the given predicate ID.
	 *
	 * @param predicateId the predicate value ID
	 * @return the list of file info entries, or an empty list if no files exist for this predicate
	 */
	public List<ParquetFileInfo> getFilesForPredicate(long predicateId) {
		return predicatePartitions.getOrDefault(String.valueOf(predicateId), Collections.emptyList());
	}

	/**
	 * Adds a Parquet file to the partition for the given predicate.
	 *
	 * @param predicateId the predicate value ID
	 * @param info        the file info to add
	 */
	public void addFile(long predicateId, ParquetFileInfo info) {
		predicatePartitions.computeIfAbsent(String.valueOf(predicateId), k -> new ArrayList<>()).add(info);
	}

	/**
	 * Removes Parquet files from the partition for the given predicate by their S3 keys.
	 *
	 * @param predicateId the predicate value ID
	 * @param s3Keys      the set of S3 keys to remove
	 */
	public void removeFiles(long predicateId, Set<String> s3Keys) {
		List<ParquetFileInfo> files = predicatePartitions.get(String.valueOf(predicateId));
		if (files != null) {
			files.removeIf(f -> s3Keys.contains(f.getS3Key()));
		}
	}

	/**
	 * Metadata about a single Parquet file in the catalog, including its location, sort order, size, and min/max
	 * statistics for subject, object, and context columns.
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
				long epoch, long sizeBytes,
				long minSubject, long maxSubject,
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
