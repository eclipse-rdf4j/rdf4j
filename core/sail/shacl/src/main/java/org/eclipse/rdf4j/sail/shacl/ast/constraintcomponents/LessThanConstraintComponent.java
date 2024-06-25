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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ValidationSettings;
import org.eclipse.rdf4j.sail.shacl.ast.Shape;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.CheckLessThanValuesBasedOnPathAndPredicate;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;

public class LessThanConstraintComponent extends AbstractPairwiseConstraintComponent {

	public LessThanConstraintComponent(IRI predicate, Shape shape) {
		super(predicate, shape);
	}

	@Override
	IRI getConstraintIri() {
		return SHACL.LESS_THAN;
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.LessThanConstraintComponent;
	}

	PlanNode getPairwiseCheck(ConnectionsGroup connectionsGroup, ValidationSettings validationSettings,
			PlanNode allTargets, StatementMatcher.Variable<Resource> subject, StatementMatcher.Variable<Value> object,
			SparqlFragment targetQueryFragment) {
		return new CheckLessThanValuesBasedOnPathAndPredicate(connectionsGroup.getBaseConnection(),
				validationSettings.getDataGraph(), allTargets, predicate, subject, object, targetQueryFragment, shape,
				this, producesValidationReport);
	}

	@Override
	public ConstraintComponent deepClone() {
		return new LessThanConstraintComponent(predicate, shape);
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

		LessThanConstraintComponent that = (LessThanConstraintComponent) o;

		return predicate.equals(that.predicate);
	}

	@Override
	public int hashCode() {
		return predicate.hashCode() + "LessThanConstraintComponent".hashCode();
	}
}
