package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.paths;

import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.planNodes.PlaneNodeWrapper;

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
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.ALTERNATIVE_PATH, alternativePath.getId());
		alternativePath.toModel(alternativePath.getId(), null, model, exported);
	}

	@Override
	public PlanNode getAdded(ConnectionsGroup connectionsGroup, PlaneNodeWrapper planeNodeWrapper) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Stream<StatementPattern> getStatementPatterns(Var subject, Var object) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getQueryFragment(Var subject, Var object) {
		throw new UnsupportedOperationException();
	}
}
