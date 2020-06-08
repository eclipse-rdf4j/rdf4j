package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.targets;

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;

public class TargetObjectsOf extends Target {

	private final Set<IRI> targetObjectsOf;

	public TargetObjectsOf(Set<IRI> targetObjectsOf) {
		this.targetObjectsOf = targetObjectsOf;
		assert !this.targetObjectsOf.isEmpty();

	}

	@Override
	public IRI getPredicate() {
		return SHACL.TARGET_OBJECTS_OF;
	}

	@Override
	public PlanNode getAdded(ConnectionsGroup connectionsGroup) {
		return null;
	}

	@Override
	public String getQueryFragment(String subjectVariable, String objectVariable,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		return null;
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		targetObjectsOf.forEach(t -> {
			model.add(subject, getPredicate(), t);
		});
	}
}
