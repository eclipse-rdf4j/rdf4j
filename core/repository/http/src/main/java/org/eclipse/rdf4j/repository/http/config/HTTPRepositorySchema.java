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

	/** The HTTPRepository schema namespace (<var>http://www.openrdf.org/config/repository/http#</var>). */
	public static final String NAMESPACE = "http://www.openrdf.org/config/repository/http#";

	/** <var>http://www.openrdf.org/config/repository/http#repositoryURL</var> */
	public final static IRI REPOSITORYURL;

	/** <var>http://www.openrdf.org/config/repository/http#username</var> */
	public final static IRI USERNAME;

	/** <var>http://www.openrdf.org/config/repository/http#password</var> */
	public final static IRI PASSWORD;

	static {
		ValueFactory factory = SimpleValueFactory.getInstance();
		REPOSITORYURL = factory.createIRI(NAMESPACE, "repositoryURL");
		USERNAME = factory.createIRI(NAMESPACE, "username");
		PASSWORD = factory.createIRI(NAMESPACE, "password");
	}
}
