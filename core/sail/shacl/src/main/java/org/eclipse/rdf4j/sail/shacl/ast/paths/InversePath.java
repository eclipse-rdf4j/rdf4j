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

	private final Path path;

	public InversePath(Resource id, Resource path, ShapeSource shapeSource) {
		super(id);
		this.path = Path.buildPath(shapeSource, path);
	}

	public InversePath(Resource id, Path path) {
		super(id);
		this.path = path;
	}

	@Override
	public String toString() {
		return "InversePath{ " + path + " }";
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.INVERSE_PATH, path.getId());
		path.toModel(path.getId(), null, model, cycleDetection);
	}

	@Override
	public PlanNode getAllAdded(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			PlanNodeWrapper planNodeWrapper) {

		PlanNode added = path.getAllAdded(connectionsGroup, dataGraph, null);
		added = new TupleMapper(added, t -> new ValidationTuple(t.getValue(), t.getActiveTarget(),
				ConstraintComponent.Scope.propertyShape, true, dataGraph), connectionsGroup);

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
		return path.isSupported();
	}

	@Override
	public String toSparqlPathString() {
		assert path.toSparqlPathString().equals(path.toSparqlPathString().trim());
		if (path instanceof SimplePath || path instanceof AlternativePath || path instanceof SequencePath) {
			return "^" + path.toSparqlPathString();
		}
		return "^(" + path.toSparqlPathString() + ")";
	}

	@Override
	public SparqlFragment getTargetQueryFragment(StatementMatcher.Variable subject, StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider, Set<String> inheritedVarNames) {

		return path.getTargetQueryFragment(object, subject, rdfsSubClassOfReasoner,
				stableRandomVariableProvider, inheritedVarNames);

	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		InversePath that = (InversePath) o;

		return path.equals(that.path);
	}

	@Override
	public int hashCode() {
		return path.hashCode();
	}
}
