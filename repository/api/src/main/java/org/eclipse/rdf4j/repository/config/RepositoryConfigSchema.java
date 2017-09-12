/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
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

	/** The HTTPRepository schema namespace (<tt>http://www.openrdf.org/config/repository#</tt>). */
	public static final String NAMESPACE = "http://www.openrdf.org/config/repository#";

	/** <tt>http://www.openrdf.org/config/repository#RepositoryContext</tt> */
	public final static IRI REPOSITORY_CONTEXT;

	/** <tt>http://www.openrdf.org/config/repository#Repository</tt> */
	public final static IRI REPOSITORY;

	/** <tt>http://www.openrdf.org/config/repository#repositoryID</tt> */
	public final static IRI REPOSITORYID;

	/** <tt>http://www.openrdf.org/config/repository#repositoryImpl</tt> */
	public final static IRI REPOSITORYIMPL;

	/** <tt>http://www.openrdf.org/config/repository#repositoryType</tt> */
	public final static IRI REPOSITORYTYPE;

	/** <tt>http://www.openrdf.org/config/repository#delegate</tt> */
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
