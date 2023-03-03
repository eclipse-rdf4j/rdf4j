/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.Objects;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserFactory;
import org.eclipse.rdf4j.query.parser.QueryParserRegistry;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.memory.MemoryStoreConnection;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Håvard Ottestad
 */
public class SparqlTargetSelect implements PlanNode {

	private static final Logger logger = LoggerFactory.getLogger(SparqlTargetSelect.class);

	private final SailConnection connection;

	private final String query;
	private final Resource[] dataGraph;
	private final String[] variables;
	private final ConstraintComponent.Scope scope;
	private final Dataset dataset;
	private final ParsedQuery parsedQuery;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	public SparqlTargetSelect(SailConnection connection, String query, ConstraintComponent.Scope scope,
			Resource[] dataGraph) {
		this.connection = connection;
		this.query = query;
		this.dataGraph = dataGraph;
		assert query.contains("?this") : "Query should contain ?this: " + query;
		this.variables = new String[] { "?this" };
		this.scope = scope;
		this.dataset = PlanNodeHelper.asDefaultGraphDataset(dataGraph);

		QueryParserFactory queryParserFactory = QueryParserRegistry.getInstance()
				.get(QueryLanguage.SPARQL)
				.get();

		try {
			this.parsedQuery = queryParserFactory.getParser().parseQuery(query, null);
		} catch (MalformedQueryException e) {
			logger.error("Malformed query: \n{}", query);
			throw e;
		}

	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {
		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			CloseableIteration<? extends BindingSet, QueryEvaluationException> results;

			@Override
			protected void init() {
				assert results == null;
				results = connection.evaluate(parsedQuery.getTupleExpr(), dataset, new MapBindingSet(), true);
			}

			@Override
			public void localClose() {
				if (results != null) {
					results.close();
				}
			}

			@Override
			protected boolean localHasNext() {
				return results.hasNext();
			}

			@Override
			protected ValidationTuple loggingNext() {
				return new ValidationTuple(results.next(), variables, scope, false, dataGraph);
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
		return "SparqlTargetSelect{" + "query='" + query.replace("\n", "  ") + '\'' + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		SparqlTargetSelect select = (SparqlTargetSelect) o;

		// added/removed connections are always newly minted per plan node, so we instead need to compare the underlying
		// sail
		return Objects.equals(
				connection instanceof MemoryStoreConnection ? ((MemoryStoreConnection) connection).getSail()
						: connection,
				select.connection instanceof MemoryStoreConnection
						? ((MemoryStoreConnection) select.connection).getSail()
						: select.connection)
				&& query.equals(select.query);
	}

	@Override
	public int hashCode() {

		// added/removed connections are always newly minted per plan node, so we instead need to compare the underlying
		// sail
		if (connection instanceof MemoryStoreConnection) {
			return Objects.hash(System.identityHashCode(((MemoryStoreConnection) connection).getSail()), query);
		}
		return Objects.hash(System.identityHashCode(connection), query);

	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
	}

	@Override
	public boolean producesSorted() {
		return true;
	}

	@Override
	public boolean requiresSorted() {
		return false;
	}
}
