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

import java.io.OutputStream;

import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriter;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriterFactory;

/**
 * A {@link TupleQueryResultWriterFactory} for writers of SPARQL/XML tuple query results.
 *
 * @author Arjohn Kampman
 */
public class SPARQLResultsXMLWriterFactory implements TupleQueryResultWriterFactory {

	/**
	 * Returns {@link TupleQueryResultFormat#SPARQL}.
	 */
	@Override
	public TupleQueryResultFormat getTupleQueryResultFormat() {
		return TupleQueryResultFormat.SPARQL;
	}

	/**
	 * Returns a new instance of {@link SPARQLResultsXMLWriter}.
	 */
	@Override
	public TupleQueryResultWriter getWriter(OutputStream out) {
		return new SPARQLResultsXMLWriter(out);
	}
}
