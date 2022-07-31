/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf.config;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;

/**
 * Defines constants for the NativeStore schema which is used by {@link NativeStoreFactory}s to initialize
 * {@link NativeStore}s.
 *
 * @author Arjohn Kampman
 */
public class NativeStoreSchema {

	/** The NativeStore schema namespace (<var>http://www.openrdf.org/config/sail/native#</var>). */
	public static final String NAMESPACE = "http://www.openrdf.org/config/sail/native#";

	/** <var>http://www.openrdf.org/config/sail/native#tripleIndexes</var> */
	public final static IRI TRIPLE_INDEXES;

	/** <var>http://www.openrdf.org/config/sail/native#forceSync</var> */
	public final static IRI FORCE_SYNC;

	/** <var>http://www.openrdf.org/config/sail/native#valueCacheSize</var> */
	public final static IRI VALUE_CACHE_SIZE;

	/** <var>http://www.openrdf.org/config/sail/native#valueIDCacheSize</var> */
	public final static IRI VALUE_ID_CACHE_SIZE;

	/** <var>http://www.openrdf.org/config/sail/native#namespaceCacheSize</var> */
	public final static IRI NAMESPACE_CACHE_SIZE;

	/** <var>http://www.openrdf.org/config/sail/native#namespaceIDCacheSize</var> */
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
