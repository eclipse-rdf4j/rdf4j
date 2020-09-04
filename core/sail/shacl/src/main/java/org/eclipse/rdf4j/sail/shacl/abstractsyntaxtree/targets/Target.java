package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.targets;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.Exportable;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.Targetable;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;

public abstract class Target implements Exportable, Targetable {

	public abstract IRI getPredicate();

	public abstract PlanNode getAdded(ConnectionsGroup connectionsGroup, ConstraintComponent.Scope scope);

	public abstract String getQueryFragment(String subjectVariable, String objectVariable,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner);

	public abstract PlanNode getTargetFilter(ConnectionsGroup connectionsGroup, PlanNode parent);
}
