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

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;

/**
 * Base class for {@link TupleQueryResultParser}s offering common functionality for query result parsers.
 */
public abstract class AbstractTupleQueryResultParser extends AbstractQueryResultParser
		implements TupleQueryResultParser {

	/**
	 * Creates a new parser base that, by default, will use an instance of {@link SimpleValueFactory} to create Value
	 * objects.
	 */
	protected AbstractTupleQueryResultParser() {
		super();
	}

	/**
	 * Creates a new parser base that will use the supplied {@link ValueFactory} to create {@link Value} objects.
	 */
	protected AbstractTupleQueryResultParser(ValueFactory valueFactory) {
		super(valueFactory);
	}

	@Override
	public QueryResultFormat getQueryResultFormat() {
		return getTupleQueryResultFormat();
	}

	@Override
	public void parseQueryResult(InputStream in)
			throws IOException, QueryResultParseException, QueryResultHandlerException {
		parse(in);
	}

	@Override
	public void setTupleQueryResultHandler(TupleQueryResultHandler handler) {
		setQueryResultHandler(handler);
	}

}
