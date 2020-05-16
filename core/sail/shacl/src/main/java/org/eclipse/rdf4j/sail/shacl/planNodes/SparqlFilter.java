/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserFactory;
import org.eclipse.rdf4j.query.parser.QueryParserRegistry;
import org.eclipse.rdf4j.sail.SailConnection;

/**
 * @author Håvard Ottestad
 */
public class SparqlFilter extends FilterPlanNode {

	private final String query;

	private final SailConnection connection;

	public SparqlFilter(SailConnection connection, PlanNode parent, String query) {
		super(parent);
		this.query = query;
		this.connection = connection;
	}

	@Override
	boolean checkTuple(Tuple t) {

		QueryParserFactory queryParserFactory = QueryParserRegistry.getInstance()
				.get(QueryLanguage.SPARQL)
				.get();

		try {
			ParsedQuery parsedQuery = queryParserFactory.getParser().parseQuery(query, null);

			MapBindingSet bindings = new MapBindingSet();
			bindings.addBinding("this", t.line.get(0));

			try (CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluate = connection
					.evaluate(parsedQuery.getTupleExpr(), parsedQuery.getDataset(), bindings, true)) {
				boolean b = evaluate.hasNext();
				return b;
			}

		} catch (QueryEvaluationException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public String toString() {
		return "SparqlFilter{" +
				"query='" + query.replace("\n", "\\n") + '\'' +
				'}';
	}
}
