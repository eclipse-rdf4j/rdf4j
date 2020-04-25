package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.targets;

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

public class TargetClass extends Target {
	private final Set<Resource> targetClass;

	public TargetClass(Set<Resource> targetClass) {
		this.targetClass = targetClass;
		assert !this.targetClass.isEmpty();

	}

	@Override
	public IRI getPredicate() {
		return SHACL.TARGET_CLASS;
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		targetClass.forEach(t -> {
			model.add(subject, getPredicate(), t);
		});
	}
}
