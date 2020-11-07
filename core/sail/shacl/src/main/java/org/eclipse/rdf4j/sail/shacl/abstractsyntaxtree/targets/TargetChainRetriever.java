package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.targets;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserFactory;
import org.eclipse.rdf4j.query.parser.QueryParserRegistry;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ValidationExecutionLogger;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ValidationTuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to run the query that represents the target and sets the bindings based on values that match the statement
 * patterns from the added/removed sail connection
 */
public class TargetChainRetriever implements PlanNode {

	private static final Logger logger = LoggerFactory.getLogger(TargetChainRetriever.class);

	private final ConnectionsGroup connectionsGroup;
	private final List<StatementPattern> statementPatterns;
	private final List<StatementPattern> removedStatementPatterns;
	private final String query;
	private final QueryParserFactory queryParserFactory;
	private final ConstraintComponent.Scope scope;
	private final StackTraceElement[] stackTrace;

	public TargetChainRetriever(ConnectionsGroup connectionsGroup,
			List<StatementPattern> statementPatterns, List<StatementPattern> removedStatementPatterns, String query,
			List<Var> vars, ConstraintComponent.Scope scope) {
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

		this.removedStatementPatterns = removedStatementPatterns != null ? removedStatementPatterns
				: Collections.emptyList();
		assert this.removedStatementPatterns.size() <= 1;
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {
		return new CloseableIteration<ValidationTuple, SailException>() {

			final Iterator<StatementPattern> statementPatternIterator = statementPatterns.iterator();
			final Iterator<StatementPattern> removedStatementIterator = removedStatementPatterns.iterator();

			StatementPattern currentStatementPattern;
			CloseableIteration<? extends Statement, SailException> statements;
			ValidationTuple next;

			CloseableIteration<? extends BindingSet, QueryEvaluationException> results;

			ParsedQuery parsedQuery;

			public void calculateNextStatementPattern() {
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
						currentStatementPattern = statementPatternIterator.next();
						connection = connectionsGroup.getAddedStatements();
					} else {
						currentStatementPattern = removedStatementIterator.next();
						connection = connectionsGroup.getRemovedStatements();
					}

					statements = connection.getStatements(
							(Resource) currentStatementPattern.getSubjectVar().getValue(),
							(IRI) currentStatementPattern.getPredicateVar().getValue(),
							currentStatementPattern.getObjectVar().getValue(), false);
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
							calculateNextStatementPattern();
							if (statements == null || !statements.hasNext()) {
								return;
							}
						}

						if (parsedQuery == null) {
							parsedQuery = queryParserFactory.getParser().parseQuery(query, null);
						}

						Statement next = statements.next();

						Var subjectVar = currentStatementPattern.getSubjectVar();
						Var predicateVar = currentStatementPattern.getPredicateVar();
						Var objectVar = currentStatementPattern.getObjectVar();


						if (!subjectVar.hasValue()) {
							bindings.addBinding(subjectVar.getName(), next.getSubject());
						}

						if (!predicateVar.hasValue()) {
							bindings.addBinding(predicateVar.getName(), next.getPredicate());
						}

						if (!objectVar.hasValue()) {
							bindings.addBinding(objectVar.getName(), next.getObject());
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

					ArrayDeque<Value> collect = nextBinding.getBindingNames()
							.stream()
							.sorted()
							.map(nextBinding::getValue)
							.collect(Collectors.toCollection(ArrayDeque::new));

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
}
