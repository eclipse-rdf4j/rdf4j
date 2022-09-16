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

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.Objects;
import java.util.function.Function;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStoreConnection;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlQueryParserCache;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Håvard Ottestad
 */
public class ExternalFilterByQuery extends FilterPlanNode {

	static private final Logger logger = LoggerFactory.getLogger(ExternalFilterByQuery.class);

	private final SailConnection connection;
	private final TupleExpr query;
	private final Dataset dataset;
	private final StatementMatcher.Variable queryVariable;
	private final Function<ValidationTuple, Value> filterOn;
	private final String queryString;

	public ExternalFilterByQuery(SailConnection connection, Resource[] dataGraph, PlanNode parent,
			String queryFragment,
			StatementMatcher.Variable queryVariable,
			Function<ValidationTuple, Value> filterOn) {
		super(parent);
		this.connection = connection;
		assert this.connection != null;
		this.queryVariable = queryVariable;
		this.filterOn = filterOn;

		queryFragment = "SELECT ?" + queryVariable.getName() + " WHERE {\n" + queryFragment + "\n}";
		this.queryString = StatementMatcher.StableRandomVariableProvider.normalize(queryFragment);
		try {
			this.query = SparqlQueryParserCache.get(queryFragment);
		} catch (MalformedQueryException e) {
			logger.error("Malformed query:\n{}", queryFragment);
			throw e;
		}
		dataset = PlanNodeHelper.asDefaultGraphDataset(dataGraph);

	}

	@Override
	boolean checkTuple(ValidationTuple t) {

		Value value = filterOn.apply(t);
		SingletonBindingSet bindings = new SingletonBindingSet(queryVariable.getName(), value);

		try (var bindingSet = connection.evaluate(query, dataset, bindings, false)) {
			return bindingSet.hasNext();
		}

	}

	@Override
	public String toString() {
		return "ExternalFilterByQuery{" +
				", queryString=" + queryString.replace("\n", "\t") +
				", queryVariable='" + queryVariable.toString().replace("\n", "  ") + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		ExternalFilterByQuery that = (ExternalFilterByQuery) o;

		if (connection instanceof MemoryStoreConnection && that.connection instanceof MemoryStoreConnection) {
			return ((MemoryStoreConnection) connection).getSail()
					.equals(((MemoryStoreConnection) that.connection).getSail()) &&
					Objects.equals(dataset, that.dataset)
					&& queryVariable.equals(that.queryVariable) && filterOn.equals(that.filterOn)
					&& queryString.equals(that.queryString);
		}

		return Objects.equals(connection, that.connection) && queryVariable.equals(that.queryVariable)
				&& Objects.equals(dataset, that.dataset)
				&& filterOn.equals(that.filterOn) && queryString.equals(that.queryString);
	}

	@Override
	public int hashCode() {
		if (connection instanceof MemoryStoreConnection) {
			return Objects.hash(super.hashCode(), ((MemoryStoreConnection) connection).getSail(), queryVariable,
					filterOn, dataset, queryString);
		}

		return Objects.hash(super.hashCode(), connection, queryVariable, filterOn, dataset, queryString);
	}
}
