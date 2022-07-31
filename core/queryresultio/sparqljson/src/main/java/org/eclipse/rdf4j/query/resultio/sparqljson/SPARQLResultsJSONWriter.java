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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriter;

/**
 * A TupleQueryResultWriter that writes query results in the <a href="http://www.w3.org/TR/rdf-sparql-json-res/">SPARQL
 * Query Results JSON Format</a>.
 */
public class SPARQLResultsJSONWriter extends AbstractSPARQLJSONWriter implements TupleQueryResultWriter {

	/*--------------*
	 * Constructors *
	 *--------------*/

	public SPARQLResultsJSONWriter(OutputStream out) {
		super(out);
	}

	public SPARQLResultsJSONWriter(Writer writer) {
		super(writer);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public TupleQueryResultFormat getTupleQueryResultFormat() {
		return TupleQueryResultFormat.JSON;
	}

	@Override
	public TupleQueryResultFormat getQueryResultFormat() {
		return getTupleQueryResultFormat();
	}

	@Override
	protected void writeValue(Value value) throws IOException, QueryResultHandlerException {
		if (value instanceof Triple) {
			jg.writeStartObject();

			jg.writeStringField(AbstractSPARQLJSONParser.TYPE, SPARQLStarResultsJSONConstants.TRIPLE);

			jg.writeObjectFieldStart(AbstractSPARQLJSONParser.VALUE);

			jg.writeFieldName(SPARQLStarResultsJSONConstants.SUBJECT);
			writeValue(((Triple) value).getSubject());

			jg.writeFieldName(SPARQLStarResultsJSONConstants.PREDICATE);
			writeValue(((Triple) value).getPredicate());

			jg.writeFieldName(SPARQLStarResultsJSONConstants.OBJECT);
			writeValue(((Triple) value).getObject());

			jg.writeEndObject();

			jg.writeEndObject();
		} else {
			super.writeValue(value);
		}
	}
}
