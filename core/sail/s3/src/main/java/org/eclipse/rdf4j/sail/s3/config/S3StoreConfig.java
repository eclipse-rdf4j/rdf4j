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

import java.util.function.Consumer;

import org.eclipse.rdf4j.model.IRI;
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
	 * The default memtable size (64 MiB).
	 */
	public static final long DEFAULT_MEM_TABLE_SIZE = 67_108_864;

	/**
	 * The default memory cache size (256 MiB).
	 */
	public static final long DEFAULT_MEMORY_CACHE_SIZE = 268_435_456;

	/**
	 * The default disk cache size (10 GiB).
	 */
	public static final long DEFAULT_DISK_CACHE_SIZE = 10_737_418_240L;

	private long memTableSize = -1;

	private long memoryCacheSize = -1;

	private long diskCacheSize = -1;

	private String diskCachePath;

	private String s3Bucket;

	private String s3Endpoint;

	private String s3Region;

	private String s3Prefix;

	private String s3AccessKey;

	private String s3SecretKey;

	private Boolean s3ForcePathStyle;

	private String dataDir;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public S3StoreConfig() {
		super(S3StoreFactory.SAIL_TYPE);
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Resolves a configuration value from (in order): environment variable, system property, or null.
	 *
	 * <p>
	 * S3 connection settings are shared at the RDF4J instance level so that multiple S3 SAIL repositories can share the
	 * same bucket. Each repository differentiates itself via {@code s3Prefix}.
	 * </p>
	 *
	 * <p>
	 * Environment variables: {@code RDF4J_S3_BUCKET}, {@code RDF4J_S3_ENDPOINT}, {@code RDF4J_S3_REGION},
	 * {@code RDF4J_S3_ACCESS_KEY}, {@code RDF4J_S3_SECRET_KEY}, {@code RDF4J_S3_FORCE_PATH_STYLE}.
	 * </p>
	 */
	private static String resolveEnv(String envVar, String sysProp) {
		String val = System.getenv(envVar);
		if (val != null && !val.isEmpty()) {
			return val;
		}
		val = System.getProperty(sysProp);
		if (val != null && !val.isEmpty()) {
			return val;
		}
		return null;
	}

	private static String resolveField(String field, String envVar, String sysProp) {
		return field != null ? field : resolveEnv(envVar, sysProp);
	}

	public long getMemTableSize() {
		return memTableSize >= 0 ? memTableSize : DEFAULT_MEM_TABLE_SIZE;
	}

	public S3StoreConfig setMemTableSize(long memTableSize) {
		this.memTableSize = memTableSize;
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

	public String getS3Bucket() {
		return resolveField(s3Bucket, "RDF4J_S3_BUCKET", "rdf4j.s3.bucket");
	}

	public S3StoreConfig setS3Bucket(String s3Bucket) {
		this.s3Bucket = s3Bucket;
		return this;
	}

	public String getS3Endpoint() {
		return resolveField(s3Endpoint, "RDF4J_S3_ENDPOINT", "rdf4j.s3.endpoint");
	}

	public S3StoreConfig setS3Endpoint(String s3Endpoint) {
		this.s3Endpoint = s3Endpoint;
		return this;
	}

	public String getS3Region() {
		String resolved = resolveField(s3Region, "RDF4J_S3_REGION", "rdf4j.s3.region");
		return resolved != null ? resolved : "us-east-1";
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
		return resolveField(s3AccessKey, "RDF4J_S3_ACCESS_KEY", "rdf4j.s3.accessKey");
	}

	public S3StoreConfig setS3AccessKey(String s3AccessKey) {
		this.s3AccessKey = s3AccessKey;
		return this;
	}

	public String getS3SecretKey() {
		return resolveField(s3SecretKey, "RDF4J_S3_SECRET_KEY", "rdf4j.s3.secretKey");
	}

	public S3StoreConfig setS3SecretKey(String s3SecretKey) {
		this.s3SecretKey = s3SecretKey;
		return this;
	}

	public boolean isS3ForcePathStyle() {
		if (s3ForcePathStyle != null) {
			return s3ForcePathStyle;
		}
		String env = resolveEnv("RDF4J_S3_FORCE_PATH_STYLE", "rdf4j.s3.forcePathStyle");
		return env == null || Boolean.parseBoolean(env);
	}

	public S3StoreConfig setS3ForcePathStyle(boolean s3ForcePathStyle) {
		this.s3ForcePathStyle = s3ForcePathStyle;
		return this;
	}

	public boolean isS3Configured() {
		return getS3Bucket() != null && !getS3Bucket().isEmpty();
	}

	public String getDataDir() {
		return resolveField(dataDir, "RDF4J_S3_DATA_DIR", "rdf4j.s3.dataDir");
	}

	public S3StoreConfig setDataDir(String dataDir) {
		this.dataDir = dataDir;
		return this;
	}

	@Override
	public Resource export(Model m) {
		Resource implNode = super.export(m);
		ValueFactory vf = SimpleValueFactory.getInstance();

		m.setNamespace("s3", S3StoreSchema.NAMESPACE);
		exportLong(m, implNode, vf, S3StoreSchema.MEM_TABLE_SIZE, memTableSize);
		exportLong(m, implNode, vf, S3StoreSchema.MEMORY_CACHE_SIZE, memoryCacheSize);
		exportLong(m, implNode, vf, S3StoreSchema.DISK_CACHE_SIZE, diskCacheSize);
		exportString(m, implNode, vf, S3StoreSchema.DISK_CACHE_PATH, diskCachePath);
		exportString(m, implNode, vf, S3StoreSchema.S3_BUCKET, s3Bucket);
		exportString(m, implNode, vf, S3StoreSchema.S3_ENDPOINT, s3Endpoint);
		exportString(m, implNode, vf, S3StoreSchema.S3_REGION, s3Region);
		exportString(m, implNode, vf, S3StoreSchema.S3_PREFIX, s3Prefix);
		exportString(m, implNode, vf, S3StoreSchema.S3_ACCESS_KEY, s3AccessKey);
		exportString(m, implNode, vf, S3StoreSchema.S3_SECRET_KEY, s3SecretKey);
		if (s3ForcePathStyle != null) {
			m.add(implNode, S3StoreSchema.S3_FORCE_PATH_STYLE, vf.createLiteral(s3ForcePathStyle));
		}
		exportString(m, implNode, vf, S3StoreSchema.DATA_DIR, dataDir);
		return implNode;
	}

	private static void exportString(Model m, Resource node, ValueFactory vf, IRI prop, String value) {
		if (value != null) {
			m.add(node, prop, vf.createLiteral(value));
		}
	}

	private static void exportLong(Model m, Resource node, ValueFactory vf, IRI prop, long value) {
		if (value >= 0) {
			m.add(node, prop, vf.createLiteral(value));
		}
	}

	@Override
	public void parse(Model m, Resource implNode) throws SailConfigException {
		super.parse(m, implNode);

		try {
			parseLong(m, implNode, S3StoreSchema.MEM_TABLE_SIZE, this::setMemTableSize);
			parseLong(m, implNode, S3StoreSchema.MEMORY_CACHE_SIZE, this::setMemoryCacheSize);
			parseLong(m, implNode, S3StoreSchema.DISK_CACHE_SIZE, this::setDiskCacheSize);
			parseString(m, implNode, S3StoreSchema.DISK_CACHE_PATH, this::setDiskCachePath);
			parseString(m, implNode, S3StoreSchema.S3_BUCKET, this::setS3Bucket);
			parseString(m, implNode, S3StoreSchema.S3_ENDPOINT, this::setS3Endpoint);
			parseString(m, implNode, S3StoreSchema.S3_REGION, this::setS3Region);
			parseString(m, implNode, S3StoreSchema.S3_PREFIX, this::setS3Prefix);
			parseString(m, implNode, S3StoreSchema.S3_ACCESS_KEY, this::setS3AccessKey);
			parseString(m, implNode, S3StoreSchema.S3_SECRET_KEY, this::setS3SecretKey);
			Models.objectLiteral(m.getStatements(implNode, S3StoreSchema.S3_FORCE_PATH_STYLE, null))
					.ifPresent(lit -> setS3ForcePathStyle(lit.booleanValue()));
			parseString(m, implNode, S3StoreSchema.DATA_DIR, this::setDataDir);
		} catch (ModelException e) {
			throw new SailConfigException(e.getMessage(), e);
		}
	}

	private static void parseString(Model m, Resource node, IRI prop, Consumer<String> setter) {
		Models.objectLiteral(m.getStatements(node, prop, null))
				.ifPresent(lit -> setter.accept(lit.getLabel()));
	}

	private static void parseLong(Model m, Resource node, IRI prop, Consumer<Long> setter) {
		Models.objectLiteral(m.getStatements(node, prop, null))
				.ifPresent(lit -> {
					try {
						setter.accept(lit.longValue());
					} catch (NumberFormatException e) {
						throw new SailConfigException(
								"Long value required for " + prop + " property, found " + lit);
					}
				});
	}

}
