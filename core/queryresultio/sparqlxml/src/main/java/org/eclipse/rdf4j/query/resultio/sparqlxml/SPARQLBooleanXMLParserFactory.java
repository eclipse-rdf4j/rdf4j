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
package org.eclipse.rdf4j.query.resultio.sparqlxml;

import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultParser;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultParserFactory;

/**
 * A {@link BooleanQueryResultParserFactory} for parsers of SPARQL/XML boolean query results.
 *
 * @author Arjohn Kampman
 */
public class SPARQLBooleanXMLParserFactory implements BooleanQueryResultParserFactory {

	/**
	 * Returns {@link BooleanQueryResultFormat#SPARQL}.
	 */
	@Override
	public BooleanQueryResultFormat getBooleanQueryResultFormat() {
		return BooleanQueryResultFormat.SPARQL;
	}

	/**
	 * Returns a new instance of SPARQLBooleanXMLParser.
	 */
	@Override
	public BooleanQueryResultParser getParser() {
		return new SPARQLBooleanXMLParser();
	}
}
