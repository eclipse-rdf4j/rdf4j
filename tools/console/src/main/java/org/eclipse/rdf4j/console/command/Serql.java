/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import java.util.Collection;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.query.parser.serql.SeRQLUtil;

/**
 * SERQL query command
 *
 * @author Bart Hanssens
 */
public class Serql extends QueryEvaluator {
	private static final String NAMESPACE = "USING NAMESPACE";

	@Override
	public String getName() {
		return "serql";
	}

	@Override
	public String getHelpShort() {
		return "Evaluate a SeRQL query";
	}

	@Override
	public String getHelpLong() {
		return PrintHelp.USAGE + "serql                         Starts multi-line input for large SeRQL queries.\n"
				+ "serql <query>                 Evaluates the SeRQL query on the currently open repository\n"
				+ "serql INFILE=\"infile.ext\"            Evaluates the query stored in a file.\n"
				+ "serql OUTFILE=\"outfile.ext\" <query>  Save the results to a file.\n"
				+ "    Supported extensions for graphs: jsonld, nt, ttl, xml\n"
				+ "    Supported extensions for tuples: csv, srj, srx, tsv\n"
				+ "serql INFILE=\"infile.ext\" OUTFILE=\"outfile.ext\" \n";
	}

	/**
	 * Constructor
	 *
	 * @param evaluator
	 */
	public Serql(TupleAndGraphQueryEvaluator evaluator) {
		super(evaluator);
	}

	@Override
	protected boolean hasQueryPrefixes(String query) {
		return query.contains(NAMESPACE + " ");
	}

	@Override
	protected void addQueryPrefixes(StringBuffer result, Collection<Namespace> namespaces) {
		StringBuilder str = new StringBuilder(512);

		str.append(" ").append(NAMESPACE).append(" ");
		for (Namespace namespace : namespaces) {
			str.append(namespace.getPrefix()).append(" = ");
			str.append("<").append(SeRQLUtil.encodeString(namespace.getName())).append(">, ");
		}
	}
}
