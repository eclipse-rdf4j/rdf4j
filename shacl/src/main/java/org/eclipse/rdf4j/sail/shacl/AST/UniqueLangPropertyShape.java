/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.planNodes.BulkedExternalLeftOuterJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.EnrichWithShape;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.NonUniqueTargetLang;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Unique;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Håvard Ottestad
 */
public class UniqueLangPropertyShape extends PathPropertyShape {

	private final boolean uniqueLang;
	private static final Logger logger = LoggerFactory.getLogger(UniqueLangPropertyShape.class);
	private static final ValueFactory vf = SimpleValueFactory.getInstance();

	UniqueLangPropertyShape(Resource id, SailRepositoryConnection connection, NodeShape nodeShape, boolean deactivated,
			Resource path,
			boolean uniqueLang) {
		super(id, connection, nodeShape, deactivated, path);

		this.uniqueLang = uniqueLang;
		assert uniqueLang : "uniqueLang should always be true";

	}

	@Override
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, NodeShape nodeShape, boolean printPlans,
			PlanNode overrideTargetNode) {
		if (deactivated) {
			return null;
		}

		if (overrideTargetNode != null) {
			PlanNode relevantTargetsWithPath = new LoggingNode(new BulkedExternalLeftOuterJoin(overrideTargetNode,
					shaclSailConnection, path.getQuery("?a", "?c", null), false), "");

			PlanNode planNode = new NonUniqueTargetLang(relevantTargetsWithPath);

			if (printPlans) {
				String planAsGraphvizDot = getPlanAsGraphvizDot(planNode, shaclSailConnection);
				logger.info(planAsGraphvizDot);
			}

			return new EnrichWithShape(new LoggingNode(planNode, ""), this);
		}

		PlanNode addedTargets = new LoggingNode(nodeShape.getPlanAddedStatements(shaclSailConnection, nodeShape, null),
				"");

		PlanNode addedByPath = new LoggingNode(super.getPlanAddedStatements(shaclSailConnection, nodeShape, null), "");

		addedByPath = new LoggingNode(nodeShape.getTargetFilter(shaclSailConnection, addedByPath), "");

		PlanNode mergeNode = new LoggingNode(new UnionNode(addedTargets, addedByPath), "");

		PlanNode trimmed = new LoggingNode(new TrimTuple(mergeNode, 0, 1), "");

		PlanNode allRelevantTargets = new LoggingNode(new Unique(trimmed), "");

		PlanNode relevantTargetsWithPath = new LoggingNode(
				new BulkedExternalLeftOuterJoin(allRelevantTargets, shaclSailConnection,
						path.getQuery("?a", "?c", null), false),
				"");

		PlanNode planNode = new NonUniqueTargetLang(relevantTargetsWithPath);

		if (printPlans) {
			String planAsGraphvizDot = getPlanAsGraphvizDot(planNode, shaclSailConnection);
			logger.info(planAsGraphvizDot);
		}

		return new EnrichWithShape(new LoggingNode(planNode, ""), this);

	}

	@Override
	public SourceConstraintComponent getSourceConstraintComponent() {
		return SourceConstraintComponent.UniqueLangConstraintComponent;
	}
}
