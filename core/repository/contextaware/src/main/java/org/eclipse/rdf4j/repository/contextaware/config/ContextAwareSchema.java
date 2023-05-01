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
import org.eclipse.rdf4j.model.vocabulary.Config;

/**
 * @author James Leigh
 *
 * @deprecated use {@link Config.ContextAwareRepository} vocabulary instead.
 */
@Deprecated(since = "4.3.0", forRemoval = true)
public class ContextAwareSchema {

	/**
	 * The obsolete ContextAwareRepository schema namespace (
	 * <var>http://www.openrdf.org/config/repository/contextaware#</var>).
	 */
	public static final String NAMESPACE = "http://www.openrdf.org/config/repository/contextaware#";

	/**
	 * @deprecated use {@link Config.ContextAwareRepository#includeInferred} instead.
	 */
	public final static IRI INCLUDE_INFERRED = iri(NAMESPACE, "includeInferrred");

	/**
	 * @deprecated use {@link Config.ContextAwareRepository#maxQueryTime} instead
	 */
	public final static IRI MAX_QUERY_TIME = iri(NAMESPACE, "maxQueryTime");

	/**
	 * @deprecated use {@link Config.ContextAwareRepository#queryLanguage} instead.
	 */
	public final static IRI QUERY_LANGUAGE = iri(NAMESPACE, "queryLanguage");

	/**
	 * @deprecated use {@link Config.ContextAwareRepository#base} instead
	 */
	public final static IRI BASE_URI = iri(NAMESPACE, "base");

	/**
	 * @deprecated use {@link Config.ContextAwareRepository#readContext} instead
	 */
	public final static IRI READ_CONTEXT = iri(NAMESPACE, "readContext");

	@Deprecated
	public final static IRI ADD_CONTEXT = iri(NAMESPACE, "addContext");

	/**
	 * @deprecated use {@link Config.ContextAwareRepository#removeContext} instead.
	 */
	public final static IRI REMOVE_CONTEXT = iri(NAMESPACE, "removeContext");

	@Deprecated
	public final static IRI ARCHIVE_CONTEXT = iri(NAMESPACE, "archiveContext");

	/**
	 * @deprecated use {@link Config.ContextAwareRepository#insertContext} instead.
	 */
	public final static IRI INSERT_CONTEXT = iri(NAMESPACE, "insertContext");
}
