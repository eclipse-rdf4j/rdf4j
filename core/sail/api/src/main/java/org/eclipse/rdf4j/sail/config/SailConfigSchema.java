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
package org.eclipse.rdf4j.sail.config;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.CONFIG;

/**
 * Defines constants for the Sail repository schema which are used to initialize repositories.
 *
 * @author Arjohn Kampman
 * @deprecated use {@link CONFIG} instead.
 */
@Deprecated(since = "4.3.0", forRemoval = true)
public class SailConfigSchema {

	public static final String NAMESPACE = CONFIG.NAMESPACE;

	/**
	 * The (Obsolete) Sail API schema namespace ( <var>http://www.openrdf.org/config/sail#</var>).
	 */
	public static final String NAMESPACE_OBSOLETE = "http://www.openrdf.org/config/sail#";

	public final static IRI SAILTYPE = CONFIG.sailType;

	public final static IRI DELEGATE = CONFIG.delegate;

	public final static IRI ITERATION_CACHE_SYNC_THRESHOLD = CONFIG.iterationCacheSyncThreshold;

	public final static IRI CONNECTION_TIME_OUT = CONFIG.connectionTimeOut;

}
