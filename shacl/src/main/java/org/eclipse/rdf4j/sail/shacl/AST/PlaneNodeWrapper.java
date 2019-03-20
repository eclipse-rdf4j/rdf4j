package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;

public interface PlaneNodeWrapper {

	PlanNode wrap(PlanNode planNode);
}
