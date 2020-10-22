/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.text.tsv;

import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;

/**
 * Parser for SPARQL* TSV results. This is equivalent to the SPARQL TSV parser with the addition of support for RDF*
 * triples. Serialized triples must be in Turtle* fashion with the notable exception that any embedded literals may not
 * use the triple quotes notation (as regular literals in SPARQL TSV).
 *
 * @author Pavel Mihaylov
 * @author Jeen Broekstra
 * @implNote this class is functionally equivalent to the standard {@link SPARQLResultsTSVParser} - its only purpose is
 *           to provide a hook for the customized content type {@link TupleQueryResultFormat#TSV_STAR}.
 */
public class SPARQLStarResultsTSVParser extends SPARQLResultsTSVParser {
	@Override
	public TupleQueryResultFormat getTupleQueryResultFormat() {
		return TupleQueryResultFormat.TSV_STAR;
	}
}