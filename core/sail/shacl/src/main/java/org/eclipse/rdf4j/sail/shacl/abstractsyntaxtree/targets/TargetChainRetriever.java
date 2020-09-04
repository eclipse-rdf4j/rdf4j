package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.targets;

import java.util.ArrayDeque;
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
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ValidationExecutionLogger;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ValidationTuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TargetChainRetriever implements PlanNode {

	private static final Logger logger = LoggerFactory.getLogger(TargetChainRetriever.class);

	private final SailConnection transactionalConnection;
	private final SailConnection baseConnection;
	private final List<StatementPattern> statementPatterns;
	private final String query;
	private final QueryParserFactory queryParserFactory;
	private final ConstraintComponent.Scope scope;

	public TargetChainRetriever(SailConnection transactionalConnection, SailConnection baseConnection,
								List<StatementPattern> statementPatterns, String query, ConstraintComponent.Scope scope) {
		this.transactionalConnection = transactionalConnection;
		this.baseConnection = baseConnection;
		this.statementPatterns = statementPatterns;
		this.scope = scope;
		this.query = "select * where {" + query + "}";

		queryParserFactory = QueryParserRegistry.getInstance()
				.get(QueryLanguage.SPARQL)
				.get();
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {
		return new CloseableIteration<ValidationTuple, SailException>() {

			final Iterator<StatementPattern> statementPatternIterator = statementPatterns.iterator();

			StatementPattern currentStatementPattern;
			CloseableIteration<? extends Statement, SailException> statements;
			ValidationTuple next;

			CloseableIteration<? extends BindingSet, QueryEvaluationException> results;

			public void calculateNextStatementPattern() {
				if (statements != null && statements.hasNext()) {
					return;
				}

				if (!statementPatternIterator.hasNext()) {
					if (statements != null) {
						statements.close();
					}

					return;
				}

				do {
					if (statements != null) {
						statements.close();
					}

					if (!statementPatternIterator.hasNext()) {
						break;
					}

					currentStatementPattern = statementPatternIterator.next();

					statements = transactionalConnection.getStatements(
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

						ParsedQuery parsedQuery = queryParserFactory.getParser().parseQuery(query, null);

						MapBindingSet bindings = new MapBindingSet();

						if (statements == null || !statements.hasNext()) {
							calculateNextStatementPattern();
							if (statements == null || !statements.hasNext()) {
								return;
							}
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

						results = baseConnection.evaluate(parsedQuery.getTupleExpr(), parsedQuery.getDataset(),
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
}
