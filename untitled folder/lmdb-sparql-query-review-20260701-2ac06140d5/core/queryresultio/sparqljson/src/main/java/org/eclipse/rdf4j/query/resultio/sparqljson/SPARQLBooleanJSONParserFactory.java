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
package org.eclipse.rdf4j.query.resultio.sparqljson;

import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultParser;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultParserFactory;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultParserFactory;

/**
 * A {@link TupleQueryResultParserFactory} for parsers of SPARQL-1.1 JSON Boolean Query Results.
 *
 * @author Peter Ansell
 */
public class SPARQLBooleanJSONParserFactory implements BooleanQueryResultParserFactory {

	/**
	 * Returns {@link BooleanQueryResultFormat#JSON}.
	 */
	@Override
	public BooleanQueryResultFormat getBooleanQueryResultFormat() {
		return BooleanQueryResultFormat.JSON;
	}

	/**
	 * Returns a new instance of SPARQLBooleanJSONParser.
	 */
	@Override
	public BooleanQueryResultParser getParser() {
		return new SPARQLBooleanJSONParser();
	}
}
