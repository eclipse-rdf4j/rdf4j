/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.config;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;

/**
 * Defines constants for the LmdbStore schema which is used by {@link LmdbStoreFactory}s to initialize
 * {@link LmdbStore}s.
 *
 * @author Arjohn Kampman
 */
public class LmdbStoreSchema {

	/** The LmdbStore schema namespace (<tt>http://www.openrdf.org/config/sail/lmdb#</tt>). */
	public static final String NAMESPACE = "http://www.openrdf.org/config/sail/lmdb#";

	/** <tt>http://www.openrdf.org/config/sail/lmdb#tripleIndexes</tt> */
	public final static IRI TRIPLE_INDEXES;

	/** <tt>http://www.openrdf.org/config/sail/lmdb#forceSync</tt> */
	public final static IRI FORCE_SYNC;

	/** <tt>http://www.openrdf.org/config/sail/lmdb#valueCacheSize</tt> */
	public final static IRI VALUE_CACHE_SIZE;

	/** <tt>http://www.openrdf.org/config/sail/lmdb#valueIDCacheSize</tt> */
	public final static IRI VALUE_ID_CACHE_SIZE;

	/** <tt>http://www.openrdf.org/config/sail/lmdb#namespaceCacheSize</tt> */
	public final static IRI NAMESPACE_CACHE_SIZE;

	/** <tt>http://www.openrdf.org/config/sail/lmdb#namespaceIDCacheSize</tt> */
	public final static IRI NAMESPACE_ID_CACHE_SIZE;

	static {
		ValueFactory factory = SimpleValueFactory.getInstance();
		TRIPLE_INDEXES = factory.createIRI(NAMESPACE, "tripleIndexes");
		FORCE_SYNC = factory.createIRI(NAMESPACE, "forceSync");
		VALUE_CACHE_SIZE = factory.createIRI(NAMESPACE, "valueCacheSize");
		VALUE_ID_CACHE_SIZE = factory.createIRI(NAMESPACE, "valueIDCacheSize");
		NAMESPACE_CACHE_SIZE = factory.createIRI(NAMESPACE, "namespaceCacheSize");
		NAMESPACE_ID_CACHE_SIZE = factory.createIRI(NAMESPACE, "namespaceIDCacheSize");
	}
}
