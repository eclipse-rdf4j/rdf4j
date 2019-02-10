package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;

public class TupleLengthFilter extends FilterPlanNode{
	private final int length;
	private final boolean exact;


	public TupleLengthFilter(PlanNode parent, PushBasedPlanNode trueNode, PushBasedPlanNode falseNode, int length, boolean exact) {
		super(parent, trueNode, falseNode);
		this.length = length;
		this.exact = exact;
	}

	@Override
	boolean checkTuple(Tuple t) {
		if(t.line.size() >= length){
			if(exact){
				return t.line.size() == length;
			}else {
				return true;
			}
		}

		return false;
	}
}
