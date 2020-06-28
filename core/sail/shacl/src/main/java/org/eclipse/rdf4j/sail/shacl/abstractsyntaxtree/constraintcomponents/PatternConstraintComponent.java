package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

import java.util.Set;

public class PatternConstraintComponent extends AbstractConstraintComponent {

	String pattern;
	String flags;

	public PatternConstraintComponent(String pattern, String flags) {
		this.pattern = pattern;
		this.flags = flags;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.PATTERN, SimpleValueFactory.getInstance().createLiteral(pattern));
		if (flags != null) {
			model.add(subject, SHACL.FLAGS, SimpleValueFactory.getInstance().createLiteral(flags));
		}

	}
}
