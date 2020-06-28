package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

import java.util.Set;

public class MaxLengthConstraintComponent extends AbstractConstraintComponent {

	long maxLength;

	public MaxLengthConstraintComponent(long maxLength) {
		this.maxLength = maxLength;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.MAX_LENGTH,
				SimpleValueFactory.getInstance().createLiteral(maxLength + "", XMLSchema.INTEGER));
	}
}
