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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Defines constants for the S3Store schema which is used by {@link S3StoreFactory}s to initialize S3Stores.
 */
public class S3StoreSchema {

	/**
	 * The S3Store schema namespace (<tt>http://rdf4j.org/config/sail/s3#</tt>).
	 */
	public static final String NAMESPACE = "http://rdf4j.org/config/sail/s3#";

	/**
	 * <tt>http://rdf4j.org/config/sail/s3#quadIndexes</tt>
	 */
	public final static IRI QUAD_INDEXES;

	/**
	 * <tt>http://rdf4j.org/config/sail/s3#memTableSize</tt>
	 */
	public final static IRI MEM_TABLE_SIZE;

	/**
	 * <tt>http://rdf4j.org/config/sail/s3#blockSize</tt>
	 */
	public final static IRI BLOCK_SIZE;

	/**
	 * <tt>http://rdf4j.org/config/sail/s3#memoryCacheSize</tt>
	 */
	public final static IRI MEMORY_CACHE_SIZE;

	/**
	 * <tt>http://rdf4j.org/config/sail/s3#diskCacheSize</tt>
	 */
	public final static IRI DISK_CACHE_SIZE;

	/**
	 * <tt>http://rdf4j.org/config/sail/s3#diskCachePath</tt>
	 */
	public final static IRI DISK_CACHE_PATH;

	/**
	 * <tt>http://rdf4j.org/config/sail/s3#valueCacheSize</tt>
	 */
	public final static IRI VALUE_CACHE_SIZE;

	/**
	 * <tt>http://rdf4j.org/config/sail/s3#valueIdCacheSize</tt>
	 */
	public final static IRI VALUE_ID_CACHE_SIZE;

	public final static IRI S3_BUCKET;

	public final static IRI S3_ENDPOINT;

	public final static IRI S3_REGION;

	public final static IRI S3_PREFIX;

	public final static IRI S3_ACCESS_KEY;

	public final static IRI S3_SECRET_KEY;

	public final static IRI S3_FORCE_PATH_STYLE;

	/**
	 * <tt>http://rdf4j.org/config/sail/s3#dataDir</tt>
	 */
	public final static IRI DATA_DIR;

	static {
		ValueFactory factory = SimpleValueFactory.getInstance();
		QUAD_INDEXES = factory.createIRI(NAMESPACE, "quadIndexes");
		MEM_TABLE_SIZE = factory.createIRI(NAMESPACE, "memTableSize");
		BLOCK_SIZE = factory.createIRI(NAMESPACE, "blockSize");
		MEMORY_CACHE_SIZE = factory.createIRI(NAMESPACE, "memoryCacheSize");
		DISK_CACHE_SIZE = factory.createIRI(NAMESPACE, "diskCacheSize");
		DISK_CACHE_PATH = factory.createIRI(NAMESPACE, "diskCachePath");
		VALUE_CACHE_SIZE = factory.createIRI(NAMESPACE, "valueCacheSize");
		VALUE_ID_CACHE_SIZE = factory.createIRI(NAMESPACE, "valueIdCacheSize");
		S3_BUCKET = factory.createIRI(NAMESPACE, "s3Bucket");
		S3_ENDPOINT = factory.createIRI(NAMESPACE, "s3Endpoint");
		S3_REGION = factory.createIRI(NAMESPACE, "s3Region");
		S3_PREFIX = factory.createIRI(NAMESPACE, "s3Prefix");
		S3_ACCESS_KEY = factory.createIRI(NAMESPACE, "s3AccessKey");
		S3_SECRET_KEY = factory.createIRI(NAMESPACE, "s3SecretKey");
		S3_FORCE_PATH_STYLE = factory.createIRI(NAMESPACE, "s3ForcePathStyle");
		DATA_DIR = factory.createIRI(NAMESPACE, "dataDir");
	}
}
