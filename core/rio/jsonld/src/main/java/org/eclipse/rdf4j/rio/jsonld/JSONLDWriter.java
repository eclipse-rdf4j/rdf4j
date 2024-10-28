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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.io.CharSink;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFWriter;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import com.github.jsonldjava.core.JsonLdConsts;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonStructure;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import no.hasmac.jsonld.JsonLd;
import no.hasmac.jsonld.JsonLdOptions;
import no.hasmac.jsonld.document.Document;
import no.hasmac.jsonld.document.JsonDocument;
import no.hasmac.jsonld.serialization.RdfToJsonld;
import no.hasmac.rdf.RdfDataset;
import no.hasmac.rdf.RdfGraph;
import no.hasmac.rdf.RdfLiteral;
import no.hasmac.rdf.RdfNQuad;
import no.hasmac.rdf.RdfResource;
import no.hasmac.rdf.RdfTriple;
import no.hasmac.rdf.RdfValue;

/**
 * An RDFWriter for JSON-LD 1.1
 *
 * @author Håvard M. Ottestad
 */
public class JSONLDWriter extends AbstractRDFWriter implements CharSink {

	private final Model model = new LinkedHashModel();

	private final StatementCollector statementCollector = new StatementCollector(model);

	private final String baseURI;

	private final Writer writer;

	private static final SimpleValueFactory vf = SimpleValueFactory.getInstance();

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
		this.writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
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
		try {

			JsonLdOptions opts = new JsonLdOptions();
			WriterConfig writerConfig = getWriterConfig();

			if (writerConfig.get(JSONLDSettings.HIERARCHICAL_VIEW)) {
				throw new UnsupportedOperationException(
						"Hierarchical view is not supported by this JSON-LD processor. Use org.eclipse.rdf4j.rio.jsonld.legacy.JSONLDWriter instead.");
			}

			opts.setCompactArrays(writerConfig.get(JSONLDSettings.COMPACT_ARRAYS));
			opts.setUseRdfType(writerConfig.get(JSONLDSettings.USE_RDF_TYPE));
			opts.setUseNativeTypes(writerConfig.get(JSONLDSettings.USE_NATIVE_TYPES));
			opts.setProduceGeneralizedRdf(writerConfig.get(JSONLDSettings.PRODUCE_GENERALIZED_RDF));
			opts.setUriValidation(false);
			opts.setExceptionOnWarning(writerConfig.get(JSONLDSettings.EXCEPTION_ON_WARNING));

			// opts.optimize = getWriterConfig().get(JSONLDSettings.OPTIMIZE);
			if (baseURI != null && writerConfig.get(BasicWriterSettings.BASE_DIRECTIVE)) {
				opts.setBase(URI.create(baseURI));
			}

			JsonStructure jsonld = RdfToJsonld.with(new RdfDataset() {
				@Override
				public RdfGraph getDefaultGraph() {
					return new RdfGraph() {
						@Override
						public boolean contains(RdfTriple triple) {
							return model.contains(toRdf4jResource(triple.getSubject()),
									toRdf4jIri(triple.getPredicate()), toRdf4jValue(triple.getObject()),
									new Resource[] { null });
						}

						@Override
						public List<RdfTriple> toList() {
							return model.filter(null, null, null, new Resource[] { null })
									.stream()
									.map(JSONLDWriter::toRdfTriple)
									.collect(Collectors.toList());
						}
					};
				}

				@Override
				public RdfDataset add(RdfNQuad nquad) {
					throw new UnsupportedOperationException();
				}

				@Override
				public RdfDataset add(RdfTriple triple) {
					throw new UnsupportedOperationException();
				}

				@Override
				public List<RdfNQuad> toList() {
					return model.filter(null, null, null)
							.stream()
							.map(JSONLDWriter::toRdfNQuad)
							.collect(Collectors.toList());
				}

				@Override
				public Set<RdfResource> getGraphNames() {
					return model.contexts()
							.stream()
							.filter(Objects::nonNull)
							.map(JSONLDWriter::toRdfResource)
							.collect(Collectors.toSet());
				}

				@Override
				public Optional<RdfGraph> getGraph(RdfResource graphName) {

					Resource context = toRdf4jResource(graphName);

					if (model.contexts().contains(context)) {
						return Optional.of(new RdfGraph() {
							@Override
							public boolean contains(RdfTriple triple) {
								return model.contains(toRdf4jResource(triple.getSubject()),
										toRdf4jIri(triple.getPredicate()), toRdf4jValue(triple.getObject()), context);
							}

							@Override
							public List<RdfTriple> toList() {
								return model.filter(null, null, null, context)
										.stream()
										.map(JSONLDWriter::toRdfTriple)
										.collect(Collectors.toList());
							}
						});
					}

					return Optional.empty();

				}

				@Override
				public int size() {
					return model.size();
				}

			})
					.useNativeTypes(writerConfig.get(JSONLDSettings.USE_NATIVE_TYPES))
					.useRdfType(writerConfig.get(JSONLDSettings.USE_RDF_TYPE))
					.build();

			JSONLDMode mode = mapJsonLdMode(getWriterConfig().get(JSONLDSettings.JSONLD_MODE));

			switch (mode) {
			case EXPAND:
				jsonld = JsonLd.expand(JsonDocument.of(jsonld)).options(opts).get();
				break;
			case FRAME:
				Document frame = Objects.requireNonNull(
						getWriterConfig().get(JSONLDSettings.FRAME),
						"Frame Document is required for JSON-LD mode FRAME"
				);
				jsonld = JsonLd.frame(JsonDocument.of(jsonld), frame).options(opts).get();
				break;
			case FLATTEN:
				jsonld = JsonLd.flatten(JsonDocument.of(jsonld)).options(opts).get();
				break;
			case COMPACT:
				JsonObjectBuilder context = Json.createObjectBuilder();
				for (Namespace namespace : model.getNamespaces()) {
					if (namespace.getPrefix().isEmpty()) {
						context.add("@vocab", namespace.getName());
					} else {
						context.add(namespace.getPrefix(), namespace.getName());
					}
				}
				jsonld = JsonLd.compact(JsonDocument.of(jsonld), JsonDocument.of(context.build())).options(opts).get();
				break;
			}

			if (writerConfig.get(BasicWriterSettings.PRETTY_PRINT)) {
				JsonWriterFactory writerFactory = Json.createWriterFactory(Map.of(JsonGenerator.PRETTY_PRINTING, true));
				JsonWriter jsonWriter = writerFactory.createWriter(writer);
				jsonWriter.write(jsonld);
			} else {
				JsonWriter jsonWriter = Json.createWriter(writer);

				jsonWriter.write(jsonld);
			}

			writer.flush();

		} catch (no.hasmac.jsonld.JsonLdError | IOException e) {
			throw new RDFHandlerException("Could not render JSONLD", e);
		}
	}

	private JSONLDMode mapJsonLdMode(Object jsonldMode) {
		if (jsonldMode instanceof JSONLDMode) {
			return (JSONLDMode) jsonldMode;
		}
		if (jsonldMode instanceof org.eclipse.rdf4j.rio.helpers.JSONLDMode) {
			return JSONLDMode.valueOf(jsonldMode.toString());
		}
		throw new IllegalArgumentException("Unknown JSONLDMode: " + jsonldMode);
	}

	private static RdfNQuad toRdfNQuad(Statement statement) {
		return new RdfNQuadAdapter(statement);
	}

	private static RdfTriple toRdfTriple(Statement statement) {
		return new RdfTripleAdapter(statement);
	}

	private static RdfValue toRdfValue(Value node) {
		if (node.isResource()) {
			return toRdfResource((Resource) node);
		} else if (node.isLiteral()) {
			return new RdfLiteralAdapter(node);

		}
		throw new IllegalArgumentException("Unknown type of node: " + node);

	}

	private static RdfResource toRdfResource(Resource node) {
		return new RdfResourceAdapter(node);
	}

	private Value toRdf4jValue(RdfValue node) {
		if (node instanceof RdfResourceAdapter) {
			return ((RdfResourceAdapter) node).node;
		}

		if (node instanceof RdfLiteralAdapter) {
			return ((RdfLiteralAdapter) node).node;
		}

		if (node.isIRI()) {
			return vf.createIRI(node.getValue());
		} else if (node.isBlankNode()) {
			return vf.createBNode(node.getValue());
		} else if (node.isLiteral()) {
			RdfLiteral literal = node.asLiteral();
			if (literal.getLanguage().isPresent()) {
				return vf.createLiteral(node.getValue(), literal.getLanguage().get());
			}
			if (literal.getDatatype() != null) {
				return vf.createLiteral(node.getValue(), vf.createIRI(literal.getDatatype()));
			}

			return vf.createLiteral(node.getValue());
		}
		throw new IllegalArgumentException("Unknown type of node: " + node);

	}

	private IRI toRdf4jIri(RdfResource node) {
		if (node.isIRI()) {
			if (node instanceof RdfResourceAdapter) {
				return (IRI) ((RdfResourceAdapter) node).node;
			}
			return vf.createIRI(node.getValue());
		}
		throw new IllegalArgumentException("Unknown type of node: " + node);
	}

	private Resource toRdf4jResource(RdfResource node) {
		if (node instanceof RdfResourceAdapter) {
			return ((RdfResourceAdapter) node).node;
		}

		if (node.isIRI()) {
			return vf.createIRI(node.getValue());
		} else if (node.isBlankNode()) {
			return vf.createBNode(node.getValue().substring(2));
		}
		throw new IllegalArgumentException("Unknown type of node: " + node);
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
		result.add(JSONLDSettings.JSONLD_MODE);
		result.add(JSONLDSettings.USE_RDF_TYPE);
		result.add(JSONLDSettings.USE_NATIVE_TYPES);
		result.add(JSONLDSettings.PRODUCE_GENERALIZED_RDF);
		result.add(JSONLDSettings.EXCEPTION_ON_WARNING);
		result.add(JSONLDSettings.FRAME);

		result.add(org.eclipse.rdf4j.rio.helpers.JSONLDSettings.COMPACT_ARRAYS);
		result.add(org.eclipse.rdf4j.rio.helpers.JSONLDSettings.JSONLD_MODE);
		result.add(org.eclipse.rdf4j.rio.helpers.JSONLDSettings.USE_RDF_TYPE);
		result.add(org.eclipse.rdf4j.rio.helpers.JSONLDSettings.USE_NATIVE_TYPES);
		result.add(org.eclipse.rdf4j.rio.helpers.JSONLDSettings.PRODUCE_GENERALIZED_RDF);
		result.add(org.eclipse.rdf4j.rio.helpers.JSONLDSettings.EXCEPTION_ON_WARNING);
		result.add(org.eclipse.rdf4j.rio.helpers.JSONLDSettings.FRAME);

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

	private static class RdfLiteralAdapter implements RdfLiteral {
		private final Value node;

		public RdfLiteralAdapter(Value node) {
			this.node = node;
		}

		@Override
		public String getValue() {
			return node.stringValue();
		}

		@Override
		public boolean isLiteral() {
			return true;
		}

		@Override
		public String getDatatype() {
			return ((Literal) node).getDatatype().stringValue();
		}

		@Override
		public Optional<String> getLanguage() {
			return ((Literal) node).getLanguage();
		}

		@Override
		public int hashCode() {
			return Objects.hash(getDatatype(), getLanguage().orElse(null), getValue());
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (obj instanceof RdfLiteral) {
				RdfLiteral other = (RdfLiteral) obj;
				return Objects.equals(getDatatype(), other.getDatatype())
						&& Objects.equals(getLanguage().orElse(null), other.getLanguage().orElse(""))
						&& Objects.equals(getValue(), other.getValue());
			}

			return false;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();

			builder.append(getValue());

			if (getLanguage().isPresent()) {
				builder.append('@');
				builder.append(getLanguage().get());

			} else if (getDatatype() != null) {
				builder.append("^^");
				builder.append(getDatatype());
			}

			return builder.toString();
		}

	}

	private static class RdfResourceAdapter implements RdfResource {
		private final Resource node;

		public RdfResourceAdapter(Resource node) {
			this.node = node;
		}

		@Override
		public String getValue() {
			if (node.isBNode()) {
				return "_:" + ((BNode) node).getID();
			}
			return node.stringValue();
		}

		@Override
		public boolean isIRI() {
			return node.isIRI();
		}

		@Override
		public boolean isBlankNode() {
			return node.isBNode();
		}

		@Override
		public int hashCode() {
			return Objects.hash(getValue());
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (obj instanceof RdfResource) {
				RdfResource other = (RdfResource) obj;
				return Objects.equals(getValue(), other.getValue());
			}

			return false;

		}

		@Override
		public String toString() {
			return Objects.toString(getValue());
		}
	}

	private static class RdfTripleAdapter implements RdfTriple {
		private final Statement statement;

		public RdfTripleAdapter(Statement statement) {
			this.statement = statement;
		}

		@Override
		public RdfResource getSubject() {
			return toRdfResource(statement.getSubject());
		}

		@Override
		public RdfResource getPredicate() {
			return toRdfResource(statement.getPredicate());
		}

		@Override
		public RdfValue getObject() {
			return toRdfValue(statement.getObject());
		}
	}

	private static class RdfNQuadAdapter implements RdfNQuad {
		private final Statement statement;

		public RdfNQuadAdapter(Statement statement) {
			this.statement = statement;
		}

		@Override
		public Optional<RdfResource> getGraphName() {
			if (statement.getContext() != null) {
				return Optional.of(toRdfResource(statement.getContext()));
			}
			return Optional.empty();
		}

		@Override
		public RdfResource getSubject() {
			return toRdfResource(statement.getSubject());
		}

		@Override
		public RdfResource getPredicate() {
			return toRdfResource(statement.getPredicate());
		}

		@Override
		public RdfValue getObject() {
			return toRdfValue(statement.getObject());
		}
	}
}
