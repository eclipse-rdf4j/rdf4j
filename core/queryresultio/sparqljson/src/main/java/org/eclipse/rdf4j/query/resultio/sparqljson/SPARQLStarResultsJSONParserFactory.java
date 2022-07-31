/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.sparqljson;

import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultParser;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultParserFactory;

/**
 * {@link TupleQueryResultParserFactory} for creating instances of {@link SPARQLStarResultsJSONParser}.
 *
 * @author Pavel Mihaylov
 */
public class SPARQLStarResultsJSONParserFactory implements TupleQueryResultParserFactory {
	/**
	 * Returns {@link TupleQueryResultFormat#JSON_STAR}.
	 */
	@Override
	public TupleQueryResultFormat getTupleQueryResultFormat() {
		return SPARQLStarResultsJSONConstants.QUERY_RESULT_FORMAT;
	}

	/**
	 * Returns a new instance of {@link SPARQLStarResultsJSONParser}.
	 */
	@Override
	public TupleQueryResultParser getParser() {
		return new SPARQLResultsJSONParser();
	}
}
