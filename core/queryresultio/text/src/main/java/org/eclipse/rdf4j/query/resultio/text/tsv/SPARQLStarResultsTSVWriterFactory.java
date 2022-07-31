/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.text.tsv;

import java.io.OutputStream;

import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriter;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriterFactory;

/**
 * {@link TupleQueryResultWriterFactory} for creating instances of {@link SPARQLStarResultsTSVWriter}.
 *
 * @author Pavel Mihaylov
 */
public class SPARQLStarResultsTSVWriterFactory implements TupleQueryResultWriterFactory {
	/**
	 * Returns {@link TupleQueryResultFormat#TSV_STAR}.
	 */
	@Override
	public TupleQueryResultFormat getTupleQueryResultFormat() {
		return TupleQueryResultFormat.TSV_STAR;
	}

	/**
	 * Returns a new instance of {@link SPARQLStarResultsTSVWriter}.
	 */
	@Override
	public TupleQueryResultWriter getWriter(OutputStream out) {
		return new SPARQLStarResultsTSVWriter(out);
	}
}
