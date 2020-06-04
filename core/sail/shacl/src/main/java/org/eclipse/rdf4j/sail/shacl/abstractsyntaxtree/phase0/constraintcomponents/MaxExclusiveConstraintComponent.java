package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents;

import java.util.Set;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

public class MaxExclusiveConstraintComponent implements ConstraintComponent {

	Literal maxExclusive;

	public MaxExclusiveConstraintComponent(Literal maxExclusive) {
		this.maxExclusive = maxExclusive;
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.MAX_EXCLUSIVE, maxExclusive);
	}
}
