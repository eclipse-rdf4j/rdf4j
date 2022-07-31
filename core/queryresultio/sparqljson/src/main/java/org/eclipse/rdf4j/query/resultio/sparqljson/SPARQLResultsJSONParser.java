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

import static org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLStarResultsJSONConstants.OBJECT;
import static org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLStarResultsJSONConstants.OBJECT_JENA;
import static org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLStarResultsJSONConstants.PREDICATE;
import static org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLStarResultsJSONConstants.PREDICATE_JENA;
import static org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLStarResultsJSONConstants.SUBJECT;
import static org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLStarResultsJSONConstants.SUBJECT_JENA;
import static org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLStarResultsJSONConstants.TRIPLE;
import static org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLStarResultsJSONConstants.TRIPLE_STARDOG;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.resultio.QueryResultFormat;
import org.eclipse.rdf4j.query.resultio.QueryResultParseException;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultParser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Parser for SPARQL-1.1 JSON Results Format documents.
 *
 * @see <a href="http://www.w3.org/TR/sparql11-results-json/">SPARQL 1.1 Query Results JSON Format</a>
 * @author Peter Ansell
 */
public class SPARQLResultsJSONParser extends AbstractSPARQLJSONParser implements TupleQueryResultParser {

	/**
	 * Default constructor.
	 */
	public SPARQLResultsJSONParser() {
		super();
	}

	/**
	 * Construct a parser with a specific {@link ValueFactory}.
	 *
	 * @param valueFactory The factory to use to create values.
	 */
	public SPARQLResultsJSONParser(ValueFactory valueFactory) {
		super(valueFactory);
	}

	@Override
	public QueryResultFormat getQueryResultFormat() {
		return getTupleQueryResultFormat();
	}

	@Override
	public TupleQueryResultFormat getTupleQueryResultFormat() {
		return TupleQueryResultFormat.JSON;
	}

	@Override
	@Deprecated
	public void setTupleQueryResultHandler(TupleQueryResultHandler handler) {
		setQueryResultHandler(handler);
	}

	@Override
	@Deprecated
	public void parse(InputStream in) throws IOException, QueryResultParseException, TupleQueryResultHandlerException {
		try {
			parseQueryResultInternal(in, false, true);
		} catch (TupleQueryResultHandlerException e) {
			throw e;
		} catch (QueryResultHandlerException e) {
			throw new TupleQueryResultHandlerException(e);
		}
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
			if (SUBJECT.equals(posName) || SUBJECT_JENA.equals(posName)) {
				if (subject != null) {
					throw new QueryResultParseException(
							posName + " field encountered twice in triple value: ",
							jp.getCurrentLocation().getLineNr(),
							jp.getCurrentLocation().getColumnNr());
				}
				subject = parseValue(jp, fieldName + ":" + posName);
			} else if (PREDICATE.equals(posName) || PREDICATE_JENA.equals(posName)) {
				if (predicate != null) {
					throw new QueryResultParseException(
							posName + " field encountered twice in triple value: ",
							jp.getCurrentLocation().getLineNr(),
							jp.getCurrentLocation().getColumnNr());
				}
				predicate = parseValue(jp, fieldName + ":" + posName);
			} else if (OBJECT.equals(posName) || OBJECT_JENA.equals(posName)) {
				if (object != null) {
					throw new QueryResultParseException(
							posName + " field encountered twice in triple value: ",
							jp.getCurrentLocation().getLineNr(),
							jp.getCurrentLocation().getColumnNr());
				}
				object = parseValue(jp, fieldName + ":" + posName);
			} else if ("g".equals(posName)) {
				// silently ignore named graph field in Stardog dialect
				parseValue(jp, fieldName + ":" + posName);
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
		if (!TRIPLE.equals(type) && !TRIPLE_STARDOG.equals(type)) {
			throw new QueryResultParseException("Found a triple value but unexpected type: " + type,
					jp.getCurrentLocation().getLineNr(),
					jp.getCurrentLocation().getColumnNr());
		}

		return true;
	}
}
