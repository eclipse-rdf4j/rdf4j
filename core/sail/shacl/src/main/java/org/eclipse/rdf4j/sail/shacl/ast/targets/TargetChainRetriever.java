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

package org.eclipse.rdf4j.sail.shacl.ast.targets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.ArrayBindingBasedQueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.impl.SimpleBinding;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserFactory;
import org.eclipse.rdf4j.query.parser.QueryParserRegistry;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher.Variable;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.LoggingCloseableIteration;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeHelper;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.SimpleBindingSet;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.SingletonBindingSet;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationExecutionLogger;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to run the query that represents the target and sets the bindings based on values that match the statement
 * patterns from the added/removed sail connection
 */
public class TargetChainRetriever implements PlanNode {

	private static final Logger logger = LoggerFactory.getLogger(TargetChainRetriever.class);

	private static final int BULK_SIZE = 1000;

	private final ConnectionsGroup connectionsGroup;
	private final List<StatementMatcher> statementMatchers;
	private final List<StatementMatcher> removedStatementMatchers;
	private final String queryFragment;
	private final QueryParserFactory queryParserFactory;
	private final ConstraintComponent.Scope scope;
	private final Resource[] dataGraph;
	private final Dataset dataset;
	private final Set<String> varNames;
	private final String sparqlProjection;
	private final EffectiveTarget.EffectiveTargetFragment removedStatementTarget;
	private final boolean hasValue;
	private final Set<String> varNamesInQueryFragment;

	private StackTraceElement[] stackTrace;
	private ValidationExecutionLogger validationExecutionLogger;

	public TargetChainRetriever(ConnectionsGroup connectionsGroup,
			Resource[] dataGraph, List<StatementMatcher> statementMatchers,
			List<StatementMatcher> removedStatementMatchers,
			EffectiveTarget.EffectiveTargetFragment removedStatementTarget, SparqlFragment queryFragment,
			List<Variable<Value>> vars, ConstraintComponent.Scope scope, boolean hasValue) {
		this.connectionsGroup = connectionsGroup;
		this.dataGraph = dataGraph;
		this.varNames = vars.stream().map(StatementMatcher.Variable::getName).collect(Collectors.toSet());
		assert !this.varNames.isEmpty();
		this.dataset = PlanNodeHelper.asDefaultGraphDataset(this.dataGraph);
		this.statementMatchers = StatementMatcher.reduce(statementMatchers);

		this.scope = scope;

		this.sparqlProjection = vars.stream()
				.map(StatementMatcher.Variable::asSparqlVariable)
				.reduce((a, b) -> a + " " + b)
				.orElseThrow(IllegalStateException::new);

		this.queryFragment = queryFragment.getNamespacesForSparql()
				+ StatementMatcher.StableRandomVariableProvider.normalize(queryFragment.getFragment());

//		this.stackTrace = Thread.currentThread().getStackTrace();

		this.queryParserFactory = QueryParserRegistry.getInstance()
				.get(QueryLanguage.SPARQL)
				.get();

		this.varNamesInQueryFragment = Set.of(ArrayBindingBasedQueryEvaluationContext
				.findAllVariablesUsedInQuery(((QueryRoot) queryParserFactory.getParser()
						.parseQuery("select * where {\n" + this.queryFragment + "\n}", null)
						.getTupleExpr())));

		assert !varNamesInQueryFragment.isEmpty();

		this.removedStatementMatchers = removedStatementMatchers != null
				? StatementMatcher.reduce(removedStatementMatchers)
				: Collections.emptyList();

		this.removedStatementTarget = removedStatementTarget;

		this.hasValue = hasValue;

		assert scope == ConstraintComponent.Scope.propertyShape || !this.hasValue;

	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {

		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			private final Iterator<StatementMatcher> statementPatternIterator = statementMatchers.iterator();
			private final Iterator<StatementMatcher> removedStatementIterator = removedStatementMatchers.iterator();

			private StatementMatcher currentStatementMatcher;
			private String sparqlValuesDecl;
			private Set<String> currentVarNames;
			private CloseableIteration<? extends Statement, SailException> statements;
			private ValidationTuple next;

			private CloseableIteration<? extends BindingSet, QueryEvaluationException> results;

			private ParsedQuery parsedQuery;

			private boolean removedStatement = false;

			private final List<BindingSet> bulk = new ArrayList<>(BULK_SIZE);

			@Override
			protected void init() {
				// no-op
			}

			public void calculateNextStatementMatcher() {
				if (statements != null && statements.hasNext()) {
					return;
				}

				if (!statementPatternIterator.hasNext() && !removedStatementIterator.hasNext()) {
					if (statements != null) {
						statements.close();
						statements = null;
					}

					return;
				}

				do {
					if (statements != null) {
						statements.close();
						statements = null;
					}

					if (!statementPatternIterator.hasNext() && !removedStatementIterator.hasNext()) {
						break;
					}

					SailConnection connection;

					if (statementPatternIterator.hasNext()) {
						currentStatementMatcher = statementPatternIterator.next();
						connection = connectionsGroup.getAddedStatements();
						removedStatement = false;
					} else {
						if (!connectionsGroup.getStats().hasRemoved()) {
							break;
						}
						currentStatementMatcher = removedStatementIterator.next();
						connection = connectionsGroup.getRemovedStatements();
						removedStatement = true;
					}

					this.sparqlValuesDecl = currentStatementMatcher.getSparqlValuesDecl(varNames, removedStatement,
							varNamesInQueryFragment);
					this.currentVarNames = currentStatementMatcher.getVarNames(varNames, removedStatement,
							varNamesInQueryFragment);

					assert !currentVarNames.isEmpty();

					statements = connection.getStatements(
							currentStatementMatcher.getSubjectValue(),
							currentStatementMatcher.getPredicateValue(),
							currentStatementMatcher.getObjectValue(), false, dataGraph);

				} while (!statements.hasNext());

				parsedQuery = null;

			}

			private void calculateNextResult() {
				if (next != null) {
					return;
				}

				while (results == null || !results.hasNext()) {
					try {
						if (results != null) {
							results.close();
							results = null;

						}

						while (statements == null || !statements.hasNext()) {
							calculateNextStatementMatcher();
							if (statements == null) {
								return;
							}
						}

						if (parsedQuery == null) {
							String query = "select " + sparqlProjection + " where {\n" +
									sparqlValuesDecl +
									queryFragment + "\n" +
									"}";

							parsedQuery = queryParserFactory.getParser().parseQuery(query, null);
						}

						List<BindingSet> bulk = readStatementsInBulk(currentVarNames);
						setBindings(currentVarNames, bulk);

						results = connectionsGroup.getBaseConnection()
								.evaluate(parsedQuery.getTupleExpr(), dataset,
										EmptyBindingSet.getInstance(), true);

					} catch (MalformedQueryException e) {
						logger.error("Malformed query:\n{}", queryFragment);
						throw e;
					}
				}

				if (results.hasNext()) {
					BindingSet nextBinding = results.next();

					if (nextBinding.size() == 1) {
						Iterator<Binding> iterator = nextBinding.iterator();
						if (iterator.hasNext()) {
							next = new ValidationTuple(iterator.next().getValue(), scope, hasValue, dataGraph);
						} else {
							next = new ValidationTuple((Value) null, scope, hasValue, dataGraph);
						}
					} else {
						Value[] values = StreamSupport.stream(nextBinding.spliterator(), false)
								.sorted(Comparator.comparing(Binding::getName))
								.map(Binding::getValue)
								.toArray(Value[]::new);
						next = new ValidationTuple(values, scope, hasValue, dataGraph);

					}

				}

			}

			private List<BindingSet> readStatementsInBulk(Set<String> varNames) {
				bulk.clear();

				while (bulk.size() < BULK_SIZE && statements.hasNext()) {
					Statement next = statements.next();
					Stream<EffectiveTarget.StatementsAndMatcher> rootStatements = Stream
							.of(new EffectiveTarget.StatementsAndMatcher(List.of(next), currentStatementMatcher));
					if (removedStatement) {
						Stream<EffectiveTarget.StatementsAndMatcher> root = removedStatementTarget.getRoot(
								connectionsGroup,
								dataGraph, currentStatementMatcher,
								next);
						if (root != null) {
							rootStatements = root;
						}
					}

					rootStatements
							.filter(EffectiveTarget.StatementsAndMatcher::hasStatements)
							.flatMap(statementsAndMatcher -> {
								StatementMatcher newCurrentStatementMatcher = statementsAndMatcher
										.getStatementMatcher();

								return statementsAndMatcher.getStatements()
										.stream()
										.map(temp -> {
											Binding[] bindings = new Binding[varNames.size()];
											int j = 0;

											if (newCurrentStatementMatcher.getSubjectValue() == null
													&& currentVarNames
															.contains(newCurrentStatementMatcher.getSubjectName())) {
												bindings[j++] = new SimpleBinding(
														newCurrentStatementMatcher.getSubjectName(),
														temp.getSubject());
											}

											if (newCurrentStatementMatcher.getPredicateValue() == null
													&& currentVarNames
															.contains(newCurrentStatementMatcher.getPredicateName())) {
												bindings[j++] = new SimpleBinding(
														newCurrentStatementMatcher.getPredicateName(),
														temp.getPredicate());
											}

											if (newCurrentStatementMatcher.getObjectValue() == null
													&& currentVarNames
															.contains(newCurrentStatementMatcher.getObjectName())) {
												bindings[j++] = new SimpleBinding(
														newCurrentStatementMatcher.getObjectName(),
														temp.getObject());
											}
											if (bindings.length == 1) {
												return new SingletonBindingSet(bindings[0].getName(),
														bindings[0].getValue());

											} else {
												return new SimpleBindingSet(varNames, bindings);
											}
										});

							})
							.distinct()
							.forEach(bulk::add);

				}

				return bulk;
			}

			private void setBindings(Set<String> varNames, List<BindingSet> bulk) {
				parsedQuery.getTupleExpr()
						.visit(new AbstractSimpleQueryModelVisitor<>(false) {
							@Override
							public void meet(BindingSetAssignment node) {
								Set<String> bindingNames = node.getBindingNames();
								if (bindingNames.equals(varNames)) {
									node.setBindingSets(bulk);
								}
								super.meet(node);
							}

						});
			}

			@Override
			public void localClose() {

				try {
					if (statements != null) {
						statements.close();
					}
				} finally {
					if (results != null) {
						results.close();
					}
				}

			}

			@Override
			protected ValidationTuple loggingNext() {
				calculateNextResult();

				ValidationTuple temp = next;
				next = null;

				return temp;
			}

			@Override
			protected boolean localHasNext() {
				calculateNextResult();

				return next != null;
			}

		};
	}

	private static boolean bindingsEquivalent(StatementMatcher currentStatementMatcher, MapBindingSet bindings,
			MapBindingSet previousBindings) {
		if (currentStatementMatcher == null || bindings == null || previousBindings == null) {
			return false;
		}

		boolean equivalent = true;

		if (equivalent && currentStatementMatcher.getSubjectValue() == null
				&& !currentStatementMatcher.subjectIsWildcard()) {
			equivalent = Objects.equals(bindings.getBinding(currentStatementMatcher.getSubjectName()),
					previousBindings.getBinding(currentStatementMatcher.getSubjectName()));
		}

		if (equivalent && currentStatementMatcher.getPredicateValue() == null
				&& !currentStatementMatcher.predicateIsWildcard()) {
			equivalent = Objects.equals(bindings.getBinding(currentStatementMatcher.getPredicateName()),
					previousBindings.getBinding(currentStatementMatcher.getPredicateName()));
		}

		if (equivalent && currentStatementMatcher.getObjectValue() == null
				&& !currentStatementMatcher.objectIsWildcard()) {
			equivalent = Objects.equals(bindings.getBinding(currentStatementMatcher.getObjectName()),
					previousBindings.getBinding(currentStatementMatcher.getObjectName()));
		}

		return equivalent;

	}

	@Override
	public int depth() {
		return 0;
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {

	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
	}

	@Override
	public boolean producesSorted() {
		return false;
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
		TargetChainRetriever that = (TargetChainRetriever) o;
		return statementMatchers.equals(that.statementMatchers) &&
				removedStatementMatchers.equals(that.removedStatementMatchers) &&
				queryFragment.equals(that.queryFragment) &&
				Objects.equals(dataset, that.dataset) &&
				scope == that.scope;
	}

	@Override
	public int hashCode() {
		return Objects.hash(statementMatchers, removedStatementMatchers, queryFragment, scope, dataset);
	}

	@Override
	public String toString() {
		return "TargetChainRetriever{" +
				"statementPatterns=" + statementMatchers +
				", removedStatementMatchers=" + removedStatementMatchers +
				", query='" + queryFragment.replace("\n", "\t") + '\'' +
				", scope=" + scope +
				'}';
	}
}
