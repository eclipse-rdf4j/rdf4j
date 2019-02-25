package org.eclipse.rdf4j.sail.shacl.planNodes;

public interface PushablePlanNode extends PlanNode {

	void push(Tuple tuple);

	boolean isClosed();
}
