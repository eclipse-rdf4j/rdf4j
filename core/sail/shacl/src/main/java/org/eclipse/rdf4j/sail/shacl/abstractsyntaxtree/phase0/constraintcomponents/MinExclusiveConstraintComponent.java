package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents;

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

public class MinExclusiveConstraintComponent extends AbstractConstraintComponent {

	Literal minExclusive;

	public MinExclusiveConstraintComponent(Literal minExclusive) {
		this.minExclusive = minExclusive;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.MIN_EXCLUSIVE, minExclusive);
	}
}
