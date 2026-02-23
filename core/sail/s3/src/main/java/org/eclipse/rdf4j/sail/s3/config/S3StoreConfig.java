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
package org.eclipse.rdf4j.sail.s3.config;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.sail.base.config.BaseSailConfig;
import org.eclipse.rdf4j.sail.config.SailConfigException;

/**
 * Configuration for S3-backed SAIL store.
 */
public class S3StoreConfig extends BaseSailConfig {

	/**
	 * The default quad indexes.
	 */
	public static final String DEFAULT_QUAD_INDEXES = "spoc,posc";

	/**
	 * The default memtable size (64 MiB).
	 */
	public static final long DEFAULT_MEM_TABLE_SIZE = 67_108_864;

	/**
	 * The default block size (4 MiB).
	 */
	public static final int DEFAULT_BLOCK_SIZE = 4_194_304;

	/**
	 * The default memory cache size (256 MiB).
	 */
	public static final long DEFAULT_MEMORY_CACHE_SIZE = 268_435_456;

	/**
	 * The default disk cache size (10 GiB).
	 */
	public static final long DEFAULT_DISK_CACHE_SIZE = 10_737_418_240L;

	/**
	 * The default value cache size.
	 */
	public static final int DEFAULT_VALUE_CACHE_SIZE = 512;

	/**
	 * The default value id cache size.
	 */
	public static final int DEFAULT_VALUE_ID_CACHE_SIZE = 128;

	private String quadIndexes;

	private long memTableSize = -1;

	private int blockSize = -1;

	private long memoryCacheSize = -1;

	private long diskCacheSize = -1;

	private String diskCachePath;

	private int valueCacheSize = -1;

	private int valueIdCacheSize = -1;

	private String s3Bucket;

	private String s3Endpoint;

	private String s3Region;

	private String s3Prefix;

	private String s3AccessKey;

	private String s3SecretKey;

	private boolean s3ForcePathStyle = true;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public S3StoreConfig() {
		super(S3StoreFactory.SAIL_TYPE);
	}

	public S3StoreConfig(String quadIndexes) {
		this();
		setQuadIndexes(quadIndexes);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public String getQuadIndexes() {
		return quadIndexes != null ? quadIndexes : DEFAULT_QUAD_INDEXES;
	}

	public S3StoreConfig setQuadIndexes(String quadIndexes) {
		this.quadIndexes = quadIndexes;
		return this;
	}

	public long getMemTableSize() {
		return memTableSize >= 0 ? memTableSize : DEFAULT_MEM_TABLE_SIZE;
	}

	public S3StoreConfig setMemTableSize(long memTableSize) {
		this.memTableSize = memTableSize;
		return this;
	}

	public int getBlockSize() {
		return blockSize >= 0 ? blockSize : DEFAULT_BLOCK_SIZE;
	}

	public S3StoreConfig setBlockSize(int blockSize) {
		this.blockSize = blockSize;
		return this;
	}

	public long getMemoryCacheSize() {
		return memoryCacheSize >= 0 ? memoryCacheSize : DEFAULT_MEMORY_CACHE_SIZE;
	}

	public S3StoreConfig setMemoryCacheSize(long memoryCacheSize) {
		this.memoryCacheSize = memoryCacheSize;
		return this;
	}

	public long getDiskCacheSize() {
		return diskCacheSize >= 0 ? diskCacheSize : DEFAULT_DISK_CACHE_SIZE;
	}

	public S3StoreConfig setDiskCacheSize(long diskCacheSize) {
		this.diskCacheSize = diskCacheSize;
		return this;
	}

	public String getDiskCachePath() {
		return diskCachePath;
	}

	public S3StoreConfig setDiskCachePath(String diskCachePath) {
		this.diskCachePath = diskCachePath;
		return this;
	}

	public int getValueCacheSize() {
		return valueCacheSize >= 0 ? valueCacheSize : DEFAULT_VALUE_CACHE_SIZE;
	}

	public S3StoreConfig setValueCacheSize(int valueCacheSize) {
		this.valueCacheSize = valueCacheSize;
		return this;
	}

	public int getValueIdCacheSize() {
		return valueIdCacheSize >= 0 ? valueIdCacheSize : DEFAULT_VALUE_ID_CACHE_SIZE;
	}

	public S3StoreConfig setValueIdCacheSize(int valueIdCacheSize) {
		this.valueIdCacheSize = valueIdCacheSize;
		return this;
	}

	public String getS3Bucket() {
		return s3Bucket;
	}

	public S3StoreConfig setS3Bucket(String s3Bucket) {
		this.s3Bucket = s3Bucket;
		return this;
	}

	public String getS3Endpoint() {
		return s3Endpoint;
	}

	public S3StoreConfig setS3Endpoint(String s3Endpoint) {
		this.s3Endpoint = s3Endpoint;
		return this;
	}

	public String getS3Region() {
		return s3Region != null ? s3Region : "us-east-1";
	}

	public S3StoreConfig setS3Region(String s3Region) {
		this.s3Region = s3Region;
		return this;
	}

	public String getS3Prefix() {
		return s3Prefix != null ? s3Prefix : "";
	}

	public S3StoreConfig setS3Prefix(String s3Prefix) {
		this.s3Prefix = s3Prefix;
		return this;
	}

	public String getS3AccessKey() {
		return s3AccessKey;
	}

	public S3StoreConfig setS3AccessKey(String s3AccessKey) {
		this.s3AccessKey = s3AccessKey;
		return this;
	}

	public String getS3SecretKey() {
		return s3SecretKey;
	}

	public S3StoreConfig setS3SecretKey(String s3SecretKey) {
		this.s3SecretKey = s3SecretKey;
		return this;
	}

	public boolean isS3ForcePathStyle() {
		return s3ForcePathStyle;
	}

	public S3StoreConfig setS3ForcePathStyle(boolean s3ForcePathStyle) {
		this.s3ForcePathStyle = s3ForcePathStyle;
		return this;
	}

	public boolean isS3Configured() {
		return s3Bucket != null && !s3Bucket.isEmpty();
	}

	@Override
	public Resource export(Model m) {
		Resource implNode = super.export(m);
		ValueFactory vf = SimpleValueFactory.getInstance();

		m.setNamespace("s3", S3StoreSchema.NAMESPACE);
		if (quadIndexes != null) {
			m.add(implNode, S3StoreSchema.QUAD_INDEXES, vf.createLiteral(quadIndexes));
		}
		if (memTableSize >= 0) {
			m.add(implNode, S3StoreSchema.MEM_TABLE_SIZE, vf.createLiteral(memTableSize));
		}
		if (blockSize >= 0) {
			m.add(implNode, S3StoreSchema.BLOCK_SIZE, vf.createLiteral(blockSize));
		}
		if (memoryCacheSize >= 0) {
			m.add(implNode, S3StoreSchema.MEMORY_CACHE_SIZE, vf.createLiteral(memoryCacheSize));
		}
		if (diskCacheSize >= 0) {
			m.add(implNode, S3StoreSchema.DISK_CACHE_SIZE, vf.createLiteral(diskCacheSize));
		}
		if (diskCachePath != null) {
			m.add(implNode, S3StoreSchema.DISK_CACHE_PATH, vf.createLiteral(diskCachePath));
		}
		if (valueCacheSize >= 0) {
			m.add(implNode, S3StoreSchema.VALUE_CACHE_SIZE, vf.createLiteral(valueCacheSize));
		}
		if (valueIdCacheSize >= 0) {
			m.add(implNode, S3StoreSchema.VALUE_ID_CACHE_SIZE, vf.createLiteral(valueIdCacheSize));
		}
		if (s3Bucket != null) {
			m.add(implNode, S3StoreSchema.S3_BUCKET, vf.createLiteral(s3Bucket));
		}
		if (s3Endpoint != null) {
			m.add(implNode, S3StoreSchema.S3_ENDPOINT, vf.createLiteral(s3Endpoint));
		}
		if (s3Region != null) {
			m.add(implNode, S3StoreSchema.S3_REGION, vf.createLiteral(s3Region));
		}
		if (s3Prefix != null) {
			m.add(implNode, S3StoreSchema.S3_PREFIX, vf.createLiteral(s3Prefix));
		}
		if (s3AccessKey != null) {
			m.add(implNode, S3StoreSchema.S3_ACCESS_KEY, vf.createLiteral(s3AccessKey));
		}
		if (s3SecretKey != null) {
			m.add(implNode, S3StoreSchema.S3_SECRET_KEY, vf.createLiteral(s3SecretKey));
		}
		m.add(implNode, S3StoreSchema.S3_FORCE_PATH_STYLE, vf.createLiteral(s3ForcePathStyle));
		return implNode;
	}

	@Override
	public void parse(Model m, Resource implNode) throws SailConfigException {
		super.parse(m, implNode);

		try {
			Models.objectLiteral(m.getStatements(implNode, S3StoreSchema.QUAD_INDEXES, null))
					.ifPresent(lit -> setQuadIndexes(lit.getLabel()));

			Models.objectLiteral(m.getStatements(implNode, S3StoreSchema.MEM_TABLE_SIZE, null))
					.ifPresent(lit -> {
						try {
							setMemTableSize(lit.longValue());
						} catch (NumberFormatException e) {
							throw new SailConfigException(
									"Long value required for " + S3StoreSchema.MEM_TABLE_SIZE
											+ " property, found " + lit);
						}
					});

			Models.objectLiteral(m.getStatements(implNode, S3StoreSchema.BLOCK_SIZE, null))
					.ifPresent(lit -> {
						try {
							setBlockSize(lit.intValue());
						} catch (NumberFormatException e) {
							throw new SailConfigException(
									"Integer value required for " + S3StoreSchema.BLOCK_SIZE
											+ " property, found " + lit);
						}
					});

			Models.objectLiteral(m.getStatements(implNode, S3StoreSchema.MEMORY_CACHE_SIZE, null))
					.ifPresent(lit -> {
						try {
							setMemoryCacheSize(lit.longValue());
						} catch (NumberFormatException e) {
							throw new SailConfigException(
									"Long value required for " + S3StoreSchema.MEMORY_CACHE_SIZE
											+ " property, found " + lit);
						}
					});

			Models.objectLiteral(m.getStatements(implNode, S3StoreSchema.DISK_CACHE_SIZE, null))
					.ifPresent(lit -> {
						try {
							setDiskCacheSize(lit.longValue());
						} catch (NumberFormatException e) {
							throw new SailConfigException(
									"Long value required for " + S3StoreSchema.DISK_CACHE_SIZE
											+ " property, found " + lit);
						}
					});

			Models.objectLiteral(m.getStatements(implNode, S3StoreSchema.DISK_CACHE_PATH, null))
					.ifPresent(lit -> setDiskCachePath(lit.getLabel()));

			Models.objectLiteral(m.getStatements(implNode, S3StoreSchema.VALUE_CACHE_SIZE, null))
					.ifPresent(lit -> {
						try {
							setValueCacheSize(lit.intValue());
						} catch (NumberFormatException e) {
							throw new SailConfigException(
									"Integer value required for " + S3StoreSchema.VALUE_CACHE_SIZE
											+ " property, found " + lit);
						}
					});

			Models.objectLiteral(m.getStatements(implNode, S3StoreSchema.VALUE_ID_CACHE_SIZE, null))
					.ifPresent(lit -> {
						try {
							setValueIdCacheSize(lit.intValue());
						} catch (NumberFormatException e) {
							throw new SailConfigException(
									"Integer value required for " + S3StoreSchema.VALUE_ID_CACHE_SIZE
											+ " property, found " + lit);
						}
					});
			Models.objectLiteral(m.getStatements(implNode, S3StoreSchema.S3_BUCKET, null))
					.ifPresent(lit -> setS3Bucket(lit.getLabel()));

			Models.objectLiteral(m.getStatements(implNode, S3StoreSchema.S3_ENDPOINT, null))
					.ifPresent(lit -> setS3Endpoint(lit.getLabel()));

			Models.objectLiteral(m.getStatements(implNode, S3StoreSchema.S3_REGION, null))
					.ifPresent(lit -> setS3Region(lit.getLabel()));

			Models.objectLiteral(m.getStatements(implNode, S3StoreSchema.S3_PREFIX, null))
					.ifPresent(lit -> setS3Prefix(lit.getLabel()));

			Models.objectLiteral(m.getStatements(implNode, S3StoreSchema.S3_ACCESS_KEY, null))
					.ifPresent(lit -> setS3AccessKey(lit.getLabel()));

			Models.objectLiteral(m.getStatements(implNode, S3StoreSchema.S3_SECRET_KEY, null))
					.ifPresent(lit -> setS3SecretKey(lit.getLabel()));

			Models.objectLiteral(m.getStatements(implNode, S3StoreSchema.S3_FORCE_PATH_STYLE, null))
					.ifPresent(lit -> setS3ForcePathStyle(lit.booleanValue()));
		} catch (ModelException e) {
			throw new SailConfigException(e.getMessage(), e);
		}
	}
}
