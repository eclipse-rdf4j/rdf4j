package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.paths;

import java.util.Set;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryConnection;

public class OneOrMorePath extends Path {

	private final Path oneOrMorePath;

	public OneOrMorePath(Resource id, Resource oneOrMorePath, RepositoryConnection connection) {
		super(id);
		this.oneOrMorePath = Path.buildPath(connection, oneOrMorePath);

	}

	@Override
	public String toString() {
		return "ZeroOrOnePath{ " + oneOrMorePath + " }";
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.ONE_OR_MORE_PATH, oneOrMorePath.getId());
		oneOrMorePath.toModel(oneOrMorePath.getId(), model, exported);
	}
}
