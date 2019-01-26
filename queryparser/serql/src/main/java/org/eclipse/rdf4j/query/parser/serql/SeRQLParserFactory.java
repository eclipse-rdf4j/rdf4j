/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.serql;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.parser.QueryParser;
import org.eclipse.rdf4j.query.parser.QueryParserFactory;

/**
 * A {@link QueryParserFactory} for SeRQL parsers
 * 
 * @author Arjohn Kampman
 */
public class SeRQLParserFactory implements QueryParserFactory {

	private final SeRQLParser singleton = new SeRQLParser();

	/**
	 * Returns {@link QueryLanguage#SERQL}.
	 */
	@Override
	public QueryLanguage getQueryLanguage() {
		return QueryLanguage.SERQL;
	}

	/**
	 * Returns a shared, thread-safe, instance of SeRQLParser.
	 */
	@Override
	public QueryParser getParser() {
		return singleton;
	}
}
