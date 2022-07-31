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
package org.eclipse.rdf4j.query.resultio;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;

/**
 * A general interface for tuple query result parsers.
 */
public interface TupleQueryResultParser extends QueryResultParser {

	/**
	 * Gets the query result format that this parser can parse.
	 */
	TupleQueryResultFormat getTupleQueryResultFormat();

	/**
	 * Sets the TupleQueryResultHandler that will handle the parsed query result data.
	 *
	 * @deprecated Use {@link #setQueryResultHandler(org.eclipse.rdf4j.query.QueryResultHandler)} instead.
	 */
	@Deprecated
	void setTupleQueryResultHandler(TupleQueryResultHandler handler);

	/**
	 * Parses the data from the supplied InputStream.
	 *
	 * @param in The InputStream from which to read the data.
	 * @throws IOException                      If an I/O error occurred while data was read from the InputStream.
	 * @throws QueryResultParseException        If the parser has encountered an unrecoverable parse error.
	 * @throws TupleQueryResultHandlerException If the configured query result handler has encountered an unrecoverable
	 *                                          error.
	 * @deprecated Use {@link #parseQueryResult(InputStream)} instead.
	 */
	@Deprecated
	void parse(InputStream in) throws IOException, QueryResultParseException, TupleQueryResultHandlerException;
}
