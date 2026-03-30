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
import static org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLStarResultsJSONConstants.PREDICATE;
import static org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLStarResultsJSONConstants.SUBJECT;
import static org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLStarResultsJSONConstants.TRIPLE_STARDOG;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.resultio.AbstractQueryResultParser;
import org.eclipse.rdf4j.query.resultio.QueryResultParseException;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.JSONSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonFactoryBuilder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.StreamWriteFeature;
import com.fasterxml.jackson.core.json.JsonReadFeature;

/**
 * Abstract base class for SPARQL Results JSON Parsers. Provides a common implementation of both boolean and tuple
 * parsing.
 *
 * @author Peter Ansell
 * @author Sebastian Schaffert
 */
public abstract class AbstractSPARQLJSONParser extends AbstractQueryResultParser {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

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
	protected AbstractSPARQLJSONParser() {
		super();
	}

	/**
	 *
	 */
	protected AbstractSPARQLJSONParser(ValueFactory valueFactory) {
		super(valueFactory);
	}

	@Override
	public void parseQueryResult(InputStream in)
			throws IOException, QueryResultParseException, QueryResultHandlerException {
		parseQueryResultInternal(in, true, true);
	}

	protected boolean parseQueryResultInternal(InputStream in, boolean attemptParseBoolean, boolean attemptParseTuple)
			throws IOException, QueryResultParseException, QueryResultHandlerException {
		if (!attemptParseBoolean && !attemptParseTuple) {
			throw new IllegalArgumentException(
					"Internal error: Did not specify whether to parse as either boolean and/or tuple");
		}

		JsonParser jp;

		boolean result = false;
		try {
			jp = configureNewJsonFactory().createParser(in);

			if (jp.nextToken() != JsonToken.START_OBJECT) {
				throw new QueryResultParseException("Expected SPARQL Results JSON document to start with an Object",
						jp.getCurrentLocation().getLineNr(), jp.getCurrentLocation().getColumnNr());
			}

			List<String> varsList = new ArrayList<>();
			boolean varsFound = false;
			Set<BindingSet> bindings = new HashSet<>();

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
										"Found tuple results variables when attempting to parse SPARQL Results JSON to boolean result",
										jp.getCurrentLocation().getLineNr(), jp.getCurrentLocation().getLineNr());
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

						} else if (headStr.equals(LINK)) {
							List<String> linksList = new ArrayList<>();
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

						} else {
							throw new QueryResultParseException("Found unexpected object in head field: " + headStr,
									jp.getCurrentLocation().getLineNr(), jp.getCurrentLocation().getColumnNr());
						}
					}
				} else if (baseStr.equals(RESULTS)) {
					if (!attemptParseTuple) {
						throw new QueryResultParseException(
								"Found tuple results bindings when attempting to parse SPARQL Results JSON to boolean result",
								jp.getCurrentLocation().getLineNr(), jp.getCurrentLocation().getLineNr());
					}
					if (jp.nextToken() != JsonToken.START_OBJECT) {
						throw new QueryResultParseException(
								"Found unexpected token in results object: " + jp.getCurrentName(),
								jp.getCurrentLocation().getLineNr(), jp.getCurrentLocation().getColumnNr());
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
									throw new QueryResultParseException(
											"Did not find object in bindings array: " + jp.getCurrentName(),
											jp.getCurrentLocation().getLineNr(), jp.getCurrentLocation().getColumnNr());
								}

								while (jp.nextToken() != JsonToken.END_OBJECT) {

									if (jp.getCurrentToken() != JsonToken.FIELD_NAME) {
										throw new QueryResultParseException("Did not find binding name",
												jp.getCurrentLocation().getLineNr(),
												jp.getCurrentLocation().getColumnNr());
									}

									final String bindingStr = jp.getCurrentName();

									nextBindingSet.addBinding(bindingStr, parseValue(jp, bindingStr));
								}
								// parsing of solution finished, report result return to
								// bindings state
								if (!varsFound) {
									// Buffer the bindings to fit with the
									// QueryResultHandler contract so that startQueryResults
									// is
									// always called before handleSolution
									bindings.add(nextBindingSet);
								} else if (handler != null) {
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
						} else {
							throw new QueryResultParseException(
									"Found unexpected field in results: " + jp.getCurrentName(),
									jp.getCurrentLocation().getLineNr(), jp.getCurrentLocation().getColumnNr());
						}
					}
				} else if (baseStr.equals(BOOLEAN)) {
					if (!attemptParseBoolean) {
						throw new QueryResultParseException(
								"Found boolean results when attempting to parse SPARQL Results JSON to tuple results",
								jp.getCurrentLocation().getLineNr(), jp.getCurrentLocation().getLineNr());
					}
					jp.nextToken();

					result = Boolean.parseBoolean(jp.getText());
					if (handler != null) {
						handler.handleBoolean(result);
					}
				} else {
					logger.debug("Found unexpected object in top level {} field #{}.{}", baseStr,
							jp.getCurrentLocation().getLineNr(), jp.getCurrentLocation().getColumnNr());
					// Consume the discovered unexpected object
					// (in particular, if it is either an array or a composite object).
					jp.nextToken();

					if (jp.currentToken() == JsonToken.START_ARRAY) {
						while (!(jp.getParsingContext().getParent().inRoot()
								&& (jp.currentToken() == JsonToken.END_ARRAY))) {
							if (jp.nextToken() == null) {
								throw new QueryResultParseException(
										"An array value of the unexpected " + baseStr + " field is not closed.",
										jp.getCurrentLocation().getLineNr(), jp.getCurrentLocation().getLineNr());
							}
						}
					} else if (jp.currentToken() == JsonToken.START_OBJECT) {
						while (!(jp.getParsingContext().getParent().inRoot()
								&& (jp.currentToken() == JsonToken.END_OBJECT))) {
							if (jp.nextToken() == null) {
								throw new QueryResultParseException(
										"An object value of the unexpected " + baseStr + " field is not closed.",
										jp.getCurrentLocation().getLineNr(), jp.getCurrentLocation().getLineNr());
							}
						}
					}
				}
			}
		} catch (JsonProcessingException e) {
			throw new QueryResultParseException("Could not parse SPARQL/JSON", e, e.getLocation().getLineNr(),
					e.getLocation().getLineNr());
		}

		return result;
	}

	protected Value parseValue(JsonParser jp, String bindingStr) throws IOException {
		if (jp.nextToken() != JsonToken.START_OBJECT) {
			throw new QueryResultParseException("Did not find object for binding value",
					jp.getCurrentLocation().getLineNr(),
					jp.getCurrentLocation().getColumnNr());
		}

		String lang = null;
		String type = null;
		String datatype = null;
		String value = null;

		Triple triple = null;

		while (jp.nextToken() != JsonToken.END_OBJECT) {
			if (jp.getCurrentToken() != JsonToken.FIELD_NAME) {
				throw new QueryResultParseException(
						"Did not find value attribute under " + bindingStr + " field",
						jp.getCurrentLocation().getLineNr(),
						jp.getCurrentLocation().getColumnNr());
			}
			String fieldName = jp.getCurrentName();

			// set the appropriate state variable
			if (TYPE.equals(fieldName)) {
				type = jp.nextTextValue();
				if (TRIPLE_STARDOG.equals(type)) {
					// Stardog RDF-star serialization dialect does not wrap the triple in a value object
					triple = parseStardogTripleValue(jp, type);
					// avoid reading away the next end-of-object token by jumping out of the loop.
					break;
				}
			} else if (XMLLANG.equals(fieldName)) {
				lang = jp.nextTextValue();
			} else if (DATATYPE.equals(fieldName)) {
				datatype = jp.nextTextValue();
			} else if (VALUE.equals(fieldName)) {
				if (jp.nextToken() == JsonToken.START_OBJECT) {
					triple = parseTripleValue(jp, fieldName);
					if (jp.getCurrentToken() != JsonToken.END_OBJECT) {
						throw new QueryResultParseException("Unexpected token: " + jp.getCurrentName(),
								jp.getCurrentLocation().getLineNr(),
								jp.getCurrentLocation().getColumnNr());
					}
				} else {
					value = jp.getText();
				}
			} else {
				throw new QueryResultParseException("Unexpected field name: " + fieldName,
						jp.getCurrentLocation().getLineNr(),
						jp.getCurrentLocation().getColumnNr());

			}
		}
		if (triple != null && checkTripleType(jp, type)) {
			return triple;
		}

		return parseValue(type, value, lang, datatype);
	}

	private Triple parseStardogTripleValue(JsonParser jp, String fieldName) throws IOException {
		Value subject = null, predicate = null, object = null;

		while (jp.nextToken() != JsonToken.END_OBJECT) {
			if (jp.getCurrentToken() != JsonToken.FIELD_NAME) {
				throw new QueryResultParseException("Did not find triple attribute in triple value",
						jp.getCurrentLocation().getLineNr(),
						jp.getCurrentLocation().getColumnNr());
			}
			String posName = jp.getCurrentName();
			if (SUBJECT.equals(posName)) {
				if (subject != null) {
					throw new QueryResultParseException(
							posName + " field encountered twice in triple value: ",
							jp.getCurrentLocation().getLineNr(),
							jp.getCurrentLocation().getColumnNr());
				}
				subject = parseValue(jp, fieldName + ":" + posName);
			} else if (PREDICATE.equals(posName)) {
				if (predicate != null) {
					throw new QueryResultParseException(
							posName + " field encountered twice in triple value: ",
							jp.getCurrentLocation().getLineNr(),
							jp.getCurrentLocation().getColumnNr());
				}
				predicate = parseValue(jp, fieldName + ":" + posName);
			} else if (OBJECT.equals(posName)) {
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

	protected Triple parseTripleValue(JsonParser jp, String fieldName) throws IOException {
		throw new QueryResultParseException("Unexpected object as value", jp.getCurrentLocation().getLineNr(),
				jp.getCurrentLocation().getColumnNr());
	}

	protected boolean checkTripleType(JsonParser jp, String type) {
		throw new IllegalStateException();
	}

	/**
	 * Parse a value out of the elements for a binding.
	 *
	 * @param type     {@link #LITERAL}, {@link #TYPED_LITERAL}, {@link #BNODE} or {@link #URI}
	 * @param value    actual value text
	 * @param language language tag, if applicable
	 * @param datatype datatype tag, if applicable
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
			} else if (datatype != null) {
				IRI datatypeIri;
				datatypeIri = valueFactory.createIRI(datatype);

				// For broken SPARQL endpoints which return LANGSTRING without a language, fall back
				// to using STRING as the datatype
				if (RDF.LANGSTRING.equals(datatypeIri)) {
					logger.debug(
							"rdf:langString typed literal missing language tag: '{}'. Falling back to xsd:string.",
							StringUtils.abbreviate(value, 10)
					);
					datatypeIri = XSD.STRING;
				}

				result = valueFactory.createLiteral(value, datatypeIri);
			} else {
				result = valueFactory.createLiteral(value);
			}
		} else if (type.equals(BNODE)) {
			result = valueFactory.createBNode(value);
		} else if (type.equals(URI)) {
			result = valueFactory.createIRI(value);
		}

		logger.debug("result value: {}", result);

		return result;
	}

	@Override
	public Collection<RioSetting<?>> getSupportedSettings() {
		Collection<RioSetting<?>> result = new HashSet<>(super.getSupportedSettings());

		result.add(JSONSettings.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER);
		result.add(JSONSettings.ALLOW_COMMENTS);
		result.add(JSONSettings.ALLOW_NON_NUMERIC_NUMBERS);
		result.add(JSONSettings.ALLOW_NUMERIC_LEADING_ZEROS);
		result.add(JSONSettings.ALLOW_SINGLE_QUOTES);
		result.add(JSONSettings.ALLOW_UNQUOTED_CONTROL_CHARS);
		result.add(JSONSettings.ALLOW_UNQUOTED_FIELD_NAMES);
		result.add(JSONSettings.ALLOW_YAML_COMMENTS);
		result.add(JSONSettings.ALLOW_TRAILING_COMMA);
		result.add(JSONSettings.INCLUDE_SOURCE_IN_LOCATION);
		result.add(JSONSettings.STRICT_DUPLICATE_DETECTION);

		return result;
	}

	/**
	 * Get an instance of JsonFactory configured using the settings from {@link #getParserConfig()}.
	 *
	 * @return A newly configured JsonFactory based on the currently enabled settings
	 */
	private JsonFactory configureNewJsonFactory() {
		final JsonFactoryBuilder builder = new JsonFactoryBuilder();
		// Disable features that may work for most JSON where the field names are
		// in limited supply,
		// but does not work for SPARQL/JSON where a wide range of URIs are used for
		// subjects and predicates
		builder.disable(JsonFactory.Feature.INTERN_FIELD_NAMES);
		builder.disable(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES);
		builder.disable(StreamWriteFeature.AUTO_CLOSE_TARGET);

		if (getParserConfig().isSet(JSONSettings.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)) {
			builder.configure(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER,
					getParserConfig().get(JSONSettings.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER));
		}
		if (getParserConfig().isSet(JSONSettings.ALLOW_COMMENTS)) {
			builder.configure(JsonReadFeature.ALLOW_JAVA_COMMENTS,
					getParserConfig().get(JSONSettings.ALLOW_COMMENTS));
		}
		if (getParserConfig().isSet(JSONSettings.ALLOW_NON_NUMERIC_NUMBERS)) {
			builder.configure(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS,
					getParserConfig().get(JSONSettings.ALLOW_NON_NUMERIC_NUMBERS));
		}
		if (getParserConfig().isSet(JSONSettings.ALLOW_NUMERIC_LEADING_ZEROS)) {
			builder.configure(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS,
					getParserConfig().get(JSONSettings.ALLOW_NUMERIC_LEADING_ZEROS));
		}
		if (getParserConfig().isSet(JSONSettings.ALLOW_SINGLE_QUOTES)) {
			builder.configure(JsonReadFeature.ALLOW_SINGLE_QUOTES,
					getParserConfig().get(JSONSettings.ALLOW_SINGLE_QUOTES));
		}
		if (getParserConfig().isSet(JSONSettings.ALLOW_UNQUOTED_CONTROL_CHARS)) {
			builder.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS,
					getParserConfig().get(JSONSettings.ALLOW_UNQUOTED_CONTROL_CHARS));
		}
		if (getParserConfig().isSet(JSONSettings.ALLOW_UNQUOTED_FIELD_NAMES)) {
			builder.configure(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES,
					getParserConfig().get(JSONSettings.ALLOW_UNQUOTED_FIELD_NAMES));
		}
		if (getParserConfig().isSet(JSONSettings.ALLOW_YAML_COMMENTS)) {
			builder.configure(JsonReadFeature.ALLOW_YAML_COMMENTS,
					getParserConfig().get(JSONSettings.ALLOW_YAML_COMMENTS));
		}
		if (getParserConfig().isSet(JSONSettings.ALLOW_TRAILING_COMMA)) {
			builder.configure(JsonReadFeature.ALLOW_TRAILING_COMMA,
					getParserConfig().get(JSONSettings.ALLOW_TRAILING_COMMA));
		}
		builder.configure(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION,
				getParserConfig().get(JSONSettings.INCLUDE_SOURCE_IN_LOCATION));
		if (getParserConfig().isSet(JSONSettings.STRICT_DUPLICATE_DETECTION)) {
			builder.configure(StreamReadFeature.STRICT_DUPLICATE_DETECTION,
					getParserConfig().get(JSONSettings.STRICT_DUPLICATE_DETECTION));
		}
		return builder.build();
	}
}
