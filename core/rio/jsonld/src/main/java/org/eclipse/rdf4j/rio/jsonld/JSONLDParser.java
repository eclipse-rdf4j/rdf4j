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
package org.eclipse.rdf4j.rio.jsonld;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RioConfig;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFParser;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import no.hasmac.jsonld.JsonLd;
import no.hasmac.jsonld.JsonLdError;
import no.hasmac.jsonld.JsonLdOptions;
import no.hasmac.jsonld.document.Document;
import no.hasmac.jsonld.document.JsonDocument;
import no.hasmac.jsonld.lang.Keywords;
import no.hasmac.jsonld.loader.DocumentLoader;
import no.hasmac.rdf.RdfConsumer;
import no.hasmac.rdf.RdfValueFactory;

/**
 * An {@link RDFParser} for JSON-LD 1.1
 *
 * @author Håvard M. Ottestad
 */
public class JSONLDParser extends AbstractRDFParser {
	private static final Logger logger = LoggerFactory.getLogger(JSONLDParser.class);

	/**
	 * Default constructor
	 */
	public JSONLDParser() {
		super();
	}

	/**
	 * Creates a JSONLD Parser using the given {@link ValueFactory} to create new {@link Value}s.
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

		result.add(JSONLDSettings.EXPAND_CONTEXT);
		result.add(JSONLDSettings.EXCEPTION_ON_WARNING);
		result.add(JSONLDSettings.SECURE_MODE);
		result.add(JSONLDSettings.WHITELIST);
		result.add(JSONLDSettings.DOCUMENT_LOADER);
		result.add(JSONLDSettings.DOCUMENT_LOADER_CACHE);
		result.add(org.eclipse.rdf4j.rio.helpers.JSONLDSettings.SECURE_MODE);
		result.add(org.eclipse.rdf4j.rio.helpers.JSONLDSettings.WHITELIST);
		result.add(org.eclipse.rdf4j.rio.helpers.JSONLDSettings.DOCUMENT_LOADER_CACHE);

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
	 * @param in
	 * @param reader
	 * @param baseURI
	 * @throws RDFParseException
	 * @throws RDFHandlerException
	 */
	private void parse(InputStream in, Reader reader, String baseURI)
			throws RDFParseException, RDFHandlerException, IOException {
		clear();

		if (rdfHandler != null) {
			rdfHandler.startRDF();
		}

		try {

			Document document = getDocument(in, reader);

			if (getParserConfig().get(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES)) {
				logger.warn("JSON-LD parser does not support the {} setting",
						BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES);
			}

			boolean secureMode = getParserConfig().get(JSONLDSettings.SECURE_MODE);
			Set<String> whitelist = getParserConfig().get(JSONLDSettings.WHITELIST);
			boolean documentLoaderCache = getParserConfig().get(JSONLDSettings.DOCUMENT_LOADER_CACHE);

			JsonLdOptions opts = new JsonLdOptions();
			opts.setUriValidation(false);
			opts.setExceptionOnWarning(getParserConfig().get(JSONLDSettings.EXCEPTION_ON_WARNING));

			Document context = getParserConfig().get(JSONLDSettings.EXPAND_CONTEXT);

			DocumentLoader defaultDocumentLoader = opts.getDocumentLoader();

			DocumentLoader documentLoader;
			if (getParserConfig().get(JSONLDSettings.DOCUMENT_LOADER) == null) {
				documentLoader = new CachingDocumentLoader(secureMode, whitelist, documentLoaderCache);
			} else {
				documentLoader = getParserConfig().get(JSONLDSettings.DOCUMENT_LOADER);
			}

			if (context != null) {

				opts.setExpandContext(context);

				if (context.getDocumentUrl() != null) {
					Optional<JsonStructure> jsonContent = context.getJsonContent();
					if (jsonContent.isEmpty()) {
						throw new RDFParseException("Expand context is not a valid JSON document");
					}
					opts.getContextCache().put(context.getDocumentUrl().toString(), jsonContent.get());
					opts.setDocumentLoader((uri, options) -> {
						if (uri.equals(context.getDocumentUrl())) {
							return context;
						}

						return documentLoader.loadDocument(uri, options);
					});
				}

			}

			if (opts.getDocumentLoader() == defaultDocumentLoader) {
				opts.setDocumentLoader(documentLoader);
			}

			if (baseURI != null && !baseURI.isEmpty()) {
				URI uri = new URI(baseURI);
				opts.setBase(uri);
			}

			RDFHandler rdfHandler = getRDFHandler();
			if (rdfHandler != null) {
				extractPrefixes(document, rdfHandler::handleNamespace);
			}

			JsonLd.toRdf(document).options(opts).base(baseURI).get(new RdfConsumer<>() {
				@Override
				public void handleTriple(Statement statement) {
					if (rdfHandler != null) {
						rdfHandler.handleStatement(statement);
					}
				}

				@Override
				public void handleQuad(Statement statement) {
					if (rdfHandler != null) {
						rdfHandler.handleStatement(statement);
					}
				}

			}, new RdfValueFactory<Statement, Statement, IRI, Resource, Resource, Literal, Value>() {
				@Override
				public Statement createTriple(Resource subject, IRI predicate, Value object) {
					return JSONLDParser.this.createStatement(subject, predicate, object);
				}

				@Override
				public Statement createQuad(Resource subject, IRI predicate, Value object, Resource graphName) {
					return JSONLDParser.this.createStatement(subject, predicate, object, graphName);
				}

				@Override
				public Statement createQuad(Statement statement, Resource graphName) {
					return JSONLDParser.this.createStatement(statement.getSubject(), statement.getPredicate(),
							statement.getObject(), graphName);
				}

				@Override
				public IRI createIRI(String value) {
					return JSONLDParser.this.createURI(value);
				}

				@Override
				public Resource createBlankNode(String value) {
					if (value.startsWith("_:")) {
						return JSONLDParser.this.createNode(value.substring(2));
					}
					return JSONLDParser.this.createNode(value);
				}

				@Override
				public Literal createTypedLiteral(String value, String datatype) {
					return JSONLDParser.this.createLiteral(value, null, valueFactory.createIRI(datatype));
				}

				@Override
				public Literal createString(String value) {
					return JSONLDParser.this.createLiteral(value, null, null);
				}

				@Override
				public Literal createLangString(String value, String lang) {
					return JSONLDParser.this.createLiteral(value, lang, null);
				}
			});

			if (rdfHandler != null) {
				rdfHandler.endRDF();
			}

		} catch (no.hasmac.jsonld.JsonLdError e) {
			throw new RDFParseException("Could not parse JSONLD", e);
		} catch (RuntimeException e) {
			if (e.getCause() != null && e.getCause() instanceof RDFParseException) {
				throw (RDFParseException) e.getCause();
			}
			throw e;
		} catch (URISyntaxException e) {
			throw new RDFParseException("Base uri is not a valid URI, " + baseURI, e);
		} finally {
			clear();
		}
	}

	/**
	 * This method is overridden by the NDJSONLDParser
	 *
	 * @param in
	 * @param reader
	 * @return
	 * @throws JsonLdError
	 */
	protected Document getDocument(InputStream in, Reader reader) throws JsonLdError, IOException {
		Document document;
		if (in == null && reader != null) {
			document = JsonDocument.of(reader);
		} else if (in != null && reader == null) {
			document = JsonDocument.of(in);
		} else {
			throw new IllegalArgumentException("Either in or reader must be set");
		}
		return document;
	}

	private static void extractPrefixes(Document document, BiConsumer<String, String> prefixConsumer) {
		try {
			extractPrefixes(document.getJsonContent().orElse(null), prefixConsumer);
		} catch (Exception e) {
			// extracting prefixes is best effort
			logger.error("Error extracting prefixes from JSON-LD", e);
		}
	}

	private static void extractPrefixes(JsonValue jsonValue, BiConsumer<String, String> prefixConsumer) {
		if (jsonValue == null) {
			return;
		}
		switch (jsonValue.getValueType()) {
		case ARRAY:
			jsonValue.asJsonArray().forEach(jv -> extractPrefixes(jv, prefixConsumer));
			break;
		case OBJECT:
			extractPrefixes(jsonValue.asJsonObject(), prefixConsumer);
			break;
		default:
			break;
		}
	}

	private static void extractPrefixes(JsonObject jsonObject, BiConsumer<String, String> prefixConsumer) {
		jsonObject.forEach((key, jsonValue) -> {
			if (Keywords.CONTEXT.equals(key) && JsonValue.ValueType.OBJECT == jsonValue.getValueType()) {
				extractPrefixes(jsonValue, prefixConsumer);
			} else {
				if (JsonValue.ValueType.STRING == jsonValue.getValueType()) {
					String prefix = key;
					if (Keywords.VOCAB.equals(key)) {
						prefix = "";
					} else if (key.startsWith("@")) {
						return;
					}

					String iri = ((JsonString) jsonValue).getString();
					if (iri.endsWith("#") || iri.endsWith("/") || iri.endsWith(":")) {
						prefixConsumer.accept(prefix, iri);
					}
				}
			}
		});

	}

}
