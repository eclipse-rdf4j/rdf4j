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

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.resultio.QueryResultFormat;
import org.eclipse.rdf4j.query.resultio.QueryResultParseException;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultParser;

/**
 * Parser for reading tuple query results formatted as SPARQL Results Documents. See
 * <a href="http://www.w3.org/TR/rdf-sparql-XMLres/">SPARQL Query Results XML Format</a> for the definition of this
 * format. The parser assumes that the XML is wellformed.
 */
public class SPARQLResultsXMLParser extends AbstractSPARQLXMLParser implements TupleQueryResultParser {

	/*-------------*
	 * Construtors *
	 *-------------*/

	/**
	 * Creates a new parser for the SPARQL Query Results XML Format that will use an instance of
	 * {@link SimpleValueFactory} to create Value objects.
	 */
	public SPARQLResultsXMLParser() {
		super();
	}

	/**
	 * Creates a new parser for the SPARQL Query Results XML Format that will use the supplied ValueFactory to create
	 * Value objects.
	 */
	public SPARQLResultsXMLParser(ValueFactory valueFactory) {
		super(valueFactory);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public TupleQueryResultFormat getTupleQueryResultFormat() {
		return TupleQueryResultFormat.SPARQL;
	}

	@Override
	public QueryResultFormat getQueryResultFormat() {
		return getTupleQueryResultFormat();
	}

	@Override
	public void setTupleQueryResultHandler(TupleQueryResultHandler handler) {
		setQueryResultHandler(handler);
	}

	@Override
	public void parseQueryResult(InputStream in)
			throws IOException, QueryResultParseException, TupleQueryResultHandlerException {
		try {
			parseQueryResultInternal(in, false, true);
		} catch (QueryResultHandlerException e) {
			if (e instanceof TupleQueryResultHandlerException) {
				throw (TupleQueryResultHandlerException) e;
			} else {
				throw new TupleQueryResultHandlerException(e);
			}
		}
	}

	@Override
	@Deprecated
	public void parse(InputStream in) throws IOException, QueryResultParseException, TupleQueryResultHandlerException {
		parseQueryResult(in);
	}
}
