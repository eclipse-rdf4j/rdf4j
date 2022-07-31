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

import static java.util.stream.Collectors.toCollection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserFactory;
import org.eclipse.rdf4j.query.parser.QueryParserRegistry;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.memory.MemoryStoreConnection;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.AbstractConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;
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
	private final Dataset dataset;
	private final Function<BindingSet, ValidationTuple> mapper;

	private final String query;
	private final List<StatementMatcher.Variable> vars;
	private final int bulkSize;
	private final PlanNode source;
	private final EffectiveTarget.Extend direction;
	private final boolean includePropertyShapeValues;
	private final List<String> varNames;
	private final ConstraintComponent.Scope scope;
	private StackTraceElement[] stackTrace;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	public BindSelect(SailConnection connection, Resource[] dataGraph, String query,
			List<StatementMatcher.Variable> vars, PlanNode source,
			List<String> varNames, ConstraintComponent.Scope scope, int bulkSize, EffectiveTarget.Extend direction,
			boolean includePropertyShapeValues) {
		this.connection = connection;
		assert this.connection != null;
		this.mapper = (bindingSet) -> new ValidationTuple(bindingSet, varNames, scope, includePropertyShapeValues,
				dataGraph);
		this.varNames = varNames;
		this.scope = scope;
		this.vars = vars;
		this.bulkSize = bulkSize;
		this.source = PlanNodeHelper.handleSorting(this, source);

		if (query.trim().equals("")) {
			throw new IllegalStateException();
		}

		this.query = StatementMatcher.StableRandomVariableProvider.normalize(query);
		this.direction = direction;
		this.includePropertyShapeValues = includePropertyShapeValues;

		dataset = PlanNodeHelper.asDefaultGraphDataset(dataGraph);

		// this.stackTrace = Thread.currentThread().getStackTrace();

	}

	private void updateQuery(ParsedQuery parsedQuery, List<BindingSet> newBindindingset, int expectedSize) {
		try {

			parsedQuery.getTupleExpr()
					.visit(new AbstractQueryModelVisitor<Exception>() {
						@Override
						public void meet(BindingSetAssignment node) throws Exception {
							Set<String> bindingNames = node.getBindingNames();
							if (bindingNames.size() == expectedSize) { // TODO consider checking if bindingnames is
								// equal to
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
			List<ValidationTuple> bulk = new ArrayList<>(bulkSize);

			ParsedQuery parsedQuery = null;

			public void calculateNext() {

				while (bindingSet == null || !bindingSet.hasNext()) {

					if (bindingSet != null) {
						bindingSet.close();
					}

					if (bulk.isEmpty() && !iterator.hasNext()) {
						return;
					}

					ValidationTuple next;
					if (bulk.isEmpty()) {
						next = iterator.next();
						bulk.add(next);
					} else {
						next = bulk.get(0);
					}

					if (includePropertyShapeValues) {
						assert next.getScope() == ConstraintComponent.Scope.propertyShape;
						assert next.hasValue();
					}

					int targetChainSize;
					if (includePropertyShapeValues || next.getScope() != ConstraintComponent.Scope.propertyShape) {
						targetChainSize = next.getFullChainSize(true);
					} else {
						targetChainSize = next.getFullChainSize(includePropertyShapeValues);
					}

					if (parsedQuery == null) {
						parsedQuery = getParsedQuery(targetChainSize);
					}

					while (bulk.size() < bulkSize && iterator.hasNext()) {
						bulk.add(iterator.next());
					}

					List<String> varNames;

					if (direction == EffectiveTarget.Extend.right) {
						varNames = vars
								.stream()
								.limit(targetChainSize)
								.map(StatementMatcher.Variable::getName)
								.collect(Collectors.toList());
					} else {
						varNames = vars
								.stream()
								.skip(vars.size() - targetChainSize)
								.map(StatementMatcher.Variable::getName)
								.collect(Collectors.toList());
					}

					Set<String> varNamesSet = new HashSet<>(varNames);

					List<BindingSet> bindingSets = bulk
							.stream()
							.filter(t -> {
								int temp;
								if (includePropertyShapeValues
										|| t.getScope() != ConstraintComponent.Scope.propertyShape) {
									temp = t.getFullChainSize(true);
								} else {
									temp = t.getFullChainSize(includePropertyShapeValues);
								}

								return temp == targetChainSize;
							})
							.map(t -> new SimpleBindingSet(varNamesSet, varNames,
									t.getTargetChain(includePropertyShapeValues)))
							.collect(Collectors.toList());

					bulk = bulk
							.stream()
							.filter(t -> {
								int temp;
								if (includePropertyShapeValues
										|| t.getScope() != ConstraintComponent.Scope.propertyShape) {
									temp = t.getFullChainSize(true);
								} else {
									temp = t.getFullChainSize(includePropertyShapeValues);
								}

								return temp != targetChainSize;
							})
							.collect(toCollection(ArrayList::new));

					updateQuery(parsedQuery, bindingSets, targetChainSize);

					bindingSet = connection.evaluate(parsedQuery.getTupleExpr(), dataset,
							EmptyBindingSet.getInstance(), true);
				}
			}

			@Override
			public void localClose() throws SailException {
				try {
					bulk = null;
					parsedQuery = null;
					assert !iterator.hasNext();
					iterator.close();
				} finally {
					if (bindingSet != null) {
						bindingSet.close();
					}
				}
			}

			@Override
			protected boolean localHasNext() throws SailException {
				calculateNext();
				return bindingSet != null && bindingSet.hasNext();
			}

			@Override
			protected ValidationTuple loggingNext() throws SailException {
				calculateNext();
				return mapper.apply(bindingSet.next());
			}

		};
	}

	private ParsedQuery getParsedQuery(int targetChainSize) {

		StringBuilder values = new StringBuilder("\nVALUES( ");
		if (direction == EffectiveTarget.Extend.right) {

			for (int i = 0; i < targetChainSize; i++) {
				values.append("?").append(vars.get(i).getName()).append(" ");
			}
		} else if (direction == EffectiveTarget.Extend.left) {
			for (int i = vars.size() - targetChainSize; i < vars.size(); i++) {
				values.append("?").append(vars.get(i).getName()).append(" ");
			}

		} else {
			throw new IllegalStateException("Unknown direction: " + direction);
		}

		values.append("){}\n");

		String query = BindSelect.this.query;

		query = query.replace(AbstractConstraintComponent.VALUES_INJECTION_POINT, values.toString());
		query = "select * where { " + values + query + "\n}";

		QueryParserFactory queryParserFactory = QueryParserRegistry.getInstance()
				.get(QueryLanguage.SPARQL)
				.get();
		ParsedQuery parsedQuery;
		try {
			parsedQuery = queryParserFactory.getParser().parseQuery(query, null);

		} catch (MalformedQueryException e) {
			logger.error("Malformed query: \n{}", query);
			throw e;
		}
		return parsedQuery;
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
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
		source.receiveLogger(validationExecutionLogger);
	}

	@Override
	public boolean producesSorted() {
		return false;
	}

	@Override
	public boolean requiresSorted() {
		return true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		BindSelect that = (BindSelect) o;

		// added/removed connections are always newly minted per plan node, so we instead need to compare the underlying
		// sail
		if (connection instanceof MemoryStoreConnection && that.connection instanceof MemoryStoreConnection) {
			return bulkSize == that.bulkSize &&
					includePropertyShapeValues == that.includePropertyShapeValues &&
					((MemoryStoreConnection) connection).getSail()
							.equals(((MemoryStoreConnection) that.connection).getSail())
					&&
					varNames.equals(that.varNames) &&
					scope.equals(that.scope) &&
					query.equals(that.query) &&
					vars.equals(that.vars) &&
					source.equals(that.source) &&
					Objects.equals(dataset, that.dataset) &&
					direction == that.direction;
		} else {
			return bulkSize == that.bulkSize &&
					includePropertyShapeValues == that.includePropertyShapeValues &&
					Objects.equals(connection, that.connection) &&
					varNames.equals(that.varNames) &&
					scope.equals(that.scope) &&
					query.equals(that.query) &&
					vars.equals(that.vars) &&
					source.equals(that.source) &&
					Objects.equals(dataset, that.dataset) &&
					direction == that.direction;
		}

	}

	@Override
	public int hashCode() {
		// added/removed connections are always newly minted per plan node, so we instead need to compare the underlying
		// sail
		if (connection instanceof MemoryStoreConnection) {
			return Objects.hash(((MemoryStoreConnection) connection).getSail(), varNames, scope, query, vars, bulkSize,
					source, direction, includePropertyShapeValues, dataset);
		} else {
			return Objects.hash(connection, varNames, scope, query, vars, bulkSize, source, direction,
					includePropertyShapeValues, dataset);
		}
	}

	@Override
	public String toString() {
		return "BindSelect{" +
				"query='" + query.replace("\n", "\t") + '\'' +
				", vars=" + vars +
				", bulkSize=" + bulkSize +
				", source=" + source +
				", direction=" + direction +
				", includePropertyShapeValues=" + includePropertyShapeValues +
				", varNames=" + varNames +
				", scope=" + scope +
				'}';
	}

}
