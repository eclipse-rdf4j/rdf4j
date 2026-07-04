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

import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultWriter;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultWriterFactory;

/**
 * A {@link BooleanQueryResultWriterFactory} for writers of SPARQL/JSON query boolean results.
 *
 * @author Peter Ansell
 */
public class SPARQLBooleanJSONWriterFactory implements BooleanQueryResultWriterFactory {

	/**
	 * Returns {@link BooleanQueryResultFormat#JSON}.
	 */
	@Override
	public BooleanQueryResultFormat getBooleanQueryResultFormat() {
		return BooleanQueryResultFormat.JSON;
	}

	/**
	 * Returns a new instance of SPARQLResultsJSONWriter.
	 */
	@Override
	public BooleanQueryResultWriter getWriter(OutputStream out) {
		return new SPARQLBooleanJSONWriter(out);
	}
}
