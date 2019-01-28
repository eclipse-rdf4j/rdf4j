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

	static public PlanNode getGenericSingleObjectPlan(ShaclSailConnection shaclSailConnection, NodeShape nodeShape, FilterAttacher filterAttacher, PathPropertyShape pathPropertyShape) {
		PlanNode addedByShape = new LoggingNode(nodeShape.getPlanAddedStatements(shaclSailConnection, nodeShape));

		BufferedSplitter bufferedSplitter = new BufferedSplitter(addedByShape);

		PlanNode addedByPath = new LoggingNode(new Select(shaclSailConnection.getAddedStatements(), pathPropertyShape.path.getQuery()));

		// this is essentially pushing the filter down below the join
		DirectTupleFromFilter invalidValuesDirectOnPath = new DirectTupleFromFilter();

		filterAttacher.attachFilter(addedByPath, null, new PushBasedLoggingNode(invalidValuesDirectOnPath));


		BufferedTupleFromFilter discardedRight = new BufferedTupleFromFilter();


		PlanNode top = new LoggingNode(new InnerJoin(bufferedSplitter.getPlanNode(), invalidValuesDirectOnPath, null, discardedRight));


		if (nodeShape instanceof TargetClass) {
			PlanNode typeFilterPlan = new LoggingNode(((TargetClass) nodeShape).getTypeFilterPlan(shaclSailConnection.getPreviousStateConnection(), discardedRight));

			top = new LoggingNode(new UnionNode(top, typeFilterPlan));
		}

		PlanNode bulkedEcternalInnerJoin = new LoggingNode(new BulkedExternalInnerJoin(bufferedSplitter.getPlanNode(), shaclSailConnection.getPreviousStateConnection(), pathPropertyShape.path.getQuery()));

		top = new LoggingNode(new UnionNode(top, bulkedEcternalInnerJoin));

		DirectTupleFromFilter invalidValues = new DirectTupleFromFilter();
		filterAttacher.attachFilter(top, null, invalidValues);


		return new LoggingNode(invalidValues);
	}


}
