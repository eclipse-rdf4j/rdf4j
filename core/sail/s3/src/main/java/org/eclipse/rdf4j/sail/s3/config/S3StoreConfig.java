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
		} catch (ModelException e) {
			throw new SailConfigException(e.getMessage(), e);
		}
	}
}
