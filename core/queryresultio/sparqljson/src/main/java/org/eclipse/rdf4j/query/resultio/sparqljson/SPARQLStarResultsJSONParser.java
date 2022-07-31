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

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;

/**
 * Parser for SPARQL-star JSON results. This is equivalent to the SPARQL JSON parser with the addition of support for
 * RDF-star triples. See {@link SPARQLStarResultsJSONConstants} for a description of the RDF-star extension.
 *
 * @author Pavel Mihaylov
 */
public class SPARQLStarResultsJSONParser extends SPARQLResultsJSONParser {
	/**
	 * Default constructor.
	 */
	public SPARQLStarResultsJSONParser() {
		super();
	}

	/**
	 * Constructs a parser with the supplied {@link ValueFactory}.
	 *
	 * @param valueFactory The factory to use to create values.
	 */
	public SPARQLStarResultsJSONParser(ValueFactory valueFactory) {
		super(valueFactory);
	}

	@Override
	public TupleQueryResultFormat getTupleQueryResultFormat() {
		return SPARQLStarResultsJSONConstants.QUERY_RESULT_FORMAT;
	}
}
