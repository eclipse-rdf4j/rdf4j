package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;

public class LessThanConstraintComponent extends AbstractConstraintComponent {

	IRI predicate;

	public LessThanConstraintComponent(IRI predicate) {
		this.predicate = predicate;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.LESS_THAN, this.predicate);
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.LessThanConstraintComponent;
	}

	@Override
	public ConstraintComponent deepClone() {
		return new LessThanConstraintComponent(predicate);
	}
}
