package org.eclipse.rdf4j.sail.shacl.planNodes;


import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.sail.shacl.plan.PlanNode;
import org.eclipse.rdf4j.sail.shacl.plan.Tuple;

public class MaxCountFilter extends FilterPlanNode {

	private final long maxCount;

	public MaxCountFilter(PlanNode parent, PushBasedPlanNode trueNode, PushBasedPlanNode falseNode, long maxCount) {
		super(parent, trueNode, falseNode);
		this.maxCount = maxCount;
	}

	@Override
	boolean checkTuple(Tuple t) {
		Literal literal = (Literal) t.line.get(1);
		return literal.longValue() <= maxCount;
	}

}
