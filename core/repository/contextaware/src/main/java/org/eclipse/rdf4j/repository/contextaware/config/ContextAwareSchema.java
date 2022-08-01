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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * @author James Leigh
 */
public class ContextAwareSchema {

	/**
	 * The ContextAwareRepository schema namespace ( <var>http://www.openrdf.org/config/repository/contextaware#</var>).
	 */
	public static final String NAMESPACE = "http://www.openrdf.org/config/repository/contextaware#";

	/** <var>http://www.openrdf.org/config/repository/contextaware#includeInferred</var> */
	public final static IRI INCLUDE_INFERRED;

	/** <var>http://www.openrdf.org/config/repository/contextaware#maxQueryTime</var> */
	public final static IRI MAX_QUERY_TIME;

	/** <var>http://www.openrdf.org/config/repository/contextaware#queryLanguage</var> */
	public final static IRI QUERY_LANGUAGE;

	/** <var>http://www.openrdf.org/config/repository/contextaware#base</var> */
	public final static IRI BASE_URI;

	/** <var>http://www.openrdf.org/config/repository/contextaware#readContext</var> */
	public final static IRI READ_CONTEXT;

	/** <var>http://www.openrdf.org/config/repository/contextaware#addContext</var> */
	@Deprecated
	public final static IRI ADD_CONTEXT;

	/** <var>http://www.openrdf.org/config/repository/contextaware#removeContext</var> */
	public final static IRI REMOVE_CONTEXT;

	/** <var>http://www.openrdf.org/config/repository/contextaware#archiveContext</var> */
	@Deprecated
	public final static IRI ARCHIVE_CONTEXT;

	/** <var>http://www.openrdf.org/config/repository/contextaware#insertContext</var> */
	public final static IRI INSERT_CONTEXT;

	static {
		ValueFactory factory = SimpleValueFactory.getInstance();
		INCLUDE_INFERRED = factory.createIRI(NAMESPACE, "includeInferred");
		QUERY_LANGUAGE = factory.createIRI(NAMESPACE, "ql");
		BASE_URI = factory.createIRI(NAMESPACE, "base");
		READ_CONTEXT = factory.createIRI(NAMESPACE, "readContext");
		ADD_CONTEXT = factory.createIRI(NAMESPACE, "addContext");
		REMOVE_CONTEXT = factory.createIRI(NAMESPACE, "removeContext");
		ARCHIVE_CONTEXT = factory.createIRI(NAMESPACE, "archiveContext");
		INSERT_CONTEXT = factory.createIRI(NAMESPACE, "insertContext");
		MAX_QUERY_TIME = factory.createIRI(NAMESPACE, "maxQueryTime");
	}
}
