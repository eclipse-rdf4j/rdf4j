/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.text;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQueryResultHandlerException;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultWriter;

/**
 * Writer for the plain text boolean result format.
 *
 * @author Arjohn Kampman
 */
public class BooleanTextWriter extends AbstractQueryResultWriter implements BooleanQueryResultWriter {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The writer to write the boolean result to.
	 */
	private final Writer writer;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public BooleanTextWriter(OutputStream out) {
		this(new OutputStreamWriter(out, StandardCharsets.US_ASCII));
	}

	public BooleanTextWriter(Writer writer) {
		this.writer = writer;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public final BooleanQueryResultFormat getBooleanQueryResultFormat() {
		return BooleanQueryResultFormat.TEXT;
	}

	@Override
	public final BooleanQueryResultFormat getQueryResultFormat() {
		return getBooleanQueryResultFormat();
	}

	@Override
	public void write(boolean value) throws IOException {
		try {
			handleBoolean(value);
		} catch (QueryResultHandlerException e) {
			if (e.getCause() != null && e.getCause() instanceof IOException) {
				throw (IOException) e.getCause();
			} else {
				throw new IOException(e);
			}
		}
	}

	@Override
	public void handleBoolean(boolean value) throws QueryResultHandlerException {
		try {
			writer.write(Boolean.toString(value));
			writer.flush();
		} catch (IOException e) {
			throw new BooleanQueryResultHandlerException(e);
		}
	}

	@Override
	public void startDocument() throws QueryResultHandlerException {
		// Ignored by BooleanTextWriter
	}

	@Override
	public void handleStylesheet(String stylesheetUrl) throws QueryResultHandlerException {
		// Ignored by BooleanTextWriter
	}

	@Override
	public void startHeader() throws QueryResultHandlerException {
		// Ignored by BooleanTextWriter
	}

	@Override
	public void handleLinks(List<String> linkUrls) throws QueryResultHandlerException {
		// Ignored by BooleanTextWriter
	}

	@Override
	public void endHeader() throws QueryResultHandlerException {
		// Ignored by BooleanTextWriter
	}

	@Override
	public void startQueryResult(List<String> bindingNames) throws TupleQueryResultHandlerException {
		throw new UnsupportedOperationException("Cannot handle tuple results");
	}

	@Override
	public void endQueryResult() throws TupleQueryResultHandlerException {
		throw new UnsupportedOperationException("Cannot handle tuple results");
	}

	@Override
	protected void handleSolutionImpl(BindingSet bindingSet) throws TupleQueryResultHandlerException {
		throw new UnsupportedOperationException("Cannot handle tuple results");
	}

	@Override
	public void handleNamespace(String prefix, String uri) throws QueryResultHandlerException {
		// Ignored by BooleanTextWriter
	}
}
