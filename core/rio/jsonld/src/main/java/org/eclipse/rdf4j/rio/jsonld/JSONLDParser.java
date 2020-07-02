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
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFParser;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;
import org.eclipse.rdf4j.rio.helpers.JSONSettings;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.core.DocumentLoader;
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
	 * @param valueFactory The ValueFactory to use
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

		result.add(JSONLDSettings.DOCUMENT_LOADER);

		return result;
	}

	@Override
	public void parse(final InputStream in, final String baseURI)
			throws IOException, RDFParseException, RDFHandlerException {
		parse(in, null, baseURI);
	}

	@Override
	public void parse(final Reader reader, final String baseURI)
			throws IOException, RDFParseException, RDFHandlerException {
		parse(null, reader, baseURI);
	}

	/**
	 * Parse
	 *
	 * @param nextParser
	 * @param baseURI
	 * @throws IOException
	 * @throws RDFParseException
	 * @throws RDFHandlerException
	 */
	private void parse(InputStream in, Reader reader, String baseURI)
			throws IOException, RDFParseException, RDFHandlerException {
		clear();

		try {
			final JSONLDInternalTripleCallback callback = new JSONLDInternalTripleCallback(getRDFHandler(),
					valueFactory, getParserConfig(), getParseErrorListener(), nodeID -> createNode(nodeID),
					() -> createNode());

			final JsonLdOptions options = new JsonLdOptions(baseURI);
			options.useNamespaces = true;

			DocumentLoader loader = getParserConfig().get(JSONLDSettings.DOCUMENT_LOADER);
			if (loader != null) {
				options.setDocumentLoader(loader);
			}
			JsonFactory factory = configureNewJsonFactory();
			JsonParser nextParser = (in != null) ? factory.createParser(in) : factory.createParser(reader);
			final Object parsedJson = JsonUtils.fromJsonParser(nextParser);

			JsonLdProcessor.toRDF(parsedJson, callback, options);
		} catch (JsonLdError e) {
			throw new RDFParseException("Could not parse JSONLD", e);
		} catch (JsonProcessingException e) {
			throw new RDFParseException("Could not parse JSONLD", e, e.getLocation().getLineNr(),
					e.getLocation().getColumnNr());
		} catch (RuntimeException e) {
			if (e.getCause() != null && e.getCause() instanceof RDFParseException) {
				throw (RDFParseException) e.getCause();
			}
			throw e;
		} finally {
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
		ParserConfig parserConfig = getParserConfig();

		if (parserConfig.isSet(JSONSettings.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)) {
			nextJsonFactory.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER,
					parserConfig.get(JSONSettings.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER));
		}
		if (parserConfig.isSet(JSONSettings.ALLOW_COMMENTS)) {
			nextJsonFactory.configure(JsonParser.Feature.ALLOW_COMMENTS,
					parserConfig.get(JSONSettings.ALLOW_COMMENTS));
		}
		if (parserConfig.isSet(JSONSettings.ALLOW_NON_NUMERIC_NUMBERS)) {
			nextJsonFactory.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS,
					parserConfig.get(JSONSettings.ALLOW_NON_NUMERIC_NUMBERS));
		}
		if (parserConfig.isSet(JSONSettings.ALLOW_NUMERIC_LEADING_ZEROS)) {
			nextJsonFactory.configure(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS,
					parserConfig.get(JSONSettings.ALLOW_NUMERIC_LEADING_ZEROS));
		}
		if (parserConfig.isSet(JSONSettings.ALLOW_SINGLE_QUOTES)) {
			nextJsonFactory.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES,
					parserConfig.get(JSONSettings.ALLOW_SINGLE_QUOTES));
		}
		if (parserConfig.isSet(JSONSettings.ALLOW_UNQUOTED_CONTROL_CHARS)) {
			nextJsonFactory.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS,
					parserConfig.get(JSONSettings.ALLOW_UNQUOTED_CONTROL_CHARS));
		}
		if (parserConfig.isSet(JSONSettings.ALLOW_UNQUOTED_FIELD_NAMES)) {
			nextJsonFactory.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES,
					parserConfig.get(JSONSettings.ALLOW_UNQUOTED_FIELD_NAMES));
		}
		if (parserConfig.isSet(JSONSettings.ALLOW_YAML_COMMENTS)) {
			nextJsonFactory.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS,
					parserConfig.get(JSONSettings.ALLOW_YAML_COMMENTS));
		}
		if (parserConfig.isSet(JSONSettings.ALLOW_TRAILING_COMMA)) {
			nextJsonFactory.configure(JsonParser.Feature.ALLOW_TRAILING_COMMA,
					parserConfig.get(JSONSettings.ALLOW_TRAILING_COMMA));
		}
		if (parserConfig.isSet(JSONSettings.INCLUDE_SOURCE_IN_LOCATION)) {
			nextJsonFactory.configure(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION,
					parserConfig.get(JSONSettings.INCLUDE_SOURCE_IN_LOCATION));
		}
		if (parserConfig.isSet(JSONSettings.STRICT_DUPLICATE_DETECTION)) {
			nextJsonFactory.configure(JsonParser.Feature.STRICT_DUPLICATE_DETECTION,
					parserConfig.get(JSONSettings.STRICT_DUPLICATE_DETECTION));
		}
		return nextJsonFactory;
	}
}
