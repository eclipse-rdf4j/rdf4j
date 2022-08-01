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
package org.eclipse.rdf4j.query.resultio.sparqlxml;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;

/**
 * Parser for reading tuple query results formatted as SPARQL Results Documents, extended with support for RDF-star
 * triples
 *
 * @author Jeen Broekstra
 * @implNote the base class {@link SPARQLResultsXMLParser} already has full support for processing extended RDF-star
 *           syntax. This class purely exists as a hook for the custom content type for
 *           {@link TupleQueryResultFormat#SPARQL_STAR}.
 */
@Experimental
public class SPARQLStarResultsXMLParser extends SPARQLResultsXMLParser {

	public SPARQLStarResultsXMLParser() {
		super();
	}

	public SPARQLStarResultsXMLParser(ValueFactory valueFactory) {
		super(valueFactory);
	}

	@Override
	public TupleQueryResultFormat getTupleQueryResultFormat() {
		return TupleQueryResultFormat.SPARQL_STAR;
	}
}
