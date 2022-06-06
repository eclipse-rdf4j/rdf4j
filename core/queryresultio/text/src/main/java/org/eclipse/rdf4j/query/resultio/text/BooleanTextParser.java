/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.text;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.resultio.AbstractQueryResultParser;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultParser;
import org.eclipse.rdf4j.query.resultio.QueryResultFormat;
import org.eclipse.rdf4j.query.resultio.QueryResultParseException;

/**
 * Reader for the plain text boolean result format.
 */
public class BooleanTextParser extends AbstractQueryResultParser implements BooleanQueryResultParser {

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new parser for the plain text boolean query result format.
	 */
	public BooleanTextParser() {
		super();
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public final BooleanQueryResultFormat getBooleanQueryResultFormat() {
		return BooleanQueryResultFormat.TEXT;
	}

	@Override
	public synchronized boolean parse(InputStream in) throws IOException, QueryResultParseException {
		Reader reader = new InputStreamReader(in, StandardCharsets.US_ASCII);
		String value = IOUtil.readString(reader, 16);
		value = value.trim();

		boolean result;

		if (value.equalsIgnoreCase("true")) {
			result = true;
		} else if (value.equalsIgnoreCase("false")) {
			result = false;
		} else {
			throw new QueryResultParseException("Invalid value: " + value);
		}

		if (this.handler != null) {
			try {
				this.handler.handleBoolean(result);
			} catch (QueryResultHandlerException e) {
				if (e.getCause() != null && e.getCause() instanceof IOException) {
					throw (IOException) e.getCause();
				} else {
					throw new QueryResultParseException("Found an issue with the query result handler", e);
				}
			}
		}

		return result;
	}

	@Override
	public final QueryResultFormat getQueryResultFormat() {
		return getBooleanQueryResultFormat();
	}

	@Override
	public void parseQueryResult(InputStream in)
			throws IOException, QueryResultParseException, QueryResultHandlerException {
		parse(in);
	}
}
