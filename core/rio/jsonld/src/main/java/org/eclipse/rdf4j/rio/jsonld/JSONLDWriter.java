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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.common.io.CharSink;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFWriter;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import com.github.jsonldjava.core.JsonLdConsts;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

/**
 * An RDFWriter that links to {@link JSONLDInternalRDFParser}.
 *
 * @author Peter Ansell
 */
public class JSONLDWriter extends AbstractRDFWriter implements CharSink {

	private final Model model = new LinkedHashModel();

	private final StatementCollector statementCollector = new StatementCollector(model);

	private final String baseURI;

	private final Writer writer;

	/**
	 * Create a JSONLDWriter using a {@link java.io.OutputStream}
	 *
	 * @param outputStream The OutputStream to write to.
	 */
	public JSONLDWriter(OutputStream outputStream) {
		this(outputStream, null);
	}

	/**
	 * Create a JSONLDWriter using a {@link java.io.OutputStream}
	 *
	 * @param outputStream The OutputStream to write to.
	 * @param baseURI      base URI
	 */
	public JSONLDWriter(OutputStream outputStream, String baseURI) {
		this.baseURI = baseURI;
		this.writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
	}

	/**
	 * Create a JSONLDWriter using a {@link java.io.Writer}
	 *
	 * @param writer The Writer to write to.
	 */
	public JSONLDWriter(Writer writer) {
		this(writer, null);
	}

	/**
	 * Create a JSONLDWriter using a {@link java.io.Writer}
	 *
	 * @param writer  The Writer to write to.
	 * @param baseURI base URI
	 */
	public JSONLDWriter(Writer writer, String baseURI) {
		this.baseURI = baseURI;
		this.writer = writer;
	}

	@Override
	public Writer getWriter() {
		return writer;
	}

	@Override
	public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
		checkWritingStarted();
		model.setNamespace(prefix, uri);
	}

	@Override
	public void startRDF() throws RDFHandlerException {
		super.startRDF();
		statementCollector.clear();
		model.clear();
	}

	@Override
	public void endRDF() throws RDFHandlerException {
		checkWritingStarted();
		final JSONLDInternalRDFParser serialiser = new JSONLDInternalRDFParser();
		try {
			final JsonLdOptions opts = new JsonLdOptions();
			// opts.addBlankNodeIDs =
			// getWriterConfig().get(BasicParserSettings.PRESERVE_BNODE_IDS);
			WriterConfig writerConfig = getWriterConfig();
			opts.setCompactArrays(writerConfig.get(JSONLDSettings.COMPACT_ARRAYS));
			opts.setProduceGeneralizedRdf(writerConfig.get(JSONLDSettings.PRODUCE_GENERALIZED_RDF));
			opts.setUseRdfType(writerConfig.get(JSONLDSettings.USE_RDF_TYPE));
			opts.setUseNativeTypes(writerConfig.get(JSONLDSettings.USE_NATIVE_TYPES));
			// opts.optimize = getWriterConfig().get(JSONLDSettings.OPTIMIZE);

			Object output = JsonLdProcessor.fromRDF(model, opts, serialiser);

			final JSONLDMode mode = getWriterConfig().get(JSONLDSettings.JSONLD_MODE);

			if (writerConfig.get(JSONLDSettings.HIERARCHICAL_VIEW)) {
				output = JSONLDHierarchicalProcessor.fromJsonLdObject(output);
			}

			if (baseURI != null && writerConfig.get(BasicWriterSettings.BASE_DIRECTIVE)) {
				opts.setBase(baseURI);
			}
			if (mode == JSONLDMode.EXPAND) {
				output = JsonLdProcessor.expand(output, opts);
			}
			// TODO: Implement inframe in JSONLDSettings
			final Object inframe = null;
			if (mode == JSONLDMode.FLATTEN) {
				output = JsonLdProcessor.flatten(output, inframe, opts);
			}
			if (mode == JSONLDMode.COMPACT) {
				final Map<String, Object> ctx = new LinkedHashMap<>();
				addPrefixes(ctx, model.getNamespaces());
				final Map<String, Object> localCtx = new HashMap<>();
				localCtx.put(JsonLdConsts.CONTEXT, ctx);

				output = JsonLdProcessor.compact(output, localCtx, opts);
			}
			if (writerConfig.get(BasicWriterSettings.PRETTY_PRINT)) {
				JsonUtils.writePrettyPrint(writer, output);
			} else {
				JsonUtils.write(writer, output);
			}

		} catch (JsonLdError | IOException e) {
			throw new RDFHandlerException("Could not render JSONLD", e);
		}
	}

	@Override
	public void consumeStatement(Statement st) throws RDFHandlerException {
		statementCollector.handleStatement(st);
	}

	@Override
	public void handleComment(String comment) throws RDFHandlerException {
		checkWritingStarted();
	}

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.JSONLD;
	}

	@Override
	public Collection<RioSetting<?>> getSupportedSettings() {
		final Collection<RioSetting<?>> result = new HashSet<>(super.getSupportedSettings());
		result.add(BasicWriterSettings.PRETTY_PRINT);
		result.add(BasicWriterSettings.BASE_DIRECTIVE);
		result.add(JSONLDSettings.COMPACT_ARRAYS);
		result.add(JSONLDSettings.HIERARCHICAL_VIEW);
		result.add(JSONLDSettings.JSONLD_MODE);
		result.add(JSONLDSettings.PRODUCE_GENERALIZED_RDF);
		result.add(JSONLDSettings.USE_RDF_TYPE);
		result.add(JSONLDSettings.USE_NATIVE_TYPES);

		return result;
	}

	/**
	 * Add name space prefixes to JSON-LD context, empty prefix gets the '@vocab' prefix
	 *
	 * @param ctx        context
	 * @param namespaces set of RDF name spaces
	 */
	private static void addPrefixes(Map<String, Object> ctx, Set<Namespace> namespaces) {
		for (final Namespace ns : namespaces) {
			ctx.put(ns.getPrefix().isEmpty() ? JsonLdConsts.VOCAB : ns.getPrefix(), ns.getName());
		}
	}
}
