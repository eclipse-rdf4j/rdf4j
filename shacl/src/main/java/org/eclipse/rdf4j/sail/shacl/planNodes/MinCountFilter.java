package org.eclipse.rdf4j.sail.shacl.planNodes;


import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.sail.shacl.plan.PlanNode;
import org.eclipse.rdf4j.sail.shacl.plan.Tuple;

public class MinCountFilter extends FilterPlanNode {


	private final long minCount;

	public MinCountFilter(PlanNode parent, PushBasedPlanNode trueNode, PushBasedPlanNode falseNode, long minCount) {
		super(parent, trueNode, falseNode);
		this.minCount = minCount;
	}

	@Override
	boolean checkTuple(Tuple t) {
		Literal literal = (Literal) t.line.get(1);
		return literal.longValue() >= minCount;
	}

}
