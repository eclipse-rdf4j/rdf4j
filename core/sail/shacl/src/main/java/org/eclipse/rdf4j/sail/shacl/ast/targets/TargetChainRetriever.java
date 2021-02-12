package org.eclipse.rdf4j.sail.shacl.ast.targets;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
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
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationExecutionLogger;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to run the query that represents the target and sets the bindings based on values that match the statement
 * patterns from the added/removed sail connection
 */
public class TargetChainRetriever implements PlanNode {

	private static final Logger logger = LoggerFactory.getLogger(TargetChainRetriever.class);

	private final ConnectionsGroup connectionsGroup;
	private final List<StatementMatcher> statementPatterns;
	private final List<StatementMatcher> removedStatementMatchers;
	private final String query;
	private final QueryParserFactory queryParserFactory;
	private final ConstraintComponent.Scope scope;
	private final StackTraceElement[] stackTrace;

	public TargetChainRetriever(ConnectionsGroup connectionsGroup,
			List<StatementMatcher> statementPatterns, List<StatementMatcher> removedStatementMatchers, String query,
			List<StatementMatcher.Variable> vars, ConstraintComponent.Scope scope) {
		this.connectionsGroup = connectionsGroup;
		this.statementPatterns = statementPatterns;
		this.scope = scope;

		String sparqlProjection = vars.stream()
				.map(s -> "?" + s.getName())
				.reduce((a, b) -> a + " " + b)
				.orElseThrow(IllegalStateException::new);

		this.query = "select " + sparqlProjection + " where {" + query + "}";
		this.stackTrace = Thread.currentThread().getStackTrace();

		queryParserFactory = QueryParserRegistry.getInstance()
				.get(QueryLanguage.SPARQL)
				.get();

		this.removedStatementMatchers = removedStatementMatchers != null ? removedStatementMatchers
				: Collections.emptyList();
		assert this.removedStatementMatchers.size() <= 1;
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {
		return new CloseableIteration<ValidationTuple, SailException>() {

			final Iterator<StatementMatcher> statementPatternIterator = statementPatterns.iterator();
			final Iterator<StatementMatcher> removedStatementIterator = removedStatementMatchers.iterator();

			StatementMatcher currentStatementMatcher;
			CloseableIteration<? extends Statement, SailException> statements;
			ValidationTuple next;

			CloseableIteration<? extends BindingSet, QueryEvaluationException> results;

			ParsedQuery parsedQuery;

			public void calculateNextStatementMatcher() {
				if (statements != null && statements.hasNext()) {
					return;
				}

				if (!statementPatternIterator.hasNext() && !removedStatementIterator.hasNext()) {
					if (statements != null) {
						statements.close();
					}

					return;
				}

				do {
					if (statements != null) {
						statements.close();
					}

					if (!statementPatternIterator.hasNext() && !removedStatementIterator.hasNext()) {
						break;
					}

					SailConnection connection;

					if (statementPatternIterator.hasNext()) {
						currentStatementMatcher = statementPatternIterator.next();
						connection = connectionsGroup.getAddedStatements();
					} else {
						currentStatementMatcher = removedStatementIterator.next();
						connection = connectionsGroup.getRemovedStatements();
					}

					statements = connection.getStatements(
							currentStatementMatcher.getSubjectValue(),
							currentStatementMatcher.getPredicateValue(),
							currentStatementMatcher.getObjectValue(), false);
				} while (!statements.hasNext());

			}

			private void calculateNextResult() {
				if (next != null) {
					return;
				}

				while (results == null || !results.hasNext()) {
					try {
						if (results != null) {
							results.close();
						}

						MapBindingSet bindings = new MapBindingSet();

						if (statements == null || !statements.hasNext()) {
							calculateNextStatementMatcher();
							if (statements == null || !statements.hasNext()) {
								return;
							}
						}

						if (parsedQuery == null) {
							parsedQuery = queryParserFactory.getParser().parseQuery(query, null);
						}

						Statement next = statements.next();

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

						// TODO: Should really bulk this operation!

						results = connectionsGroup.getBaseConnection()
								.evaluate(parsedQuery.getTupleExpr(), parsedQuery.getDataset(),
										bindings, true);

					} catch (MalformedQueryException e) {
						logger.error("Malformed query: \n{}", query);
						throw e;
					}
				}

				if (results.hasNext()) {
					BindingSet nextBinding = results.next();

					List<Value> collect = nextBinding.getBindingNames()
							.stream()
							.sorted()
							.map(nextBinding::getValue)
							.collect(Collectors.toList());

					next = new ValidationTuple(collect, scope, false);

				}

			}

			@Override
			public void close() throws SailException {

			}

			@Override
			public boolean hasNext() throws SailException {
				calculateNextResult();

				return next != null;
			}

			@Override
			public ValidationTuple next() throws SailException {
				calculateNextResult();

				ValidationTuple temp = next;
				next = null;

				return temp;
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

	}

	@Override
	public String getId() {
		return null;
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {

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
		return statementPatterns.equals(that.statementPatterns) &&
				removedStatementMatchers.equals(that.removedStatementMatchers) &&
				query.equals(that.query) &&
				scope == that.scope;
	}

	@Override
	public int hashCode() {
		return Objects.hash(statementPatterns, removedStatementMatchers, query, scope);
	}

	@Override
	public String toString() {
		return "TargetChainRetriever{" +
				"statementPatterns=" + statementPatterns +
				", removedStatementMatchers=" + removedStatementMatchers +
				", query='" + query.replace("\n", "\t") + '\'' +
				", scope=" + scope +
				'}';
	}
}
