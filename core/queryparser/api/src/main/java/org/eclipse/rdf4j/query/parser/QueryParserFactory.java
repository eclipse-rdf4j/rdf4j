/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser;

import org.eclipse.rdf4j.query.QueryLanguage;

/**
 * A QueryParserFactory returns {@link QueryParser}s for a specific query language.
 *
 * @author Arjohn Kampman
 */
public interface QueryParserFactory {

	/**
	 * Returns the query language for this factory.
	 */
	public QueryLanguage getQueryLanguage();

	/**
	 * Returns a QueryParser instance.
	 */
	public QueryParser getParser();
}
