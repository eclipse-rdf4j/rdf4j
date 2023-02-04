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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Defines constants for the repository configuration schema that is used by
 * {@link org.eclipse.rdf4j.repository.manager.RepositoryManager}s.
 *
 * @author Arjohn Kampman
 */
public class RepositoryConfigSchema {

	/**
	 * The Repository config schema namespace (<var>http://rdf4j.org/vocabulary/repository#</var>).
	 */
	public static final String NAMESPACE = "http://rdf4j.org/schema/repository#";

	@Deprecated(forRemoval = true)
	public static final String NAMESPACE_OBSOLETE = "http://www.openrdf.org/config/repository#";

	/**
	 * <var>"http://rdf4j.org/schema/repository#RepositoryContext</var>
	 */
	public final static IRI REPOSITORY_CONTEXT;

	/**
	 * <var>http://rdf4j.org/schema/repository#Repository</var>
	 */
	public final static IRI REPOSITORY;

	/**
	 * <var>http://rdf4j.org/schema/repository#repositoryID</var>
	 */
	public final static IRI REPOSITORYID;

	/**
	 * <var>http://rdf4j.org/schema/repository#repositoryImpl</var>
	 */
	public final static IRI REPOSITORYIMPL;

	/**
	 * <var>http://rdf4j.org/schema/repository#repositoryType</var>
	 */
	public final static IRI REPOSITORYTYPE;

	/**
	 * <var>http://rdf4j.org/schema/repository#delegate</var>
	 */
	public final static IRI DELEGATE;

	static {
		ValueFactory factory = SimpleValueFactory.getInstance();
		REPOSITORY_CONTEXT = factory.createIRI(NAMESPACE, "RepositoryContext");
		REPOSITORY = factory.createIRI(NAMESPACE, "Repository");
		REPOSITORYID = factory.createIRI(NAMESPACE, "repositoryID");
		REPOSITORYIMPL = factory.createIRI(NAMESPACE, "repositoryImpl");
		REPOSITORYTYPE = factory.createIRI(NAMESPACE, "repositoryType");
		DELEGATE = factory.createIRI(NAMESPACE, "delegate");
	}
}
