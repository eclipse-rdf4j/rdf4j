/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.planNodes.SetFilterNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.planNodes.ValuesBackedNode;

import java.util.Arrays;
import java.util.Objects;
import java.util.TreeSet;

/**
 * sh:targetNode
 *
 * @author Håvard Ottestad
 */
public class TargetNode extends NodeShape {

	private final TreeSet<Value> targetNodeSet;

	TargetNode(Resource id, SailRepositoryConnection connection, boolean deactivated, TreeSet<Value> targetNode) {
		super(id, connection, deactivated);
		this.targetNodeSet = targetNode;
		assert !this.targetNodeSet.isEmpty();
	}

	@Override
	public PlanNode getPlan(ConnectionsGroup connectionsGroup, boolean printPlans,
			PlanNodeProvider overrideTargetNode, boolean negateThisPlan, boolean negateSubPlans) {
		assert !negateSubPlans : "There are no subplans!";
		assert !negateThisPlan;

		PlanNode parent = connectionsGroup.getCachedNodeFor(new Select(connectionsGroup.getBaseConnection(),
				getQuery("?a", "?c", connectionsGroup.getRdfsSubClassOfReasoner()), "?a", "?c"));
		return new Unique(new TrimTuple(parent, 0, 1));
	}

	@Override
	public PlanNode getPlanAddedStatements(ConnectionsGroup connectionsGroup,
			PlaneNodeWrapper planeNodeWrapper) {

		return new ValuesBackedNode(targetNodeSet);

	}

	@Override
	public PlanNode getPlanRemovedStatements(ConnectionsGroup connectionsGroup,
			PlaneNodeWrapper planeNodeWrapper) {
		PlanNode parent = connectionsGroup.getCachedNodeFor(
				new Select(connectionsGroup.getRemovedStatements(), getQuery("?a", "?c", null), "?a", "?c"));
		return new Unique(new TrimTuple(parent, 0, 1));
	}

	@Override
	public boolean requiresEvaluation(SailConnection addedStatements, SailConnection removedStatements) {
		return true;
	}

	@Override
	public String getQuery(String subjectVariable, String objectVariable,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {

		StringBuilder sb = new StringBuilder();
		sb.append("VALUES ( ").append(subjectVariable).append(" ) {\n");

		targetNodeSet.stream()
				.map(targetNode -> {
					if (targetNode instanceof Resource) {
						return "<" + targetNode + ">";
					}
					if (targetNode instanceof Literal) {
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

		sb.append("}\n");

		return sb.toString();

	}

	@Override
	public PlanNode getTargetFilter(SailConnection shaclSailConnection, PlanNode parent) {
		return new SetFilterNode(targetNodeSet, parent, 0, true);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		TargetNode that = (TargetNode) o;
		return targetNodeSet.equals(that.targetNodeSet);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), targetNodeSet);
	}

	@Override
	public String toString() {
		return "TargetNode{" +
				"targetNodeSet=" + Arrays.toString(targetNodeSet.toArray()) +
				", id=" + id +
				'}';
	}
}
