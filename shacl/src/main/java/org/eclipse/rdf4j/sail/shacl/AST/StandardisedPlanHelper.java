/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.planNodes.BufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.BulkedExternalInnerJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.FilterPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.InnerJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnionNode;

public class StandardisedPlanHelper {

	interface FilterAttacher {
		FilterPlanNode attachFilter(PlanNode parent);
	}

	static public PlanNode getGenericSingleObjectPlan(ShaclSailConnection shaclSailConnection, NodeShape nodeShape,
			FilterAttacher filterAttacher, PathPropertyShape pathPropertyShape, PlanNode overrideTargetNode) {
		if (overrideTargetNode != null) {
			PlanNode bulkedExternalInnerJoin = new LoggingNode(new BulkedExternalInnerJoin(overrideTargetNode,
					shaclSailConnection, pathPropertyShape.path.getQuery("?a", "?c", null), false), "");

			return new LoggingNode(
					filterAttacher.attachFilter(bulkedExternalInnerJoin).getFalseNode(UnBufferedPlanNode.class), "");
		}

		PlanNode addedByPath = new LoggingNode(pathPropertyShape.getPlanAddedStatements(shaclSailConnection, nodeShape),
				"");

		// this is essentially pushing the filter down below the join
		PlanNode invalidValuesDirectOnPath = new LoggingNode(
				filterAttacher.attachFilter(addedByPath).getFalseNode(UnBufferedPlanNode.class), "");

		InnerJoin innerJoin = new InnerJoin(
				new LoggingNode(nodeShape.getPlanAddedStatements(shaclSailConnection, nodeShape), ""),
				invalidValuesDirectOnPath);

		if (shaclSailConnection.stats.isBaseSailEmpty()) {
			return new LoggingNode(innerJoin.getJoined(UnBufferedPlanNode.class), "");

		} else {

			PlanNode top = new LoggingNode(innerJoin.getJoined(BufferedPlanNode.class), "");

			PlanNode discardedRight = innerJoin.getDiscardedRight(BufferedPlanNode.class);

			PlanNode typeFilterPlan = new LoggingNode(nodeShape.getTargetFilter(shaclSailConnection, discardedRight),
					"");

			top = new LoggingNode(new UnionNode(top, typeFilterPlan), "");

			PlanNode bulkedExternalInnerJoin = new LoggingNode(new BulkedExternalInnerJoin(
					new LoggingNode(nodeShape.getPlanAddedStatements(shaclSailConnection, nodeShape), ""),
					shaclSailConnection, pathPropertyShape.path.getQuery("?a", "?c", null), true), "");

			top = new LoggingNode(new UnionNode(top, bulkedExternalInnerJoin), "");

			return new LoggingNode(filterAttacher.attachFilter(top).getFalseNode(UnBufferedPlanNode.class), "");

		}

	}

}
