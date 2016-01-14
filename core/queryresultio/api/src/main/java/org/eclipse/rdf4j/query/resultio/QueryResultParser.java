/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.QueryResultHandler;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RioSetting;

/**
 * Base interface for parsers of query results in both boolean and tuple forms.
 * 
 * @author Peter Ansell
 * @since 2.7.0
 */
public interface QueryResultParser {

	/**
	 * Gets the query result format that this parser can parse.
	 * 
	 * @return The {@link QueryResultFormat} supported by this parser.
	 * @since 2.7.0
	 */
	QueryResultFormat getQueryResultFormat();
	
	/**
	 * Sets the {@link QueryResultHandler} to be used when parsing query results
	 * using {@link #parseQueryResult(InputStream)}.
	 * 
	 * @param handler
	 *        The {@link QueryResultHandler} to use for handling results.
	 * @since 2.7.0
	 */
	void setQueryResultHandler(QueryResultHandler handler);

	/**
	 * Sets the ValueFactory that the parser will use to create Value objects for
	 * the parsed query result.
	 * 
	 * @param valueFactory
	 *        The value factory that the parser should use.
	 */
	void setValueFactory(ValueFactory valueFactory);

	/**
	 * Parse the query results out of the given {@link InputStream} into the
	 * handler setup using {@link #setQueryResultHandler(QueryResultHandler)}.
	 * 
	 * @param in
	 *        The {@link InputStream} to parse the results from.
	 * @throws IOException
	 *         If there is an exception from the InputStream.
	 * @throws QueryResultParseException
	 *         If the query results are not parsable by this parser.
	 * @throws QueryResultHandlerException
	 *         If the {@link QueryResultHandler} set in
	 *         {@link #setQueryResultHandler(QueryResultHandler)} throws an
	 *         exception.
	 * @since 2.7.0
	 */
	void parseQueryResult(InputStream in)
		throws IOException, QueryResultParseException, QueryResultHandlerException;
	

	/**
	 * Sets all supplied parser configuration options.
	 * 
	 * @param config
	 *        a parser configuration object.
	 * @since 2.7.0
	 */
	public void setParserConfig(ParserConfig config);

	/**
	 * Retrieves the current parser configuration as a single object.
	 * 
	 * @return a parser configuration object representing the current
	 *         configuration of the parser.
	 * @since 2.7.0
	 */
	public ParserConfig getParserConfig();

	/**
	 * @return A collection of {@link RioSetting}s that are supported by this
	 *         QueryResultParser.
	 * @since 2.7.0
	 */
	public Collection<RioSetting<?>> getSupportedSettings();

}
