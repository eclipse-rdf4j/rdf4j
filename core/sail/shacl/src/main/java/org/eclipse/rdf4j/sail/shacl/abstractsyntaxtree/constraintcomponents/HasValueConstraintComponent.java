package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

import java.util.Set;

public class HasValueConstraintComponent extends AbstractConstraintComponent {

	Value hasValue;

	public HasValueConstraintComponent(Value hasValue) {
		this.hasValue = hasValue;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.HAS_VALUE, hasValue);
	}
}
