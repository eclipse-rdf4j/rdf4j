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
package org.eclipse.rdf4j.query.resultio.text.csv;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.resultio.AbstractTupleQueryResultParser;
import org.eclipse.rdf4j.query.resultio.QueryResultParseException;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultParser;

import com.opencsv.bean.CsvToBeanBuilder;

/**
 * SPARQL Results CSV format parser.
 *
 * @author Jeen Broekstra
 * @author Andrew Rucker Jones
 */
public class SPARQLResultsCSVParser extends AbstractTupleQueryResultParser {

	@Override
	public TupleQueryResultFormat getTupleQueryResultFormat() {
		return TupleQueryResultFormat.CSV;
	}

	@Override
	public void parse(InputStream in) throws QueryResultParseException, TupleQueryResultHandlerException {
		if (handler != null) {
			SPARQLResultsCSVMappingStrategy strategy = new SPARQLResultsCSVMappingStrategy(valueFactory);

			List<BindingSet> bindingSets = new CsvToBeanBuilder<BindingSet>(
					new InputStreamReader(in, StandardCharsets.UTF_8)).withType(BindingSet.class)
							.withMappingStrategy(strategy)
							.build()
							.parse();
			List<String> bindingNames = strategy.getBindingNames();
			handler.startQueryResult(bindingNames);
			for (BindingSet bs : bindingSets) {
				handler.handleSolution(bs);
			}
			handler.endQueryResult();
		}
	}
}
