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
package org.eclipse.rdf4j.query.resultio.text.csv;

import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultParser;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultParserFactory;

/**
 * A {@link TupleQueryResultParserFactory} for SPARQL CSV result parsers.
 *
 * @author Jeen Broekstra
 */
public class SPARQLResultsCSVParserFactory implements TupleQueryResultParserFactory {

	/**
	 * Returns {@link TupleQueryResultFormat#CSV}.
	 */
	@Override
	public TupleQueryResultFormat getTupleQueryResultFormat() {
		return TupleQueryResultFormat.CSV;
	}

	/**
	 * Returns a new instance of SPARQLResultsCSVParser.
	 */
	@Override
	public TupleQueryResultParser getParser() {
		return new SPARQLResultsCSVParser();
	}
}
