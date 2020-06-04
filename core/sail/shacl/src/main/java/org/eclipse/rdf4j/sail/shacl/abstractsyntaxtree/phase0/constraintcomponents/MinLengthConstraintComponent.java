package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents;

import java.util.Set;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

public class MinLengthConstraintComponent implements ConstraintComponent {

	long minLength;

	public MinLengthConstraintComponent(long minLength) {
		this.minLength = minLength;
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.MIN_LENGTH,
				SimpleValueFactory.getInstance().createLiteral(minLength + "", XMLSchema.INTEGER));
	}
}
