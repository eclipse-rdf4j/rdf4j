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
import java.util.Collections;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.QueryResultHandler;
import org.eclipse.rdf4j.rio.ParseErrorListener;
import org.eclipse.rdf4j.rio.ParseLocationListener;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;

/**
 * Base class for {@link QueryResultParser}s offering common functionality for query result parsers.
 */
public abstract class AbstractQueryResultParser implements QueryResultParser {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The {@link ValueFactory} to use for creating RDF model objects.
	 */
	protected ValueFactory valueFactory;

	/**
	 * The {@link QueryResultHandler} that will handle the parsed query results.
	 */
	protected QueryResultHandler handler;

	/**
	 * A collection of configuration options for this parser.
	 */
	private ParserConfig parserConfig;

	/**
	 * An optional ParseErrorListener to report parse errors to.
	 */
	private ParseErrorListener errListener;

	/**
	 * An optional ParseLocationListener to report parse progress in the form of line- and column numbers to.
	 */
	private ParseLocationListener locationListener;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new parser base that, by default, will use the global instance of {@link SimpleValueFactory} to create
	 * Value objects.
	 */
	protected AbstractQueryResultParser() {
		this(SimpleValueFactory.getInstance());
	}

	/**
	 * Creates a new parser base that will use the supplied ValueFactory to create Value objects.
	 */
	protected AbstractQueryResultParser(ValueFactory valueFactory) {
		setValueFactory(valueFactory);
		setParserConfig(new ParserConfig());
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public QueryResultParser setValueFactory(ValueFactory valueFactory) {
		this.valueFactory = valueFactory;
		return this;
	}

	@Override
	public QueryResultParser setQueryResultHandler(QueryResultHandler handler) {
		if (getParserConfig().get(BasicParserSettings.PROCESS_ENCODED_RDF_STAR)) {
			handler = new RDFStarDecodingQueryResultHandler(handler);
		}
		this.handler = handler;
		return this;
	}

	@Override
	public QueryResultParser setParserConfig(ParserConfig config) {
		this.parserConfig = config;
		return this;
	}

	@Override
	public ParserConfig getParserConfig() {
		return this.parserConfig;
	}

	@Override
	public QueryResultParser setParseErrorListener(ParseErrorListener el) {
		errListener = el;
		return this;
	}

	public ParseErrorListener getParseErrorListener() {
		return errListener;
	}

	@Override
	public QueryResultParser setParseLocationListener(ParseLocationListener el) {
		locationListener = el;
		return this;
	}

	public ParseLocationListener getParseLocationListener() {
		return locationListener;
	}

	/*
	 * Default implementation. Implementing classes may override this to declare their supported settings.
	 */
	@Override
	public Collection<RioSetting<?>> getSupportedSettings() {
		return Collections.emptyList();
	}

	@Override
	public <T> QueryResultParser set(RioSetting<T> setting, T value) {
		getParserConfig().set(setting, value);
		return this;
	}
}
