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
package org.eclipse.rdf4j.repository.config;

import static org.eclipse.rdf4j.model.util.Values.iri;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.Config;

/**
 * Defines constants for the repository configuration schema that is used by
 * {@link org.eclipse.rdf4j.repository.manager.RepositoryManager}s.
 *
 * @author Arjohn Kampman
 * @author Jeen Broekstra
 *
 * @deprecated use {@link Config} vocabulary instead.
 */
@Deprecated(since = "4.3.0", forRemoval = true)
public class RepositoryConfigSchema {

	public static final String NAMESPACE = "http://www.openrdf.org/config/repository#";

	/**
	 * This IRI is not in the CONFIG vocabulary because it is no longer necessary - it was only used in the old-style
	 * SYSTEM-repo configurations.
	 */
	public final static IRI REPOSITORY_CONTEXT = iri(NAMESPACE, "RepositoryContext");

	/**
	 * @deprecated use {@link Config#Repository} instead.
	 */
	public final static IRI REPOSITORY = iri(NAMESPACE, "Repository");

	/**
	 * @deprecated use {@link Config#id} instead.
	 */
	public final static IRI REPOSITORYID = iri(NAMESPACE, "repositoryID");

	/**
	 * @deprecated use {@link Config#impl} instead.
	 */
	public final static IRI REPOSITORYIMPL = iri(NAMESPACE, "repositoryImpl");

	/**
	 * @deprecated use {@link Config#type} instead.
	 */
	public final static IRI REPOSITORYTYPE = iri(NAMESPACE, "repositoryType");

	/**
	 * @deprecated use {@link Config#delegate} instead.
	 */
	public final static IRI DELEGATE = iri(NAMESPACE, "delegate");
}
