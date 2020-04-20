/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.AST;

import java.util.Objects;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.planNodes.EnrichWithShape;
import org.eclipse.rdf4j.sail.shacl.planNodes.LiteralComparatorFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Håvard Ottestad
 */
public class MinExclusivePropertyShape extends AbstractSimplePropertyShape {

	private final Literal minExclusive;
	private static final Logger logger = LoggerFactory.getLogger(MinExclusivePropertyShape.class);

	MinExclusivePropertyShape(Resource id, SailRepositoryConnection connection, NodeShape nodeShape,
			boolean deactivated, PathPropertyShape parent, Resource path, Literal minExclusive) {
		super(id, connection, nodeShape, deactivated, parent, path);

		this.minExclusive = minExclusive;

	}

	@Override
	public PlanNode getPlan(ConnectionsGroup connectionsGroup, boolean printPlans,
			PlanNodeProvider overrideTargetNode, boolean negateThisPlan, boolean negateSubPlans) {

		if (deactivated) {
			return null;
		}
		assert !negateSubPlans : "There are no subplans!";

		PlanNode invalidValues = getGenericSingleObjectPlan(connectionsGroup, nodeShape,
				(parent) -> new LiteralComparatorFilter(parent, minExclusive, value -> value < 0), this,
				overrideTargetNode, negateThisPlan);

		if (printPlans) {
			String planAsGraphvizDot = getPlanAsGraphvizDot(invalidValues, connectionsGroup);
			logger.info(planAsGraphvizDot);
		}

		return new EnrichWithShape(invalidValues, this);

	}

	@Override
	public SourceConstraintComponent getSourceConstraintComponent() {
		return SourceConstraintComponent.MinExclusiveConstraintComponent;
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
		MinExclusivePropertyShape that = (MinExclusivePropertyShape) o;
		return minExclusive.equals(that.minExclusive);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), minExclusive);
	}

	@Override
	public String toString() {
		return "MinExclusivePropertyShape{" +
				"minExclusive=" + minExclusive +
				", path=" + getPath() +
				", id=" + id +
				'}';
	}
}
