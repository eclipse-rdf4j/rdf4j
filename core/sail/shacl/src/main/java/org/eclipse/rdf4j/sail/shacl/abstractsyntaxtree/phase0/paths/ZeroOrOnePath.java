package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.paths;

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryConnection;

public class ZeroOrOnePath extends Path {

	private final Path zeroOrOnePath;

	public ZeroOrOnePath(Resource id, Resource zeroOrOnePath, RepositoryConnection connection) {
		super(id);
		this.zeroOrOnePath = Path.buildPath(connection, zeroOrOnePath);

	}

	@Override
	public String toString() {
		return "ZeroOrOnePath{ " + zeroOrOnePath + " }";
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.ZERO_OR_ONE_PATH, zeroOrOnePath.getId());
		zeroOrOnePath.toModel(zeroOrOnePath.getId(), model, exported);
	}
}
