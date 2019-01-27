/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.jsonld;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Collection;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFParser;
import org.eclipse.rdf4j.rio.helpers.JSONSettings;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

/**
 * An {@link RDFParser} that links to {@link JSONLDInternalTripleCallback}.
 * 
 * @author Peter Ansell
 */
public class JSONLDParser extends AbstractRDFParser implements RDFParser {

	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	/**
	 * Default constructor
	 */
	public JSONLDParser() {
		super();
	}

	/**
	 * Creates a Sesame JSONLD Parser using the given {@link ValueFactory} to create new {@link Value}s.
	 * 
	 * @param valueFactory
	 *        The ValueFactory to use
	 */
	public JSONLDParser(final ValueFactory valueFactory) {
		super(valueFactory);
	}

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.JSONLD;
	}

	@Override
	public Collection<RioSetting<?>> getSupportedSettings() {
		Collection<RioSetting<?>> result = super.getSupportedSettings();

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

	@Override
	public void parse(final InputStream in, final String baseURI)
		throws IOException,
		RDFParseException,
		RDFHandlerException
	{
		clear();

		try {
			final JSONLDInternalTripleCallback callback = new JSONLDInternalTripleCallback(getRDFHandler(),
					valueFactory, getParserConfig(), getParseErrorListener(), nodeID -> createNode(nodeID),
					() -> createNode());

			final JsonLdOptions options = new JsonLdOptions(baseURI);
			options.useNamespaces = true;

			final JsonFactory nextJsonFactory = configureNewJsonFactory();

			final JsonParser nextParser = nextJsonFactory.createParser(in);

			final Object parsedJson = JsonUtils.fromJsonParser(nextParser);

			JsonLdProcessor.toRDF(parsedJson, callback, options);
		}
		catch (final JsonLdError e) {
			throw new RDFParseException("Could not parse JSONLD", e);
		}
		catch (final JsonProcessingException e) {
			throw new RDFParseException("Could not parse JSONLD", e, e.getLocation().getLineNr(),
					e.getLocation().getColumnNr());
		}
		catch (final RuntimeException e) {
			if (e.getCause() != null && e.getCause() instanceof RDFParseException) {
				throw (RDFParseException)e.getCause();
			}
			throw e;
		}
		finally {
			clear();
		}
	}

	@Override
	public void parse(final Reader reader, final String baseURI)
		throws IOException,
		RDFParseException,
		RDFHandlerException
	{
		clear();

		try {
			final JSONLDInternalTripleCallback callback = new JSONLDInternalTripleCallback(getRDFHandler(),
					valueFactory, getParserConfig(), getParseErrorListener(), nodeID -> createNode(nodeID),
					() -> createNode());

			final JsonLdOptions options = new JsonLdOptions(baseURI);
			options.useNamespaces = true;

			final JsonFactory nextJsonFactory = configureNewJsonFactory();

			final JsonParser nextParser = nextJsonFactory.createParser(reader);

			final Object parsedJson = JsonUtils.fromJsonParser(nextParser);

			JsonLdProcessor.toRDF(parsedJson, callback, options);
		}
		catch (final JsonLdError e) {
			throw new RDFParseException("Could not parse JSONLD", e);
		}
		catch (final JsonProcessingException e) {
			throw new RDFParseException("Could not parse JSONLD", e, e.getLocation().getLineNr(),
					e.getLocation().getColumnNr());
		}
		catch (final RuntimeException e) {
			if (e.getCause() != null && e.getCause() instanceof RDFParseException) {
				throw (RDFParseException)e.getCause();
			}
			throw e;
		}
		finally {
			clear();
		}
	}

	/**
	 * Get an instance of JsonFactory configured using the settings from {@link #getParserConfig()}.
	 * 
	 * @return A newly configured JsonFactory based on the currently enabled settings
	 */
	private JsonFactory configureNewJsonFactory() {
		final JsonFactory nextJsonFactory = new JsonFactory(JSON_MAPPER);

		if (getParserConfig().isSet(JSONSettings.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)) {
			nextJsonFactory.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER,
					getParserConfig().get(JSONSettings.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER));
		}
		if (getParserConfig().isSet(JSONSettings.ALLOW_COMMENTS)) {
			nextJsonFactory.configure(JsonParser.Feature.ALLOW_COMMENTS,
					getParserConfig().get(JSONSettings.ALLOW_COMMENTS));
		}
		if (getParserConfig().isSet(JSONSettings.ALLOW_NON_NUMERIC_NUMBERS)) {
			nextJsonFactory.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS,
					getParserConfig().get(JSONSettings.ALLOW_NON_NUMERIC_NUMBERS));
		}
		if (getParserConfig().isSet(JSONSettings.ALLOW_NUMERIC_LEADING_ZEROS)) {
			nextJsonFactory.configure(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS,
					getParserConfig().get(JSONSettings.ALLOW_NUMERIC_LEADING_ZEROS));
		}
		if (getParserConfig().isSet(JSONSettings.ALLOW_SINGLE_QUOTES)) {
			nextJsonFactory.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES,
					getParserConfig().get(JSONSettings.ALLOW_SINGLE_QUOTES));
		}
		if (getParserConfig().isSet(JSONSettings.ALLOW_UNQUOTED_CONTROL_CHARS)) {
			nextJsonFactory.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS,
					getParserConfig().get(JSONSettings.ALLOW_UNQUOTED_CONTROL_CHARS));
		}
		if (getParserConfig().isSet(JSONSettings.ALLOW_UNQUOTED_FIELD_NAMES)) {
			nextJsonFactory.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES,
					getParserConfig().get(JSONSettings.ALLOW_UNQUOTED_FIELD_NAMES));
		}
		if (getParserConfig().isSet(JSONSettings.ALLOW_YAML_COMMENTS)) {
			nextJsonFactory.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS,
					getParserConfig().get(JSONSettings.ALLOW_YAML_COMMENTS));
		}
		if (getParserConfig().isSet(JSONSettings.ALLOW_TRAILING_COMMA)) {
			nextJsonFactory.configure(JsonParser.Feature.ALLOW_TRAILING_COMMA,
					getParserConfig().get(JSONSettings.ALLOW_TRAILING_COMMA));
		}
		if (getParserConfig().isSet(JSONSettings.INCLUDE_SOURCE_IN_LOCATION)) {
			nextJsonFactory.configure(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION,
					getParserConfig().get(JSONSettings.INCLUDE_SOURCE_IN_LOCATION));
		}
		if (getParserConfig().isSet(JSONSettings.STRICT_DUPLICATE_DETECTION)) {
			nextJsonFactory.configure(JsonParser.Feature.STRICT_DUPLICATE_DETECTION,
					getParserConfig().get(JSONSettings.STRICT_DUPLICATE_DETECTION));
		}
		return nextJsonFactory;
	}

}
