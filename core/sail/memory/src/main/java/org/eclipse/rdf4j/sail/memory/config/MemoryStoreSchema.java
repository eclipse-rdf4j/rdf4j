/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory.config;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

/**
 * Defines constants for the MemoryStore schema which is used by {@link MemoryStoreFactory}s to initialize
 * {@link MemoryStore}s.
 *
 * @author Arjohn Kampman
 */
public class MemoryStoreSchema {

	/** The MemoryStore schema namespace (<tt>http://www.openrdf.org/config/sail/memory#</tt>). */
	public static final String NAMESPACE = "http://www.openrdf.org/config/sail/memory#";

	/** <tt>http://www.openrdf.org/config/sail/memory#persist</tt> */
	public final static IRI PERSIST;

	/** <tt>http://www.openrdf.org/config/sail/memory#syncDelay</tt> */
	public final static IRI SYNC_DELAY;

	static {
		ValueFactory factory = SimpleValueFactory.getInstance();
		PERSIST = factory.createIRI(NAMESPACE, "persist");
		SYNC_DELAY = factory.createIRI(NAMESPACE, "syncDelay");
	}
}
