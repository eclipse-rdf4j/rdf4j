/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.QueryResultHandler;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RioSetting;

/**
 * Base class for {@link QueryResultParser}s offering common functionality for
 * query result parsers.
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

	private ParserConfig parserConfig = new ParserConfig();

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new parser base that, by default, will use the global instance
	 * of {@link SimpleValueFactory} to create Value objects.
	 */
	public AbstractQueryResultParser() {
		this(SimpleValueFactory.getInstance());
	}

	/**
	 * Creates a new parser base that will use the supplied ValueFactory to
	 * create Value objects.
	 */
	public AbstractQueryResultParser(ValueFactory valueFactory) {
		setValueFactory(valueFactory);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public void setValueFactory(ValueFactory valueFactory) {
		this.valueFactory = valueFactory;
	}

	@Override
	public void setQueryResultHandler(QueryResultHandler handler) {
		this.handler = handler;
	}

	@Override
	public void setParserConfig(ParserConfig config) {
		this.parserConfig = config;
	}

	@Override
	public ParserConfig getParserConfig() {
		return this.parserConfig;
	}

	/*
	 * Default implementation. Implementing classes may override this to declare their supported settings.
	 */
	@Override
	public Collection<RioSetting<?>> getSupportedSettings() {
		return Collections.emptyList();
	}
}
