/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.config;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Defines constants for the Sail repository schema which are used to initialize
 * repositories.
 * 
 * @author Arjohn Kampman
 */
public class SailConfigSchema {

	/**
	 * The Sail API schema namespace (
	 * <tt>http://www.openrdf.org/config/sail#</tt>).
	 */
	public static final String NAMESPACE = "http://www.openrdf.org/config/sail#";

	/** <tt>http://www.openrdf.org/config/sail#sailType</tt> */
	public final static IRI SAILTYPE;

	/** <tt>http://www.openrdf.org/config/sail#delegate</tt> */
	public final static IRI DELEGATE;
	
	/** <tt>http://www.openrdf.org/config/sail#iterationCacheSyncTreshold</tt> */
	public final static IRI ITERATION_CACHE_SYNC_THRESHOLD;

	static {
		ValueFactory factory = SimpleValueFactory.getInstance();
		SAILTYPE = factory.createIRI(NAMESPACE, "sailType");
		DELEGATE = factory.createIRI(NAMESPACE, "delegate");
		ITERATION_CACHE_SYNC_THRESHOLD = factory.createIRI(NAMESPACE, "iterationCacheSyncTreshold");
	}
}
