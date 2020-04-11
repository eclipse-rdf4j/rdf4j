package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.paths;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

import java.util.Set;

public class SimplePath extends Path {

	IRI predicate;

	public SimplePath(IRI predicate) {
		this.predicate = predicate;
	}

	public IRI getPredicate() {
		return predicate;
	}

	@Override
	public Resource getId() {
		return predicate;
	}

	@Override
	public String toString() {
		return "SimplePath{ <" + predicate + "> }";
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.PATH, predicate);
	}
}
