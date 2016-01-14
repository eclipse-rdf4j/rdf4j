/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.sparqljson;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.resultio.AbstractQueryResultParser;
import org.eclipse.rdf4j.query.resultio.QueryResultParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for SPARQL Results JSON Parsers. Provides a common
 * implementation of both boolean and tuple parsing.
 * 
 * @author Peter Ansell
 * @author Sebastian Schaffert
 */
public abstract class AbstractSPARQLJSONParser extends AbstractQueryResultParser {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private static final JsonFactory JSON_FACTORY = new JsonFactory();

	static {
		JSON_FACTORY.disable(JsonFactory.Feature.INTERN_FIELD_NAMES);
		JSON_FACTORY.disable(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES);
		JSON_FACTORY.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
	}

	public static final String HEAD = "head";

	public static final String LINK = "link";

	public static final String VARS = "vars";

	public static final String BOOLEAN = "boolean";

	public static final String RESULTS = "results";

	public static final String BINDINGS = "bindings";

	public static final String TYPE = "type";

	public static final String VALUE = "value";

	public static final String XMLLANG = "xml:lang";

	public static final String DATATYPE = "datatype";

	public static final String LITERAL = "literal";

	public static final String TYPED_LITERAL = "typed-literal";

	public static final String BNODE = "bnode";

	public static final String URI = "uri";

	/**
	 * Backwards compatibility with very early version of original SPARQL spec.
	 */
	private static final String DISTINCT = "distinct";

	/**
	 * Backwards compatibility with very early version of original SPARQL spec.
	 */
	private static final String ORDERED = "ordered";

	/**
	 * 
	 */
	public AbstractSPARQLJSONParser() {
		super();
	}

	/**
	 * 
	 */
	public AbstractSPARQLJSONParser(ValueFactory valueFactory) {
		super(valueFactory);
	}

	@Override
	public void parseQueryResult(InputStream in)
		throws IOException, QueryResultParseException, QueryResultHandlerException
	{
		parseQueryResultInternal(in, true, true);
	}

	protected boolean parseQueryResultInternal(InputStream in, boolean attemptParseBoolean,
			boolean attemptParseTuple)
		throws IOException, QueryResultParseException, QueryResultHandlerException
	{
		if (!attemptParseBoolean && !attemptParseTuple) {
			throw new IllegalArgumentException(
					"Internal error: Did not specify whether to parse as either boolean and/or tuple");
		}

		JsonParser jp = JSON_FACTORY.createParser(in);
		boolean result = false;

		if (jp.nextToken() != JsonToken.START_OBJECT) {
			throw new QueryResultParseException("Expected SPARQL Results JSON document to start with an Object",
					jp.getCurrentLocation().getLineNr(), jp.getCurrentLocation().getColumnNr());
		}

		List<String> varsList = new ArrayList<String>();
		boolean varsFound = false;
		Set<BindingSet> bindings = new HashSet<BindingSet>();

		while (jp.nextToken() != JsonToken.END_OBJECT) {

			final String baseStr = jp.getCurrentName();

			if (baseStr.equals(HEAD)) {
				if (jp.nextToken() != JsonToken.START_OBJECT) {
					throw new QueryResultParseException("Did not find object under " + baseStr + " field",
							jp.getCurrentLocation().getLineNr(), jp.getCurrentLocation().getColumnNr());
				}

				while (jp.nextToken() != JsonToken.END_OBJECT) {
					final String headStr = jp.getCurrentName();

					if (headStr.equals(VARS)) {
						if (!attemptParseTuple) {
							throw new QueryResultParseException(
									"Found tuple results variables when attempting to parse SPARQL Results JSON to boolean result");
						}
						
						if (jp.nextToken() != JsonToken.START_ARRAY) {
							throw new QueryResultParseException("Expected variable labels to be an array",
									jp.getCurrentLocation().getLineNr(), jp.getCurrentLocation().getColumnNr());
						}

						while (jp.nextToken() != JsonToken.END_ARRAY) {
							varsList.add(jp.getText());
						}

						if (this.handler != null) {
							handler.startQueryResult(varsList);
						}

						varsFound = true;

						// If the bindings were populated before this point push them
						// out now.
						if (!bindings.isEmpty() && this.handler != null) {
							for (BindingSet nextBinding : bindings) {
								handler.handleSolution(nextBinding);
								handler.endQueryResult();
							}
							bindings.clear();
						}

					}
					else if (headStr.equals(LINK)) {
						List<String> linksList = new ArrayList<String>();
						if (jp.nextToken() != JsonToken.START_ARRAY) {
							throw new QueryResultParseException("Expected links to be an array",
									jp.getCurrentLocation().getLineNr(), jp.getCurrentLocation().getColumnNr());
						}

						while (jp.nextToken() != JsonToken.END_ARRAY) {
							linksList.add(jp.getText());
						}

						if (this.handler != null) {
							handler.handleLinks(linksList);
						}

					}
					else {
						throw new QueryResultParseException("Found unexpected object in head field: " + headStr,
								jp.getCurrentLocation().getLineNr(), jp.getCurrentLocation().getColumnNr());
					}
				}
			}
			else if (baseStr.equals(RESULTS)) {
				if (!attemptParseTuple) {
					throw new QueryResultParseException(
							"Found tuple results bindings when attempting to parse SPARQL Results JSON to boolean result");
				}
				if (jp.nextToken() != JsonToken.START_OBJECT) {
					throw new QueryResultParseException("Found unexpected token in results object: "
							+ jp.getCurrentName(), jp.getCurrentLocation().getLineNr(),
							jp.getCurrentLocation().getColumnNr());
				}

				while (jp.nextToken() != JsonToken.END_OBJECT) {

					if (jp.getCurrentName().equals(BINDINGS)) {
						if (jp.nextToken() != JsonToken.START_ARRAY) {
							throw new QueryResultParseException("Found unexpected token in bindings object",
									jp.getCurrentLocation().getLineNr(), jp.getCurrentLocation().getColumnNr());
						}

						while (jp.nextToken() != JsonToken.END_ARRAY) {

							MapBindingSet nextBindingSet = new MapBindingSet();

							if (jp.getCurrentToken() != JsonToken.START_OBJECT) {
								throw new QueryResultParseException("Did not find object in bindings array: "
										+ jp.getCurrentName(), jp.getCurrentLocation().getLineNr(),
										jp.getCurrentLocation().getColumnNr());
							}

							while (jp.nextToken() != JsonToken.END_OBJECT) {

								if (jp.getCurrentToken() != JsonToken.FIELD_NAME) {
									throw new QueryResultParseException("Did not find binding name",
											jp.getCurrentLocation().getLineNr(), jp.getCurrentLocation().getColumnNr());
								}

								final String bindingStr = jp.getCurrentName();

								if (jp.nextToken() != JsonToken.START_OBJECT) {
									throw new QueryResultParseException("Did not find object for binding value",
											jp.getCurrentLocation().getLineNr(), jp.getCurrentLocation().getColumnNr());
								}

								String lang = null;
								String type = null;
								String datatype = null;
								String value = null;

								while (jp.nextToken() != JsonToken.END_OBJECT) {

									if (jp.getCurrentToken() != JsonToken.FIELD_NAME) {
										throw new QueryResultParseException("Did not find value attribute under "
												+ bindingStr + " field", jp.getCurrentLocation().getLineNr(),
												jp.getCurrentLocation().getColumnNr());
									}
									String fieldName = jp.getCurrentName();

									// move to the value token
									jp.nextToken();

									// set the appropriate state variable
									if (TYPE.equals(fieldName)) {
										type = jp.getText();
									}
									else if (XMLLANG.equals(fieldName)) {
										lang = jp.getText();
									}
									else if (DATATYPE.equals(fieldName)) {
										datatype = jp.getText();
									}
									else if (VALUE.equals(fieldName)) {
										value = jp.getText();
									}
									else {
										throw new QueryResultParseException("Unexpected field name: " + fieldName,
												jp.getCurrentLocation().getLineNr(),
												jp.getCurrentLocation().getColumnNr());

									}
								}

								nextBindingSet.addBinding(bindingStr, parseValue(type, value, lang, datatype));
							}
							// parsing of solution finished, report result return to
							// bindings state
							if (!varsFound) {
								// Buffer the bindings to fit with the
								// QueryResultHandler contract so that startQueryResults
								// is
								// always called before handleSolution
								bindings.add(nextBindingSet);
							}
							else if (handler != null) {
								handler.handleSolution(nextBindingSet);
							}
						}
						if (handler != null) {
							handler.endQueryResult();
						}
					}
					// Backwards compatibility with very old draft of the original
					// SPARQL spec
					else if (jp.getCurrentName().equals(DISTINCT) || jp.getCurrentName().equals(ORDERED)) {
						jp.nextToken();
					}
					else {
						throw new QueryResultParseException("Found unexpected field in results: "
								+ jp.getCurrentName(), jp.getCurrentLocation().getLineNr(),
								jp.getCurrentLocation().getColumnNr());
					}
				}
			}
			else if (baseStr.equals(BOOLEAN)) {
				if (!attemptParseBoolean) {
					throw new QueryResultParseException(
							"Found boolean results when attempting to parse SPARQL Results JSON to tuple results");
				}
				jp.nextToken();

				result = Boolean.parseBoolean(jp.getText());
				if (handler != null) {
					handler.handleBoolean(result);
				}
			}
			else {
				throw new QueryResultParseException("Found unexpected object in top level " + baseStr + " field",
						jp.getCurrentLocation().getLineNr(), jp.getCurrentLocation().getColumnNr());
			}
		}

		return result;
	}

	/**
	 * Parse a value out of the elements for a binding.
	 * 
	 * @param type
	 *        {@link #LITERAL}, {@link #TYPED_LITERAL}, {@link #BNODE} or
	 *        {@link #URI}
	 * @param value
	 *        actual value text
	 * @param language
	 *        language tag, if applicable
	 * @param datatype
	 *        datatype tag, if applicable
	 * @return the value corresponding to the given parameters
	 */
	private Value parseValue(String type, String value, String language, String datatype) {
		logger.trace("type: {}", type);
		logger.trace("value: {}", value);
		logger.trace("language: {}", language);
		logger.trace("datatype: {}", datatype);

		Value result = null;

		if (type.equals(LITERAL) || type.equals(TYPED_LITERAL)) {
			if (language != null) {
				result = valueFactory.createLiteral(value, language);
			}
			else if (datatype != null) {
				result = valueFactory.createLiteral(value, valueFactory.createIRI(datatype));
			}
			else {
				result = valueFactory.createLiteral(value);
			}
		}
		else if (type.equals(BNODE)) {
			result = valueFactory.createBNode(value);
		}
		else if (type.equals(URI)) {
			result = valueFactory.createIRI(value);
		}

		logger.debug("result value: {}", result);

		return result;
	}
}