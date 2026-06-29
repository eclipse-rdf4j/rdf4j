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

import static org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLTripleTermResultsJSONConstants.OBJECT;
import static org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLTripleTermResultsJSONConstants.PREDICATE;
import static org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLTripleTermResultsJSONConstants.SUBJECT;
import static org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLTripleTermResultsJSONConstants.TRIPLE_STARDOG;
import static org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLTripleTermResultsJSONConstants.TRIPLE_TERM;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.TripleTerm;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.resultio.QueryResultFormat;
import org.eclipse.rdf4j.query.resultio.QueryResultParseException;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultParser;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

/**
 * Parser for SPARQL-1.1 JSON Results Format documents.
 *
 * @author Peter Ansell
 * @see <a href="http://www.w3.org/TR/sparql11-results-json/">SPARQL 1.1 Query Results JSON Format</a>
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
	protected TripleTerm parseTripleTermValue(JsonParser jp, String fieldName) throws IOException {
		Value subject = null, predicate = null, object = null;

		while (jp.nextToken() != JsonToken.END_OBJECT) {
			if (jp.currentToken() != JsonToken.PROPERTY_NAME) {
				throw new QueryResultParseException("Did not find triple attribute in triple value",
						jp.currentLocation().getLineNr(),
						jp.currentLocation().getColumnNr());
			}
			String posName = jp.currentName();
			String normalizedPosName = normalizeTriplePosition(posName);
			if (SUBJECT.equals(normalizedPosName)) {
				if (subject != null) {
					throw new QueryResultParseException(
							posName + " field encountered twice in triple value: ",
							jp.currentLocation().getLineNr(),
							jp.currentLocation().getColumnNr());
				}
				subject = parseValue(jp, fieldName + ":" + posName);
			} else if (PREDICATE.equals(normalizedPosName)) {
				if (predicate != null) {
					throw new QueryResultParseException(
							posName + " field encountered twice in triple value: ",
							jp.currentLocation().getLineNr(),
							jp.currentLocation().getColumnNr());
				}
				predicate = parseValue(jp, fieldName + ":" + posName);
			} else if (OBJECT.equals(normalizedPosName)) {
				if (object != null) {
					throw new QueryResultParseException(
							posName + " field encountered twice in triple value: ",
							jp.currentLocation().getLineNr(),
							jp.currentLocation().getColumnNr());
				}
				object = parseValue(jp, fieldName + ":" + posName);
			} else if ("g".equals(posName)) {
				// silently ignore named graph field in Stardog dialect
				parseValue(jp, fieldName + ":" + posName);
			} else {
				throw new QueryResultParseException("Unexpected field name in triple value: " + posName,
						jp.currentLocation().getLineNr(),
						jp.currentLocation().getColumnNr());
			}
		}

		if (subject instanceof Resource && predicate instanceof IRI && object != null) {
			return valueFactory.createTripleTerm((Resource) subject, (IRI) predicate, object);
		} else {
			throw new QueryResultParseException("Incomplete or invalid triple value",
					jp.currentLocation().getLineNr(),
					jp.currentLocation().getColumnNr());
		}
	}

	@Override
	protected boolean checkTripleType(JsonParser jp, String type) {
		if (!TRIPLE_TERM.equals(type) && !TRIPLE_STARDOG.equals(type)) {
			throw new QueryResultParseException("Found a triple value but unexpected type: " + type,
					jp.currentLocation().getLineNr(),
					jp.currentLocation().getColumnNr());
		}

		return true;
	}

	private String normalizeTriplePosition(String posName) {
		return switch (posName) {
		case "s" -> SUBJECT;
		case "p" -> PREDICATE;
		case "o" -> OBJECT;
		default -> posName;
		};
	}
}
