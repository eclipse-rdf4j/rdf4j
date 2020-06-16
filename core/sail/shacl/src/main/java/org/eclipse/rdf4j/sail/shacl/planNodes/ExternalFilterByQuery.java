/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserFactory;
import org.eclipse.rdf4j.query.parser.QueryParserRegistry;
import org.eclipse.rdf4j.sail.SailConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Håvard Ottestad
 */
public class ExternalFilterByQuery extends FilterPlanNode {

	static private final Logger logger = LoggerFactory.getLogger(ExternalFilterByQuery.class);

	private final SailConnection connection;
	private final int index;
	private final ParsedQuery query;
	private final String queryVariable;
	private final String bindingVariable;

	public ExternalFilterByQuery(SailConnection connection, PlanNode parent, int index, String queryFragment,
			String queryVariable) {
		super(parent);
		this.connection = connection;
		this.index = index;
		this.queryVariable = queryVariable;
		this.bindingVariable = queryVariable.substring(1);

		assert queryVariable.startsWith("?");
		assert !bindingVariable.startsWith("?");

		QueryParserFactory queryParserFactory = QueryParserRegistry.getInstance()
				.get(QueryLanguage.SPARQL)
				.get();

		queryFragment = "SELECT " + queryVariable + " WHERE {\n" + queryFragment + "\n}";
		try {
			this.query = queryParserFactory.getParser().parseQuery(queryFragment, null);
		} catch (MalformedQueryException e) {
			logger.error("Malformed query: \n{}", queryFragment);
			throw e;
		}

	}

	@Override
	boolean checkTuple(Tuple t) {

		Value value = t.getLine().get(index);

		MapBindingSet bindings = new MapBindingSet();

		bindings.addBinding(bindingVariable, value);

		try (CloseableIteration<? extends BindingSet, QueryEvaluationException> bindingSet = connection.evaluate(
				query.getTupleExpr(), query.getDataset(),
				bindings, false)) {
			return bindingSet.hasNext();
		}

	}

	@Override
	public String toString() {
		return "ExternalFilterByQuery{" +
				"index=" + index +
				", query=" + query +
				", queryVariable='" + queryVariable + '\'' +
				'}';
	}
}
