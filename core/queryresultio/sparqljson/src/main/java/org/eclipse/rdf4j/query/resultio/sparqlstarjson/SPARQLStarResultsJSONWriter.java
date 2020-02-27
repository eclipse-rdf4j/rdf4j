/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.sparqlstarjson;

import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriter;
import org.eclipse.rdf4j.query.resultio.sparqljson.AbstractSPARQLJSONParser;
import org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLResultsJSONWriter;

import java.io.IOException;
import java.io.OutputStream;

import static org.eclipse.rdf4j.query.resultio.sparqlstarjson.SPARQLStarResultsJSONConstants.OBJECT;
import static org.eclipse.rdf4j.query.resultio.sparqlstarjson.SPARQLStarResultsJSONConstants.PREDICATE;
import static org.eclipse.rdf4j.query.resultio.sparqlstarjson.SPARQLStarResultsJSONConstants.QUERY_RESULT_FORMAT;
import static org.eclipse.rdf4j.query.resultio.sparqlstarjson.SPARQLStarResultsJSONConstants.SUBJECT;
import static org.eclipse.rdf4j.query.resultio.sparqlstarjson.SPARQLStarResultsJSONConstants.TRIPLE;

/**
 * Writer for SPARQL* JSON results. This is equivalent to the SPARQL JSON writer with the addition of support for RDF*
 * triples. See {@link SPARQLStarResultsJSONConstants} for a description of the RDF* extension.
 *
 * @author Pavel Mihaylov
 */
public class SPARQLStarResultsJSONWriter extends SPARQLResultsJSONWriter implements TupleQueryResultWriter {
	public SPARQLStarResultsJSONWriter(OutputStream out) {
		super(out);
	}

	@Override
	public TupleQueryResultFormat getTupleQueryResultFormat() {
		return QUERY_RESULT_FORMAT;
	}

	@Override
	public TupleQueryResultFormat getQueryResultFormat() {
		return getTupleQueryResultFormat();
	}

	@Override
	protected void writeValue(Value value) throws IOException, QueryResultHandlerException {
		if (value instanceof Triple) {
			jg.writeStartObject();

			jg.writeStringField(AbstractSPARQLJSONParser.TYPE, TRIPLE);

			jg.writeObjectFieldStart(AbstractSPARQLJSONParser.VALUE);

			jg.writeFieldName(SUBJECT);
			writeValue(((Triple) value).getSubject());

			jg.writeFieldName(PREDICATE);
			writeValue(((Triple) value).getPredicate());

			jg.writeFieldName(OBJECT);
			writeValue(((Triple) value).getObject());

			jg.writeEndObject();

			jg.writeEndObject();
		} else {
			super.writeValue(value);
		}
	}
}
