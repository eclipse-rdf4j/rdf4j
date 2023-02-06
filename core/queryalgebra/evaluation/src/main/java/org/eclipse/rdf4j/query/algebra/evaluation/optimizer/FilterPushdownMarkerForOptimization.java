/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.optimizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.Exists;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.IsLiteral;
import org.eclipse.rdf4j.query.algebra.IsResource;
import org.eclipse.rdf4j.query.algebra.IsURI;
import org.eclipse.rdf4j.query.algebra.Lang;
import org.eclipse.rdf4j.query.algebra.Or;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;

/**
 * While this class is in the optimiser package it is not strictly speaking an optimisation itself. The aim here is to
 * add as much information about what is inside an variable that we can avoid significant work during query execution.
 *
 * @author jerven bolleman
 *
 */
public class FilterPushdownMarkerForOptimization implements QueryOptimizer {

	private static class FilterValueExprMarker extends AbstractSimpleQueryModelVisitor<RuntimeException> {
		private final Map<String, InferedConstantKnowledge> state = new HashMap<>();
		private boolean aCandidate = true;

		@Override
		public void meet(Or node) throws RuntimeException {
			// For now we don't deal with OR.
			state.clear();
			aCandidate = false;
			super.meet(node);
		}

		@Override
		public void meet(IsResource node) throws RuntimeException {
			if (node.getArg() instanceof Var) {
				getState(((Var) node.getArg()).getName(), state).isResource = true;
			}
			super.meet(node);
		}

		@Override
		public void meet(IsURI node) throws RuntimeException {
			if (node.getArg() instanceof Var) {
				InferedConstantKnowledge ick = getState(((Var) node.getArg()).getName(), state);
				ick.isIri = true;
				ick.isResource = true;
			}
			super.meet(node);
		}

		@Override
		public void meet(IsLiteral node) throws RuntimeException {
			if (node.getArg() instanceof Var) {
				InferedConstantKnowledge ick = getState(((Var) node.getArg()).getName(), state);
				ick.isIri = false;
				ick.isResource = false;
				ick.filters.add(node);
			}
			super.meet(node);
		}

		@Override
		public void meet(Lang node) throws RuntimeException {
			if (node.getArg() instanceof Var) {
				InferedConstantKnowledge ick = getState(((Var) node.getArg()).getName(), state);
				ick.filters.add(node);
			}
			super.meet(node);
		}

		@Override
		public void meet(Compare node) throws RuntimeException {
			if (node.getLeftArg() instanceof Var && node.getRightArg() instanceof Var) {
				Var l = (Var) node.getLeftArg();
				Var r = (Var) node.getRightArg();
				if (!l.isConstant() && r.isConstant()) {
					InferedConstantKnowledge ick = getState(((Var) node.getLeftArg()).getName(), state);
					ick.filters.add(node);
				} else if (l.isConstant() && !r.isConstant()) {
					InferedConstantKnowledge ick = getState(((Var) node.getRightArg()).getName(), state);
					ick.filters.add(node);
				}
			} else if (node.getLeftArg() instanceof Var && node.getRightArg() instanceof ValueConstant) {
				Var l = (Var) node.getLeftArg();
				if (!l.isConstant()) {
					InferedConstantKnowledge ick = getState(((Var) node.getLeftArg()).getName(), state);
					ick.filters.add(node);
				}
			} else if (node.getRightArg() instanceof Var && node.getLeftArg() instanceof ValueConstant) {
				Var r = (Var) node.getRightArg();
				if (!r.isConstant()) {
					InferedConstantKnowledge ick = getState(((Var) node.getRightArg()).getName(), state);
					ick.filters.add(node);
				}
			}
			super.meet(node);
		}
	}

	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		VarToMarkVisitor mark = new VarToMarkVisitor();
		tupleExpr.visit(mark);
		tupleExpr.visit(new StatementPatternToMarkVisitor(mark.state));
	}

	private class VarToMarkVisitor extends AbstractSimpleQueryModelVisitor<RuntimeException> {
		private final Map<String, InferedConstantKnowledge> state = new HashMap<>();

		public VarToMarkVisitor() {
			// We don't separately meet StatmentPattern child elements
			super(false);
		}

		@Override
		public void meet(Exists node) throws RuntimeException {
			// We do not decent into an Exists query in this instance
			// Still we do want the kind of results in here.
			TupleExpr arg = node.getSubQuery();
			VarToMarkVisitor argVisit = new VarToMarkVisitor();
			arg.visit(argVisit);
			arg.visit(new StatementPatternToMarkVisitor(argVisit.state));
		}

		@Override
		public void meet(Service node) throws RuntimeException {
			// We do not decent into Service node at all
		}

		@Override
		public void meet(BindingSetAssignment node) throws RuntimeException {
			// We do not decent into Service node at all
			for (String bindingName : node.getBindingNames()) {
				boolean allIri = true;
				boolean allResource = true;
				for (BindingSet bs : node.getBindingSets()) {
					Value val = bs.getValue(bindingName);
					if (val != null) {
						if (!val.isResource()) {
							allResource = false;
						}
						if (!val.isIRI()) {
							allIri = false;
						}
					}
					InferedConstantKnowledge ick = getState(bindingName, state);
					ick.isIri = allIri;
					ick.isResource = allResource;
				}
			}
		}

		@Override
		public void meet(StatementPattern node) throws RuntimeException {
			Var subject = node.getSubjectVar();
			if (subject != null && !subject.isConstant()) {
				String name = subject.getName();
				InferedConstantKnowledge ick = getState(name, state);
				ick.isResource = true;
			}
			Var predicate = node.getPredicateVar();
			if (predicate != null && !predicate.isConstant()) {
				InferedConstantKnowledge ick = getState(predicate.getName(), state);
				ick.isIri = true;
				ick.isResource = true;
			}
			Var context = node.getContextVar();
			if (context != null && !context.isConstant()) {
				String name = context.getName();
				InferedConstantKnowledge ick = getState(name, state);
				ick.isResource = true;
			}
		}

		@Override
		public void meet(Union node) throws RuntimeException {

			TupleExpr leftArg = node.getLeftArg();
			VarToMarkVisitor leftVisit = new VarToMarkVisitor();
			leftArg.visit(leftVisit);
			leftArg.visit(new StatementPatternToMarkVisitor(leftVisit.state));

			TupleExpr rightArg = node.getLeftArg();
			VarToMarkVisitor rightVisit = new VarToMarkVisitor();
			rightArg.visit(rightVisit);
			rightArg.visit(new StatementPatternToMarkVisitor(rightVisit.state));
		}

		@Override
		public void meet(Filter node) throws RuntimeException {

			ValueExpr valueExpr = node.getCondition();
			FilterValueExprMarker visitor = new FilterValueExprMarker();
			valueExpr.visit(visitor);
			if (visitor.aCandidate) {
				for (Map.Entry<String, InferedConstantKnowledge> en : visitor.state.entrySet()) {
					InferedConstantKnowledge toMerge = getState(en.getKey(), state);
					if (en.getValue().isIri) {
						toMerge.isIri = true;
					}
					if (en.getValue().isResource) {
						toMerge.isResource = true;
					}
					toMerge.filters.addAll(en.getValue().filters);
				}
			}
		}
	}

	private static InferedConstantKnowledge getState(String name, Map<String, InferedConstantKnowledge> state) {
		InferedConstantKnowledge ick = state.get(name);
		if (ick == null) {
			ick = new InferedConstantKnowledge();
			state.put(name, ick);
		}
		return ick;
	}

	private static class StatementPatternToMarkVisitor extends AbstractSimpleQueryModelVisitor<RuntimeException> {
		private final Map<String, InferedConstantKnowledge> state;

		public StatementPatternToMarkVisitor(Map<String, InferedConstantKnowledge> state) {
			super();
			this.state = state;
		}

		@Override
		public void meet(StatementPattern node) throws RuntimeException {
			Var subject = node.getSubjectVar();
			if (subject != null && !subject.isConstant()) {
				InferedConstantKnowledge subjectState = state.get(subject.getName());
				if (mergeCandidate(subjectState)) {
					node = replaceIfneeded(node, (sp) -> merge(sp.subject, subjectState));
				}
			}
			Var object = node.getObjectVar();
			if (object != null && !object.isConstant()) {
				InferedConstantKnowledge objectState = state.get(object.getName());
				if (mergeCandidate(objectState)) {
					node = replaceIfneeded(node, (sp) -> merge(sp.object, objectState));
				}
			}
			Var context = node.getContextVar();
			if (context != null && !context.isConstant()) {
				InferedConstantKnowledge contextState = state.get(context.getName());
				if (mergeCandidate(contextState)) {
					node = replaceIfneeded(node, (sp) -> merge(sp.context, contextState));
				}
			}
		}

		private boolean mergeCandidate(InferedConstantKnowledge objectState) {
			return objectState != null
					&& (objectState.isIri || objectState.isResource || !objectState.filters.isEmpty());
		}

		private InferedConstantKnowledge merge(InferedConstantKnowledge prev, InferedConstantKnowledge toAdd) {
			if (prev == null)
				return toAdd.copy();
			else {
				if (toAdd.isIri)
					prev.isIri = true;
				if (toAdd.isResource)
					prev.isResource = true;
				prev.filters.addAll(toAdd.filters);
			}
			return prev;
		}

		private StatementPattern replaceIfneeded(StatementPattern node, Consumer<MarkedUpStatementPattern> set) {
			if (node instanceof MarkedUpStatementPattern) {
				set.accept((MarkedUpStatementPattern) node);
				return node;
			} else {
				MarkedUpStatementPattern markedUpStatementPattern = new MarkedUpStatementPattern(node);
				node.getParentNode().replaceChildNode(node, markedUpStatementPattern);
				set.accept(markedUpStatementPattern);
				return markedUpStatementPattern;
			}
		}
	}

	// TODO: Find a good name for this
	@InternalUseOnly
	public static final class InferedConstantKnowledge {
		private boolean isIri;

		private boolean isResource;
		private List<ValueExpr> filters = new ArrayList<>();

		public InferedConstantKnowledge(boolean isIri2, boolean isResource2, List<ValueExpr> filters2) {
			isIri = isIri2;
			isResource = isResource2;
			filters = filters2;
		}

		public InferedConstantKnowledge() {
			// TODO Auto-generated constructor stub
		}

		public InferedConstantKnowledge copy() {

			return new InferedConstantKnowledge(isIri, isResource, filters);
		}

		public List<ValueExpr> filters() {
			return filters;
		}

		public boolean isIri() {
			return isIri;
		}

		public boolean isResource() {
			return isResource;
		}
	}

	@InternalUseOnly
	public static class MarkedUpStatementPattern extends StatementPattern {
		private final StatementPattern wrapped;
		private InferedConstantKnowledge subject = new InferedConstantKnowledge();
		private InferedConstantKnowledge object = new InferedConstantKnowledge();
		private InferedConstantKnowledge predicate = new InferedConstantKnowledge();
		private InferedConstantKnowledge context = new InferedConstantKnowledge();

		public QueryModelNode getParentNode() {
			return wrapped.getParentNode();
		}

		public void setParentNode(QueryModelNode parent) {
			wrapped.setParentNode(parent);
		}

		public boolean isVariableScopeChange() {
			return wrapped.isVariableScopeChange();
		}

		public void setVariableScopeChange(boolean isVariableScopeChange) {
			wrapped.setVariableScopeChange(isVariableScopeChange);
		}

		public boolean isGraphPatternGroup() {
			return wrapped.isGraphPatternGroup();
		}

		public void setGraphPatternGroup(boolean isGraphPatternGroup) {
			wrapped.setGraphPatternGroup(isGraphPatternGroup);
		}

		public void replaceWith(QueryModelNode replacement) {
			wrapped.replaceWith(replacement);
		}

		public Scope getScope() {
			return wrapped.getScope();
		}

		public String toString() {
			return wrapped.toString();
		}

		public void setScope(Scope scope) {
			wrapped.setScope(scope);
		}

		public Var getSubjectVar() {
			return wrapped.getSubjectVar();
		}

		public void setSubjectVar(Var subject) {
			wrapped.setSubjectVar(subject);
		}

		public Var getPredicateVar() {
			return wrapped.getPredicateVar();
		}

		public void setPredicateVar(Var predicate) {
			wrapped.setPredicateVar(predicate);
		}

		public Var getObjectVar() {
			return wrapped.getObjectVar();
		}

		public void setObjectVar(Var object) {
			wrapped.setObjectVar(object);
		}

		public double getResultSizeEstimate() {
			return wrapped.getResultSizeEstimate();
		}

		public void setResultSizeEstimate(double resultSizeEstimate) {
			wrapped.setResultSizeEstimate(resultSizeEstimate);
		}

		public long getResultSizeActual() {
			return wrapped.getResultSizeActual();
		}

		public Var getContextVar() {
			return wrapped.getContextVar();
		}

		public void setResultSizeActual(long resultSizeActual) {
			wrapped.setResultSizeActual(resultSizeActual);
		}

		public void setContextVar(Var context) {
			wrapped.setContextVar(context);
		}

		public double getCostEstimate() {
			return wrapped.getCostEstimate();
		}

		public void setCostEstimate(double costEstimate) {
			wrapped.setCostEstimate(costEstimate);
		}

		public long getTotalTimeNanosActual() {
			return wrapped.getTotalTimeNanosActual();
		}

		public Set<String> getBindingNames() {
			return wrapped.getBindingNames();
		}

		public void setTotalTimeNanosActual(long totalTimeNanosActual) {
			wrapped.setTotalTimeNanosActual(totalTimeNanosActual);
		}

		public Set<String> getAssuredBindingNames() {
			return wrapped.getAssuredBindingNames();
		}

		public double getCardinality() {
			return wrapped.getCardinality();
		}

		public void setCardinality(double cardinality) {
			wrapped.setCardinality(cardinality);
		}

		public void resetCardinality() {
			wrapped.resetCardinality();
		}

		public boolean isCardinalitySet() {
			return wrapped.isCardinalitySet();
		}

		public List<Var> getVarList() {
			return wrapped.getVarList();
		}

		public <L extends Collection<Var>> L getVars(L varCollection) {
			return wrapped.getVars(varCollection);
		}

		public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
			wrapped.visit(visitor);
		}

		public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
			wrapped.visitChildren(visitor);
		}

		public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
			wrapped.replaceChildNode(current, replacement);
		}

		public String getSignature() {
			return wrapped.getSignature();
		}

		public boolean equals(Object other) {
			return wrapped.equals(other);
		}

		public int hashCode() {
			return wrapped.hashCode();
		}

		public StatementPattern clone() {
			return wrapped.clone();
		}

		public MarkedUpStatementPattern(StatementPattern wrapped) {
			super();
			this.wrapped = wrapped;
		}

		public void setSubjectIck(InferedConstantKnowledge ick) {
			this.subject = ick;
		}

		public void setPredicateIck(InferedConstantKnowledge ick) {
			this.predicate = ick;
		}

		public void setObjectIck(InferedConstantKnowledge ick) {
			this.object = ick;
		}

		public void setContexttIck(InferedConstantKnowledge ick) {
			this.context = ick;
		}

		public InferedConstantKnowledge getSubjectIck() {
			return this.subject;
		}

		public InferedConstantKnowledge getPredicateIck() {
			return this.predicate;
		}

		public InferedConstantKnowledge getObjectIck() {
			return this.object;
		}

		public InferedConstantKnowledge getContextIck() {
			return this.context;
		}

	}
}
