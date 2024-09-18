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

package org.eclipse.rdf4j.sail.shacl.ast.targets;

import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.DASH;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.FilterTargetIsObject;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnorderedSelect;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;

public class DashAllObjects extends Target {

	private final Resource id;

	public DashAllObjects(Resource id) {
		this.id = id;
	}

	@Override
	public IRI getPredicate() {
		return DASH.AllObjectsTarget;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {

		model.add(subject, SHACL.TARGET_PROP, id);
		model.add(id, RDF.TYPE, getPredicate());

	}

	@Override
	public PlanNode getAdded(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			ConstraintComponent.Scope scope) {
		return getAddedRemovedInner(connectionsGroup.getAddedStatements(), dataGraph, scope, connectionsGroup);
	}

	private PlanNode getAddedRemovedInner(SailConnection connection, Resource[] dataGraph,
			ConstraintComponent.Scope scope, ConnectionsGroup connectionsGroup) {

		return Unique.getInstance(new UnorderedSelect(connection, null,
				null, null, dataGraph, UnorderedSelect.Mapper.ObjectScopedMapper.getFunction(scope), null), false,
				connectionsGroup);

	}

	@Override
	public PlanNode getTargetFilter(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			PlanNode parent) {
		return new FilterTargetIsObject(connectionsGroup.getBaseConnection(), dataGraph, parent, connectionsGroup)
				.getTrueNode(UnBufferedPlanNode.class);
	}

	@Override
	public SparqlFragment getTargetQueryFragment(StatementMatcher.Variable subject, StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider, Set<String> inheritedVarNames) {
		assert (subject == null);

		String tempVar1 = stableRandomVariableProvider.next().asSparqlVariable();
		String tempVar2 = stableRandomVariableProvider.next().asSparqlVariable();

		String queryFragment = tempVar1 + " " + tempVar2 + " " + object.asSparqlVariable() + " .";

		return SparqlFragment.bgp(List.of(), queryFragment, new StatementMatcher(null, null, object, this, Set.of()));
	}

	@Override
	public Set<Namespace> getNamespaces() {
		return Set.of();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		return o != null && getClass() == o.getClass();
	}

	@Override
	public int hashCode() {
		return 57821738;
	}

}
