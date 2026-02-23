/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.config;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;

/**
 * Defines constants for the LmdbStore schema which is used by {@link LmdbStoreFactory}s to initialize
 * {@link LmdbStore}s.
 */
public class LmdbStoreSchema {

	/**
	 * The LmdbStore schema namespace (<tt>http://rdf4j.org/config/sail/lmdb#</tt>).
	 */
	public static final String NAMESPACE = "http://rdf4j.org/config/sail/lmdb#";

	/**
	 * <tt>http://rdf4j.org/config/sail/lmdb#tripleIndexes</tt>
	 */
	public final static IRI TRIPLE_INDEXES;

	/**
	 * <tt>http://rdf4j.org/config/sail/lmdb#tripleDBSize</tt>
	 */
	public final static IRI TRIPLE_DB_SIZE;

	/**
	 * <tt>http://rdf4j.org/config/sail/lmdb#forceSync</tt>
	 */
	public final static IRI FORCE_SYNC;

	/**
	 * <tt>http://rdf4j.org/config/sail/lmdb#valueCacheSize</tt>
	 */
	public final static IRI VALUE_CACHE_SIZE;

	/**
	 * <tt>http://rdf4j.org/config/sail/lmdb#valueDBSize</tt>
	 */
	public final static IRI VALUE_DB_SIZE;

	/**
	 * <tt>http://rdf4j.org/config/sail/lmdb#valueIDCacheSize</tt>
	 */
	public final static IRI VALUE_ID_CACHE_SIZE;

	/**
	 * <tt>http://rdf4j.org/config/sail/lmdb#namespaceCacheSize</tt>
	 */
	public final static IRI NAMESPACE_CACHE_SIZE;

	/**
	 * <tt>http://rdf4j.org/config/sail/lmdb#namespaceIDCacheSize</tt>
	 */
	public final static IRI NAMESPACE_ID_CACHE_SIZE;

	/**
	 * <tt>http://rdf4j.org/config/sail/lmdb#autoGrow</tt>
	 */
	public final static IRI AUTO_GROW;

	/**
	 * <tt>http://rdf4j.org/config/sail/lmdb#pageCardinalityEstimator</tt>
	 */
	public final static IRI PAGE_CARDINALITY_ESTIMATOR;

	/**
	 * <tt>http://rdf4j.org/config/sail/lmdb#valueEvictionInterval</tt>
	 */
	public final static IRI VALUE_EVICTION_INTERVAL;

	static {
		ValueFactory factory = SimpleValueFactory.getInstance();
		TRIPLE_INDEXES = factory.createIRI(NAMESPACE, "tripleIndexes");
		TRIPLE_DB_SIZE = factory.createIRI(NAMESPACE, "tripleDBSize");
		FORCE_SYNC = factory.createIRI(NAMESPACE, "forceSync");
		VALUE_DB_SIZE = factory.createIRI(NAMESPACE, "valueDBSize");
		VALUE_CACHE_SIZE = factory.createIRI(NAMESPACE, "valueCacheSize");
		VALUE_ID_CACHE_SIZE = factory.createIRI(NAMESPACE, "valueIDCacheSize");
		NAMESPACE_CACHE_SIZE = factory.createIRI(NAMESPACE, "namespaceCacheSize");
		NAMESPACE_ID_CACHE_SIZE = factory.createIRI(NAMESPACE, "namespaceIDCacheSize");
		AUTO_GROW = factory.createIRI(NAMESPACE, "autoGrow");
		PAGE_CARDINALITY_ESTIMATOR = factory.createIRI(NAMESPACE, "pageCardinalityEstimator");
		VALUE_EVICTION_INTERVAL = factory.createIRI(NAMESPACE, "valueEvictionInterval");
	}
}
