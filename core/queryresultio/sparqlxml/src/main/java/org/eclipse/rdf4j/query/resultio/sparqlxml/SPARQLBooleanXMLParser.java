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
package org.eclipse.rdf4j.query.resultio.sparqlxml;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultParser;
import org.eclipse.rdf4j.query.resultio.QueryResultFormat;
import org.eclipse.rdf4j.query.resultio.QueryResultParseException;

/**
 * Parser for reading boolean query results formatted as SPARQL Results Documents. See
 * <a href="http://www.w3.org/TR/rdf-sparql-XMLres/">SPARQL Query Results XML Format</a> for the definition of this
 * format. The parser assumes that the XML is wellformed.
 */
public class SPARQLBooleanXMLParser extends AbstractSPARQLXMLParser implements BooleanQueryResultParser {

	/*-------------*
	 * Construtors *
	 *-------------*/

	/**
	 * Creates a new parser for the SPARQL Query Results XML Format.
	 */
	public SPARQLBooleanXMLParser() {
		super();
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public BooleanQueryResultFormat getBooleanQueryResultFormat() {
		return BooleanQueryResultFormat.SPARQL;
	}

	@Override
	public QueryResultFormat getQueryResultFormat() {
		return getBooleanQueryResultFormat();
	}

	@Override
	public synchronized void parseQueryResult(InputStream in) throws IOException, QueryResultParseException {
		try {
			parseQueryResultInternal(in, true, false);
		} catch (QueryResultHandlerException e) {
			throw new QueryResultParseException(e);
		}
	}

	@Override
	@Deprecated
	public boolean parse(InputStream in) throws IOException, QueryResultParseException {
		try {
			return parseQueryResultInternal(in, true, false);
		} catch (QueryResultHandlerException e) {
			throw new QueryResultParseException(e);

		}
	}
}
