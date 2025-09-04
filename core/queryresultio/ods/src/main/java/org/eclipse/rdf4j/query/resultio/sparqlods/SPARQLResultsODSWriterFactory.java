/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.sparqlods;

import java.io.OutputStream;

import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriter;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriterFactory;

public class SPARQLResultsODSWriterFactory implements TupleQueryResultWriterFactory {

	public SPARQLResultsODSWriterFactory() {
		super();
	}

	@Override
	public TupleQueryResultFormat getTupleQueryResultFormat() {
		return TupleQueryResultFormat.ODS;
	}

	/**
	 * Returns a new instance of SPARQLResultsODSWriter.
	 */
	@Override
	public TupleQueryResultWriter getWriter(OutputStream out) {
		return new SPARQLResultsODSWriter(out);
	}
}
