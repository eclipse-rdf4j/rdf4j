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
package org.eclipse.rdf4j.query.resultio.sparqlxml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import org.eclipse.rdf4j.common.xml.XMLWriter;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultWriter;

/**
 * A {@link BooleanQueryResultWriter} that writes boolean query results in the
 * <a href="http://www.w3.org/TR/rdf-sparql-XMLres/">SPARQL Query Results XML Format</a>.
 */
public class SPARQLBooleanXMLWriter extends AbstractSPARQLXMLWriter implements BooleanQueryResultWriter {

	/*--------------*
	 * Constructors *
	 *--------------*/

	public SPARQLBooleanXMLWriter(OutputStream out) {
		super(out);
	}

	public SPARQLBooleanXMLWriter(Writer writer) {
		super(writer);
	}

	public SPARQLBooleanXMLWriter(XMLWriter xmlWriter) {
		super(xmlWriter);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public final BooleanQueryResultFormat getBooleanQueryResultFormat() {
		return BooleanQueryResultFormat.SPARQL;
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

}
