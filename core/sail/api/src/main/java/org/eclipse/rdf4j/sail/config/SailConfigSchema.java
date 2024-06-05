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

import static org.eclipse.rdf4j.model.util.Values.iri;

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

	/**
	 * The (Obsolete) Sail API schema namespace ( <var>http://www.openrdf.org/config/sail#</var>).
	 */
	public static final String NAMESPACE = "http://www.openrdf.org/config/sail#";

	/**
	 * @deprecated use {@link CONFIG.Sail#type} instead.
	 */
	public final static IRI SAILTYPE = iri(NAMESPACE, "sailType");

	/**
	 * @deprecated use {@link CONFIG#delegate} instead.
	 */
	public final static IRI DELEGATE = iri(NAMESPACE, "delegate");

	/**
	 * @deprecated use {@link CONFIG.Sail#iterationCacheSyncThreshold} instead.
	 */
	public final static IRI ITERATION_CACHE_SYNC_THRESHOLD = iri(NAMESPACE, "iterationCacheSyncThreshold");

	/**
	 * @deprecated use {@link CONFIG.Sail#connectionTimeOut} instead.
	 */
	public final static IRI CONNECTION_TIME_OUT = iri(NAMESPACE, "connectionTimeOut");
}
