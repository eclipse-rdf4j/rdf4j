package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.FilterIteration;
import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.StatementPattern.Scope;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategy;

public class StatementPatternIteration {
	private static final class ConvertStatmentToBindingSetIterator
			extends ConvertingIteration<Statement, BindingSet, QueryEvaluationException> {
		private final Supplier<QueryBindingSet> bindings;
		private final BiConsumer<QueryBindingSet, Statement> converter;

		private ConvertStatmentToBindingSetIterator(
				Iteration<? extends Statement, ? extends QueryEvaluationException> iter, Var subjVar, Var predVar,
				Var objVar, Var conVar, BindingSet bindings) {
			super(iter);
			if (bindings.size() == 0) {
				this.bindings = QueryBindingSet::new;
			} else {
				this.bindings = () -> new QueryBindingSet(bindings);
			}

			// We are going to chain biconsumer functions allowing us to avoid a lot of hasBindings etc. code
			// once the query is getting executed.
			BiConsumer<QueryBindingSet, Statement> co = null;

			if (subjVar != null && !subjVar.isConstant() && !bindings.hasBinding(subjVar.getName())) {
				co = andThen(co, (result, st) -> result.addBinding(subjVar.getName(), st.getSubject()));
			}
			// We should not overwrite previous set values so if pred == subj we don't need to call this again.
			if (predVar != null && !predVar.isConstant() && !bindings.hasBinding(predVar.getName())
					&& !predVar.equals(subjVar)) {
				co = andThen(co, (result, st) -> result.addBinding(predVar.getName(), st.getPredicate()));
			}
			if (objVar != null && !objVar.isConstant() && !bindings.hasBinding(objVar.getName())
					&& !objVar.equals(subjVar) && !objVar.equals(predVar)) {
				co = andThen(co, (result, st) -> result.addBinding(objVar.getName(), st.getObject()));
			}
			if (conVar != null && !conVar.isConstant() && !bindings.hasBinding(conVar.getName())
					&& !conVar.equals(subjVar) && !conVar.equals(predVar) && !conVar.equals(objVar)) {
				co = andThen(co, (result, st) -> {
					if (st.getContext() != null) {
						result.addBinding(conVar.getName(), st.getContext());
					}
				});
			}
			if (co == null) {
				co = (result, statement) -> {
				};
			}
			converter = co;
		}

		private BiConsumer<QueryBindingSet, Statement> andThen(BiConsumer<QueryBindingSet, Statement> co,
				BiConsumer<QueryBindingSet, Statement> and) {
			if (co == null)
				return and;
			return co.andThen(and);
		}

		@Override
		protected BindingSet convert(Statement st) {
			QueryBindingSet result = bindings.get();
			converter.accept(result, st);
			return result;
		}
	}

	private final StatementPattern statementPattern;
	private final BindingSet bindings;
	private final Dataset dataset;
	private final TripleSource tripleSource;

	public StatementPatternIteration(StatementPattern statementPattern, BindingSet bindings, Dataset dataset,
			TripleSource tripleSource) {
		super();
		this.statementPattern = statementPattern;
		this.bindings = bindings;
		this.dataset = dataset;
		this.tripleSource = tripleSource;
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate() {
		final Var subjVar = statementPattern.getSubjectVar();
		final Var predVar = statementPattern.getPredicateVar();
		final Var objVar = statementPattern.getObjectVar();
		final Var conVar = statementPattern.getContextVar();

		if (isUnbound(subjVar, bindings) || isUnbound(predVar, bindings) || isUnbound(objVar, bindings)
				|| isUnbound(conVar, bindings)) {
			// the variable must remain unbound for this solution see
			// https://www.w3.org/TR/sparql11-query/#assignment
			return new EmptyIteration<>();
		}

		final Value contextValue = StrictEvaluationStrategy.getVarValue(conVar, bindings);
		Resource[] contexts = extractContextsFromDatasets(statementPattern, contextValue, dataset);
		if (contexts == null) {
			return new EmptyIteration<>();
		}

		final Value subjValue = StrictEvaluationStrategy.getVarValue(subjVar, bindings);
		final Value predValue = StrictEvaluationStrategy.getVarValue(predVar, bindings);
		final Value objValue = StrictEvaluationStrategy.getVarValue(objVar, bindings);
		// Check that the subject is a Resource and the predicate can be an IRI
		// if not we can't return any value.
		Resource subjResouce;
		IRI predIri;
		try {
			subjResouce = (Resource) subjValue;
			predIri = (IRI) predValue;
		} catch (ClassCastException e) {
			// Invalid value type for subject, predicate and/or context
			return new EmptyIteration<>();
		}

		CloseableIteration<? extends Statement, QueryEvaluationException> stIter1 = tripleSource
				.getStatements(subjResouce, predIri, objValue, contexts);
		Predicate<Statement> filter = filterContextOrEqualVariables(statementPattern, subjVar, predVar, objVar,
				conVar,
				subjValue,
				predValue, objValue, contexts);
		if (filter != null) {
			stIter1 = new FilterIteration<Statement, QueryEvaluationException>(stIter1) {

				@Override
				protected boolean accept(Statement object) throws QueryEvaluationException {
					return filter.test(object);
				}
			};
		}

		// Return an iterator that converts the statements to var bindings
		return new ConvertStatmentToBindingSetIterator(stIter1, subjVar, predVar, objVar, conVar,
				bindings);
	}

	/**
	 * Generate a predicate that tests for Named contexts are matched by retrieving all statements from the store and
	 * filtering out the statements that do not have a context. Or the same variable might have been used multiple times
	 * in this StatementPattern, verify value equality in those cases.
	 */
	protected Predicate<Statement> filterContextOrEqualVariables(StatementPattern statementPattern,
			final Var subjVar,
			final Var predVar,
			final Var objVar, final Var conVar, final Value subjValue, final Value predValue, final Value objValue,
			Resource[] contexts) {
		Predicate<Statement> filter = null;
		if (contexts.length == 0 && statementPattern.getScope() == Scope.NAMED_CONTEXTS) {
			filter = (st) -> st.getContext() != null;
		}
		return filterSameVariable(subjVar, predVar, objVar, conVar, subjValue, predValue, objValue, filter);
	}

	protected Predicate<Statement> filterSameVariable(final Var subjVar, final Var predVar, final Var objVar,
			final Var conVar, final Value subjValue, final Value predValue, final Value objValue,
			Predicate<Statement> filter) {
		if (subjVar != null && (subjVar.equals(predVar) || subjVar.equals(objVar) || subjVar.equals(conVar))
				&& subjValue == null) {
			filter = andThen(filter, (st) -> {
				Resource subj = st.getSubject();
				IRI pred = st.getPredicate();
				Value obj = st.getObject();
				Resource context = st.getContext();

				if (subjVar.equals(predVar) && !subj.equals(pred)) {
					return false;
				}
				if (subjVar.equals(objVar) && !subj.equals(obj)) {
					return false;
				}
				if (subjVar.equals(conVar) && !subj.equals(context)) {
					return false;
				}
				return true;
			});
		}
		if (predVar != null && (predVar.equals(objVar) || predVar.equals(conVar)) && predValue == null) {
			filter = andThen(filter, (st) -> {
				IRI pred = st.getPredicate();
				Value obj = st.getObject();
				Resource context = st.getContext();

				if (predVar.equals(objVar) && !pred.equals(obj)) {
					return false;
				}
				if (predVar.equals(conVar) && !pred.equals(context)) {
					return false;
				}
				return true;
			});
		}
		if (objVar != null && (objVar.equals(conVar)) && objValue == null) {
			filter = andThen(filter, (st) -> {
				Value obj = st.getObject();
				Resource context = st.getContext();
				if (objVar.equals(conVar) && !obj.equals(context)) {
					return false;
				}
				return true;
			});
		}
		return filter;
	}

	private Predicate<Statement> andThen(Predicate<Statement> pred, Predicate<Statement> and) {
		if (pred == null) {
			return and;
		}
		return pred.and(and);
	}

	/**
	 * @param statementPattern
	 * @param contextValue
	 * @return the contexts that are valid for this statement pattern or null
	 */
	protected static Resource[] extractContextsFromDatasets(StatementPattern statementPattern,
			final Value contextValue, Dataset dataset) {
		Resource[] contexts;

		boolean emptyGraph = false;

		Set<IRI> graphs = null;
		if (dataset != null) {
			if (statementPattern.getScope() == Scope.DEFAULT_CONTEXTS) {
				graphs = dataset.getDefaultGraphs();
				emptyGraph = graphs.isEmpty() && !dataset.getNamedGraphs().isEmpty();
			} else {
				graphs = dataset.getNamedGraphs();
				emptyGraph = graphs.isEmpty() && !dataset.getDefaultGraphs().isEmpty();
			}
		}

		if (emptyGraph) {
			// Search zero contexts
			return null;
		} else if (graphs == null || graphs.isEmpty()) {
			// store default behaviour
			if (contextValue != null) {
				if (RDF4J.NIL.equals(contextValue) || SESAME.NIL.equals(contextValue)) {
					contexts = new Resource[] { (Resource) null };
				} else {
					contexts = new Resource[] { (Resource) contextValue };
				}
			}
			/*
			 * TODO activate this to have an exclusive (rather than inclusive) interpretation of the default graph in
			 * SPARQL querying. else if (statementPattern.getScope() == Scope.DEFAULT_CONTEXTS ) { contexts = new
			 * Resource[] { (Resource)null }; }
			 */
			else {
				contexts = new Resource[0];
			}
		} else if (contextValue != null) {
			if (graphs.contains(contextValue)) {
				contexts = new Resource[] { (Resource) contextValue };
			} else {
				// Statement pattern specifies a context that is not part of
				// the dataset
				return null;
			}
		} else {
			contexts = new Resource[graphs.size()];
			int i = 0;
			for (IRI graph : graphs) {
				IRI context = null;
				if (!(RDF4J.NIL.equals(graph) || SESAME.NIL.equals(graph))) {
					context = graph;
				}
				contexts[i++] = context;
			}
		}
		return contexts;
	}

	protected boolean isUnbound(Var var, BindingSet bindings) {
		if (var == null) {
			return false;
		} else {
			return bindings.hasBinding(var.getName()) && bindings.getValue(var.getName()) == null;
		}
	}

}
