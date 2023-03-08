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

package org.eclipse.rdf4j.sail.shacl.ast.paths;

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeWrapper;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.TupleMapper;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.ShapeSource;

public class InversePath extends Path {

	private final Path inversePath;

	public InversePath(Resource id, Resource inversePath, ShapeSource shapeSource) {
		super(id);
		this.inversePath = Path.buildPath(shapeSource, inversePath);

	}

	@Override
	public String toString() {
		return "InversePath{ " + inversePath + " }";
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.INVERSE_PATH, inversePath.getId());
		inversePath.toModel(inversePath.getId(), null, model, cycleDetection);
	}

	@Override
	public PlanNode getAllAdded(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			PlanNodeWrapper planNodeWrapper) {

		PlanNode added = inversePath.getAllAdded(connectionsGroup, dataGraph, null);
		added = new TupleMapper(added, t -> new ValidationTuple(t.getValue(), t.getActiveTarget(),
				ConstraintComponent.Scope.propertyShape, true, dataGraph));

		if (planNodeWrapper != null) {
			added = planNodeWrapper.apply(added);
		}

		return connectionsGroup.getCachedNodeFor(added);
	}

	@Override
	public PlanNode getAnyAdded(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			PlanNodeWrapper planNodeWrapper) {
		return getAllAdded(connectionsGroup, dataGraph, planNodeWrapper);
	}

	@Override
	public boolean isSupported() {
		return inversePath.isSupported();
	}

	@Override
	public String toSparqlPathString() {
		assert inversePath.toSparqlPathString().equals(inversePath.toSparqlPathString().trim());
		return "^(" + inversePath.toSparqlPathString() + ")";
	}

	@Override
	public SparqlFragment getTargetQueryFragment(StatementMatcher.Variable subject, StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider, Set<String> inheritedVarNames) {

		return inversePath.getTargetQueryFragment(object, subject, rdfsSubClassOfReasoner,
				stableRandomVariableProvider, Set.of());

	}

}
