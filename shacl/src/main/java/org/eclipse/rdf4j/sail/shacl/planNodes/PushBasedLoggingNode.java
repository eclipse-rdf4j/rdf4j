package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

import java.util.List;

public class PushBasedLoggingNode implements SupportsParentProvider, PushBasedPlanNode, PlanNode {

	private final PushBasedPlanNode parent;
	private List<PlanNode> parents;

	public PushBasedLoggingNode(PushBasedPlanNode parent) {
		this.parent = parent;
	}


	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		throw new IllegalStateException();
	}

	@Override
	public int depth() {
		return ((PlanNode)parent).depth();
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		((PlanNode)parent).getPlanAsGraphvizDot(stringBuilder);
	}

	@Override
	public String getId() {
		return ((PlanNode)parent).getId();
	}

	@Override
	public IteratorData getIteratorDataType() {
		return ((PlanNode)parent).getIteratorDataType();
	}

	@Override
	public void push(Tuple t) {
		if(LoggingNode.loggingEnabled){
			System.out.println(leadingSpace() + parent.getClass().getSimpleName() + ".next(): " + " " + t.toString());
		}
		parent.push(t);
	}

	@Override
	public void parentIterator(CloseableIteration<Tuple, SailException> iterator) {
		this.parent.parentIterator(iterator);
	}

	@Override
	public void receiveParentProvider(ParentProvider parentProvider) {

		this.parents = parentProvider.parent();
		((SupportsParentProvider)this.parent).receiveParentProvider(parentProvider);
	}

	private String leadingSpace() {
		StringBuilder ret = new StringBuilder();
		int depth = depth();
		while (--depth > 0) {
			ret.append("    ");
		}
		return ret.toString();
	}
}
