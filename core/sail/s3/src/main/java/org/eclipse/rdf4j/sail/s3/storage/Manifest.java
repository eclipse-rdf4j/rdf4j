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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON manifest tracking which SSTables exist in the object store.
 *
 * <h3>S3 Layout</h3>
 *
 * <pre>
 * manifest/current           -> plain text "v{epoch}.json"
 * manifest/v{epoch}.json     -> JSON manifest
 * sstables/L0-{epoch}-{indexName}.sst
 * values/current             -> serialized value store
 * namespaces/current         -> JSON namespace map
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Manifest {

	@JsonProperty("version")
	private int version = 1;

	@JsonProperty("nextValueId")
	private long nextValueId;

	@JsonProperty("sstables")
	private List<SSTableInfo> sstables = new ArrayList<>();

	public Manifest() {
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public long getNextValueId() {
		return nextValueId;
	}

	public void setNextValueId(long nextValueId) {
		this.nextValueId = nextValueId;
	}

	public List<SSTableInfo> getSstables() {
		return sstables;
	}

	public void setSstables(List<SSTableInfo> sstables) {
		this.sstables = sstables;
	}

	public static Manifest load(ObjectStore store, ObjectMapper mapper) {
		byte[] pointer = store.get("manifest/current");
		if (pointer == null) {
			return new Manifest();
		}
		String manifestKey = "manifest/" + new String(pointer, StandardCharsets.UTF_8).trim();
		byte[] json = store.get(manifestKey);
		if (json == null) {
			return new Manifest();
		}
		try {
			return mapper.readValue(json, Manifest.class);
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to parse manifest", e);
		}
	}

	public void save(ObjectStore store, ObjectMapper mapper, long epoch) {
		try {
			String versionedKey = "v" + epoch + ".json";
			byte[] json = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(this);
			store.put("manifest/" + versionedKey, json);
			store.put("manifest/current", versionedKey.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to save manifest", e);
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class SSTableInfo {

		@JsonProperty("s3Key")
		private String s3Key;

		@JsonProperty("level")
		private int level;

		@JsonProperty("indexName")
		private String indexName;

		@JsonProperty("minKeyHex")
		private String minKeyHex;

		@JsonProperty("maxKeyHex")
		private String maxKeyHex;

		@JsonProperty("entryCount")
		private long entryCount;

		@JsonProperty("epoch")
		private long epoch;

		public SSTableInfo() {
		}

		public SSTableInfo(String s3Key, int level, String indexName, String minKeyHex, String maxKeyHex,
				long entryCount, long epoch) {
			this.s3Key = s3Key;
			this.level = level;
			this.indexName = indexName;
			this.minKeyHex = minKeyHex;
			this.maxKeyHex = maxKeyHex;
			this.entryCount = entryCount;
			this.epoch = epoch;
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

		public String getIndexName() {
			return indexName;
		}

		public void setIndexName(String indexName) {
			this.indexName = indexName;
		}

		public String getMinKeyHex() {
			return minKeyHex;
		}

		public void setMinKeyHex(String minKeyHex) {
			this.minKeyHex = minKeyHex;
		}

		public String getMaxKeyHex() {
			return maxKeyHex;
		}

		public void setMaxKeyHex(String maxKeyHex) {
			this.maxKeyHex = maxKeyHex;
		}

		public long getEntryCount() {
			return entryCount;
		}

		public void setEntryCount(long entryCount) {
			this.entryCount = entryCount;
		}

		public long getEpoch() {
			return epoch;
		}

		public void setEpoch(long epoch) {
			this.epoch = epoch;
		}
	}
}
