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

import java.io.OutputStream;

import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriter;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriterFactory;

/**
 * {@link TupleQueryResultWriterFactory} for creating instances of {@link SPARQLStarResultsJSONWriter}.
 *
 * @author Pavel Mihaylov
 */
public class SPARQLStarResultsJSONWriterFactory implements TupleQueryResultWriterFactory {
	/**
	 * Returns {@link TupleQueryResultFormat#JSON_STAR}.
	 */
	@Override
	public TupleQueryResultFormat getTupleQueryResultFormat() {
		return SPARQLStarResultsJSONConstants.QUERY_RESULT_FORMAT;
	}

	/**
	 * Returns a new instance of {@link SPARQLStarResultsJSONWriter}.
	 */
	@Override
	public TupleQueryResultWriter getWriter(OutputStream out) {
		return new SPARQLStarResultsJSONWriter(out);
	}
}
