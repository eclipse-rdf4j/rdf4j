package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

import java.util.Set;

public class MaxInclusiveConstraintComponent implements ConstraintComponent {

	Literal maxInclusive;

	public MaxInclusiveConstraintComponent(Literal maxInclusive) {
		this.maxInclusive = maxInclusive;
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.MAX_INCLUSIVE, maxInclusive);
	}
}
