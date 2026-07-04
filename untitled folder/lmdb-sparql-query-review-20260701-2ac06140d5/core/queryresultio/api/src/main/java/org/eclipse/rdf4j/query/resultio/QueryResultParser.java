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
import java.util.Collection;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.QueryResultHandler;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.rio.ParseErrorListener;
import org.eclipse.rdf4j.rio.ParseLocationListener;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RioSetting;

/**
 * Base interface for parsers of query results in both boolean and tuple forms.
 *
 * @author Peter Ansell
 */
public interface QueryResultParser {

	/**
	 * Gets the query result format that this parser can parse.
	 *
	 * @return The {@link QueryResultFormat} supported by this parser.
	 */
	QueryResultFormat getQueryResultFormat();

	/**
	 * Sets the {@link QueryResultHandler} to be used when parsing query results using
	 * {@link #parseQueryResult(InputStream)}.
	 *
	 * @param handler The {@link QueryResultHandler} to use for handling results.
	 */
	QueryResultParser setQueryResultHandler(QueryResultHandler handler);

	/**
	 * Sets the ValueFactory that the parser will use to create Value objects for the parsed query result.
	 *
	 * @param valueFactory The value factory that the parser should use.
	 */
	QueryResultParser setValueFactory(ValueFactory valueFactory);

	/**
	 * Sets the ParseErrorListener that will be notified of any errors that this parser finds during parsing.
	 *
	 * @param el The ParseErrorListener that will be notified of errors or warnings.
	 * @return Either a copy of this parser, if it is immutable, or this object, to allow chaining of method calls.
	 */
	QueryResultParser setParseErrorListener(ParseErrorListener el);

	/**
	 * Sets the ParseLocationListener that will be notified of the parser's progress during the parse process.
	 *
	 * @param ll The ParseLocationListener that will be notified of the parser's progress.
	 * @return Either a copy of this parser, if it is immutable, or this object, to allow chaining of method calls.
	 */
	QueryResultParser setParseLocationListener(ParseLocationListener ll);

	/**
	 * Parse the query results out of the given {@link InputStream} into the handler setup using
	 * {@link #setQueryResultHandler(QueryResultHandler)}.
	 *
	 * @param in The {@link InputStream} to parse the results from.
	 * @throws IOException                 If there is an exception from the InputStream.
	 * @throws QueryResultParseException   If the query results are not parsable by this parser.
	 * @throws QueryResultHandlerException If the {@link QueryResultHandler} set in
	 *                                     {@link #setQueryResultHandler(QueryResultHandler)} throws an exception.
	 */
	void parseQueryResult(InputStream in) throws IOException, QueryResultParseException, QueryResultHandlerException;

	/**
	 * Sets all supplied parser configuration options.
	 *
	 * @param config a parser configuration object.
	 */
	QueryResultParser setParserConfig(ParserConfig config);

	/**
	 * Retrieves the current parser configuration as a single object.
	 *
	 * @return a parser configuration object representing the current configuration of the parser.
	 */
	ParserConfig getParserConfig();

	/**
	 * @return A collection of {@link RioSetting}s that are supported by this QueryResultParser.
	 */
	Collection<RioSetting<?>> getSupportedSettings();

	/**
	 * Set a setting on the parser, and return this parser object to allow chaining.
	 *
	 * @param setting The setting to change.
	 * @param value   The value to change.
	 * @return Either a copy of this parser, if it is immutable, or this object, to allow chaining of method calls.
	 */
	<T> QueryResultParser set(RioSetting<T> setting, T value);

}
