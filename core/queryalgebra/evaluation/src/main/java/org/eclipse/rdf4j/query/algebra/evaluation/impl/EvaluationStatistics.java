/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.FN;
import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.BinaryTupleOperator;
import org.eclipse.rdf4j.query.algebra.BinaryValueOperator;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.Compare.CompareOp;
import org.eclipse.rdf4j.query.algebra.EmptySet;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.FunctionCall;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.NAryValueOperator;
import org.eclipse.rdf4j.query.algebra.Or;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.SameTerm;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.SubQueryValueOperator;
import org.eclipse.rdf4j.query.algebra.TripleRef;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.UnaryTupleOperator;
import org.eclipse.rdf4j.query.algebra.UnaryValueOperator;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.ZeroLengthPath;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.helpers.collectors.VarNameCollector;

/**
 * Supplies various query model statistics to the query engine/optimizer.
 *
 * @author Arjohn Kampman
 * @author James Leigh
 */
public class EvaluationStatistics {

	// static UUID as prefix together with a thread safe incrementing long ensures a unique identifier.
	private final static String uniqueIdPrefix = UUID.randomUUID().toString().replace("-", "");
	private final static AtomicLong uniqueIdSuffix = new AtomicLong();

	// Pre-built strings for lengths 0 through 9
	private static final String[] RANDOMIZE_LENGTH = new String[10];
	static {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i <= 9; i++) {
			RANDOMIZE_LENGTH[i] = sb.toString();
			sb.append(i);
		}
	}

	private CardinalityCalculator calculator;

	public double getCardinality(TupleExpr expr) {
		if (calculator == null) {
			calculator = createCardinalityCalculator();
			assert calculator != null;
		}

		if (expr instanceof AbstractQueryModelNode && ((AbstractQueryModelNode) expr).isCardinalitySet()) {
			return ((AbstractQueryModelNode) expr).getCardinality();
		}

		expr.visit(calculator);
		return calculator.getCardinality();
	}

	protected CardinalityCalculator createCardinalityCalculator() {
		return new CardinalityCalculator();
	}

	public boolean supportsJoinEstimation() {
		return false;
	}

	/*-----------------------------------*
	 * Inner class CardinalityCalculator *
	 *-----------------------------------*/

	protected static class CardinalityCalculator extends AbstractQueryModelVisitor<RuntimeException> {

		private static final double VAR_CARDINALITY = 10;
		private static final double UNBOUND_SERVICE_CARDINALITY = 100000;
		private static final double RANGE_FILTER_SELECTIVITY = 0.1;
		private static final double NOT_EQUALS_FILTER_SELECTIVITY = 0.9;
		private static final double CONTAINS_FILTER_SELECTIVITY = 0.05;

		protected double cardinality;

		public double getCardinality() {
			return cardinality;
		}

		@Override
		public void meet(EmptySet node) {
			// no binding sets
			cardinality = 0.0;
		}

		@Override
		public void meet(SingletonSet node) {
			// one empty binding set
			cardinality = 1.0;
		}

		@Override
		public void meet(BindingSetAssignment node) {
			cardinality = getCardinalityInternal(node);
		}

		@Override
		public void meet(ZeroLengthPath node) {
			Var subjVar = node.getSubjectVar();
			Var objVar = node.getObjectVar();
			if (subjVar != null && subjVar.hasValue() || objVar != null && objVar.hasValue()) {
				// subj = obj
				cardinality = 1.0;
			} else {
				// actual cardinality = count(union(subjs, objs))
				// but cost is equivalent to ?s ?p ?o ?c (impl scans all statements)
				// so due to the lower actual cardinality we value it in preference to a fully unbound statement
				// pattern.
				cardinality = getSubjectCardinality(subjVar) * getObjectCardinality(objVar)
						* getContextCardinality(node.getContextVar());
			}
		}

		@Override
		public void meet(ArbitraryLengthPath node) {
			long suffix = uniqueIdSuffix.getAndIncrement();
			final Var pathVar = Var.of(
					"_anon_path_" + uniqueIdPrefix + suffix
							+ RANDOMIZE_LENGTH[(int) (Math.abs(suffix % RANDOMIZE_LENGTH.length))],
					true);
			// cardinality of ALP is determined based on the cost of a
			// single ?s ?p ?o ?c pattern where ?p is unbound, compensating for the fact that
			// the length of the path is unknown but expected to be _at least_ twice that of a normal
			// statement pattern.
			cardinality = 2.0 * getCardinalityInternal(new StatementPattern(node.getSubjectVar().clone(), pathVar,
					node.getObjectVar().clone(), node.getContextVar() != null ? node.getContextVar().clone() : null));
		}

		@Override
		public void meet(Service node) {
			if (!node.getServiceRef().hasValue()) {
				// the IRI is not available, may be computed in the course of the
				// query
				// => use high cost to order the SERVICE node late in the query plan
				cardinality = UNBOUND_SERVICE_CARDINALITY;
			} else {
				ServiceNodeAnalyzer serviceAnalyzer = new ServiceNodeAnalyzer();
				node.visitChildren(serviceAnalyzer);
				int count = serviceAnalyzer.getStatementCount();

				// more than one free variable in a single triple pattern
				if (count == 1 && node.getServiceVars().size() > 1) {
					cardinality = 100 + node.getServiceVars().size(); // TODO (should
					// be higher
					// than other
					// simple
					// stmts)
				} else {
					// only very selective statements should be better than this
					// => evaluate service expressions first
					cardinality = 1 + node.getServiceVars().size() * 0.1;
				}
			}
		}

		@Override
		public void meet(StatementPattern sp) {
			cardinality = getCardinalityInternal(sp);
		}

		@Override
		public void meet(TripleRef tripleRef) {
			cardinality = getCardinalityInternal(tripleRef);
		}

		private double getCardinalityInternal(StatementPattern node) {
			if (!node.isCardinalitySet()) {
				node.setCardinality(getCardinality(node));
			}
			return node.getCardinality();
		}

		private double getCardinalityInternal(TripleRef node) {
			if (!node.isCardinalitySet()) {
				node.setCardinality(getCardinality(node));
			}
			return node.getCardinality();
		}

		private double getCardinalityInternal(BindingSetAssignment node) {
			if (!node.isCardinalitySet()) {
				node.setCardinality(getCardinality(node));
			}
			return node.getCardinality();
		}

		protected double getCardinality(StatementPattern sp) {
			return getSubjectCardinality(sp) * getPredicateCardinality(sp) * getObjectCardinality(sp)
					* getContextCardinality(sp);
		}

		protected double getCardinality(BindingSetAssignment bindingSetAssignment) {
			// actual cardinality is node.getBindingSets().size() binding sets
			// but cost is cheap as we don't need to query the triple store
			// so effective cardinality is 1 or a very slowly increasing function of node.getBindingSets().size().
			return 1.0;
		}

		protected double getCardinality(TripleRef tripleRef) {
			return getSubjectCardinality(tripleRef.getSubjectVar())
					* getPredicateCardinality(tripleRef.getPredicateVar())
					* getObjectCardinality(tripleRef.getObjectVar());
		}

		/**
		 * Override this if you are able to determine the cardinality based not only on the subjectVar itself but also
		 * the other vars (e.g. the predicate value might determine a subject subset).
		 */
		protected double getSubjectCardinality(StatementPattern sp) {
			return getSubjectCardinality(sp.getSubjectVar());
		}

		protected double getSubjectCardinality(Var var) {
			return getCardinality(VAR_CARDINALITY, var);
		}

		/**
		 * Override this if you are able to determine the cardinality based not only on the predicateVar itself but also
		 * the other vars (e.g. the subject value might determine a predicate subset).
		 */
		protected double getPredicateCardinality(StatementPattern sp) {
			return getPredicateCardinality(sp.getPredicateVar());
		}

		protected double getPredicateCardinality(Var var) {
			return getCardinality(VAR_CARDINALITY, var);
		}

		/**
		 * Override this if you are able to determine the cardinality based not only on the objectVar itself but also
		 * the other vars (e.g. the predicate value might determine an object subset).
		 */
		protected double getObjectCardinality(StatementPattern sp) {
			return getObjectCardinality(sp.getObjectVar());
		}

		protected double getObjectCardinality(Var var) {
			return getCardinality(VAR_CARDINALITY, var);
		}

		/**
		 * Override this if you are able to determine the cardinality based not only on the contextVar itself but also
		 * the other vars (e.g. the subject value might determine a context subset).
		 */
		protected double getContextCardinality(StatementPattern sp) {
			return getContextCardinality(sp.getContextVar());
		}

		protected double getContextCardinality(Var var) {
			return getCardinality(VAR_CARDINALITY, var);
		}

		protected double getCardinality(double varCardinality, Var var) {
			return var == null || var.hasValue() ? 1.0 : varCardinality;
		}

		protected double getCardinality(double varCardinality, Collection<Var> vars) {
			int constantVarCount = countConstantVars(vars);
			double unboundVarFactor = vars.size() - constantVarCount;
			return Math.pow(varCardinality, unboundVarFactor);
		}

		protected int countConstantVars(Iterable<Var> vars) {
			int constantVarCount = 0;

			for (Var var : vars) {
				if (var.hasValue()) {
					constantVarCount++;
				}
			}

			return constantVarCount;
		}

		@Override
		public void meet(Join node) {
			node.getLeftArg().visit(this);
			double leftArgCost = this.cardinality;

			node.getRightArg().visit(this);
			cardinality *= leftArgCost;
		}

		@Override
		public void meet(LeftJoin node) {
			node.getLeftArg().visit(this);
			double leftArgCost = this.cardinality;

			node.getRightArg().visit(this);
			cardinality *= leftArgCost;
		}

		@Override
		public void meet(Filter node) {
			TupleExpr arg = node.getArg();
			if (arg == null) {
				cardinality = 0.0;
				return;
			}
			arg.visit(this);
			double base = cardinality;
			Map<String, Value> bindings = extractLiteralBindings(node.getCondition(), arg);
			double estimate = base;
			if (!bindings.isEmpty()) {
				TupleExpr clone = arg.clone();
				for (Map.Entry<String, Value> entry : bindings.entrySet()) {
					clone.visit(new VarValueBinder(entry.getKey(), entry.getValue()));
				}
				CardinalityCalculator calculator = newCalculator();
				clone.visit(calculator);
				estimate = calculator.getCardinality();
				double boundPatternEstimate = minBoundPatternCardinality(clone, bindings.keySet());
				if (Double.isFinite(boundPatternEstimate)) {
					estimate = Math.min(estimate, boundPatternEstimate);
				}
			}
			double selectivity = estimateFilterSelectivity(node.getCondition(), arg);
			double filteredEstimate = Math.min(base, estimate) * selectivity;
			double filterPatternEstimate = estimateFilterPatternCardinality(node.getCondition(), arg, selectivity);
			if (Double.isFinite(filterPatternEstimate)) {
				filteredEstimate = Math.min(filteredEstimate, filterPatternEstimate);
			}
			cardinality = filteredEstimate;
		}

		protected CardinalityCalculator newCalculator() {
			return new CardinalityCalculator();
		}

		private double estimateFilterSelectivity(ValueExpr condition, TupleExpr arg) {
			if (condition == null || arg == null) {
				return 1.0;
			}
			Collection<String> bindingNames = arg.getBindingNames();
			if (bindingNames.isEmpty()) {
				return 1.0;
			}
			Map<String, String> aliasMap = collectAliasMap(arg);
			Set<String> bindableNames = new HashSet<>(bindingNames);
			bindableNames.addAll(aliasMap.keySet());
			return estimateFilterSelectivity(condition, bindableNames);
		}

		private double estimateFilterSelectivity(ValueExpr expr, Set<String> bindableNames) {
			if (expr instanceof And) {
				And and = (And) expr;
				return estimateFilterSelectivity(and.getLeftArg(), bindableNames)
						* estimateFilterSelectivity(and.getRightArg(), bindableNames);
			}
			if (expr instanceof Or) {
				Or or = (Or) expr;
				double left = estimateFilterSelectivity(or.getLeftArg(), bindableNames);
				double right = estimateFilterSelectivity(or.getRightArg(), bindableNames);
				return clampSelectivity(1.0 - (1.0 - left) * (1.0 - right));
			}
			if (expr instanceof Compare) {
				Compare compare = (Compare) expr;
				CompareOp op = compare.getOperator();
				if (op == CompareOp.GT || op == CompareOp.GE || op == CompareOp.LT || op == CompareOp.LE) {
					return hasBoundLiteralOperand(compare, bindableNames) ? RANGE_FILTER_SELECTIVITY : 1.0;
				}
				if (op == CompareOp.NE) {
					return hasBoundLiteralOperand(compare, bindableNames) ? NOT_EQUALS_FILTER_SELECTIVITY : 1.0;
				}
			}
			if (expr instanceof FunctionCall) {
				return estimateFunctionCallSelectivity((FunctionCall) expr, bindableNames);
			}
			return 1.0;
		}

		private double estimateFunctionCallSelectivity(FunctionCall call, Set<String> bindableNames) {
			String uri = call.getURI();
			if (FN.CONTAINS.stringValue().equals(uri)
					|| FN.STARTS_WITH.stringValue().equals(uri)
					|| FN.ENDS_WITH.stringValue().equals(uri)) {
				return hasBoundLiteralArgument(call, bindableNames) ? CONTAINS_FILTER_SELECTIVITY : 1.0;
			}
			return 1.0;
		}

		private boolean hasBoundLiteralArgument(FunctionCall call, Set<String> bindableNames) {
			for (ValueExpr expr : call.getArgs()) {
				if (isBindableVar(expr, bindableNames)) {
					for (ValueExpr other : call.getArgs()) {
						if (other != expr && asValue(other) != null) {
							return true;
						}
					}
				}
			}
			return false;
		}

		private boolean hasBoundLiteralOperand(Compare compare, Set<String> bindableNames) {
			return isBindableVar(compare.getLeftArg(), bindableNames) && asValue(compare.getRightArg()) != null
					|| isBindableVar(compare.getRightArg(), bindableNames) && asValue(compare.getLeftArg()) != null;
		}

		private double estimateFilterPatternCardinality(ValueExpr condition, TupleExpr arg, double selectivity) {
			if (condition == null || arg == null) {
				return Double.POSITIVE_INFINITY;
			}
			Set<String> filterVars = collectFilterBindingNames(condition, arg);
			if (filterVars.isEmpty()) {
				return Double.POSITIVE_INFINITY;
			}
			double boundPatternEstimate = minBoundPatternCardinality(arg, filterVars);
			if (!Double.isFinite(boundPatternEstimate)) {
				return Double.POSITIVE_INFINITY;
			}
			return boundPatternEstimate * selectivity;
		}

		private Set<String> collectFilterBindingNames(ValueExpr condition, TupleExpr arg) {
			Set<String> names = new HashSet<>(VarNameCollector.process(condition));
			if (names.isEmpty()) {
				return Set.of();
			}
			Collection<String> bindingNames = arg.getBindingNames();
			if (bindingNames.isEmpty()) {
				return Set.of();
			}
			Map<String, String> aliasMap = collectAliasMap(arg);
			if (!aliasMap.isEmpty()) {
				Set<String> resolved = new HashSet<>();
				for (String name : names) {
					String mapped = aliasMap.getOrDefault(name, name);
					resolved.add(mapped);
				}
				names = resolved;
			}
			names.retainAll(bindingNames);
			return names;
		}

		private double clampSelectivity(double value) {
			if (value < 0.0) {
				return 0.0;
			}
			if (value > 1.0) {
				return 1.0;
			}
			return value;
		}

		private boolean isBindableVar(ValueExpr expr, Set<String> bindableNames) {
			Var var = asUnboundVar(expr);
			return var != null && bindableNames.contains(var.getName());
		}

		private double minBoundPatternCardinality(TupleExpr expr, Set<String> boundNames) {
			if (boundNames.isEmpty()) {
				return Double.POSITIVE_INFINITY;
			}
			BoundPatternCollector collector = new BoundPatternCollector(boundNames);
			expr.visit(collector);
			return collector.getMin();
		}

		private boolean usesBoundVar(Var var, Set<String> boundNames) {
			return var != null && boundNames.contains(var.getName());
		}

		private boolean usesBoundVar(StatementPattern node, Set<String> boundNames) {
			return usesBoundVar(node.getSubjectVar(), boundNames)
					|| usesBoundVar(node.getPredicateVar(), boundNames)
					|| usesBoundVar(node.getObjectVar(), boundNames)
					|| usesBoundVar(node.getContextVar(), boundNames);
		}

		private boolean usesBoundVar(TripleRef node, Set<String> boundNames) {
			return usesBoundVar(node.getSubjectVar(), boundNames)
					|| usesBoundVar(node.getPredicateVar(), boundNames)
					|| usesBoundVar(node.getObjectVar(), boundNames);
		}

		private final class BoundPatternCollector extends AbstractSimpleQueryModelVisitor<RuntimeException> {
			private final Set<String> boundNames;
			private double min = Double.POSITIVE_INFINITY;

			private BoundPatternCollector(Set<String> boundNames) {
				super(true);
				this.boundNames = boundNames;
			}

			@Override
			public void meet(StatementPattern node) {
				if (usesBoundVar(node, boundNames)) {
					CardinalityCalculator calculator = newCalculator();
					node.visit(calculator);
					min = Math.min(min, calculator.getCardinality());
				}
			}

			@Override
			public void meet(TripleRef node) {
				if (usesBoundVar(node, boundNames)) {
					CardinalityCalculator calculator = newCalculator();
					node.visit(calculator);
					min = Math.min(min, calculator.getCardinality());
				}
			}

			private double getMin() {
				return min;
			}
		}

		@Override
		protected void meetBinaryTupleOperator(BinaryTupleOperator node) {
			node.getLeftArg().visit(this);
			double leftArgCost = this.cardinality;

			node.getRightArg().visit(this);
			cardinality += leftArgCost;
		}

		@Override
		protected void meetUnaryTupleOperator(UnaryTupleOperator node) {
			node.getArg().visit(this);
		}

		@Override
		public void meet(QueryRoot node) {
			node.getArg().visit(this);
		}

		@Override
		protected void meetNode(QueryModelNode node) {
			throw new IllegalArgumentException("Unhandled node type: " + node.getClass());
		}
	}

	private static Map<String, Value> extractLiteralBindings(ValueExpr condition, TupleExpr arg) {
		if (condition == null || arg == null) {
			return Map.of();
		}
		Collection<String> bindingNames = arg.getBindingNames();
		if (bindingNames.isEmpty()) {
			return Map.of();
		}
		Map<String, String> aliasMap = collectAliasMap(arg);
		Set<String> bindableNames = Set.copyOf(bindingNames);
		if (!aliasMap.isEmpty()) {
			Map<String, String> resolvedAliases = new HashMap<>();
			for (Map.Entry<String, String> entry : aliasMap.entrySet()) {
				if (bindingNames.contains(entry.getValue())) {
					resolvedAliases.put(entry.getKey(), entry.getValue());
				}
			}
			aliasMap = resolvedAliases;
			if (!aliasMap.isEmpty()) {
				bindableNames = new HashSet<>(bindingNames);
				bindableNames.addAll(aliasMap.keySet());
			}
		}
		Map<String, Value> bindings = new HashMap<>();
		if (!collectLiteralBindings(condition, bindings, bindableNames)) {
			return Map.of();
		}
		if (bindings.isEmpty()) {
			return Map.of();
		}
		if (aliasMap.isEmpty()) {
			return bindings;
		}
		for (Map.Entry<String, String> entry : aliasMap.entrySet()) {
			String alias = entry.getKey();
			Value aliasValue = bindings.get(alias);
			if (aliasValue == null) {
				continue;
			}
			String source = entry.getValue();
			Value existing = bindings.get(source);
			if (existing != null && !existing.equals(aliasValue)) {
				return Map.of();
			}
			bindings.put(source, aliasValue);
		}
		return bindings;
	}

	private static boolean collectLiteralBindings(ValueExpr expr, Map<String, Value> bindings,
			Collection<String> assuredBindings) {
		if (expr instanceof And) {
			And and = (And) expr;
			return collectLiteralBindings(and.getLeftArg(), bindings, assuredBindings)
					&& collectLiteralBindings(and.getRightArg(), bindings, assuredBindings);
		}
		if (expr instanceof Compare) {
			Compare compare = (Compare) expr;
			if (compare.getOperator() != CompareOp.EQ) {
				return false;
			}
			return extractBinding(compare.getLeftArg(), compare.getRightArg(), bindings, assuredBindings)
					|| extractBinding(compare.getRightArg(), compare.getLeftArg(), bindings, assuredBindings);
		}
		if (expr instanceof SameTerm) {
			SameTerm sameTerm = (SameTerm) expr;
			return extractBinding(sameTerm.getLeftArg(), sameTerm.getRightArg(), bindings, assuredBindings)
					|| extractBinding(sameTerm.getRightArg(), sameTerm.getLeftArg(), bindings, assuredBindings);
		}
		return false;
	}

	private static boolean extractBinding(ValueExpr varExpr, ValueExpr valueExpr, Map<String, Value> bindings,
			Collection<String> assuredBindings) {
		Var var = asUnboundVar(varExpr);
		Value value = asValue(valueExpr);
		if (var == null || value == null) {
			return false;
		}
		if (!assuredBindings.contains(var.getName())) {
			return false;
		}
		Value existing = bindings.get(var.getName());
		if (existing != null && !existing.equals(value)) {
			return false;
		}
		bindings.put(var.getName(), value);
		return true;
	}

	private static Var asUnboundVar(ValueExpr expr) {
		if (expr instanceof Var) {
			Var var = (Var) expr;
			return var.hasValue() ? null : var;
		}
		return null;
	}

	private static Value asValue(ValueExpr expr) {
		if (expr instanceof ValueConstant) {
			return ((ValueConstant) expr).getValue();
		}
		if (expr instanceof Var) {
			Var var = (Var) expr;
			return var.hasValue() ? var.getValue() : null;
		}
		return null;
	}

	private static Map<String, String> collectAliasMap(TupleExpr arg) {
		Map<String, String> aliasMap = new HashMap<>();
		arg.visit(new AliasCollector(aliasMap));
		return aliasMap;
	}

	private static class AliasCollector extends StopAtScopeChange {
		private final Map<String, String> aliasMap;

		AliasCollector(Map<String, String> aliasMap) {
			super(true);
			this.aliasMap = aliasMap;
		}

		@Override
		public void meet(Extension node) {
			for (ExtensionElem elem : node.getElements()) {
				if (elem.getExpr() instanceof Var) {
					String alias = elem.getName();
					String source = ((Var) elem.getExpr()).getName();
					String existing = aliasMap.get(alias);
					if (existing == null) {
						aliasMap.put(alias, source);
					} else if (!existing.equals(source)) {
						aliasMap.remove(alias);
					}
				}
			}
			super.meet(node);
		}
	}

	private static class StopAtScopeChange extends AbstractSimpleQueryModelVisitor<RuntimeException> {

		StopAtScopeChange(boolean meetStatementPatternChildren) {
			super(meetStatementPatternChildren);
		}

		@Override
		public void meetUnaryTupleOperator(UnaryTupleOperator node) {
			if (!node.isVariableScopeChange()) {
				super.meetUnaryTupleOperator(node);
			}
		}

		@Override
		public void meetBinaryTupleOperator(BinaryTupleOperator node) {
			if (!node.isVariableScopeChange()) {
				super.meetBinaryTupleOperator(node);
			}
		}

		@Override
		protected void meetBinaryValueOperator(BinaryValueOperator node) throws RuntimeException {
			if (!node.isVariableScopeChange()) {
				super.meetBinaryValueOperator(node);
			}
		}

		@Override
		protected void meetNAryValueOperator(NAryValueOperator node) throws RuntimeException {
			if (!node.isVariableScopeChange()) {
				super.meetNAryValueOperator(node);
			}
		}

		@Override
		protected void meetSubQueryValueOperator(SubQueryValueOperator node) throws RuntimeException {
			if (!node.isVariableScopeChange()) {
				super.meetSubQueryValueOperator(node);
			}
		}

		@Override
		protected void meetUnaryValueOperator(UnaryValueOperator node) throws RuntimeException {
			if (!node.isVariableScopeChange()) {
				super.meetUnaryValueOperator(node);
			}
		}
	}

	private static class VarValueBinder extends StopAtScopeChange {
		private final String varName;
		private final Value value;

		VarValueBinder(String varName, Value value) {
			super(true);
			this.varName = varName;
			this.value = value;
		}

		@Override
		public void meet(Var var) {
			if (var.getName().equals(varName)) {
				var.replaceWith(Var.of(varName, value, var.isAnonymous(), var.isConstant()));
			}
		}
	}

	// count the number of triple patterns
	private static class ServiceNodeAnalyzer extends AbstractSimpleQueryModelVisitor<RuntimeException> {

		private int count = 0;

		private ServiceNodeAnalyzer() {
			super(true);
		}

		public int getStatementCount() {
			return count;
		}

		@Override
		public void meet(StatementPattern node) throws RuntimeException {
			count++;
		}
	}

}
