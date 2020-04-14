package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

import java.util.Set;

public class MinLengthConstraintComponent implements ConstraintComponent {

	long minLength;

	public MinLengthConstraintComponent(ConstraintComponent parent, long minLength) {
		this.minLength = minLength;
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.MIN_LENGTH,
			SimpleValueFactory.getInstance().createLiteral(minLength + "", XMLSchema.INTEGER));
	}
}
