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
 * @deprecated use {@link CONFIG.ContextAware} vocabulary instead.
 */
@Deprecated(since = "4.3.0", forRemoval = true)
public class ContextAwareSchema {

	/**
	 * The obsolete ContextAwareRepository schema namespace (
	 * <var>http://www.openrdf.org/config/repository/contextaware#</var>).
	 */
	public static final String NAMESPACE = "http://www.openrdf.org/config/repository/contextaware#";

	/**
	 * @deprecated use {@link CONFIG.ContextAware#includeInferred} instead.
	 */
	public final static IRI INCLUDE_INFERRED = iri(NAMESPACE, "includeInferrred");

	/**
	 * @deprecated use {@link CONFIG.ContextAware#maxQueryTime} instead
	 */
	public final static IRI MAX_QUERY_TIME = iri(NAMESPACE, "maxQueryTime");

	/**
	 * @deprecated use {@link CONFIG.ContextAware#queryLanguage} instead.
	 */
	public final static IRI QUERY_LANGUAGE = iri(NAMESPACE, "queryLanguage");

	/**
	 * @deprecated use {@link CONFIG.ContextAware#base} instead
	 */
	public final static IRI BASE_URI = iri(NAMESPACE, "base");

	/**
	 * @deprecated use {@link CONFIG.ContextAware#readContext} instead
	 */
	public final static IRI READ_CONTEXT = iri(NAMESPACE, "readContext");

	@Deprecated
	public final static IRI ADD_CONTEXT = iri(NAMESPACE, "addContext");

	/**
	 * @deprecated use {@link CONFIG.ContextAware#removeContext} instead.
	 */
	public final static IRI REMOVE_CONTEXT = iri(NAMESPACE, "removeContext");

	@Deprecated
	public final static IRI ARCHIVE_CONTEXT = iri(NAMESPACE, "archiveContext");

	/**
	 * @deprecated use {@link CONFIG.ContextAware#insertContext} instead.
	 */
	public final static IRI INSERT_CONTEXT = iri(NAMESPACE, "insertContext");
}
