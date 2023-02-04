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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.http.HTTPRepository;

/**
 * Defines constants for the HTTPRepository schema which is used by {@link HTTPRepositoryFactory}s to initialize
 * {@link HTTPRepository}s.
 *
 * @author Arjohn Kampman
 */
public class HTTPRepositorySchema {

	/**
	 * The HTTPRepository schema namespace (<var>tag:rdf4j.org:2023:config/http-repository#</var>).
	 */
	public static final String NAMESPACE = "tag:rdf4j.org:2023:config/http-repository#";

	@Deprecated(since = "4.3.0", forRemoval = true)
	public static final String NAMESPACE_OBSOLETE = "http://www.openrdf.org/config/repository/http#";

	/**
	 * <var>tag:rdf4j.org:2023:config/http-repository#repositoryURL</var>
	 */
	public final static IRI REPOSITORYURL;

	/**
	 * <var>tag:rdf4j.org:2023:config/http-repository#username</var>
	 */
	public final static IRI USERNAME;

	/**
	 * <var>tag:rdf4j.org:2023:config/http-repository#password</var>
	 */
	public final static IRI PASSWORD;

	static {
		ValueFactory factory = SimpleValueFactory.getInstance();
		REPOSITORYURL = factory.createIRI(NAMESPACE, "repositoryURL");
		USERNAME = factory.createIRI(NAMESPACE, "username");
		PASSWORD = factory.createIRI(NAMESPACE, "password");
	}
}
