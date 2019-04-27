/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.planNodes.EnrichWithShape;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.planNodes.ValueInFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author Håvard Ottestad
 */
public class InPropertyShape extends AbstractSimplePropertyShape {

	private final Set<Value> in;
	private static final Logger logger = LoggerFactory.getLogger(InPropertyShape.class);

	InPropertyShape(Resource id, SailRepositoryConnection connection, NodeShape nodeShape, boolean deactivated,
			PathPropertyShape parent, Resource path,
			Resource in) {
		super(id, connection, nodeShape, deactivated, parent, path);

		this.in = new HashSet<>(toList(connection, in));

	}

	@Override
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, boolean printPlans,
			PlanNodeProvider overrideTargetNode, boolean negateThisPlan, boolean negateSubPlans) {

		if (deactivated) {
			return null;
		}
		assert !negateSubPlans : "There are no subplans!";

		PlanNode invalidValues = getGenericSingleObjectPlan(shaclSailConnection, nodeShape,
				(parent) -> new ValueInFilter(parent, in), this, overrideTargetNode, negateThisPlan);

		if (printPlans) {
			String planAsGraphvizDot = getPlanAsGraphvizDot(invalidValues, shaclSailConnection);
			logger.info(planAsGraphvizDot);
		}

		return new EnrichWithShape(invalidValues, this);

	}

	@Override
	public SourceConstraintComponent getSourceConstraintComponent() {
		return SourceConstraintComponent.InConstraintComponent;
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
		InPropertyShape that = (InPropertyShape) o;
		return in.equals(that.in);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), in);
	}

	@Override
	public String toString() {
		return "InPropertyShape{" +
				"in=" + Arrays.toString(in.toArray()) +
				", path=" + getPath() +
				'}';
	}
}
