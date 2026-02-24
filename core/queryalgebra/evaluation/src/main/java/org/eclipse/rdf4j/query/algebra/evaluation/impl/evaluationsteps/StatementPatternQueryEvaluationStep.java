/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.FilterIteration;
import org.eclipse.rdf4j.common.iteration.IndexReportingIterator;
import org.eclipse.rdf4j.common.order.StatementOrder;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.StatementPattern.Scope;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.explanation.TelemetryMetricNames;

/**
 * Evaluate the StatementPattern - taking care of graph/datasets - avoiding redoing work every call of evaluate if
 * possible.
 */
public class StatementPatternQueryEvaluationStep implements QueryEvaluationStep {

	public static final EmptyIteration<? extends Statement> EMPTY_ITERATION = new EmptyIteration<>();

	public static final Resource[] DEFAULT_CONTEXT = { null };
	public static final Resource[] ALL_CONTEXT = new Resource[0];
	private static final Function<Value, Resource[]> RETURN_NULL_VALUE_RESOURCE_ARRAY = v -> null;

	private final StatementPattern statementPattern;
	private final StatementPattern statementPatternForMetrics;
	private final TripleSource tripleSource;
	private final boolean emptyGraph;
	private final Function<Value, Resource[]> contextSup;
	private BiConsumer<MutableBindingSet, Statement> converter;
	private BiConsumer<MutableBindingSet, Statement> convertStatementConverter;
	private final QueryEvaluationContext context;
	private final StatementOrder order;

	private final Predicate<BindingSet> unboundTest;

	private final Function<BindingSet, Value> getContextVar;
	private final Function<BindingSet, Value> getSubjectVar;
	private final Function<BindingSet, Value> getPredicateVar;
	private final Function<BindingSet, Value> getObjectVar;

	private final Var normalizedSubjectVar;
	private final Var normalizedPredicateVar;
	private final Var normalizedObjectVar;
	private final Var normalizedContextVar;

	// We try to do as much work as possible in the constructor.
	// With the aim of making the evaluate method as cheap as possible.
	public StatementPatternQueryEvaluationStep(StatementPattern statementPattern, QueryEvaluationContext context,
			TripleSource tripleSource) {
		super();
		this.statementPatternForMetrics = statementPattern;
		this.order = statementPattern.getStatementOrder();
		this.context = context;
		this.tripleSource = tripleSource;
		Set<IRI> graphs = null;
		// If the graph part is empty we do not need to check this
		// in the conversion etc.
		Dataset dataset = context.getDataset();
		if (dataset != null) {
			if (statementPattern.getScope() == Scope.DEFAULT_CONTEXTS) {
				graphs = dataset.getDefaultGraphs();
				emptyGraph = graphs.isEmpty() && !dataset.getNamedGraphs().isEmpty();
			} else {
				graphs = dataset.getNamedGraphs();
				emptyGraph = graphs.isEmpty() && !dataset.getDefaultGraphs().isEmpty();
			}
		} else {
			emptyGraph = false;
		}

		contextSup = extractContextsFromDatasets(statementPattern.getContextVar(), emptyGraph, graphs);

		// Normalize the variables.
		// This helps performance because in ?a ?a ?a with normalization
		// There is just one unbound test for ?a instead of three.
		// Same for the conversion into an actual binding.
		Var subjVar = statementPattern.getSubjectVar();
		Var predVar = statementPattern.getPredicateVar();
		Var objVar = statementPattern.getObjectVar();
		Var conVar = statementPattern.getContextVar();

		subjVar = replaceValueWithNewValue(subjVar, tripleSource.getValueFactory());
		predVar = replaceValueWithNewValue(predVar, tripleSource.getValueFactory());
		objVar = replaceValueWithNewValue(objVar, tripleSource.getValueFactory());
		conVar = replaceValueWithNewValue(conVar, tripleSource.getValueFactory());

		this.statementPattern = new StatementPattern(statementPattern.getScope(), subjVar, predVar, objVar, conVar);
		this.statementPattern.setVariableScopeChange(statementPattern.isVariableScopeChange());

		// First create the getters before removing duplicate vars since we need the getters when creating
		// JoinStatementWithBindingSetIterator. If there are duplicate vars, for instance ?v1 as both subject and
		// context then we still need to bind the value from ?v1 in the subject and context arguments of
		// getStatement(...).
		getContextVar = makeGetVarValue(conVar, context);
		getSubjectVar = makeGetVarValue(subjVar, context);
		getPredicateVar = makeGetVarValue(predVar, context);
		getObjectVar = makeGetVarValue(objVar, context);

		// then remove duplicate vars
		if (subjVar != null) {
			if (predVar != null && predVar.getName().equals(subjVar.getName())) {
				predVar = null;
			}
			if (objVar != null && objVar.getName().equals(subjVar.getName())) {
				objVar = null;
			}
			if (conVar != null && conVar.getName().equals(subjVar.getName())) {
				conVar = null;
			}
		}

		if (predVar != null) {
			if (objVar != null && objVar.getName().equals(predVar.getName())) {
				objVar = null;
			}
			if (conVar != null && conVar.getName().equals(predVar.getName())) {
				conVar = null;
			}
		}

		if (objVar != null) {
			if (conVar != null && conVar.getName().equals(objVar.getName())) {
				conVar = null;
			}
		}

		normalizedSubjectVar = subjVar;
		normalizedPredicateVar = predVar;
		normalizedObjectVar = objVar;
		normalizedContextVar = conVar;

		unboundTest = getUnboundTest(context, normalizedSubjectVar, normalizedPredicateVar, normalizedObjectVar,
				normalizedContextVar);

	}

	private Var replaceValueWithNewValue(Var var, ValueFactory valueFactory) {
		if (var == null) {
			return null;
		} else if (!var.hasValue()) {
			return var.clone();
		} else {
			Var ret = getVarWithNewValue(var, valueFactory);
			ret.setVariableScopeChange(var.isVariableScopeChange());
			return ret;
		}
	}

	private static Var getVarWithNewValue(Var var, ValueFactory valueFactory) {
		boolean constant = var.isConstant();
		boolean anonymous = var.isAnonymous();

		Value value = var.getValue();
		if (value.isIRI()) {
			return Var.of(var.getName(), valueFactory.createIRI(value.stringValue()), anonymous, constant);
		} else if (value.isBNode()) {
			return Var.of(var.getName(), valueFactory.createBNode(value.stringValue()), anonymous, constant);
		} else if (value.isLiteral()) {
			// preserve label + (language | datatype)
			Literal lit = (Literal) value;

			// If the literal has a language tag, recreate it with the same language
			if (lit.getLanguage().isPresent()) {
				return Var.of(var.getName(), valueFactory.createLiteral(lit.getLabel(), lit.getLanguage().get()),
						anonymous, constant);
			}

			CoreDatatype coreDatatype = lit.getCoreDatatype();
			if (coreDatatype != CoreDatatype.NONE) {
				// If the literal has a core datatype, recreate it with the same core datatype
				return Var.of(var.getName(), valueFactory.createLiteral(lit.getLabel(), coreDatatype), anonymous,
						constant);
			}

			// Otherwise, preserve the datatype (falls back to xsd:string if none)
			IRI dt = lit.getDatatype();
			if (dt != null) {
				return Var.of(var.getName(), valueFactory.createLiteral(lit.getLabel(), dt), anonymous, constant);
			} else {
				return Var.of(var.getName(), valueFactory.createLiteral(lit.getLabel()), anonymous, constant);
			}
		}
		return var;
	}

	// test if the variable must remain unbound for this solution see
	// https://www.w3.org/TR/sparql11-query/#assignment
	private static Predicate<BindingSet> getUnboundTest(QueryEvaluationContext context, Var s, Var p,
			Var o, Var c) {

		if (s != null && !s.isConstant()) {
			if (p != null && !p.isConstant()) {
				if (o != null && !o.isConstant()) {
					if (c != null && !c.isConstant()) {
						return UnboundTest.spoc(context, s, p, o, c);
					} else {
						return UnboundTest.spo(context, s, p, o);
					}
				} else if (c != null && !c.isConstant()) {
					return UnboundTest.spc(context, s, p, c);
				} else {
					return UnboundTest.sp(context, s, p);
				}
			} else if (o != null && !o.isConstant()) {
				if (c != null && !c.isConstant()) {
					return UnboundTest.soc(context, s, o, c);
				} else {
					return UnboundTest.so(context, s, o);
				}
			} else if (c != null && !c.isConstant()) {
				return UnboundTest.sc(context, s, c);
			} else {
				return UnboundTest.s(context, s);
			}
		} else if (p != null && !p.isConstant()) {
			if (o != null && !o.isConstant()) {
				if (c != null && !c.isConstant()) {
					return UnboundTest.poc(context, p, o, c);
				} else {
					return UnboundTest.po(context, p, o);
				}
			} else if (c != null && !c.isConstant()) {
				return UnboundTest.pc(context, p, c);
			} else {
				return UnboundTest.p(context, p);
			}
		} else if (o != null && !o.isConstant()) {
			if (c != null && !c.isConstant()) {
				return UnboundTest.oc(context, o, c);
			} else {
				return UnboundTest.o(context, o);
			}
		} else if (c != null && !c.isConstant()) {
			return UnboundTest.c(context, c);
		}

		return b -> false;

	}

	private static Function<BindingSet, Value> makeGetVarValue(Var var, QueryEvaluationContext context) {
		if (var == null) {
			return null;
		} else if (var.hasValue()) {
			Value value = var.getValue();
			return b -> value;
		} else {
			return context.getValue(var.getName());
		}
	}

	@Override
	public CloseableIteration<BindingSet> evaluate(BindingSet bindings) {
		if (emptyGraph) {
			return QueryEvaluationStep.EMPTY_ITERATION;
		} else if (bindings.isEmpty()) {
			ConvertStatementToBindingSetIterator iteration = getIteration();
			if (iteration == null) {
				return QueryEvaluationStep.EMPTY_ITERATION;
			}
			return iteration;

		} else if (unboundTest.test(bindings)) {
			// the variable must remain unbound for this solution see
			// https://www.w3.org/TR/sparql11-query/#assignment
			return QueryEvaluationStep.EMPTY_ITERATION;
		} else {
			JoinStatementWithBindingSetIterator iteration = getIteration(bindings);
			if (iteration == null) {
				return QueryEvaluationStep.EMPTY_ITERATION;
			}
			return iteration;
		}
	}

	private JoinStatementWithBindingSetIterator getIteration(BindingSet bindings) {
		final Value contextValue = getContextVar != null ? getContextVar.apply(bindings) : null;

		Resource[] contexts = contextSup.apply(contextValue);
		if (contexts == null) {
			return null;
		}

		// Check that the subject is a Resource and the predicate can be an IRI
		// if not we can't return any value.

		Value subject = getSubjectVar != null ? getSubjectVar.apply(bindings) : null;
		if (subject != null && !subject.isResource()) {
			return null;
		}

		Value predicate = getPredicateVar != null ? getPredicateVar.apply(bindings) : null;
		if (predicate != null && !predicate.isIRI()) {
			return null;
		}

		Value object = getObjectVar != null ? getObjectVar.apply(bindings) : null;

		CloseableIteration<? extends Statement> iteration = null;
		try {
			incrementIndexLookupCount();
			if (order != null) {
				iteration = tripleSource.getStatements(order, (Resource) subject, (IRI) predicate, object, contexts);

			} else {
				iteration = tripleSource.getStatements((Resource) subject, (IRI) predicate, object, contexts);
			}

			if (iteration instanceof IndexReportingIterator) {
				String indexName = ((IndexReportingIterator) iteration).getIndexName();
				statementPattern.setIndexName(indexName);
				statementPatternForMetrics.setIndexName(indexName);
			}

			if (iteration instanceof EmptyIteration) {
				return null;
			}

			iteration = handleFilter(contexts, (Resource) subject, (IRI) predicate, object, iteration);

			// Return an iterator that converts the statements to var bindings
			return new JoinStatementWithBindingSetIterator(iteration, getConverter(), bindings, context);
		} catch (Throwable t) {
			if (iteration != null) {
				iteration.close();
			}
			if (t instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			throw new QueryEvaluationException(t);
		}
	}

	private ConvertStatementToBindingSetIterator getIteration() {

		Var contextVar = statementPattern.getContextVar();
		Resource[] contexts = contextSup.apply(contextVar != null ? contextVar.getValue() : null);

		if (contexts == null) {
			return null;
		}

		Value subject = statementPattern.getSubjectVar().getValue();

		if (subject != null && !subject.isResource()) {
			return null;
		}

		Value predicate = statementPattern.getPredicateVar().getValue();

		if (predicate != null && !predicate.isIRI()) {
			return null;
		}

		Value object = statementPattern.getObjectVar().getValue();

		CloseableIteration<? extends Statement> iteration = null;
		try {
			incrementIndexLookupCount();
			if (order != null) {
				iteration = tripleSource.getStatements(order, (Resource) subject, (IRI) predicate, object, contexts);
			} else {
				iteration = tripleSource.getStatements((Resource) subject, (IRI) predicate, object, contexts);
			}
			if (iteration instanceof IndexReportingIterator) {
				String indexName = ((IndexReportingIterator) iteration).getIndexName();
				statementPattern.setIndexName(indexName);
				statementPatternForMetrics.setIndexName(indexName);
			}

			if (iteration instanceof EmptyIteration) {
				return null;
			}
			iteration = handleFilter(contexts, (Resource) subject, (IRI) predicate, object, iteration);

			// Return an iterator that converts the statements to var bindings
			return new ConvertStatementToBindingSetIterator(iteration, getConvertStatementConverter(), context);
		} catch (Throwable t) {
			if (iteration != null) {
				iteration.close();
			}
			if (t instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			throw new QueryEvaluationException(t);
		}
	}

	private BiConsumer<MutableBindingSet, Statement> getConverter() {
		BiConsumer<MutableBindingSet, Statement> localConverter = converter;
		if (localConverter == null) {
			synchronized (this) {
				localConverter = converter;
				if (localConverter == null) {
					localConverter = makeConverter(context, normalizedSubjectVar, normalizedPredicateVar,
							normalizedObjectVar, normalizedContextVar);
					converter = localConverter;
				}
			}
		}
		return localConverter;
	}

	private BiConsumer<MutableBindingSet, Statement> getConvertStatementConverter() {
		BiConsumer<MutableBindingSet, Statement> localConverter = convertStatementConverter;
		if (localConverter == null) {
			synchronized (this) {
				localConverter = convertStatementConverter;
				if (localConverter == null) {
					localConverter = makeConvertStatementConverter(context, normalizedSubjectVar,
							normalizedPredicateVar, normalizedObjectVar, normalizedContextVar);
					convertStatementConverter = localConverter;
				}
			}
		}
		return localConverter;
	}

	private CloseableIteration<? extends Statement> handleFilter(Resource[] contexts,
			Resource subject, IRI predicate, Value object,
			CloseableIteration<? extends Statement> iteration) {

		Predicate<Statement> filter = filterContextOrEqualVariables(statementPattern, subject, predicate, object,
				contexts);

		if (filter != null) {
			// Only if there is filter code to execute do we make this filter iteration.
			return new MetricsReportingFilterIteration(iteration, filter);
		} else {
			return iteration;
		}
	}

	/**
	 * Generate a predicate that tests for Named contexts are matched by retrieving all statements from the store and
	 * filtering out the statements that do not have a context. Or the same variable might have been used multiple times
	 * in this StatementPattern, verify value equality in those cases.
	 */
	protected static Predicate<Statement> filterContextOrEqualVariables(StatementPattern statementPattern,
			Value subjValue, Value predValue, Value objValue, Resource[] contexts) {
		Predicate<Statement> filter = null;
		if (contexts.length == 0 && statementPattern.getScope() == Scope.NAMED_CONTEXTS) {
			filter = st -> st.getContext() != null;
		}
		return filterSameVariable(statementPattern, subjValue, predValue, objValue, filter);
	}

	/**
	 * Build one predicate that filters the statements for ?s ?p ?s cases. But only generates code that is actually
	 * needed else returns null.
	 */
	private static Predicate<Statement> filterSameVariable(StatementPattern statementPattern, Value subjValue,
			Value predValue, Value objValue, Predicate<Statement> filter) {

		Var subjVar = statementPattern.getSubjectVar();
		Var predVar = statementPattern.getPredicateVar();
		Var objVar = statementPattern.getObjectVar();
		Var conVar = statementPattern.getContextVar();

		if (subjVar != null && subjValue == null) {
			boolean subEqPredVar = subjVar.equals(predVar);
			boolean subEqObjVar = subjVar.equals(objVar);
			boolean subEqConVar = subjVar.equals(conVar);
			if (subEqPredVar || subEqObjVar || subEqConVar) {
				filter = andThen(filter, subjectVariableHasEquals(subEqPredVar, subEqObjVar, subEqConVar));
			}
		}

		if (predVar != null && predValue == null) {
			boolean predEqObjVar = predVar.equals(objVar);
			boolean predEqConVar = predVar.equals(conVar);
			if (predEqObjVar || predEqConVar) {
				filter = andThen(filter, predicateVariableHasEquals(predEqObjVar, predEqConVar));
			}
		}

		if (objVar != null && objValue == null) {
			boolean objEqConVar = objVar.equals(conVar);
			if (objEqConVar) {
				filter = andThen(filter, st -> {
					Value obj = st.getObject();
					Resource context = st.getContext();
					return obj.equals(context);
				});
			}
		}

		return filter;
	}

	private static Predicate<Statement> predicateVariableHasEquals(boolean predEqObjVar, boolean predEqConVar) {
		Predicate<Statement> eq = null;
		if (predEqObjVar) {
			eq = st -> st.getPredicate().equals(st.getObject());
		}
		if (predEqConVar) {
			eq = andThen(eq, st -> st.getPredicate().equals(st.getContext()));
		}
		return eq;
	}

	private static Predicate<Statement> subjectVariableHasEquals(boolean subEqPredVar, boolean subEqObjVar,
			boolean subEqConVar) {
		Predicate<Statement> eq = null;
		if (subEqPredVar) {
			eq = st -> st.getSubject().equals(st.getPredicate());
		}
		if (subEqObjVar) {
			eq = andThen(eq, st -> st.getSubject().equals(st.getObject()));
		}
		if (subEqConVar) {
			eq = andThen(eq, st -> st.getSubject().equals(st.getContext()));
		}
		return eq;
	}

	/**
	 * @return the contexts that are valid for this statement pattern or null
	 */
	protected static Function<Value, Resource[]> extractContextsFromDatasets(Var contextVar, boolean emptyGraph,
			Set<IRI> graphs) {

		if (emptyGraph) {
			return RETURN_NULL_VALUE_RESOURCE_ARRAY;
		}

		if (graphs == null || graphs.isEmpty()) {
			// store default behaviour
			return StatementPatternQueryEvaluationStep::contextsGivenContextVal;
		} else {
			Resource[] filled = fillContextsFromDatasSetGraphs(graphs);
			// if contextVar is null contextValue must always be null;
			if (contextVar == null) {
				return contextValue -> filled;
			} else {
				return contextValue -> {
					if (contextValue != null) {
						if (contextValue.isIRI() && graphs.contains((IRI) contextValue)) {
							return new Resource[] { (Resource) contextValue };
						} else {
							// Statement pattern specifies a context that is not part of
							// the dataset
							return null;
						}
					} else {
						return filled;
					}
				};
			}
		}
	}

	private static Resource[] contextsGivenContextVal(Value contextValue) {
		if (contextValue != null) {
			if (RDF4J.NIL.equals(contextValue) || SESAME.NIL.equals(contextValue)) {
				return DEFAULT_CONTEXT;
			} else {
				return new Resource[] { (Resource) contextValue };
			}
		}
		/*
		 * TODO activate this to have an exclusive (rather than inclusive) interpretation of the default graph in SPARQL
		 * querying. else if (statementPattern.getScope() == Scope.DEFAULT_CONTEXTS ) { contexts = new Resource[] {
		 * (Resource)null }; }
		 */
		else {
			return ALL_CONTEXT;
		}
	}

	private static Resource[] fillContextsFromDatasSetGraphs(Set<IRI> graphs) {
		Resource[] contexts = new Resource[graphs.size()];
		int i = 0;
		for (IRI graph : graphs) {
			IRI context = null;
			if (!(RDF4J.NIL.equals(graph) || SESAME.NIL.equals(graph))) {
				context = graph;
			}
			contexts[i++] = context;
		}
		return contexts;
	}

	private void incrementIndexLookupCount() {
		long next = Math.max(0L,
				statementPatternForMetrics.getLongMetricActual(TelemetryMetricNames.INDEX_LOOKUP_COUNT_ACTUAL)) + 1L;
		statementPatternForMetrics.setLongMetricActual(TelemetryMetricNames.INDEX_LOOKUP_COUNT_ACTUAL, next);
		statementPattern.setLongMetricActual(TelemetryMetricNames.INDEX_LOOKUP_COUNT_ACTUAL, next);
	}

	/**
	 * Converts statements into the required bindingsets. A lot of work is done in the constructor and then uses
	 * invokedynamic code with lambdas for the actual conversion.
	 * <p>
	 * This allows avoiding of significant work during the iteration. Which pays of if the iteration is long, otherwise
	 * it of course is an unneeded expense.
	 */
	private static final class ConvertStatementToBindingSetIterator
			implements CloseableIteration<BindingSet>, IndexReportingIterator {

		private final BiConsumer<MutableBindingSet, Statement> converter;
		private final QueryEvaluationContext context;
		private final CloseableIteration<? extends Statement> iteration;
		private boolean closed = false;

		private ConvertStatementToBindingSetIterator(
				CloseableIteration<? extends Statement> iteration,
				BiConsumer<MutableBindingSet, Statement> converter, QueryEvaluationContext context) {
			assert iteration != null;
			this.iteration = iteration;
			this.converter = converter;
			this.context = context;
		}

		private BindingSet convert(Statement st) {
			MutableBindingSet made = context.createBindingSet();
			converter.accept(made, st);
			return made;
		}

		@Override
		public boolean hasNext() throws QueryEvaluationException {
			return iteration.hasNext();
		}

		@Override
		public BindingSet next() throws QueryEvaluationException {
			return convert(iteration.next());
		}

		@Override
		public void remove() throws QueryEvaluationException {
			iteration.remove();
		}

		@Override
		public void close() throws QueryEvaluationException {
			if (!closed) {
				closed = true;
				iteration.close();
			}
		}

		@Override
		public String getIndexName() {
			IndexReportingIterator metrics = indexReporter();
			return metrics == null ? "" : metrics.getIndexName();
		}

		@Override
		public long getSourceRowsScannedActual() {
			IndexReportingIterator metrics = indexReporter();
			return metrics == null ? -1 : metrics.getSourceRowsScannedActual();
		}

		@Override
		public long getSourceRowsMatchedActual() {
			IndexReportingIterator metrics = indexReporter();
			return metrics == null ? -1 : metrics.getSourceRowsMatchedActual();
		}

		@Override
		public long getSourceRowsFilteredActual() {
			IndexReportingIterator metrics = indexReporter();
			return metrics == null ? -1 : metrics.getSourceRowsFilteredActual();
		}

		private IndexReportingIterator indexReporter() {
			return iteration instanceof IndexReportingIterator ? (IndexReportingIterator) iteration : null;
		}
	}

	private static final class JoinStatementWithBindingSetIterator
			implements CloseableIteration<BindingSet>, IndexReportingIterator {

		private final BiConsumer<MutableBindingSet, Statement> converter;
		private final QueryEvaluationContext context;
		private final BindingSet bindings;
		private final CloseableIteration<? extends Statement> iteration;
		private boolean closed = false;

		private JoinStatementWithBindingSetIterator(
				CloseableIteration<? extends Statement> iteration,
				BiConsumer<MutableBindingSet, Statement> converter, BindingSet bindings,
				QueryEvaluationContext context) {
			assert iteration != null;
			this.iteration = iteration;
			assert !bindings.isEmpty();
			this.converter = converter;
			this.context = context;
			this.bindings = bindings;

		}

		private BindingSet convert(Statement st) {
			MutableBindingSet made = context.createBindingSet(bindings);
			converter.accept(made, st);
			return made;
		}

		@Override
		public boolean hasNext() throws QueryEvaluationException {
			return iteration.hasNext();
		}

		@Override
		public BindingSet next() throws QueryEvaluationException {
			return convert(iteration.next());
		}

		@Override
		public void remove() throws QueryEvaluationException {
			iteration.remove();
		}

		@Override
		public void close() throws QueryEvaluationException {
			if (!closed) {
				closed = true;
				iteration.close();
			}
		}

		@Override
		public String getIndexName() {
			IndexReportingIterator metrics = indexReporter();
			return metrics == null ? "" : metrics.getIndexName();
		}

		@Override
		public long getSourceRowsScannedActual() {
			IndexReportingIterator metrics = indexReporter();
			return metrics == null ? -1 : metrics.getSourceRowsScannedActual();
		}

		@Override
		public long getSourceRowsMatchedActual() {
			IndexReportingIterator metrics = indexReporter();
			return metrics == null ? -1 : metrics.getSourceRowsMatchedActual();
		}

		@Override
		public long getSourceRowsFilteredActual() {
			IndexReportingIterator metrics = indexReporter();
			return metrics == null ? -1 : metrics.getSourceRowsFilteredActual();
		}

		private IndexReportingIterator indexReporter() {
			return iteration instanceof IndexReportingIterator ? (IndexReportingIterator) iteration : null;
		}
	}

	private static final class MetricsReportingFilterIteration extends FilterIteration<Statement>
			implements IndexReportingIterator {

		private final CloseableIteration<? extends Statement> iteration;
		private final Predicate<Statement> filter;
		private long locallyMatchedRows;
		private long locallyFilteredRows;

		private MetricsReportingFilterIteration(CloseableIteration<? extends Statement> iteration,
				Predicate<Statement> filter) {
			super(iteration);
			this.iteration = iteration;
			this.filter = filter;
		}

		@Override
		protected boolean accept(Statement object) throws QueryEvaluationException {
			boolean accepted = filter.test(object);
			if (accepted) {
				locallyMatchedRows++;
			} else {
				locallyFilteredRows++;
			}
			return accepted;
		}

		@Override
		protected void handleClose() {
			// no-op
		}

		@Override
		public String getIndexName() {
			IndexReportingIterator metrics = indexReporter();
			return metrics == null ? "" : metrics.getIndexName();
		}

		@Override
		public long getSourceRowsScannedActual() {
			IndexReportingIterator metrics = indexReporter();
			if (metrics != null) {
				long sourceRowsScannedActual = metrics.getSourceRowsScannedActual();
				if (sourceRowsScannedActual >= 0) {
					return sourceRowsScannedActual;
				}
			}
			long locallySeenRows = locallyMatchedRows + locallyFilteredRows;
			return locallySeenRows > 0 ? locallySeenRows : -1;
		}

		@Override
		public long getSourceRowsMatchedActual() {
			IndexReportingIterator metrics = indexReporter();
			if (metrics != null) {
				long sourceRowsMatchedActual = metrics.getSourceRowsMatchedActual();
				if (sourceRowsMatchedActual >= 0) {
					return Math.max(0L, sourceRowsMatchedActual - locallyFilteredRows);
				}
			}
			long locallySeenRows = locallyMatchedRows + locallyFilteredRows;
			return locallySeenRows > 0 ? locallyMatchedRows : -1;
		}

		@Override
		public long getSourceRowsFilteredActual() {
			IndexReportingIterator metrics = indexReporter();
			if (metrics != null) {
				long sourceRowsFilteredActual = metrics.getSourceRowsFilteredActual();
				if (sourceRowsFilteredActual >= 0) {
					return sourceRowsFilteredActual + locallyFilteredRows;
				}
			}
			long locallySeenRows = locallyMatchedRows + locallyFilteredRows;
			return locallySeenRows > 0 ? locallyFilteredRows : -1;
		}

		private IndexReportingIterator indexReporter() {
			return iteration instanceof IndexReportingIterator ? (IndexReportingIterator) iteration : null;
		}
	}

	/**
	 * We need to test every binding with hasBinding etc. as these are not guaranteed to be equivalent between calls of
	 * evaluate(bs).
	 * <p>
	 * Each conversion kind is special cased in with a specific method.
	 *
	 * @return a converter from statement into MutableBindingSet
	 */
	private static BiConsumer<MutableBindingSet, Statement> makeConverter(QueryEvaluationContext context, Var s,
			Var p, Var o, Var c) {

		if (s != null && !s.isConstant()) {
			if (p != null && !p.isConstant()) {
				if (o != null && !o.isConstant()) {
					if (c != null && !c.isConstant()) {
						return StatementConvertor.spoc(context, s, p, o, c);
					} else {
						return StatementConvertor.spo(context, s, p, o);
					}
				} else if (c != null && !c.isConstant()) {
					return StatementConvertor.spc(context, s, p, c);
				} else {
					return StatementConvertor.sp(context, s, p);
				}
			} else if (o != null && !o.isConstant()) {
				if (c != null && !c.isConstant()) {
					return StatementConvertor.soc(context, s, o, c);
				} else {
					return StatementConvertor.so(context, s, o);
				}
			} else if (c != null && !c.isConstant()) {
				return StatementConvertor.sc(context, s, c);
			} else {
				return StatementConvertor.s(context, s);
			}
		} else if (p != null && !p.isConstant()) {
			if (o != null && !o.isConstant()) {
				if (c != null && !c.isConstant()) {
					return StatementConvertor.poc(context, p, o, c);
				} else {
					return StatementConvertor.po(context, p, o);
				}
			} else if (c != null && !c.isConstant()) {
				return StatementConvertor.pc(context, p, c);
			} else {
				return StatementConvertor.p(context, p);
			}
		} else if (o != null && !o.isConstant()) {
			if (c != null && !c.isConstant()) {
				return StatementConvertor.oc(context, o, c);
			} else {
				return StatementConvertor.o(context, o);
			}
		} else if (c != null && !c.isConstant()) {
			return StatementConvertor.c(context, c);
		}

		return (a, b) -> {
		};

	}

	private static BiConsumer<MutableBindingSet, Statement> makeConvertStatementConverter(
			QueryEvaluationContext context,
			Var s, Var p, Var o, Var c) {

		if (s != null && !s.isConstant()) {
			if (p != null && !p.isConstant()) {
				if (o != null && !o.isConstant()) {
					if (c != null && !c.isConstant()) {
						return StatementConvertorWithoutBindingChecks.spoc(context, s, p, o, c);
					} else {
						return StatementConvertorWithoutBindingChecks.spo(context, s, p, o);
					}
				} else if (c != null && !c.isConstant()) {
					return StatementConvertorWithoutBindingChecks.spc(context, s, p, c);
				} else {
					return StatementConvertorWithoutBindingChecks.sp(context, s, p);
				}
			} else if (o != null && !o.isConstant()) {
				if (c != null && !c.isConstant()) {
					return StatementConvertorWithoutBindingChecks.soc(context, s, o, c);
				} else {
					return StatementConvertorWithoutBindingChecks.so(context, s, o);
				}
			} else if (c != null && !c.isConstant()) {
				return StatementConvertorWithoutBindingChecks.sc(context, s, c);
			} else {
				return StatementConvertorWithoutBindingChecks.s(context, s);
			}
		} else if (p != null && !p.isConstant()) {
			if (o != null && !o.isConstant()) {
				if (c != null && !c.isConstant()) {
					return StatementConvertorWithoutBindingChecks.poc(context, p, o, c);
				} else {
					return StatementConvertorWithoutBindingChecks.po(context, p, o);
				}
			} else if (c != null && !c.isConstant()) {
				return StatementConvertorWithoutBindingChecks.pc(context, p, c);
			} else {
				return StatementConvertorWithoutBindingChecks.p(context, p);
			}
		} else if (o != null && !o.isConstant()) {
			if (c != null && !c.isConstant()) {
				return StatementConvertorWithoutBindingChecks.oc(context, o, c);
			} else {
				return StatementConvertorWithoutBindingChecks.o(context, o);
			}
		} else if (c != null && !c.isConstant()) {
			return StatementConvertorWithoutBindingChecks.c(context, c);
		}

		return (a, b) -> {
		};
	}

	private static Predicate<Statement> andThen(Predicate<Statement> pred, Predicate<Statement> and) {
		if (pred == null) {
			return and;
		}
		return pred.and(and);
	}

}
