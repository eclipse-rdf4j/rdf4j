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
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.planNodes.EnrichWithShape;
import org.eclipse.rdf4j.sail.shacl.planNodes.LiteralComparatorFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author Håvard Ottestad
 */
public class MinExclusivePropertyShape extends PathPropertyShape {

	private final Literal minExclusive;
	private static final Logger logger = LoggerFactory.getLogger(MinExclusivePropertyShape.class);

	MinExclusivePropertyShape(Resource id, SailRepositoryConnection connection, NodeShape nodeShape,
			boolean deactivated, Resource path, Literal minExclusive) {
		super(id, connection, nodeShape, deactivated, path);

		this.minExclusive = minExclusive;

	}

	@Override
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, NodeShape nodeShape, boolean printPlans,
			PlanNodeProvider overrideTargetNode) {
		if (deactivated) {
			return null;
		}

		PlanNode invalidValues = StandardisedPlanHelper.getGenericSingleObjectPlan(shaclSailConnection, nodeShape,
				(parent) -> new LiteralComparatorFilter(parent, minExclusive, value -> value < 0), this,
				overrideTargetNode);

		if (printPlans) {
			String planAsGraphvizDot = getPlanAsGraphvizDot(invalidValues, shaclSailConnection);
			logger.info(planAsGraphvizDot);
		}

		return new EnrichWithShape(new LoggingNode(invalidValues, ""), this);

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
				", path=" + path +
				'}';
	}
}
