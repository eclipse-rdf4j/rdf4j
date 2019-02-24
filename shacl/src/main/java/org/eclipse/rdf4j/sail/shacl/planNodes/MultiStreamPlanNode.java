package org.eclipse.rdf4j.sail.shacl.planNodes;

public interface MultiStreamPlanNode {
	void init();

	void close();

	boolean incrementIterator();
}
