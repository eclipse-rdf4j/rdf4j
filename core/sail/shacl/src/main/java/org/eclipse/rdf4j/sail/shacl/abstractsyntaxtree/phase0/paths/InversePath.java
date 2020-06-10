package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.paths;

import java.util.Set;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.AST.PlaneNodeWrapper;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.tempPlanNodes.TupleValidationPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;

public class InversePath extends Path {

	private final Path inversePath;

	public InversePath(Resource id, Resource inversePath, RepositoryConnection connection) {
		super(id);
		this.inversePath = Path.buildPath(connection, inversePath);

	}

	@Override
	public String toString() {
		return "InversePath{ " + inversePath + " }";
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.INVERSE_PATH, inversePath.getId());
		inversePath.toModel(inversePath.getId(), model, exported);
	}

	@Override
	public TupleValidationPlanNode getAdded(ConnectionsGroup connectionsGroup, PlaneNodeWrapper planeNodeWrapper) {
		return null;
	}
}
