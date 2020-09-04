/*******************************************************************************
 * .Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.impl.ListBindingSet;
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
 * Takes a plan node as a source and for each tuple in the source it will build a BindingSet from the vars and the tuple
 * and inject it into the query
 *
 * @author Håvard Ottestad
 */
public class BindSelect implements PlanNode {

	private static final Logger logger = LoggerFactory.getLogger(BindSelect.class);

	private final SailConnection connection;
	private final Function<BindingSet, ValidationTuple> mapper;

	private final String query;
	private final List<Var> vars;
	private final int bulkSize;
	private final PlanNode source;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	public BindSelect(SailConnection connection, String query, List<Var> vars, PlanNode source,
			Function<BindingSet, ValidationTuple> mapper, int bulkSize) {
		this.connection = connection;
		this.mapper = mapper;
		this.vars = vars;
		this.bulkSize = bulkSize;
		this.source = source;

		if (query.trim().equals("")) {
			throw new IllegalStateException();
		}

		this.query = query;

	}

	private void updateQuery(ParsedQuery parsedQuery, List<BindingSet> newBindindingset, int size) {
		try {

			parsedQuery.getTupleExpr()
					.visit(new AbstractQueryModelVisitor<Exception>() {
						@Override
						public void meet(BindingSetAssignment node) throws Exception {
							Set<String> bindingNames = node.getBindingNames();
							if (bindingNames.size() == size) { // TODO consider checking if bindingnames is equal to
																// vars
								node.setBindingSets(newBindindingset);
							}
							super.meet(node);
						}

					});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {
		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			CloseableIteration<? extends BindingSet, QueryEvaluationException> bindingSet;

			final CloseableIteration<? extends ValidationTuple, SailException> iterator = source.iterator();

			Integer targetChainSize = null;

			public void calculateNext() {

				// already results available
				if (bindingSet != null && bindingSet.hasNext()) {
					return;
				}

				if (bindingSet != null) {
					bindingSet.close();
				}

				if (!iterator.hasNext()) {
					return;
				}

				List<ValidationTuple> bulk = new ArrayList<>(bulkSize);

				ValidationTuple next = iterator.next();
				bulk.add(next);

				int targetChainSize = next.getChain().size();
				if (this.targetChainSize != null) {
					assert targetChainSize == this.targetChainSize;
				} else {
					this.targetChainSize = targetChainSize;
				}

				StringBuilder orderBy = new StringBuilder("");

				StringBuilder values = new StringBuilder("\nVALUES( ");
				for (int i = 0; i < targetChainSize; i++) {
					values.append("?").append(vars.get(i).getName()).append(" ");
				}
				values.append("){}\n");

				for (Var var : vars) {
					orderBy.append("?").append(var.getName()).append(" ");
				}

				String query = BindSelect.this.query;

				query = query.replace("#VALUES_INJECTION_POINT#", values.toString());
				query = "select * where { " + values.toString() + query + "\n}\nORDER BY " + orderBy;

				QueryParserFactory queryParserFactory = QueryParserRegistry.getInstance()
						.get(QueryLanguage.SPARQL)
						.get();

				try {
					ParsedQuery parsedQuery = queryParserFactory.getParser().parseQuery(query, null);
					for (int i = 1; i < bulkSize && iterator.hasNext(); i++) {
						bulk.add(iterator.next());
					}

					List<String> varNames = vars
							.stream()
							.limit(targetChainSize)
							.map(Var::getName)
							.collect(Collectors.toList());

					List<BindingSet> bindingSets = bulk
							.stream()
							.map(t -> new ListBindingSet(varNames, new ArrayList<>(t.getChain())))
							.collect(Collectors.toList());

					updateQuery(parsedQuery, bindingSets, targetChainSize);

					bindingSet = connection.evaluate(parsedQuery.getTupleExpr(), parsedQuery.getDataset(),
							new MapBindingSet(), true);
				} catch (MalformedQueryException e) {
					logger.error("Malformed query: \n{}", query);
					throw e;
				}

			}

			@Override
			public void close() throws SailException {
				try {
					iterator.close();
				} finally {
					if (bindingSet != null) {
						bindingSet.close();
					}
				}
			}

			@Override
			boolean localHasNext() throws SailException {
				calculateNext();
				if (bindingSet != null && bindingSet.hasNext()) {
					return true;
				} else {
					return false;
				}
			}

			@Override
			ValidationTuple loggingNext() throws SailException {
				calculateNext();
				return mapper.apply(bindingSet.next());
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
		BindSelect select = (BindSelect) o;

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
		source.receiveLogger(validationExecutionLogger);
	}
}
