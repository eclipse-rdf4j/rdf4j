package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents;

import java.util.Set;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

public class MaxCountConstraintComponent extends AbstractConstraintComponent {

	long maxCount;

	public MaxCountConstraintComponent(long maxCount) {
		this.maxCount = maxCount;
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.MAX_COUNT,
				SimpleValueFactory.getInstance().createLiteral(maxCount + "", XMLSchema.INTEGER));
	}
}
