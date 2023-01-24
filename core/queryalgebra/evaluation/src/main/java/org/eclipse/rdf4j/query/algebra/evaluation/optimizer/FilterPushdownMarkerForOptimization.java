/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.optimizer;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Exists;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
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

	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		VarToMarkVisitor mark = new VarToMarkVisitor();
		tupleExpr.visit(mark);
		tupleExpr.visit(new StatementPatternToMarkVisitor(mark.state));
	}

	private static class VarToMarkVisitor extends AbstractSimpleQueryModelVisitor<RuntimeException> {
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
				for (BindingSet bs: node.getBindingSets()) {
					Value val = bs.getValue(bindingName);
					if (! val.isResource()) {
						allResource = false;
					}
					if (! val.isIRI()) {
						allIri = false;
					}
				}
				InferedConstantKnowledge ick = getState(bindingName);
				ick.isIri = allIri;
				ick.isResource = allResource;
			}
		}

		@Override
		public void meet(StatementPattern node) throws RuntimeException {
			Var subject = node.getSubjectVar();
			if (subject != null && !subject.isConstant()) {
				String name = subject.getName();
				InferedConstantKnowledge ick = getState(name);
				ick.isResource = true;
			}
			Var predicate = node.getPredicateVar();
			if (predicate != null && !predicate.isConstant()) {
				InferedConstantKnowledge ick = getState(predicate.getName());
				ick.isIri = true;
				ick.isResource = true;
			}
			Var context = node.getContextVar();
			if (context != null && !context.isConstant() && state.containsKey(context.getName())) {
				String name = context.getName();
				InferedConstantKnowledge ick = getState(name);
				ick.isResource = true;
			}
		}

		private InferedConstantKnowledge getState(String name) {
			InferedConstantKnowledge ick = state.get(name);
			if (ick == null) {
				ick = new InferedConstantKnowledge();
				state.put(name, ick);
			}
			return ick;
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
				if (subjectState != null && subjectState.isIri) {
					node = replaceIfneeded(node, MarkedUpStatementPattern::setSubjectIsIri);
				}
				if (subjectState != null && subjectState.isResource) {
					node = replaceIfneeded(node, MarkedUpStatementPattern::setSubjectIsResource);
				}
			}
			Var object = node.getObjectVar();
			if (object != null && !object.isConstant()) {
				InferedConstantKnowledge objectState = state.get(object.getName());
				if (objectState != null && objectState.isIri) {
					node = replaceIfneeded(node, MarkedUpStatementPattern::setObjectIsIri);
				}
				if (objectState != null && objectState.isResource) {
					node = replaceIfneeded(node, MarkedUpStatementPattern::setObjectIsResource);
				}
			}
			Var context = node.getContextVar();
			if (context != null && !context.isConstant()) {
				InferedConstantKnowledge contextState = state.get(context.getName());
				if (contextState != null && contextState.isIri) {
					node = replaceIfneeded(node, MarkedUpStatementPattern::setContextIsIri);
				}
			}

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

	private static final class InferedConstantKnowledge {
		boolean isIri;
		boolean isResource;
	}

	public static class MarkedUpStatementPattern extends StatementPattern {
		private final StatementPattern wrapped;
		private boolean subjectIsIri;
		private boolean objectIsIri;
		private boolean subjectIsResource;
		private boolean objectIsResource;
		private boolean contextIsIri;

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

		public void setSubjectIsIri() {
			this.subjectIsIri = true;
			this.subjectIsResource = true;
		}

		public void setSubjectIsResource() {
			this.subjectIsResource = true;
		}

		public void setObjectIsIri() {
			this.objectIsIri = true;
			this.objectIsResource = true;
		}

		public void setObjectIsResource() {
			this.objectIsResource = true;
		}

		public void setContextIsIri() {
			this.contextIsIri = true;
		}

		public boolean isObjectIsIri() {
			return objectIsIri;
		}

		public void setObjectIsIri(boolean objectIsIri) {
			this.objectIsIri = objectIsIri;
		}

		public boolean isSubjectIsIri() {
			return subjectIsIri;
		}

		public boolean isSubjectIsResource() {
			return subjectIsResource;
		}

		public void setSubjectIsResource(boolean subjectIsResource) {
			this.subjectIsResource = subjectIsResource;
		}

		public boolean isObjectIsResource() {
			return objectIsResource;
		}

		public void setObjectIsResource(boolean objectIsResource) {
			this.objectIsResource = objectIsResource;
		}

		public boolean isContextIsIri() {
			return contextIsIri;
		}

		public void setContextIsIri(boolean contextIsIri) {
			this.contextIsIri = contextIsIri;
		}

		public void setSubjectIsIri(boolean subjectIsIri) {
			this.subjectIsIri = subjectIsIri;
		}
	}
}
