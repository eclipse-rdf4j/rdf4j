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

import java.io.OutputStream;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriter;

/**
 * A {@link TupleQueryResultWriter} that writes tuple query results in the extended form
 * <a href="http://www.w3.org/TR/rdf-sparql-XMLres/">SPARQL Query Results XML Format</a>.
 *
 * @author Jeen Broekstra
 * @implNote the base class {@link SPARQLResultsXMLWriter} already has full support for writing extended RDF-star
 *           syntax. This class purely exists as a hook for the custom content type for
 *           {@link TupleQueryResultFormat#SPARQL_STAR}.
 */
@Experimental
public class SPARQLStarResultsXMLWriter extends SPARQLResultsXMLWriter {

	/**
	 * @param out
	 */
	public SPARQLStarResultsXMLWriter(OutputStream out) {
		super(out);
	}

	@Override
	public final TupleQueryResultFormat getTupleQueryResultFormat() {
		return TupleQueryResultFormat.SPARQL_STAR;
	}

	@Override
	public boolean acceptsFileFormat(FileFormat format) {
		return super.acceptsFileFormat(format) || TupleQueryResultFormat.SPARQL.equals(format);
	}

}
