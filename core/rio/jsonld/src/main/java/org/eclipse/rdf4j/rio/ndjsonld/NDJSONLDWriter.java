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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.RDFVersionsConversionContext;
import org.eclipse.rdf4j.model.util.VersionLabel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFWriter;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.BufferedGroupingRDFHandler;
import org.eclipse.rdf4j.rio.jsonld.JSONLDWriter;

public class NDJSONLDWriter extends AbstractRDFWriter {

	private final BufferedGroupingRDFHandler bufferedGroupingRDFHandler;

	private final LinkedHashMap<String, String> namespacesBuffer;

	/**
	 * Creates a new NDJSONLDWriter that will write to the supplied OutputStream.
	 *
	 * @param outputStream The OutputStream to write the NDJSONLD document to.
	 */
	public NDJSONLDWriter(OutputStream outputStream) {
		this(outputStream, null);
	}

	public NDJSONLDWriter(Writer writer) {
		this(writer, null);
	}

	public NDJSONLDWriter(OutputStream out, String baseURI) {
		this(new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8)), baseURI);
	}

	public NDJSONLDWriter(Writer writer, String baseURI) {
		namespacesBuffer = new LinkedHashMap<>();
		bufferedGroupingRDFHandler = new BufferedGroupingRDFHandler() {

			@Override
			protected void processBuffer() throws RDFHandlerException {
				RDFVersionsConversionContext conversionContext = null;
				if (getWriterConfig().get(BasicWriterSettings.RDF_OUTPUT_VERSION) == VersionLabel.RDF_1_1 ||
						getWriterConfig().get(BasicWriterSettings.RDF_OUTPUT_VERSION) == VersionLabel.RDF_1_2_BASIC) {
					// Share a single conversion context across all JSONLD writers so that the
					// same triple term is consistently mapped to the same blank node.
					conversionContext = new RDFVersionsConversionContext();
				}

				for (Resource context : getBufferedStatements().contexts()) {
					for (Resource subject : getBufferedStatements().subjects()) {
						JSONLDWriter jsonldWriter = getJsonldWriter(writer, baseURI);
						Iterable<Statement> statements = getBufferedStatements().getStatements(subject, null, null,
								context);
						jsonldWriter.startRDF();
						jsonldWriter.setRdfVersionsConversionContext(conversionContext); // null if not needed
						for (String key : namespacesBuffer.keySet()) {
							jsonldWriter.handleNamespace(key, namespacesBuffer.get(key));
						}
						for (Statement st : statements) {
							jsonldWriter.handleStatement(st);
						}
						jsonldWriter.endRDF();
						try {
							jsonldWriter.getWriter().write(System.lineSeparator());
						} catch (IOException e) {
							throw new RDFHandlerException(e);
						}
					}
				}

				getBufferedStatements().clear();
			}
		};
	}

	private JSONLDWriter getJsonldWriter(Writer writer, String baseURI) {
		JSONLDWriter jsonldWriter = new JSONLDWriter(writer, baseURI);
		jsonldWriter.setWriterConfig(getWriterConfig());
		jsonldWriter.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, false);
		return jsonldWriter;
	}

	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		bufferedGroupingRDFHandler.handleStatement(st);
	}

	@Override
	public void startRDF() throws RDFHandlerException {
		bufferedGroupingRDFHandler.startRDF();
	}

	@Override
	public void endRDF() throws RDFHandlerException {
		bufferedGroupingRDFHandler.endRDF();
	}

	@Override
	public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
		namespacesBuffer.put(prefix, uri);
	}

	@Override
	public void handleComment(String comment) throws RDFHandlerException {
		// comments are not handled by JSON-LD Writer, so do nothing
	}

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.NDJSONLD;
	}

	@Override
	public Collection<RioSetting<?>> getSupportedSettings() {
		return new JSONLDWriter(new StringWriter()).getSupportedSettings();
	}
}
