/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.sparqlstarjson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.resultio.QueryResultParseException;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLResultsJSONParser;

import java.io.IOException;

import static org.eclipse.rdf4j.query.resultio.sparqlstarjson.SPARQLStarResultsJSONConstants.OBJECT;
import static org.eclipse.rdf4j.query.resultio.sparqlstarjson.SPARQLStarResultsJSONConstants.PREDICATE;
import static org.eclipse.rdf4j.query.resultio.sparqlstarjson.SPARQLStarResultsJSONConstants.QUERY_RESULT_FORMAT;
import static org.eclipse.rdf4j.query.resultio.sparqlstarjson.SPARQLStarResultsJSONConstants.SUBJECT;
import static org.eclipse.rdf4j.query.resultio.sparqlstarjson.SPARQLStarResultsJSONConstants.TRIPLE;

/**
 * Parser for SPARQL* JSON results. This is equivalent to the SPARQL JSON parser with the addition of support for RDF*
 * triples. See {@link SPARQLStarResultsJSONConstants} for a description of the RDF* extension.
 *
 * @author Pavel Mihaylov
 */
public class SPARQLStarResultsJSONParser extends SPARQLResultsJSONParser {
	/**
	 * Default constructor.
	 */
	public SPARQLStarResultsJSONParser() {
		super();
	}

	/**
	 * Constructs a parser with the supplied {@link ValueFactory}.
	 *
	 * @param valueFactory The factory to use to create values.
	 */
	public SPARQLStarResultsJSONParser(ValueFactory valueFactory) {
		super(valueFactory);
	}

	@Override
	public TupleQueryResultFormat getTupleQueryResultFormat() {
		return QUERY_RESULT_FORMAT;
	}

	@Override
	protected Triple parseTripleValue(JsonParser jp, String fieldName) throws IOException {
		Value subject = null, predicate = null, object = null;

		while (jp.nextToken() != JsonToken.END_OBJECT) {
			if (jp.getCurrentToken() != JsonToken.FIELD_NAME) {
				throw new QueryResultParseException("Did not find triple attribute in triple value",
						jp.getCurrentLocation().getLineNr(),
						jp.getCurrentLocation().getColumnNr());
			}
			String posName = jp.getCurrentName();
			if (SUBJECT.equals(posName)) {
				subject = parseValue(jp, fieldName + ":" + posName);
			} else if (PREDICATE.equals(posName)) {
				predicate = parseValue(jp, fieldName + ":" + posName);
			} else if (OBJECT.equals(posName)) {
				object = parseValue(jp, fieldName + ":" + posName);
			} else {
				throw new QueryResultParseException("Unexpected field name in triple value: " + posName,
						jp.getCurrentLocation().getLineNr(),
						jp.getCurrentLocation().getColumnNr());
			}
		}

		if (subject instanceof Resource && predicate instanceof IRI && object != null) {
			return valueFactory.createTriple((Resource) subject, (IRI) predicate, object);
		} else {
			throw new QueryResultParseException("Incomplete or invalid triple value",
					jp.getCurrentLocation().getLineNr(),
					jp.getCurrentLocation().getColumnNr());
		}
	}

	@Override
	protected boolean checkTripleType(JsonParser jp, String type) {
		if (!TRIPLE.equals(type)) {
			throw new QueryResultParseException("Found a triple value but unexpected type: " + type,
					jp.getCurrentLocation().getLineNr(),
					jp.getCurrentLocation().getColumnNr());
		}

		return true;
	}
}
