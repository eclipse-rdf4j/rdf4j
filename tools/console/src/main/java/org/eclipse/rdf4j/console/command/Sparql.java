/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import java.util.Collection;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLQueries;

/**
 * SPARQL query command
 *
 * @author Bart Hanssens
 */
public class Sparql extends QueryEvaluator {
	private static final String PREFIX = "PREFIX";

	@Override
	public String getName() {
		return "sparql";
	}

	@Override
	public String getHelpShort() {
		return "Evaluate a SPARQL query";
	}

	@Override
	public String getHelpLong() {
		return PrintHelp.USAGE
				+ "sparql                               Starts multi-line input for large SPARQL queries.\n"
				+ "sparql <query>                       Evaluates the SPARQL query on the currently open repository.\n"
				+ "\n" + "sparql INFILE=\"infile.ext\"           Evaluates the query stored in a file.\n"
				+ "sparql OUTFILE=\"outfile.ext\" <query> Save the results to a file.\n"
				+ "    Supported extensions for graphs: jsonld, nt, ttl, xml\n"
				+ "    Supported extensions for tuples: csv, srj, srx, tsv\n"
				+ "sparql INFILE=\"infile.ext\" OUTFILE=\"outfile.ext\" \n" + "\n"
				+ "select|construct|ask|describe|prefix|base <rest-of-query>\n"
				+ "                                     Evaluates a SPARQL query on the currently open repository.\n";
	}

	/**
	 * Constructor
	 *
	 * @param evaluator
	 */
	public Sparql(TupleAndGraphQueryEvaluator evaluator) {
		super(evaluator);
	}

	@Override
	protected boolean hasQueryPrefixes(String qry) {
		return qry.trim().startsWith(PREFIX);
	}

	@Override
	protected void addQueryPrefixes(StringBuffer result, Collection<Namespace> namespaces) {
		StringBuilder str = new StringBuilder(512);

		for (Namespace namespace : namespaces) {
			str.append(PREFIX).append(" ").append(namespace.getPrefix()).append(": ");
			str.append("<").append(SPARQLQueries.escape(namespace.getName())).append("> ");
		}
		result.insert(0, str);
	}
}
