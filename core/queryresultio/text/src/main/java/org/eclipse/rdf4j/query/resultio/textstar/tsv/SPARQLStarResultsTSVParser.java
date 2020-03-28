/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.textstar.tsv;

import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.text.tsv.SPARQLResultsTSVMappingStrategy;
import org.eclipse.rdf4j.query.resultio.text.tsv.SPARQLResultsTSVParser;

/**
 * Parser for SPARQL* TSV results. This is equivalent to the SPARQL TSV parser with the addition of support for RDF*
 * triples. Serialized triples must be in Turtle* fashion with the notable exception that any embedded literals may not
 * use the triple quotes notation (as regular literals in SPARQL TSV).
 *
 * @author Pavel Mihaylov
 */
public class SPARQLStarResultsTSVParser extends SPARQLResultsTSVParser {
	@Override
	public TupleQueryResultFormat getTupleQueryResultFormat() {
		return TupleQueryResultFormat.TSV_STAR;
	}

	@Override
	protected SPARQLResultsTSVMappingStrategy createMappingStrategy() {
		return new SPARQLStarResultsTSVMappingStrategy(valueFactory);
	}
}
