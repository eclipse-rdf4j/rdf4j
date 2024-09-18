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

import static org.eclipse.rdf4j.model.util.Values.literal;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher.Variable;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.FilterPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.MinLengthFilter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;

public class MinLengthConstraintComponent extends AbstractSimpleConstraintComponent {

	long minLength;

	public MinLengthConstraintComponent(long minLength) {
		super();
		this.minLength = minLength;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.MIN_LENGTH,
				literal(BigInteger.valueOf(minLength)));
	}

	@Override
	String getSparqlFilterExpression(Variable<Value> variable, boolean negated) {
		if (negated) {
			return "STRLEN(STR(" + variable.asSparqlVariable() + ")) >= " + minLength;
		} else {
			return "STRLEN(STR(" + variable.asSparqlVariable() + ")) < " + minLength;
		}
	}

	@Override
	Function<PlanNode, FilterPlanNode> getFilterAttacher(ConnectionsGroup connectionsGroup) {
		return (parent) -> new MinLengthFilter(parent, minLength, connectionsGroup);
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.MinLengthConstraintComponent;
	}

	@Override
	public ConstraintComponent deepClone() {
		return new MinLengthConstraintComponent(minLength);
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

		MinLengthConstraintComponent that = (MinLengthConstraintComponent) o;

		return minLength == that.minLength;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(minLength) + "MinLengthConstraintComponent".hashCode();
	}
}
