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
package org.eclipse.rdf4j.rio.rdfjson;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.common.io.CharSink;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFWriter;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.PrettyPrinter;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.TokenStreamFactory;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.core.util.DefaultPrettyPrinter.Indenter;

/**
 * {@link RDFWriter} implementation for the RDF/JSON format
 *
 * @author Peter Ansell p_ansell@yahoo.com
 */
public class RDFJSONWriter extends AbstractRDFWriter implements CharSink {

	private final Writer writer;

	private final RDFFormat actualFormat;

	private Model graph;

	private Resource lastWrittenSubject;

	private IRI lastWrittenPredicate;

	private JsonGenerator jg;

	private boolean isEmptyStream;

	private boolean isStreaming;

	public RDFJSONWriter(final OutputStream out, final RDFFormat actualFormat) {
		this.actualFormat = actualFormat;
		this.writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
	}

	public RDFJSONWriter(final Writer writer, final RDFFormat actualFormat) {
		this.writer = writer;
		this.actualFormat = actualFormat;
	}

	@Override
	public Writer getWriter() {
		return writer;
	}

	@Override
	public void startRDF() throws RDFHandlerException {
		super.startRDF();
		try {
			isStreaming = getWriterConfig().get(RDFJSONWriterSettings.ALLOW_MULTIPLE_OBJECT_VALUES);
			PrettyPrinter pp = null;
			if (getWriterConfig().get(BasicWriterSettings.PRETTY_PRINT)) {
				Indenter indenter = DefaultIndenter.SYSTEM_LINEFEED_INSTANCE;
				pp = new DefaultPrettyPrinter().withArrayIndenter(indenter).withObjectIndenter(indenter);
			}
			final PrettyPrinter finalPp = pp;
			jg = configureNewJsonFactory().createGenerator(new ObjectWriteContext.Base() {
				@Override
				public PrettyPrinter getPrettyPrinter() {
					return finalPp;
				}
			}, writer);
			if (isStreaming) {
				isEmptyStream = true;
				lastWrittenPredicate = null;
				lastWrittenSubject = null;
				jg.writeStartObject();
			} else {
				graph = new TreeModel();
			}
		} catch (final JacksonException e) {
			throw new RDFHandlerException(e);
		}
	}

	@Override
	public void endRDF() throws RDFHandlerException {
		checkWritingStarted();
		try {
			try {
				if (isStreaming) {
					if (!isEmptyStream) {
						jg.writeEndArray();
					}
					jg.writeEndObject();
					lastWrittenPredicate = null;
					lastWrittenSubject = null;
				} else {
					RDFJSONWriter.modelToRdfJsonInternal(this.graph, this.getWriterConfig(), jg);
				}
			} finally {
				jg.close();
				writer.flush();
			}
		} catch (final IOException | JacksonException e) {
			throw new RDFHandlerException(e);
		}
	}

	@Override
	public RDFFormat getRDFFormat() {
		return actualFormat;
	}

	@Override
	public Collection<RioSetting<?>> getSupportedSettings() {
		final Set<RioSetting<?>> results = new HashSet<>(super.getSupportedSettings());

		results.add(BasicWriterSettings.PRETTY_PRINT);
		results.add(RDFJSONWriterSettings.ALLOW_MULTIPLE_OBJECT_VALUES);

		return results;
	}

	@Override
	public void handleComment(final String comment) throws RDFHandlerException {
		checkWritingStarted();
		// Comments are ignored.
	}

	@Override
	public void handleNamespace(final String prefix, final String uri) throws RDFHandlerException {
		checkWritingStarted();
		// Namespace prefixes are not used in RDF/JSON.
	}

	@Override
	public void consumeStatement(final Statement statement) throws RDFHandlerException {
		if (isStreaming) {
			consumeStreamingStatement(statement);
		} else {
			graph.add(statement);
		}
	}

	/**
	 * Helper method to reduce complexity of the JSON serialisation algorithm Any null contexts will only be serialised
	 * to JSON if there are also non-null contexts in the contexts array
	 *
	 * @param object   The RDF value to serialise
	 * @param contexts The set of contexts that are relevant to this object, including null contexts as they are found.
	 * @param jg       the {@link JsonGenerator} to write to.
	 * @throws JacksonException
	 */
	protected static void writeObject(final Value object, final Set<Resource> contexts, final JsonGenerator jg) {
		jg.writeStartObject();
		if (object instanceof Literal) {
			jg.writeStringProperty(RDFJSONUtility.VALUE, object.stringValue());

			jg.writeStringProperty(RDFJSONUtility.TYPE, RDFJSONUtility.LITERAL);
			final Literal l = (Literal) object;

			if (Literals.isLanguageLiteral(l)) {
				jg.writeStringProperty(RDFJSONUtility.LANG, l.getLanguage().orElse(null));
			} else {
				jg.writeStringProperty(RDFJSONUtility.DATATYPE, l.getDatatype().stringValue());
			}
		} else if (object instanceof BNode) {
			jg.writeStringProperty(RDFJSONUtility.VALUE, resourceToString((BNode) object));

			jg.writeStringProperty(RDFJSONUtility.TYPE, RDFJSONUtility.BNODE);
		} else if (object instanceof IRI) {
			jg.writeStringProperty(RDFJSONUtility.VALUE, resourceToString((IRI) object));

			jg.writeStringProperty(RDFJSONUtility.TYPE, RDFJSONUtility.URI);
		}

		if (contexts != null && !contexts.isEmpty() && !(contexts.size() == 1 && contexts.iterator().next() == null)) {
			jg.writeArrayPropertyStart(RDFJSONUtility.GRAPHS);
			for (final Resource nextContext : contexts) {
				if (nextContext == null) {
					jg.writeNull();
				} else {
					jg.writeString(resourceToString(nextContext));
				}
			}
			jg.writeEndArray();
		}

		jg.writeEndObject();
	}

	/**
	 * Returns the correct syntax for a Resource, depending on whether it is a URI or a Blank Node (ie, BNode)
	 *
	 * @param uriOrBnode The resource to serialise to a string
	 * @return The string value of the RDF4J resource
	 */
	protected static String resourceToString(final Resource uriOrBnode) {
		if (uriOrBnode instanceof IRI) {
			return uriOrBnode.stringValue();
		} else {
			return "_:" + ((BNode) uriOrBnode).getID();
		}
	}

	protected static void modelToRdfJsonInternal(final Model graph, final WriterConfig writerConfig,
			final JsonGenerator jg) {
		jg.writeStartObject();
		for (final Resource nextSubject : graph.subjects()) {
			jg.writeObjectPropertyStart(RDFJSONWriter.resourceToString(nextSubject));
			for (final IRI nextPredicate : graph.filter(nextSubject, null, null).predicates()) {
				jg.writeArrayPropertyStart(nextPredicate.stringValue());
				for (final Value nextObject : graph.filter(nextSubject, nextPredicate, null).objects()) {
					// contexts are optional, so this may return empty in some
					// scenarios depending on the interpretation of the way contexts
					// work
					final Set<Resource> contexts = graph.filter(nextSubject, nextPredicate, nextObject).contexts();

					RDFJSONWriter.writeObject(nextObject, contexts, jg);
				}
				jg.writeEndArray();
			}
			jg.writeEndObject();
		}
		jg.writeEndObject();
	}

	/**
	 * Get an instance of JsonFactory.
	 *
	 * @return A newly configured JsonFactory based on the currently enabled settings
	 */
	private JsonFactory configureNewJsonFactory() {
		return JsonFactory.builder()
				// Disable features that may work for most JSON where the field names are
				// in limited supply, but does not work for RDF/JSON where a wide range of
				// URIs are used for subjects and predicates
				.disable(TokenStreamFactory.Feature.INTERN_PROPERTY_NAMES)
				.disable(TokenStreamFactory.Feature.CANONICALIZE_PROPERTY_NAMES)
				.disable(StreamWriteFeature.AUTO_CLOSE_TARGET)
				.build();
	}

	/**
	 * Consumes statement when RDF/JSON writer is streaming output.
	 */
	private void consumeStreamingStatement(final Statement statement) throws RDFHandlerException {
		Resource subj = statement.getSubject();
		IRI pred = statement.getPredicate();
		Value obj = statement.getObject();
		Resource context = statement.getContext();

		try {
			if (!subj.equals(lastWrittenSubject)) {
				if (lastWrittenSubject != null) {
					// close previous predicate-object array and then close the previous subject object
					jg.writeEndArray();
					jg.writeEndObject();
					lastWrittenPredicate = null;
				}

				jg.writeObjectPropertyStart(RDFJSONWriter.resourceToString(subj));
				lastWrittenSubject = subj;
			}

			if (!pred.equals(lastWrittenPredicate)) {
				if (lastWrittenPredicate != null) {
					// close previous predicate array and then close the previous subject object
					jg.writeEndArray();
				}

				jg.writeArrayPropertyStart(pred.stringValue());
				lastWrittenPredicate = pred;
			}

			writeObject(obj, Collections.singleton(context), jg);
			if (isEmptyStream) {
				isEmptyStream = false;
			}
		} catch (final JacksonException e) {
			throw new RDFHandlerException(e);
		}
	}
}
