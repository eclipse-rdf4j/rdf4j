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
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.planNodes.SetFilterNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.Unique;

import java.util.Objects;
import java.util.Set;

/**
 * sh:targetNode
 *
 * @author Håvard Ottestad
 */
public class TargetNode extends NodeShape {

	private final Set<Value> targetNodeSet;

	TargetNode(Resource id, SailRepositoryConnection connection, boolean deactivated, Set<Value> targetNode) {
		super(id, connection, deactivated);
		this.targetNodeSet = targetNode;
		assert !this.targetNodeSet.isEmpty();
	}

	@Override
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, NodeShape nodeShape, boolean printPlans,
			PlanNode overrideTargetNode) {
		PlanNode parent = shaclSailConnection.getCachedNodeFor(new Select(shaclSailConnection,
				getQuery("?a", "?c", shaclSailConnection.getRdfsSubClassOfReasoner()), "*"));
		return new Unique(new TrimTuple(new LoggingNode(parent, ""), 0, 1));
	}

	@Override
	public PlanNode getPlanAddedStatements(ShaclSailConnection shaclSailConnection, NodeShape nodeShape,
			PlaneNodeWrapper planeNodeWrapper) {
		PlanNode parent = shaclSailConnection.getCachedNodeFor(
				new Select(shaclSailConnection.getAddedStatements(), getQuery("?a", "?c", null), "*"));
		return new Unique(new TrimTuple(new LoggingNode(parent, ""), 0, 1));

	}

	@Override
	public PlanNode getPlanRemovedStatements(ShaclSailConnection shaclSailConnection, NodeShape nodeShape,
			PlaneNodeWrapper planeNodeWrapper) {
		PlanNode parent = shaclSailConnection.getCachedNodeFor(
				new Select(shaclSailConnection.getRemovedStatements(), getQuery("?a", "?c", null), "*"));
		return new Unique(new TrimTuple(parent, 0, 1));
	}

	@Override
	public boolean requiresEvaluation(SailConnection addedStatements, SailConnection removedStatements) {
		return true;
	}

	@Override
	public String getQuery(String subjectVariable, String objectVariable,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {

		return targetNodeSet.stream()
				.map(node -> {
					if (node instanceof Resource)
						return "<" + node + ">";
					if (node instanceof Literal) {
						IRI datatype = ((Literal) node).getDatatype();
						if (datatype == null)
							return "\"" + node.stringValue() + "\"";
						return "\"" + node.stringValue() + "\"^^<" + datatype.stringValue() + ">";
					}

					throw new IllegalStateException(node.getClass().getSimpleName());

				})
				.map(r -> "{{ select * where {BIND(" + r + " as " + subjectVariable + "). " + subjectVariable + " ?b1 "
						+ objectVariable + " .}}}"
						+ "\n UNION \n"
						+ "{{ select * where {BIND(" + r + " as " + subjectVariable + "). " + objectVariable + " ?b1 "
						+ subjectVariable + " .}}}")
				.reduce((a, b) -> a + " UNION " + b)
				.get();

	}

	@Override
	public PlanNode getTargetFilter(NotifyingSailConnection shaclSailConnection, PlanNode parent) {
		return new LoggingNode(new SetFilterNode(targetNodeSet, parent, 0, true), "targetNode filter");
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
}
