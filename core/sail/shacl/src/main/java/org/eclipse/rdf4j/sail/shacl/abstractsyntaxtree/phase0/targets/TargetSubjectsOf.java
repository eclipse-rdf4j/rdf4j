package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.targets;

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

public class TargetSubjectsOf extends Target {

	private final Set<IRI> targetSubjectsOf;

	public TargetSubjectsOf(Set<IRI> targetSubjectsOf) {
		this.targetSubjectsOf = targetSubjectsOf;
		assert !this.targetSubjectsOf.isEmpty();

	}

	@Override
	public IRI getPredicate() {
		return SHACL.TARGET_SUBJECTS_OF;
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		targetSubjectsOf.forEach(t -> {
			model.add(subject, getPredicate(), t);
		});
	}
}
