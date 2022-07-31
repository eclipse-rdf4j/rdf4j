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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
import org.eclipse.rdf4j.query.resultio.QueryResultWriter;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter.Indenter;

/**
 * An abstract class to implement the base functionality for both SPARQLBooleanJSONWriter and SPARQLResultsJSONWriter.
 *
 * @author Peter Ansell
 */
abstract class AbstractSPARQLJSONWriter extends AbstractQueryResultWriter implements CharSink {

	private static final JsonFactory JSON_FACTORY = new JsonFactory();

	static {
		// Disable features that may work for most JSON where the field names are
		// in limited supply,
		// but does not work for RDF/JSON where a wide range of URIs are used for
		// subjects and
		// predicates
		JSON_FACTORY.disable(JsonFactory.Feature.INTERN_FIELD_NAMES);
		JSON_FACTORY.disable(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES);
		JSON_FACTORY.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
	}

	protected boolean firstTupleWritten = false;

	protected boolean documentOpen = false;

	protected boolean headerOpen = false;

	protected boolean headerComplete = false;

	protected boolean tupleVariablesFound = false;

	protected boolean linksFound = false;

	protected final JsonGenerator jg;

	private final Writer writer;

	protected AbstractSPARQLJSONWriter(OutputStream out) {
		this(new OutputStreamWriter(out, StandardCharsets.UTF_8));
	}

	protected AbstractSPARQLJSONWriter(Writer writer) {
		this.writer = writer;
		try {
			jg = JSON_FACTORY.createGenerator(writer);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
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
					jg.writeObjectFieldStart("results");

					jg.writeArrayFieldStart("bindings");
				}

				headerComplete = true;
			} catch (IOException e) {
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
			jg.writeArrayFieldStart("vars");
			for (String nextColumn : columnHeaders) {
				jg.writeString(nextColumn);
			}
			jg.writeEndArray();
		} catch (IOException | QueryResultHandlerException e) {
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
				jg.writeFieldName(binding.getName());
				writeValue(binding.getValue());
			}

			jg.writeEndObject();
		} catch (IOException | QueryResultHandlerException e) {
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
		} catch (IOException | QueryResultHandlerException e) {
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
				DefaultPrettyPrinter pp = new DefaultPrettyPrinter().withArrayIndenter(indenter)
						.withObjectIndenter(indenter);
				jg.setPrettyPrinter(pp);
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
			} catch (IOException e) {
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
				jg.writeObjectFieldStart("head");

				headerOpen = true;
			} catch (IOException e) {
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

			jg.writeArrayFieldStart("link");
			for (String nextLink : linkUrls) {
				jg.writeString(nextLink);
			}
			jg.writeEndArray();
		} catch (IOException e) {
			throw new QueryResultHandlerException(e);
		}
	}

	protected void writeValue(Value value) throws IOException, QueryResultHandlerException {
		jg.writeStartObject();

		if (value instanceof IRI) {
			jg.writeStringField("type", "uri");
			jg.writeStringField("value", ((IRI) value).toString());
		} else if (value instanceof BNode) {
			jg.writeStringField("type", "bnode");
			jg.writeStringField("value", ((BNode) value).getID());
		} else if (value instanceof Literal) {
			Literal lit = (Literal) value;

			if (Literals.isLanguageLiteral(lit)) {
				jg.writeObjectField("xml:lang", lit.getLanguage().orElse(null));
			} else {
				IRI datatype = lit.getDatatype();
				boolean ignoreDatatype = datatype.equals(XSD.STRING) && xsdStringToPlainLiteral();
				if (!ignoreDatatype) {
					jg.writeObjectField("datatype", lit.getDatatype().stringValue());
				}
			}

			jg.writeObjectField("type", "literal");

			jg.writeObjectField("value", lit.getLabel());
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
				jg.writeBooleanField("boolean", Boolean.TRUE);
			} else {
				jg.writeBooleanField("boolean", Boolean.FALSE);
			}

			endDocument();
		} catch (IOException e) {
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

	protected void endDocument() throws IOException {
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
