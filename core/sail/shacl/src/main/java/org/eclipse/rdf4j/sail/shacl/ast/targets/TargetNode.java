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
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.SetFilterNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValuesBackedNode;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;

public class TargetNode extends Target {
	private final TreeSet<Value> targetNodes;
	private final Resource[] sourceContexts;

	public TargetNode(TreeSet<Value> targetNodes, Resource[] sourceContexts) {
		this.targetNodes = targetNodes;
		assert !this.targetNodes.isEmpty();
		this.sourceContexts = sourceContexts;

	}

	@Override
	public IRI getPredicate() {
		return SHACL.TARGET_NODE;
	}

	@Override
	public PlanNode getAdded(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			ConstraintComponent.Scope scope) {
		return new ValuesBackedNode(targetNodes, scope, sourceContexts);
	}

	@Override
	public PlanNode getTargetFilter(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			PlanNode parent) {
		return new SetFilterNode(targetNodes, parent, 0, true, connectionsGroup);
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		targetNodes.forEach(t -> {
			model.add(subject, getPredicate(), t);
		});
	}

	@Override
	public SparqlFragment getTargetQueryFragment(StatementMatcher.Variable subject, StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider, Set<String> inheritedVarNames) {
		assert subject == null;

		StringBuilder sb = new StringBuilder();
		sb.append("VALUES ( ").append(object.asSparqlVariable()).append(" ) {\n");

		targetNodes.stream()
				.map(targetNode -> {
					if (targetNode.isResource()) {
						return "<" + targetNode + ">";
					}
					if (targetNode.isLiteral()) {
						IRI datatype = ((Literal) targetNode).getDatatype();
						if (datatype == null) {
							return "\"" + targetNode.stringValue() + "\"";
						}
						if (((Literal) targetNode).getLanguage().isPresent()) {
							return "\"" + targetNode.stringValue() + "\"@" + ((Literal) targetNode).getLanguage().get();
						}
						return "\"" + targetNode.stringValue() + "\"^^<" + datatype.stringValue() + ">";
					}

					throw new IllegalStateException(targetNode.getClass().getSimpleName());

				})
				.forEach(targetNode -> sb.append("( ").append(targetNode).append(" )\n"));

		sb.append("}");

		return SparqlFragment.bgp(List.of(), sb.toString());
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
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TargetNode that = (TargetNode) o;
		return targetNodes.equals(that.targetNodes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(targetNodes) + "TargetNode".hashCode();
	}

}
