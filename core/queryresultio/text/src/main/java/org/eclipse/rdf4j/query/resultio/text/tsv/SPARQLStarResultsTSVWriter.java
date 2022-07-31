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

/**
 * Writer for SPARQL-star TSV results. This is equivalent to the SPARQL TSV writer with the addition of support for
 * RDF-star triples. Triples will be serialized in Turtle-star fashion with the notable exception that any embedded
 * literals will not use the triple quotes notation (as regular literals in SPARQL TSV).
 *
 * @author Pavel Mihaylov
 */
public class SPARQLStarResultsTSVWriter extends SPARQLResultsTSVWriter {
	public SPARQLStarResultsTSVWriter(OutputStream out) {
		super(out);
	}

	@Override
	public TupleQueryResultFormat getTupleQueryResultFormat() {
		return TupleQueryResultFormat.TSV_STAR;
	}

}
