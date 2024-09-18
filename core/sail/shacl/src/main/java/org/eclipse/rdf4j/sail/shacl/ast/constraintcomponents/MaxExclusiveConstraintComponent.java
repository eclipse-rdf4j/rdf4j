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

package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher.Variable;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.FilterPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.LiteralComparatorFilter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;

public class MaxExclusiveConstraintComponent extends AbstractSimpleConstraintComponent {

	Literal maxExclusive;

	public MaxExclusiveConstraintComponent(Literal maxExclusive) {
		super();
		this.maxExclusive = maxExclusive;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.MAX_EXCLUSIVE, maxExclusive);
	}

	@Override
	String getSparqlFilterExpression(Variable<Value> variable, boolean negated) {
		if (negated) {
			return literalToString(maxExclusive) + " > " + variable.asSparqlVariable();
		} else {
			return literalToString(maxExclusive) + " <= " + variable.asSparqlVariable() + "";
		}
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.MaxExclusiveConstraintComponent;
	}

	@Override
	public ConstraintComponent deepClone() {
		return new MaxExclusiveConstraintComponent(maxExclusive);
	}

	@Override
	Function<PlanNode, FilterPlanNode> getFilterAttacher(ConnectionsGroup connectionsGroup) {
		return (parent) -> new LiteralComparatorFilter(parent, maxExclusive, Compare.CompareOp.GT, connectionsGroup);
	}

	@Override
	public List<Literal> getDefaultMessage() {
		return List.of();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		MaxExclusiveConstraintComponent that = (MaxExclusiveConstraintComponent) o;

		return maxExclusive.equals(that.maxExclusive);
	}

	@Override
	public int hashCode() {
		return maxExclusive.hashCode() + "MaxExclusiveConstraintComponent".hashCode();
	}
}
