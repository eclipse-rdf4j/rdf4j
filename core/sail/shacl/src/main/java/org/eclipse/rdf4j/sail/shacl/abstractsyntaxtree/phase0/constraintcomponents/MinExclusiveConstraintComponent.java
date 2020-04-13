package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

import java.util.Set;

public class MinExclusiveConstraintComponent implements ConstraintComponent {

	Literal minExclusive;

	public MinExclusiveConstraintComponent(Literal minExclusive) {
		this.minExclusive = minExclusive;
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.MIN_EXCLUSIVE, minExclusive);
	}
}
