package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.targets;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

import java.util.Set;

public class TargetObjectsOf extends Target {

	private final Set<IRI> targetObjectsOf;

	public TargetObjectsOf(Set<IRI> targetObjectsOf) {
		this.targetObjectsOf = targetObjectsOf;
		assert !this.targetObjectsOf.isEmpty();

	}

	@Override
	public IRI getPredicate() {
		return SHACL.TARGET_SUBJECTS_OF;
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		targetObjectsOf.forEach(t -> {
			model.add(subject, getPredicate(), t);
		});
	}
}
