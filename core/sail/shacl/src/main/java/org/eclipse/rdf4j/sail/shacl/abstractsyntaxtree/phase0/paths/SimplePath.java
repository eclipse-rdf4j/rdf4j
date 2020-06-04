package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.paths;

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;

public class SimplePath extends Path {

	IRI predicate;

	public SimplePath(IRI predicate) {
		super(predicate);
		this.predicate = predicate;
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
	}
}
