package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.paths;

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

public class InversePath extends Path {

	private final IRI inversePath;

	public InversePath(Resource id, IRI inversePath) {
		super(id);
		this.inversePath = inversePath;
	}

	@Override
	public String toString() {
		return "InversePath{ <" + inversePath + "> }";
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.PATH, id);
		model.add(id, SHACL.INVERSE_PATH, inversePath);
	}
}
