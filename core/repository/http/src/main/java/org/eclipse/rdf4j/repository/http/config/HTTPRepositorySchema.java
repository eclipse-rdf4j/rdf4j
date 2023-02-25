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
package org.eclipse.rdf4j.repository.http.config;

import static org.eclipse.rdf4j.model.util.Values.iri;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.CONFIG;
import org.eclipse.rdf4j.repository.http.HTTPRepository;

/**
 * Defines constants for the HTTPRepository schema which is used by {@link HTTPRepositoryFactory}s to initialize
 * {@link HTTPRepository}s.
 *
 * @author Arjohn Kampman
 *
 * @deprecated since 4.3.0. Use {@link CONFIG} instead.
 *
 */
@Deprecated(since = "4.3.0", forRemoval = true)
public class HTTPRepositorySchema {

	public static final String NAMESPACE = "http://www.openrdf.org/config/repository/http#";

	/**
	 * @deprecated use {@link CONFIG#url} instead.
	 */
	public final static IRI REPOSITORYURL = iri(NAMESPACE, "repositoryURL");

	/**
	 * @deprecated use {@link CONFIG#username} instead.
	 */
	public final static IRI USERNAME = iri(NAMESPACE, "username");

	/**
	 * @deprecated use {@link CONFIG#password} instead.
	 */
	public final static IRI PASSWORD = iri(NAMESPACE, "password");
}
