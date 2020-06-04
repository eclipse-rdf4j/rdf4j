package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents;

import java.util.Set;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

public class MaxLengthConstraintComponent implements ConstraintComponent {

	long maxLength;

	public MaxLengthConstraintComponent(long maxLength) {
		this.maxLength = maxLength;
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.MAX_LENGTH,
				SimpleValueFactory.getInstance().createLiteral(maxLength + "", XMLSchema.INTEGER));
	}
}
