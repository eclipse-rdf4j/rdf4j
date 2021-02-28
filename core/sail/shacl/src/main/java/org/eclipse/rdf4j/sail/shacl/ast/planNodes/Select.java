/*******************************************************************************
 * .Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.Objects;
import java.util.function.Function;

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

/**
 * @author Håvard Ottestad
 */
public class Select implements PlanNode {

	private static final Logger logger = LoggerFactory.getLogger(Select.class);

	private final SailConnection connection;
	private final Function<BindingSet, ValidationTuple> mapper;

	private final String query;
	private final boolean sorted;
	private StackTraceElement[] stackTrace;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	public Select(SailConnection connection, String query, String orderBy,
			Function<BindingSet, ValidationTuple> mapper) {
		this.connection = connection;
		this.mapper = mapper;
		if (query.trim().equals("")) {
			logger.error("Query is empty", new Throwable("This throwable is just to log the stack trace"));

			// empty set
			query = "" +
					"?a <http://fjiewojfiwejfioewhgurh8924y.com/f289h8fhn> ?c. \n" +
					"FILTER (NOT EXISTS {?a <http://fjiewojfiwejfioewhgurh8924y.com/f289h8fhn> ?c}) \n";
		}
		sorted = orderBy != null;

		this.query = "select * where {\n" + query + "\n} " + (orderBy != null ? "order by " + orderBy : "");

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
					bindingSet = connection.evaluate(parsedQuery.getTupleExpr(), parsedQuery.getDataset(),
							new MapBindingSet(), true);
				} catch (MalformedQueryException e) {
					logger.error("Malformed query: \n{}", query);
					throw e;
				}
			}

			@Override
			public void close() throws SailException {
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
		Select select = (Select) o;
		// added/removed connections are always newly minted per plan node, so we instead need to compare the underlying
		// sail
		if (connection instanceof MemoryStoreConnection && select.connection instanceof MemoryStoreConnection) {
			return sorted == select.sorted &&
					((MemoryStoreConnection) connection).getSail()
							.equals(((MemoryStoreConnection) select.connection).getSail())
					&&
					mapper.equals(select.mapper) &&
					query.equals(select.query);
		} else {
			return sorted == select.sorted &&
					connection.equals(select.connection) &&
					mapper.equals(select.mapper) &&
					query.equals(select.query);
		}
	}

	@Override
	public int hashCode() {
		// added/removed connections are always newly minted per plan node, so we instead need to compare the underlying
		// sail
		if (connection instanceof MemoryStoreConnection) {
			return Objects.hash(((MemoryStoreConnection) connection).getSail(), mapper, query, sorted);
		} else {
			return Objects.hash(connection, mapper, query, sorted);
		}

	}
}
