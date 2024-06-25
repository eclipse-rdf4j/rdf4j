/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.ndjsonld;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.input.BOMInputStream;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.jsonld.JSONLDParser;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import no.hasmac.jsonld.JsonLdError;
import no.hasmac.jsonld.document.Document;
import no.hasmac.jsonld.document.JsonDocument;

/**
 * Introduce a parser capable of parsing Newline Delimited JSON-LD, where each line is a serialized JSON-LD record. The
 * format is inspired by Newline Delimited JSON format<a>http://ndjson.org/</a>. Even though each line is a separate
 * JSON-LD document, the whole document is treated as a single RDF document, having one single BNodes context to
 * preserve BNodes identifiers.
 *
 * @author Desislava Hristova
 */
public class NDJSONLDParser extends JSONLDParser {

	/**
	 * Default constructor
	 */
	public NDJSONLDParser() {
		super();
	}

	/**
	 * Creates a RDF4J NDJSONLD Parser using the given {@link ValueFactory} to create new {@link Value}s.
	 *
	 * @param valueFactory The ValueFactory to use
	 */
	public NDJSONLDParser(final ValueFactory valueFactory) {
		super(valueFactory);
	}

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.NDJSONLD;
	}

	@Override
	protected Document getDocument(InputStream in, Reader reader) throws JsonLdError, IOException {
		if (in != null && reader == null) {
			reader = new InputStreamReader(in, StandardCharsets.UTF_8);
		}

		List<JsonValue> arrayOfJSONLD = new ArrayList<>();
		try (BufferedReader bufferedReader = new BufferedReader(reader)) {
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				if (!line.isEmpty()) {
					JsonDocument jsonDocument = JsonDocument
							.of(new ByteArrayInputStream(line.getBytes(StandardCharsets.UTF_8)));
					Optional<JsonStructure> jsonContent = jsonDocument.getJsonContent();
					if (jsonContent.isPresent()) {
						JsonStructure jsonStructure = jsonContent.get();
						if (jsonStructure.getValueType() == JsonStructure.ValueType.ARRAY) {
							arrayOfJSONLD.addAll(jsonStructure.asJsonArray());
						}
						arrayOfJSONLD.add(jsonContent.get());
					}
				}
			}

			JsonArrayBuilder arrayBuilder = Json.createArrayBuilder(arrayOfJSONLD);
			JsonArray jsonArray = arrayBuilder.build();

			return JsonDocument.of(jsonArray);
		}

	}

	@Override
	public void parse(InputStream in, String baseURI) throws RDFParseException, RDFHandlerException, IOException {
		if (in == null) {
			throw new IllegalArgumentException("Input stream must not be 'null'");
		}

		parse(new InputStreamReader(new BOMInputStream(in, false), StandardCharsets.UTF_8), baseURI);
	}
}
