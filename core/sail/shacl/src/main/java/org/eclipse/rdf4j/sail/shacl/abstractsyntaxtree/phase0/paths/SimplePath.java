package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.paths;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;

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
}
