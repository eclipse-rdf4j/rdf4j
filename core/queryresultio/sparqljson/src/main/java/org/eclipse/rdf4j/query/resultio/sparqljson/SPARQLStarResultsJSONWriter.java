/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.sparqljson;

import java.io.OutputStream;

import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriter;

/**
 * Writer for SPARQL* JSON results. This is equivalent to the SPARQL JSON writer with the addition of support for RDF*
 * triples. See {@link SPARQLStarResultsJSONConstants} for a description of the RDF* extension.
 *
 * @author Pavel Mihaylov
 * @implNote the actual {@link SPARQLResultsJSONWriter} itself already supports writing RDF* triples according to the
 *           extension. This subclass functions as an anchor point for the custom
 *           {@link TupleQueryResultFormat#JSON_STAR} content type.
 */
public class SPARQLStarResultsJSONWriter extends SPARQLResultsJSONWriter implements TupleQueryResultWriter {
	public SPARQLStarResultsJSONWriter(OutputStream out) {
		super(out);
	}

	@Override
	public TupleQueryResultFormat getTupleQueryResultFormat() {
		return SPARQLStarResultsJSONConstants.QUERY_RESULT_FORMAT;
	}

	@Override
	public TupleQueryResultFormat getQueryResultFormat() {
		return getTupleQueryResultFormat();
	}

	@Override
	public boolean acceptsFileFormat(FileFormat format) {
		// since SPARQL*/JSON is a superset of regular SPARQL/JSON, this Sink also accepts regular SPARQL/JSON
		// serialization
		return super.acceptsFileFormat(format) || TupleQueryResultFormat.JSON.equals(format);
	}
}
