/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.parser.QueryParser;
import org.eclipse.rdf4j.query.parser.QueryParserFactory;

/**
 * A {@link QueryParserFactory} for SPARQL parsers.
 * 
 * @author Arjohn Kampman
 */
public class SPARQLParserFactory implements QueryParserFactory {

	private final SPARQLParser singleton = new SPARQLParser();

	/**
	 * Returns {@link QueryLanguage#SPARQL}.
	 */
	@Override
	public QueryLanguage getQueryLanguage() {
		return QueryLanguage.SPARQL;
	}

	/**
	 * Returns a shared, thread-safe, instance of SPARQLParser.
	 */
	@Override
	public QueryParser getParser() {
		return singleton;
	}
}
