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

public class ZeroOrMorePath extends Path {

	private final Path zeroOrMorePath;

	public ZeroOrMorePath(Resource id, Resource zeroOrMorePath, RepositoryConnection connection) {
		super(id);
		this.zeroOrMorePath = Path.buildPath(connection, zeroOrMorePath);

	}

	@Override
	public String toString() {
		return "ZeroOrMorePath{ " + zeroOrMorePath + " }";
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.ZERO_OR_MORE_PATH, zeroOrMorePath.getId());
		zeroOrMorePath.toModel(zeroOrMorePath.getId(), model, exported);
	}

	@Override
	public TupleValidationPlanNode getAdded(ConnectionsGroup connectionsGroup, PlaneNodeWrapper planeNodeWrapper) {
		return null;
	}
}
