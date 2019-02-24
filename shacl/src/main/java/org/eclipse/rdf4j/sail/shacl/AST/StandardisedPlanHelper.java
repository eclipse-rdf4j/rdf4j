package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.planNodes.BufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.BufferedSplitter;
import org.eclipse.rdf4j.sail.shacl.planNodes.BulkedExternalInnerJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.DirectTupleFromFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.FilterPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.InnerJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PushBasedLoggingNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PushBasedPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnionNode;

public class StandardisedPlanHelper {

	interface FilterAttacher {
		FilterPlanNode attachFilter(PlanNode parent);
	}

	static public PlanNode getGenericSingleObjectPlan(ShaclSailConnection shaclSailConnection, NodeShape nodeShape, FilterAttacher filterAttacher, PathPropertyShape pathPropertyShape, PlanNode overrideTargetNode) {
		if (overrideTargetNode != null) {
			PlanNode bulkedExternalInnerJoin = new LoggingNode(new BulkedExternalInnerJoin(overrideTargetNode, shaclSailConnection, pathPropertyShape.path.getQuery("?a", "?c", null), false), "");

			return new LoggingNode(filterAttacher.attachFilter(bulkedExternalInnerJoin).getFalseNode(UnBufferedPlanNode.class), "");
		}

		PlanNode addedByShape = new LoggingNode(nodeShape.getPlanAddedStatements(shaclSailConnection, nodeShape), "");

		BufferedSplitter bufferedSplitter = new BufferedSplitter(addedByShape);

		PlanNode addedByPath = new LoggingNode(shaclSailConnection.getCachedNodeFor(new Select(shaclSailConnection.getAddedStatements(), pathPropertyShape.path.getQuery("?a", "?c", null))), "");

		// this is essentially pushing the filter down below the join
		PlanNode invalidValuesDirectOnPath = new LoggingNode(filterAttacher.attachFilter(addedByPath).getFalseNode(UnBufferedPlanNode.class), "");


//		PlanNode top = new LoggingNode(new InnerJoin(bufferedSplitter.getPlanNode(), invalidValuesDirectOnPath, null, new PushBasedLoggingNode(discardedRight)), "");
		InnerJoin innerJoin = new InnerJoin(bufferedSplitter.getPlanNode(), invalidValuesDirectOnPath);
		PlanNode top = new LoggingNode(innerJoin.getJoined(BufferedPlanNode.class), "");
		PlanNode discardedRight = innerJoin.getDiscardedRight(BufferedPlanNode.class);



		if(!shaclSailConnection.stats.isBaseSailEmpty()) {
			if (nodeShape instanceof TargetClass) {
				PlanNode typeFilterPlan = new LoggingNode(((TargetClass) nodeShape).getTypeFilterPlan(shaclSailConnection, discardedRight), "");

				top = new LoggingNode(new UnionNode(top, typeFilterPlan), "");
			}

			PlanNode bulkedExternalInnerJoin = new LoggingNode(new BulkedExternalInnerJoin(bufferedSplitter.getPlanNode(), shaclSailConnection, pathPropertyShape.path.getQuery("?a", "?c", null), true), "");

			top = new LoggingNode(new UnionNode(top, bulkedExternalInnerJoin), "");
		}

		PlanNode invalidValues = new LoggingNode(filterAttacher.attachFilter(top).getFalseNode(UnBufferedPlanNode.class), "");


		return invalidValues;
	}


}
