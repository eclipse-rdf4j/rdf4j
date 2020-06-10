package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.targets;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.Exportable;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.Targetable;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.tempPlanNodes.TupleValidationPlanNode;

public abstract class Target implements Exportable, Targetable {

	public abstract IRI getPredicate();

	public abstract TupleValidationPlanNode getAdded(ConnectionsGroup connectionsGroup);

	public abstract String getQueryFragment(String subjectVariable, String objectVariable,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner);
}
