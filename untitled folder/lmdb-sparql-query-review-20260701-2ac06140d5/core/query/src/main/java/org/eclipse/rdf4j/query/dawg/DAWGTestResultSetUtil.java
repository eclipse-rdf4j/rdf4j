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
package org.eclipse.rdf4j.query.dawg;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.impl.TupleQueryResultBuilder;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

/**
 * @author Arjohn Kampman
 */
public class DAWGTestResultSetUtil {

	public static TupleQueryResult toTupleQueryResult(Iterable<? extends Statement> dawgGraph)
			throws DAWGTestResultSetParseException {
		TupleQueryResultBuilder tqrBuilder = new TupleQueryResultBuilder();
		DAWGTestResultSetParser parser = new DAWGTestResultSetParser(tqrBuilder);

		try {
			parser.startRDF();
			for (Statement st : dawgGraph) {
				parser.handleStatement(st);
			}
			parser.endRDF();

			return tqrBuilder.getQueryResult();
		} catch (RDFHandlerException e) {
			throw new DAWGTestResultSetParseException(e.getMessage(), e);
		}
	}

	public static Model toGraph(TupleQueryResult tqr) throws QueryEvaluationException {
		Model graph = new TreeModel();
		DAWGTestResultSetWriter writer = new DAWGTestResultSetWriter(new StatementCollector(graph));

		try {
			writer.startQueryResult(tqr.getBindingNames());
			while (tqr.hasNext()) {
				writer.handleSolution(tqr.next());
			}
			writer.endQueryResult();
		} catch (TupleQueryResultHandlerException e) {
			// No exceptions expected from DAWGTestResultSetWriter or
			// StatementCollector, foud a bug?
			throw new RuntimeException(e);
		}

		return graph;
	}

	public static boolean toBooleanQueryResult(Iterable<? extends Statement> dawgGraph)
			throws DAWGTestResultSetParseException {
		DAWGTestBooleanParser parser = new DAWGTestBooleanParser();

		try {
			parser.startRDF();
			for (Statement st : dawgGraph) {
				parser.handleStatement(st);
			}
			parser.endRDF();

			return parser.getValue();
		} catch (RDFHandlerException e) {
			throw new DAWGTestResultSetParseException(e.getMessage(), e);
		}
	}
}
