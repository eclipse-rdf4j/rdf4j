package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents;

import java.util.Set;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

public class ClassConstraintComponent implements ConstraintComponent {

	Resource clazz;

	public ClassConstraintComponent(Resource clazz) {
		this.clazz = clazz;
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.CLASS, clazz);
	}
}
