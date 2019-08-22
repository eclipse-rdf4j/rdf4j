/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.planNodes.EnrichWithShape;
import org.eclipse.rdf4j.sail.shacl.planNodes.LiteralComparatorFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author Håvard Ottestad
 */
public class MaxExclusivePropertyShape extends AbstractSimplePropertyShape {

	private final Literal maxExclusive;
	private static final Logger logger = LoggerFactory.getLogger(MaxExclusivePropertyShape.class);

	MaxExclusivePropertyShape(Resource id, SailRepositoryConnection connection, NodeShape nodeShape,
			boolean deactivated, PathPropertyShape parent, Resource path, Literal maxExclusive) {
		super(id, connection, nodeShape, deactivated, parent, path);

		this.maxExclusive = maxExclusive;

	}

	@Override
	public PlanNode getPlan(ConnectionsGroup connectionsGroup, boolean printPlans,
			PlanNodeProvider overrideTargetNode, boolean negateThisPlan, boolean negateSubPlans) {

		if (deactivated) {
			return null;
		}
		assert !negateSubPlans : "There are no subplans!";

		PlanNode invalidValues = getGenericSingleObjectPlan(connectionsGroup, nodeShape,
				(parent) -> new LiteralComparatorFilter(parent, maxExclusive, value -> value > 0), this,
				overrideTargetNode, negateThisPlan);

		if (printPlans) {
			String planAsGraphvizDot = getPlanAsGraphvizDot(invalidValues, connectionsGroup);
			logger.info(planAsGraphvizDot);
		}

		return new EnrichWithShape(invalidValues, this);

	}

	@Override
	public SourceConstraintComponent getSourceConstraintComponent() {
		return SourceConstraintComponent.MaxExclusiveConstraintComponent;
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
		MaxExclusivePropertyShape that = (MaxExclusivePropertyShape) o;
		return maxExclusive.equals(that.maxExclusive);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), maxExclusive);
	}

	@Override
	public String toString() {
		return "MaxExclusivePropertyShape{" +
				"maxExclusive=" + maxExclusive +
				", path=" + getPath() +
				'}';
	}
}
