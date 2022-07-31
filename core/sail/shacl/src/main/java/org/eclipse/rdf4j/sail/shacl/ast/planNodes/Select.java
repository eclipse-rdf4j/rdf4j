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

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserFactory;
import org.eclipse.rdf4j.query.parser.QueryParserRegistry;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.memory.MemoryStoreConnection;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Håvard Ottestad
 */
public class Select implements PlanNode {

	private static final Logger logger = LoggerFactory.getLogger(Select.class);

	private final SailConnection connection;
	private final Function<BindingSet, ValidationTuple> mapper;

	private final String query;
	private final boolean sorted;
	private final Dataset dataset;
	private StackTraceElement[] stackTrace;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	public Select(SailConnection connection, String queryFregment, String orderBy,
			Function<BindingSet, ValidationTuple> mapper, Resource[] dataGraph) {
		this.connection = connection;
		assert this.connection != null;
		this.mapper = mapper;
		if (queryFregment.trim().equals("")) {
			logger.error("Query is empty", new Throwable("This throwable is just to log the stack trace"));

			// empty set
			queryFregment = "" +
					"?a <http://fjiewojfiwejfioewhgurh8924y.com/f289h8fhn> ?c. \n" +
					"FILTER (NOT EXISTS {?a <http://fjiewojfiwejfioewhgurh8924y.com/f289h8fhn> ?c}) \n";
		}
		sorted = orderBy != null;

		if (!sorted && queryFregment.trim().startsWith("select ")) {
			this.query = queryFregment;
		} else {
			this.query = "select * where {\n" + queryFregment + "\n} " + (sorted ? "order by " + orderBy : "");
		}

		dataset = PlanNodeHelper.asDefaultGraphDataset(dataGraph);

	}

	public Select(SailConnection connection, String query, Function<BindingSet, ValidationTuple> mapper,
			Resource[] dataGraph) {
		assert !query.toLowerCase().contains("order by") : "Queries with order by are not supported.";
		assert query.trim().toLowerCase().startsWith("select") : "Expected query to start with select.";

		this.connection = connection;
		assert this.connection != null;
		this.mapper = mapper;
		this.query = StatementMatcher.StableRandomVariableProvider.normalize(query);
		this.dataset = PlanNodeHelper.asDefaultGraphDataset(dataGraph);

		this.sorted = false;
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {
		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			CloseableIteration<? extends BindingSet, QueryEvaluationException> bindingSet = null;

			private void init() {

				if (bindingSet != null) {
					return;
				}

				QueryParserFactory queryParserFactory = QueryParserRegistry.getInstance()
						.get(QueryLanguage.SPARQL)
						.get();

				try {
					ParsedQuery parsedQuery = queryParserFactory.getParser().parseQuery(query, null);
					bindingSet = connection.evaluate(parsedQuery.getTupleExpr(), dataset,
							EmptyBindingSet.getInstance(), true);
				} catch (MalformedQueryException e) {
					logger.error("Malformed query: \n{}", query);
					throw e;
				}
			}

			@Override
			public void localClose() throws SailException {
				if (bindingSet != null) {
					bindingSet.close();
				}
			}

			@Override
			protected boolean localHasNext() throws SailException {
				init();
				return bindingSet.hasNext();
			}

			@Override
			protected ValidationTuple loggingNext() throws SailException {
				init();
				return mapper.apply(bindingSet.next());
			}

		};
	}

	@Override
	public int depth() {
		return 0;
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		if (printed) {
			return;
		}
		printed = true;
		stringBuilder.append(getId() + " [label=\"" + StringEscapeUtils.escapeJava(this.toString()) + "\"];")
				.append("\n");

		// added/removed connections are always newly minted per plan node, so we instead need to compare the underlying
		// sail
		if (connection instanceof MemoryStoreConnection) {
			stringBuilder
					.append(System.identityHashCode(((MemoryStoreConnection) connection).getSail()) + " -> " + getId())
					.append("\n");
		} else {
			stringBuilder.append(System.identityHashCode(connection) + " -> " + getId()).append("\n");
		}

	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}

	@Override
	public String toString() {
		return "Select{" + "query='" + query.replace("\n", "  ") + '\'' + '}';
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
	}

	@Override
	public boolean producesSorted() {
		return sorted;
	}

	@Override
	public boolean requiresSorted() {
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Select that = (Select) o;
		// added/removed connections are always newly minted per plan node, so we instead need to compare the underlying
		// sail
		if (connection instanceof MemoryStoreConnection && that.connection instanceof MemoryStoreConnection) {
			return sorted == that.sorted &&
					((MemoryStoreConnection) connection).getSail()
							.equals(((MemoryStoreConnection) that.connection).getSail())
					&&
					mapper.equals(that.mapper) &&
					dataset.equals(that.dataset) &&
					query.equals(that.query);
		} else {
			return sorted == that.sorted &&
					Objects.equals(connection, that.connection) &&
					mapper.equals(that.mapper) &&
					dataset.equals(that.dataset) &&
					query.equals(that.query);
		}
	}

	@Override
	public int hashCode() {
		// added/removed connections are always newly minted per plan node, so we instead need to compare the underlying
		// sail
		if (connection instanceof MemoryStoreConnection) {
			return Objects.hash(((MemoryStoreConnection) connection).getSail(), mapper, query, sorted, dataset);
		} else {
			return Objects.hash(connection, mapper, query, sorted, dataset);
		}

	}
}
