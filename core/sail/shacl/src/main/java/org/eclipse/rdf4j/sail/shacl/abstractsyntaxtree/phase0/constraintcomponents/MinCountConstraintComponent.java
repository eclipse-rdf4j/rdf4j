package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

import java.util.Set;

public class MinCountConstraintComponent implements ConstraintComponent {

	long minCount;

	public MinCountConstraintComponent(ConstraintComponent parent, long minCount) {
		this.minCount = minCount;
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.MIN_COUNT,
			SimpleValueFactory.getInstance().createLiteral(minCount + "", XMLSchema.INTEGER));
	}
}
