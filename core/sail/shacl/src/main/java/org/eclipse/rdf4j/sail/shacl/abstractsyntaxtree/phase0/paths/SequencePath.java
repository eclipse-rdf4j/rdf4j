package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.paths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryConnection;

public class SequencePath extends Path {

	private final List<Path> sequence;

	public SequencePath(Resource id, RepositoryConnection connection) {
		super(id);
		sequence = new ArrayList<>();
	}

	@Override
	public String toString() {
		return "SequencePath{ " + Arrays.toString(sequence.toArray()) + " }";
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {

	}
}
