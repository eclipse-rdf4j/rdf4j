/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author Håvard Ottestad
 */
public class Select implements PlanNode {

	private static final Logger logger = LoggerFactory.getLogger(Select.class);

	private final SailConnection connection;

	private final String query;
	private final String[] variables;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	public Select(SailConnection connection, String query, String... variables) {
		assert variables.length > 0;
		this.connection = connection;
		this.query = "select " + String.join(" ", variables) + " where { " + query + "} order by ?a";
		this.variables = variables;
	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			CloseableIteration<? extends BindingSet, QueryEvaluationException> bindingSet;

			{

				QueryParserFactory queryParserFactory = QueryParserRegistry.getInstance()
						.get(QueryLanguage.SPARQL)
						.get();

				try {
					ParsedQuery parsedQuery = queryParserFactory.getParser().parseQuery(query, null);
					bindingSet = connection.evaluate(parsedQuery.getTupleExpr(), parsedQuery.getDataset(),
							new MapBindingSet(), true);
				} catch (MalformedQueryException e) {
					logger.error("Malformed query: \n{}", query);
					throw e;
				}
			}

			@Override
			public void close() throws SailException {
				bindingSet.close();
			}

			@Override
			boolean localHasNext() throws SailException {
				return bindingSet.hasNext();
			}

			@Override
			Tuple loggingNext() throws SailException {
				return new Tuple(bindingSet.next(), variables);
			}

			@Override
			public void remove() throws SailException {

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
	public IteratorData getIteratorDataType() {
		return IteratorData.tripleBased;
	}

	@Override
	public String toString() {
		return "Select{" + "query='" + query.replace("\n", "  ") + '\'' + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Select select = (Select) o;

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

		if (connection instanceof MemoryStoreConnection) {
			return Objects.hash(System.identityHashCode(((MemoryStoreConnection) connection).getSail()), query);
		}
		return Objects.hash(System.identityHashCode(connection), query);

	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
	}
}
