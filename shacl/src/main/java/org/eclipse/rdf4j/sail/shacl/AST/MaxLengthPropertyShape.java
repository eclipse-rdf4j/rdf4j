/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
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
import org.eclipse.rdf4j.sail.shacl.planNodes.MaxLengthFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Håvard Ottestad
 */
public class MaxLengthPropertyShape extends PathPropertyShape {

	private final long maxLength;
	private static final Logger logger = LoggerFactory.getLogger(MaxLengthPropertyShape.class);

	MaxLengthPropertyShape(Resource id, SailRepositoryConnection connection, NodeShape nodeShape, Long maxLength) {
		super(id, connection, nodeShape);

		this.maxLength = maxLength;

	}


	@Override
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, NodeShape nodeShape, boolean printPlans, PlanNode overrideTargetNode) {

		PlanNode invalidValues = StandardisedPlanHelper.getGenericSingleObjectPlan(
			shaclSailConnection,
			nodeShape,
			(parent) -> new MaxLengthFilter(parent, maxLength),
			this,
			overrideTargetNode);

		if (printPlans) {
			String planAsGraphvizDot = getPlanAsGraphvizDot(invalidValues, shaclSailConnection);
			logger.info(planAsGraphvizDot);
		}

		return new EnrichWithShape(invalidValues, this);

	}

	@Override
	public SourceConstraintComponent getSourceConstraintComponent() {

		return SourceConstraintComponent.MaxLengthConstraintComponent;
	}

}
