package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.planNodes.BufferedSplitter;
import org.eclipse.rdf4j.sail.shacl.planNodes.BufferedTupleFromFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.BulkedExternalInnerJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.DirectTupleFromFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.InnerJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PushBasedLoggingNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PushBasedPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnionNode;

public class StandardisedPlanHelper {

	interface FilterAttacher {
		void attachFilter(PlanNode parent, PushBasedPlanNode trueNode, PushBasedPlanNode falseNode);
	}

	static public PlanNode getGenericSingleObjectPlan(ShaclSailConnection shaclSailConnection, NodeShape nodeShape, FilterAttacher filterAttacher, PathPropertyShape pathPropertyShape, PlanNode overrideTargetNode) {
		if (overrideTargetNode != null) {
			PlanNode bulkedExternalInnerJoin = new LoggingNode(new BulkedExternalInnerJoin(overrideTargetNode, shaclSailConnection, pathPropertyShape.path.getQuery("?a", "?c"), false), "");

			DirectTupleFromFilter invalidValues = new DirectTupleFromFilter();
			filterAttacher.attachFilter(bulkedExternalInnerJoin, null, new PushBasedLoggingNode(invalidValues));

			return invalidValues;
		}

		PlanNode addedByShape = new LoggingNode(nodeShape.getPlanAddedStatements(shaclSailConnection, nodeShape), "");

		BufferedSplitter bufferedSplitter = new BufferedSplitter(addedByShape);

		PlanNode addedByPath = new LoggingNode(shaclSailConnection.getCachedNodeFor(new Select(shaclSailConnection.getAddedStatements(), pathPropertyShape.path.getQuery("?a", "?c"))), "");

		// this is essentially pushing the filter down below the join
		DirectTupleFromFilter invalidValuesDirectOnPath = new DirectTupleFromFilter();

		filterAttacher.attachFilter(addedByPath, null, new PushBasedLoggingNode(invalidValuesDirectOnPath));


		BufferedTupleFromFilter discardedRight = new BufferedTupleFromFilter();

		PlanNode top = new LoggingNode(new InnerJoin(bufferedSplitter.getPlanNode(), invalidValuesDirectOnPath, null, new PushBasedLoggingNode(discardedRight)), "");

		if(!shaclSailConnection.stats.baseSailEmpty) {
			if (nodeShape instanceof TargetClass) {
				PlanNode typeFilterPlan = new LoggingNode(((TargetClass) nodeShape).getTypeFilterPlan(shaclSailConnection, discardedRight), "");

				top = new LoggingNode(new UnionNode(top, typeFilterPlan), "");
			}

			PlanNode bulkedExternalInnerJoin = new LoggingNode(new BulkedExternalInnerJoin(bufferedSplitter.getPlanNode(), shaclSailConnection, pathPropertyShape.path.getQuery("?a", "?c"), true), "");

			top = new LoggingNode(new UnionNode(top, bulkedExternalInnerJoin), "");
		}

		DirectTupleFromFilter invalidValues = new DirectTupleFromFilter();
		filterAttacher.attachFilter(top, null, new PushBasedLoggingNode(invalidValues));


		return invalidValues;
	}


}
