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

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStoreConnection;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlQueryParserCache;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
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
	private final BiFunction<ValidationTuple, BindingSet, ValidationTuple> map;

	public ExternalFilterByQuery(SailConnection connection, Resource[] dataGraph, PlanNode parent,
			SparqlFragment queryFragment,
			StatementMatcher.Variable queryVariable,
			Function<ValidationTuple, Value> filterOn, BiFunction<ValidationTuple, BindingSet, ValidationTuple> map,
			ConnectionsGroup connectionsGroup) {
		super(parent, connectionsGroup);
		this.connection = connection;
		assert this.connection != null;
		this.queryVariable = queryVariable;
		this.filterOn = filterOn;

		if (map != null) {
			this.queryString = queryFragment.getNamespacesForSparql()
					+ StatementMatcher.StableRandomVariableProvider.normalize("SELECT * "
							+ " WHERE {\n" + queryFragment.getFragment() + "\n}", List.of(queryVariable), List.of());

		} else {
			this.queryString = queryFragment.getNamespacesForSparql()
					+ StatementMatcher.StableRandomVariableProvider
							.normalize("SELECT " + queryVariable.asSparqlVariable()
									+ " WHERE {\n" + queryFragment.getFragment() + "\n}", List.of(queryVariable),
									List.of());
		}
		try {
			this.query = SparqlQueryParserCache.get(queryString);
		} catch (MalformedQueryException e) {
			logger.error("Malformed query:\n{}", queryString);
			throw e;
		}

		dataset = PlanNodeHelper.asDefaultGraphDataset(dataGraph);
		this.map = map;

	}

	@Override
	boolean checkTuple(Reference t) {

		Value value = filterOn.apply(t.get());
		SingletonBindingSet bindings = new SingletonBindingSet(queryVariable.getName(), value);

		try (var bindingSet = connection.evaluate(query, dataset, bindings, false)) {
			if (bindingSet.hasNext()) {
				if (map != null) {
					do {
						t.set(map.apply(t.get(), bindingSet.next()));
					} while (bindingSet.hasNext());
				}
				logger.trace("Tuple accepted because it matches the external query. Value: {}, Query: {}, Tuple: {}",
						value, queryString, t);
				return true;
			}
		}
		logger.debug("Tuple rejected because it does not match the external query. Value: {}, Query: {}, Tuple: {}",
				value, queryString, t);
		return false;

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
