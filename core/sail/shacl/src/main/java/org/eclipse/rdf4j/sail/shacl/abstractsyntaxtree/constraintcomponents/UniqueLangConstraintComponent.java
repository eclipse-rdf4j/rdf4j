package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents;

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;

public class UniqueLangConstraintComponent extends AbstractConstraintComponent {

	public UniqueLangConstraintComponent() {
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.UNIQUE_LANG, SimpleValueFactory.getInstance().createLiteral(true));
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.UniqueLangConstraintComponent;
	}
}
