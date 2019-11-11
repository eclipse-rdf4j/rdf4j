/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearchstore.config;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Defines constants for the ElasticsearchStore schema which is used by {@link ElasticsearchStoreFactory}s to initialize
 * {@link ElasticsearchStore}s.
 *
 * @author Håvard Mikkelsen Ottestad
 */
public class ElasticsearchStoreSchema {

	/** The ElasticsearchStore schema namespace (<code>http://rdf4j.org/config/sail/elasticsearchstore#</code>). */
	public static final String NAMESPACE = "http://rdf4j.org/config/sail/elasticsearchstore#";

//	/** <tt>http://www.openrdf.org/config/sail/memory#persist</tt> */
//	public final static IRI PERSIST;
//
//	/** <tt>http://www.openrdf.org/config/sail/memory#syncDelay</tt> */
//	public final static IRI SYNC_DELAY;

	static {
		ValueFactory factory = SimpleValueFactory.getInstance();
//		PERSIST = factory.createIRI(NAMESPACE, "persist");
//		SYNC_DELAY = factory.createIRI(NAMESPACE, "syncDelay");
	}
}
