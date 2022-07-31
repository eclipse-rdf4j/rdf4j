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

import java.util.Collection;

import org.eclipse.rdf4j.query.QueryResultHandler;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.WriterConfig;

/**
 * The base interface for writers of query results sets and boolean results.
 *
 * @author Peter Ansell
 */
public interface QueryResultWriter extends QueryResultHandler {

	/**
	 * Gets the query result format that this writer uses.
	 */
	QueryResultFormat getQueryResultFormat();

	/**
	 * Handles a namespace prefix declaration. If this is called, it should be called before {@link #startDocument()} to
	 * ensure that it has a document wide effect.
	 * <p>
	 * NOTE: If the format does not support namespaces, it must silently ignore calls to this method.
	 *
	 * @param prefix The prefix to use for the namespace
	 * @param uri    The full URI that is to be represented by the prefix.
	 * @throws QueryResultHandlerException
	 */
	void handleNamespace(String prefix, String uri) throws QueryResultHandlerException;

	/**
	 * Indicates the start of the document.
	 *
	 * @throws QueryResultHandlerException If there was an error starting the writing of the results.
	 */
	void startDocument() throws QueryResultHandlerException;

	/**
	 * Handles a stylesheet URL. If this is called, it must be called after {@link #startDocument} and before
	 * {@link #startHeader}.
	 * <p>
	 * NOTE: If the format does not support stylesheets, it must silently ignore calls to this method.
	 *
	 * @param stylesheetUrl The URL of the stylesheet to be used to style the results.
	 * @throws QueryResultHandlerException If there was an error handling the stylesheet. This error is not thrown in
	 *                                     cases where stylesheets are not supported.
	 */
	void handleStylesheet(String stylesheetUrl) throws QueryResultHandlerException;

	/**
	 * Indicates the start of the header.
	 *
	 * @see <a href="http://www.w3.org/TR/2012/PER-rdf-sparql-XMLres-20121108/#head">SPARQL Query Results XML Format
	 *      documentation for head element.</a>
	 * @throws QueryResultHandlerException If there was an error writing the start of the header.
	 */
	void startHeader() throws QueryResultHandlerException;

	/**
	 * Indicates the end of the header. This must be called after {@link #startHeader} and before any calls to
	 * {@link #handleSolution}.
	 *
	 * @throws QueryResultHandlerException If there was an error writing the end of the header.
	 */
	void endHeader() throws QueryResultHandlerException;

	/**
	 * Sets all supplied writer configuration options.
	 *
	 * @param config a writer configuration object.
	 */
	void setWriterConfig(WriterConfig config);

	/**
	 * Retrieves the current writer configuration as a single object.
	 *
	 * @return a writer configuration object representing the current configuration of the writer.
	 */
	WriterConfig getWriterConfig();

	/**
	 * @return A collection of {@link RioSetting}s that are supported by this {@link QueryResultWriter}.
	 */
	Collection<RioSetting<?>> getSupportedSettings();

}
