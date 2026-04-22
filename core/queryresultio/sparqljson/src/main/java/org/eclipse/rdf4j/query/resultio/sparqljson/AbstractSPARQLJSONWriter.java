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

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.rdf4j.common.io.CharSink;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter;
import org.eclipse.rdf4j.query.resultio.BasicQueryWriterSettings;
import org.eclipse.rdf4j.rio.RioSetting;
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
 * An abstract class to implement the base functionality for both SPARQLBooleanJSONWriter and SPARQLResultsJSONWriter.
 *
 * @author Peter Ansell
 */
abstract class AbstractSPARQLJSONWriter extends AbstractQueryResultWriter implements CharSink {

	private static final JsonFactory JSON_FACTORY = JsonFactory.builder()
			// Disable features that may work for most JSON where the field names are
			// in limited supply,
			// but does not work for RDF/JSON where a wide range of URIs are used for
			// subjects and
			// predicates
			.disable(TokenStreamFactory.Feature.INTERN_PROPERTY_NAMES)
			.disable(TokenStreamFactory.Feature.CANONICALIZE_PROPERTY_NAMES)
			.disable(StreamWriteFeature.AUTO_CLOSE_TARGET)
			.build();

	protected boolean firstTupleWritten = false;

	protected boolean documentOpen = false;

	protected boolean headerOpen = false;

	protected boolean headerComplete = false;

	protected boolean tupleVariablesFound = false;

	protected boolean linksFound = false;

	// supplier for a pretty printer, which is initialized in startDocument based on writer settings
	protected Supplier<PrettyPrinter> prettyPrinterSupplier = () -> null;

	protected final JsonGenerator jg;

	private final Writer writer;

	protected AbstractSPARQLJSONWriter(OutputStream out) {
		this(new OutputStreamWriter(out, StandardCharsets.UTF_8));
	}

	protected AbstractSPARQLJSONWriter(Writer writer) {
		this.writer = writer;
		jg = JSON_FACTORY.createGenerator(new ObjectWriteContext.Base() {
			@Override
			public PrettyPrinter getPrettyPrinter() {
				return prettyPrinterSupplier.get();
			}
		}, writer);
	}

	@Override
	public final Writer getWriter() {
		return writer;
	}

	@Override
	public void endHeader() throws QueryResultHandlerException {
		if (!headerComplete) {
			try {
				jg.writeEndObject();

				if (tupleVariablesFound) {
					// Write results
					jg.writeObjectPropertyStart("results");

					jg.writeArrayPropertyStart("bindings");
				}

				headerComplete = true;
			} catch (JacksonException e) {
				throw new QueryResultHandlerException(e);
			}
		}
	}

	@Override
	public void startQueryResult(List<String> columnHeaders) throws TupleQueryResultHandlerException {
		super.startQueryResult(columnHeaders);

		try {
			if (!documentOpen) {
				startDocument();
			}

			if (!headerOpen) {
				startHeader();
			}

			tupleVariablesFound = true;
			jg.writeArrayPropertyStart("vars");
			for (String nextColumn : columnHeaders) {
				jg.writeString(nextColumn);
			}
			jg.writeEndArray();
		} catch (JacksonException | QueryResultHandlerException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	@Override
	protected void handleSolutionImpl(BindingSet bindingSet) throws TupleQueryResultHandlerException {
		try {
			if (!documentOpen) {
				startDocument();
			}

			if (!headerOpen) {
				startHeader();
			}

			if (!headerComplete) {
				endHeader();
			}

			if (!tupleVariablesFound) {
				throw new IllegalStateException("Must call startQueryResult before handleSolution");
			}

			firstTupleWritten = true;

			jg.writeStartObject();

			Iterator<Binding> bindingIter = bindingSet.iterator();
			while (bindingIter.hasNext()) {
				Binding binding = bindingIter.next();
				jg.writeName(binding.getName());
				writeValue(binding.getValue());
			}

			jg.writeEndObject();
		} catch (JacksonException | QueryResultHandlerException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	@Override
	public void endQueryResult() throws TupleQueryResultHandlerException {
		try {
			if (!documentOpen) {
				startDocument();
			}

			if (!headerOpen) {
				startHeader();
			}

			if (!headerComplete) {
				endHeader();
			}

			if (!tupleVariablesFound) {
				throw new IllegalStateException("Could not end query result as startQueryResult was not called first.");
			}

			// bindings array
			jg.writeEndArray();
			// results braces
			jg.writeEndObject();
			endDocument();
		} catch (JacksonException | QueryResultHandlerException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	@Override
	public void startDocument() throws QueryResultHandlerException {
		if (!documentOpen) {
			documentOpen = true;
			headerOpen = false;
			headerComplete = false;
			tupleVariablesFound = false;
			firstTupleWritten = false;
			linksFound = false;

			if (getWriterConfig().get(BasicWriterSettings.PRETTY_PRINT)) {
				// SES-2011: Always use \n for consistency
				Indenter indenter = DefaultIndenter.SYSTEM_LINEFEED_INSTANCE;
				// By default Jackson does not pretty print, so enable this unless
				// PRETTY_PRINT setting is disabled
				final PrettyPrinter pp = new DefaultPrettyPrinter().withArrayIndenter(indenter)
						.withObjectIndenter(indenter);
				prettyPrinterSupplier = () -> pp;
			}

			try {
				if (getWriterConfig().isSet(BasicQueryWriterSettings.JSONP_CALLBACK)) {
					// SES-1019 : Write the callbackfunction name as a wrapper for
					// the results here
					String callbackName = getWriterConfig().get(BasicQueryWriterSettings.JSONP_CALLBACK);
					jg.writeRaw(callbackName);
					jg.writeRaw("(");
				}
				jg.writeStartObject();
			} catch (JacksonException e) {
				throw new QueryResultHandlerException(e);
			}
		}
	}

	@Override
	public void handleStylesheet(String stylesheetUrl) throws QueryResultHandlerException {
		// Ignore, as JSON does not support stylesheets
	}

	@Override
	public void startHeader() throws QueryResultHandlerException {
		if (!documentOpen) {
			startDocument();
		}

		if (!headerOpen) {
			try {
				// Write header
				jg.writeObjectPropertyStart("head");

				headerOpen = true;
			} catch (JacksonException e) {
				throw new QueryResultHandlerException(e);
			}
		}
	}

	@Override
	public void handleLinks(List<String> linkUrls) throws QueryResultHandlerException {
		try {
			if (!documentOpen) {
				startDocument();
			}

			if (!headerOpen) {
				startHeader();
			}

			jg.writeArrayPropertyStart("link");
			for (String nextLink : linkUrls) {
				jg.writeString(nextLink);
			}
			jg.writeEndArray();
		} catch (JacksonException e) {
			throw new QueryResultHandlerException(e);
		}
	}

	protected void writeValue(Value value) throws QueryResultHandlerException {
		jg.writeStartObject();

		if (value instanceof IRI) {
			jg.writeStringProperty("type", "uri");
			jg.writeStringProperty("value", ((IRI) value).toString());
		} else if (value instanceof BNode) {
			jg.writeStringProperty("type", "bnode");
			jg.writeStringProperty("value", ((BNode) value).getID());
		} else if (value instanceof Literal) {
			Literal lit = (Literal) value;

			if (Literals.isLanguageLiteral(lit)) {
				jg.writeStringProperty("xml:lang", lit.getLanguage().orElse(null));
			} else {
				IRI datatype = lit.getDatatype();
				boolean ignoreDatatype = datatype.equals(XSD.STRING) && xsdStringToPlainLiteral();
				if (!ignoreDatatype) {
					jg.writeStringProperty("datatype", lit.getDatatype().stringValue());
				}
			}

			jg.writeStringProperty("type", "literal");

			jg.writeStringProperty("value", lit.getLabel());
		} else {
			throw new TupleQueryResultHandlerException("Unknown Value object type: " + value.getClass());
		}
		jg.writeEndObject();
	}

	@Override
	public void handleBoolean(boolean value) throws QueryResultHandlerException {
		if (!documentOpen) {
			startDocument();
		}

		if (!headerOpen) {
			startHeader();
		}

		if (!headerComplete) {
			endHeader();
		}

		if (tupleVariablesFound) {
			throw new QueryResultHandlerException("Cannot call handleBoolean after startQueryResults");
		}

		try {
			if (value) {
				jg.writeBooleanProperty("boolean", Boolean.TRUE);
			} else {
				jg.writeBooleanProperty("boolean", Boolean.FALSE);
			}

			endDocument();
		} catch (JacksonException e) {
			throw new QueryResultHandlerException(e);
		}
	}

	@Override
	public final Collection<RioSetting<?>> getSupportedSettings() {
		Set<RioSetting<?>> result = new HashSet<>(super.getSupportedSettings());
		result.add(BasicQueryWriterSettings.JSONP_CALLBACK);
		result.add(BasicWriterSettings.PRETTY_PRINT);

		return result;
	}

	@Override
	public void handleNamespace(String prefix, String uri) throws QueryResultHandlerException {
		// Ignored by SPARQLJSONWriterBase
	}

	protected void endDocument() throws JacksonException {
		jg.writeEndObject();
		if (getWriterConfig().isSet(BasicQueryWriterSettings.JSONP_CALLBACK)) {
			jg.writeRaw(");");
		}
		jg.flush();
		documentOpen = false;
		headerOpen = false;
		headerComplete = false;
		tupleVariablesFound = false;
		firstTupleWritten = false;
		linksFound = false;
	}

}
