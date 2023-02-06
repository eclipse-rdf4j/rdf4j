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
package org.eclipse.rdf4j.repository.contextaware.config;

import static org.eclipse.rdf4j.model.util.Values.iri;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.CONFIG;

/**
 * @author James Leigh
 *
 * @deprecated use {@link CONFIG} instead.
 */
@Deprecated(since = "4.3.0", forRemoval = true)
public class ContextAwareSchema {

	/**
	 * The obsolete ContextAwareRepository schema namespace (
	 * <var>http://www.openrdf.org/config/repository/contextaware#</var>).
	 */
	public static final String NAMESPACE_OBSOLETE = "http://www.openrdf.org/config/repository/contextaware#";

	public final static IRI INCLUDE_INFERRED = CONFIG.INCLUDE_INFERRED;

	public final static IRI MAX_QUERY_TIME = CONFIG.MAX_QUERY_TIME;

	public final static IRI QUERY_LANGUAGE = CONFIG.QUERY_LANGUAGE;

	public final static IRI BASE_URI = CONFIG.BASE_URI;

	public final static IRI READ_CONTEXT = CONFIG.READ_CONTEXT;

	@Deprecated
	public final static IRI ADD_CONTEXT = iri(NAMESPACE_OBSOLETE, "addContext");

	public final static IRI REMOVE_CONTEXT = CONFIG.REMOVE_CONTEXT;

	@Deprecated
	public final static IRI ARCHIVE_CONTEXT = iri(NAMESPACE_OBSOLETE, "archiveContext");

	public final static IRI INSERT_CONTEXT = CONFIG.INSERT_CONTEXT;
}
