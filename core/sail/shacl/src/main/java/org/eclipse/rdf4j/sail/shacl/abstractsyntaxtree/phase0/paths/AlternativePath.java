package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.paths;

import java.util.Set;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.AST.PlaneNodeWrapper;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;

public class AlternativePath extends Path {

	private final Path alternativePath;

	public AlternativePath(Resource id, Resource alternativePath, RepositoryConnection connection) {
		super(id);
		this.alternativePath = Path.buildPath(connection, alternativePath);

	}

	@Override
	public String toString() {
		return "AlternativePath{ " + alternativePath + " }";
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.ALTERNATIVE_PATH, alternativePath.getId());
		alternativePath.toModel(alternativePath.getId(), model, exported);
	}

	@Override
	public PlanNode getAdded(ConnectionsGroup connectionsGroup, PlaneNodeWrapper planeNodeWrapper) {
		return null;
	}
}
