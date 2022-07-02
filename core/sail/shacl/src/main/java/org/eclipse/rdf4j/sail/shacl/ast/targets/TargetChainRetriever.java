/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.targets;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
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
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserFactory;
import org.eclipse.rdf4j.query.parser.QueryParserRegistry;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.LoggingCloseableIteration;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeHelper;
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

	private final ConnectionsGroup connectionsGroup;
	private final List<StatementMatcher> statementMatchers;
	private final List<StatementMatcher> removedStatementMatchers;
	private final String query;
	private final QueryParserFactory queryParserFactory;
	private final ConstraintComponent.Scope scope;
	private final Resource[] dataGraph;
	private final Dataset dataset;
	private StackTraceElement[] stackTrace;
	private ValidationExecutionLogger validationExecutionLogger;

	public TargetChainRetriever(ConnectionsGroup connectionsGroup,
			Resource[] dataGraph, List<StatementMatcher> statementMatchers,
			List<StatementMatcher> removedStatementMatchers, String query,
			List<StatementMatcher.Variable> vars, ConstraintComponent.Scope scope) {
		this.connectionsGroup = connectionsGroup;
		this.dataGraph = dataGraph;
		this.dataset = PlanNodeHelper.asDefaultGraphDataset(this.dataGraph);
		this.statementMatchers = StatementMatcher.reduce(statementMatchers);

		this.scope = scope;

		String sparqlProjection = vars.stream()
				.map(s -> "?" + s.getName())
				.reduce((a, b) -> a + " " + b)
				.orElseThrow(IllegalStateException::new);

		this.query = "select " + sparqlProjection + " where {\n"
				+ StatementMatcher.StableRandomVariableProvider.normalize(query) + "\n}";
//		this.stackTrace = Thread.currentThread().getStackTrace();

		queryParserFactory = QueryParserRegistry.getInstance()
				.get(QueryLanguage.SPARQL)
				.get();

		this.removedStatementMatchers = removedStatementMatchers != null
				? StatementMatcher.reduce(removedStatementMatchers)
				: Collections.emptyList();

	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {

		return new LoggingCloseableIteration(this, validationExecutionLogger) {

			final Iterator<StatementMatcher> statementPatternIterator = statementMatchers.iterator();
			final Iterator<StatementMatcher> removedStatementIterator = removedStatementMatchers.iterator();

			StatementMatcher currentStatementMatcher;
			CloseableIteration<? extends Statement, SailException> statements;
			ValidationTuple next;

			CloseableIteration<? extends BindingSet, QueryEvaluationException> results;

			ParsedQuery parsedQuery;

			// for de-duping bindings
			MapBindingSet previousBindings;

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
					} else {
						if (!connectionsGroup.getStats().hasRemoved()) {
							break;
						}
						currentStatementMatcher = removedStatementIterator.next();
						connection = connectionsGroup.getRemovedStatements();
					}

					statements = connection.getStatements(
							currentStatementMatcher.getSubjectValue(),
							currentStatementMatcher.getPredicateValue(),
							currentStatementMatcher.getObjectValue(), false, dataGraph);

				} while (!statements.hasNext());

				previousBindings = null;

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
							parsedQuery = queryParserFactory.getParser().parseQuery(query, null);
						}

						Statement next = statements.next();

						MapBindingSet bindings = new MapBindingSet();

						if (currentStatementMatcher.getSubjectValue() == null
								&& !currentStatementMatcher.subjectIsWildcard()) {
							bindings.addBinding(currentStatementMatcher.getSubjectName(), next.getSubject());
						}

						if (currentStatementMatcher.getPredicateValue() == null
								&& !currentStatementMatcher.predicateIsWildcard()) {
							bindings.addBinding(currentStatementMatcher.getPredicateName(), next.getPredicate());
						}

						if (currentStatementMatcher.getObjectValue() == null
								&& !currentStatementMatcher.objectIsWildcard()) {
							bindings.addBinding(currentStatementMatcher.getObjectName(), next.getObject());
						}

						if (bindingsEquivalent(currentStatementMatcher, bindings, previousBindings)) {
							continue;
						}

						previousBindings = bindings;

						// TODO: Should really bulk this operation!

						results = connectionsGroup.getBaseConnection()
								.evaluate(parsedQuery.getTupleExpr(), dataset,
										bindings, true);

					} catch (MalformedQueryException e) {
						logger.error("Malformed query: \n{}", query);
						throw e;
					}
				}

				if (results.hasNext()) {
					BindingSet nextBinding = results.next();

					if (nextBinding.size() == 1) {
						Iterator<Binding> iterator = nextBinding.iterator();
						if (iterator.hasNext()) {
							next = new ValidationTuple(iterator.next().getValue(), scope, false, dataGraph);
						} else {
							next = new ValidationTuple((Value) null, scope, false, dataGraph);
						}
					} else {
						Value[] values = StreamSupport.stream(nextBinding.spliterator(), false)
								.sorted(Comparator.comparing(Binding::getName))
								.map(Binding::getValue)
								.toArray(Value[]::new);
						next = new ValidationTuple(values, scope, false, dataGraph);

					}

				}

			}

			@Override
			public void localClose() throws SailException {

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
			protected ValidationTuple loggingNext() throws SailException {
				calculateNextResult();

				ValidationTuple temp = next;
				next = null;

				return temp;
			}

			@Override
			protected boolean localHasNext() throws SailException {
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
				query.equals(that.query) &&
				Objects.equals(dataset, that.dataset) &&
				scope == that.scope;
	}

	@Override
	public int hashCode() {
		return Objects.hash(statementMatchers, removedStatementMatchers, query, scope, dataset);
	}

	@Override
	public String toString() {
		return "TargetChainRetriever{" +
				"statementPatterns=" + statementMatchers +
				", removedStatementMatchers=" + removedStatementMatchers +
				", query='" + query.replace("\n", "\t") + '\'' +
				", scope=" + scope +
				'}';
	}
}
