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

import java.io.OutputStream;
import java.io.Writer;

import org.eclipse.rdf4j.common.xml.XMLWriter;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriter;

/**
 * A {@link TupleQueryResultWriter} that writes tuple query results in the
 * <a href="http://www.w3.org/TR/rdf-sparql-XMLres/">SPARQL Query Results XML Format</a>.
 */
public class SPARQLResultsXMLWriter extends AbstractSPARQLXMLWriter implements TupleQueryResultWriter {

	/*--------------*
	 * Constructors *
	 *--------------*/

	public SPARQLResultsXMLWriter(OutputStream out) {
		super(out);
	}

	public SPARQLResultsXMLWriter(Writer writer) {
		super(writer);
	}

	public SPARQLResultsXMLWriter(XMLWriter xmlWriter) {
		super(xmlWriter);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public TupleQueryResultFormat getTupleQueryResultFormat() {
		return TupleQueryResultFormat.SPARQL;
	}

	@Override
	public final TupleQueryResultFormat getQueryResultFormat() {
		return getTupleQueryResultFormat();
	}

}
