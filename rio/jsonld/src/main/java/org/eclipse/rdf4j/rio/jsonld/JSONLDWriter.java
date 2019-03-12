/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.jsonld;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFWriter;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.JSONLDMode;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

/**
 * An RDFWriter that links to {@link JSONLDInternalRDFParser}.
 * 
 * @author Peter Ansell
 */
public class JSONLDWriter extends AbstractRDFWriter implements RDFWriter {

	private final Model model = new LinkedHashModel();

	private final StatementCollector statementCollector = new StatementCollector(model);

	private final String baseURI;

	private final Writer writer;

	/**
	 * Create a SesameJSONLDWriter using a {@link java.io.OutputStream}
	 *
	 * @param outputStream The OutputStream to write to.
	 */
	public JSONLDWriter(OutputStream outputStream) {
		this(outputStream, null);
	}

	/**
	 * Create a SesameJSONLDWriter using a {@link java.io.OutputStream}
	 *
	 * @param outputStream The OutputStream to write to.
	 */
	public JSONLDWriter(OutputStream outputStream, String baseURI) {
		this(new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)), baseURI);
	}

	/**
	 * Create a SesameJSONLDWriter using a {@link java.io.Writer}
	 *
	 * @param writer The Writer to write to.
	 */
	public JSONLDWriter(Writer writer) {
		this(writer, null);
	}

	/**
	 * Create a SesameJSONLDWriter using a {@link java.io.Writer}
	 *
	 * @param writer The Writer to write to.
	 */
	public JSONLDWriter(Writer writer, String baseURI) {
		this.baseURI = baseURI;
		this.writer = writer;
	}

	@Override
	public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
		model.setNamespace(prefix, uri);
	}

	@Override
	public void startRDF() throws RDFHandlerException {
		statementCollector.clear();
		model.clear();
	}

	@Override
	public void endRDF() throws RDFHandlerException {
		final JSONLDInternalRDFParser serialiser = new JSONLDInternalRDFParser();
		try {
			Object output = JsonLdProcessor.fromRDF(model, serialiser);

			final JSONLDMode mode = getWriterConfig().get(JSONLDSettings.JSONLD_MODE);

			final JsonLdOptions opts = new JsonLdOptions();
			// opts.addBlankNodeIDs =
			// getWriterConfig().get(BasicParserSettings.PRESERVE_BNODE_IDS);
			opts.setUseRdfType(getWriterConfig().get(JSONLDSettings.USE_RDF_TYPE));
			opts.setUseNativeTypes(getWriterConfig().get(JSONLDSettings.USE_NATIVE_TYPES));
			// opts.optimize = getWriterConfig().get(JSONLDSettings.OPTIMIZE);

			if (getWriterConfig().get(JSONLDSettings.HIERARCHICAL_VIEW)) {
				output = JSONLDHierarchicalProcessor.fromJsonLdObject(output);
			}

			if (baseURI != null && getWriterConfig().get(BasicWriterSettings.BASE_DIRECTIVE)) {
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
				localCtx.put("@context", ctx);

				output = JsonLdProcessor.compact(output, localCtx, opts);
			}
			if (getWriterConfig().get(BasicWriterSettings.PRETTY_PRINT)) {
				JsonUtils.writePrettyPrint(writer, output);
			} else {
				JsonUtils.write(writer, output);
			}

		} catch (final JsonLdError e) {
			throw new RDFHandlerException("Could not render JSONLD", e);
		} catch (final JsonGenerationException e) {
			throw new RDFHandlerException("Could not render JSONLD", e);
		} catch (final JsonMappingException e) {
			throw new RDFHandlerException("Could not render JSONLD", e);
		} catch (final IOException e) {
			throw new RDFHandlerException("Could not render JSONLD", e);
		}
	}

	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		statementCollector.handleStatement(st);
	}

	@Override
	public void handleComment(String comment) throws RDFHandlerException {
	}

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.JSONLD;
	}

	private static void addPrefixes(Map<String, Object> ctx, Set<Namespace> namespaces) {
		for (final Namespace ns : namespaces) {
			ctx.put(ns.getPrefix(), ns.getName());
		}

	}
}
