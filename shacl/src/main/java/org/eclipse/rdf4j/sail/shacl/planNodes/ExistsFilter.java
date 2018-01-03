package org.eclipse.rdf4j.sail.shacl.planNodes;


import org.eclipse.rdf4j.sail.shacl.plan.PlanNode;
import org.eclipse.rdf4j.sail.shacl.plan.Tuple;

public class ExistsFilter extends FilterPlanNode{

	public ExistsFilter(PlanNode parent, PushBasedPlanNode trueNode, PushBasedPlanNode falseNode) {
		super(parent, trueNode, falseNode);
	}

	@Override
	boolean checkTuple(Tuple t) {
		return false;
	}
}
