package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

import java.util.Set;

public class ClassConstraintComponent implements ConstraintComponent {

	Resource clazz;

	public ClassConstraintComponent(ConstraintComponent parent, Resource clazz) {
		this.clazz = clazz;
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.CLASS, clazz);
	}
}
