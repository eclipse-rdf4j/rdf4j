package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

import java.util.Set;

public class MinInclusiveConstraintComponent extends AbstractConstraintComponent {

	Literal minInclusive;

	public MinInclusiveConstraintComponent(Literal minInclusive) {
		this.minInclusive = minInclusive;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.MIN_INCLUSIVE, minInclusive);
	}
}
