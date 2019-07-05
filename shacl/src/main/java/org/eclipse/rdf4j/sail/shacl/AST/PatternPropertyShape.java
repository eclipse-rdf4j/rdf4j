/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.planNodes.EnrichWithShape;
import org.eclipse.rdf4j.sail.shacl.planNodes.PatternFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author Håvard Ottestad
 */
public class PatternPropertyShape extends AbstractSimplePropertyShape {

	private final String pattern;
	private final String flags;
	private static final Logger logger = LoggerFactory.getLogger(PatternPropertyShape.class);

	PatternPropertyShape(Resource id, SailRepositoryConnection connection, NodeShape nodeShape, boolean deactivated,
			PathPropertyShape parent, Resource path,
			String pattern, String flags) {
		super(id, connection, nodeShape, deactivated, parent, path);

		this.pattern = pattern;
		this.flags = flags;

	}

	@Override
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, boolean printPlans,
			PlanNodeProvider overrideTargetNode, boolean negateThisPlan, boolean negateSubPlans) {

		if (deactivated) {
			return null;
		}
		assert !negateSubPlans : "There are no subplans!";

		PlanNode invalidValues = getGenericSingleObjectPlan(shaclSailConnection, nodeShape,
				(parent) -> new PatternFilter(parent, pattern, flags), this, overrideTargetNode, negateThisPlan);

		if (printPlans) {
			String planAsGraphvizDot = getPlanAsGraphvizDot(invalidValues, shaclSailConnection);
			logger.info(planAsGraphvizDot);
		}

		return new EnrichWithShape(invalidValues, this);

	}

	@Override
	public SourceConstraintComponent getSourceConstraintComponent() {
		return SourceConstraintComponent.PatternConstraintComponent;
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
		PatternPropertyShape that = (PatternPropertyShape) o;
		return pattern.equals(that.pattern) &&
				Objects.equals(flags, that.flags);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), pattern, flags);
	}

	@Override
	public String toString() {
		return "PatternPropertyShape{" +
				"pattern='" + pattern + '\'' +
				", flags='" + flags + '\'' +
				", path=" + getPath() +
				'}';
	}
}
