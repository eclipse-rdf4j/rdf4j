package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.paths;

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
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.ShaclFeatureUnsupportedException;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNodeWrapper;

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
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.ONE_OR_MORE_PATH, oneOrMorePath.getId());
		oneOrMorePath.toModel(oneOrMorePath.getId(), null, model, exported);
	}

	@Override
	public PlanNode getAdded(ConnectionsGroup connectionsGroup, PlanNodeWrapper planNodeWrapper) {
		throw new ShaclFeatureUnsupportedException();
	}

	@Override
	public Stream<StatementPattern> getStatementPatterns(Var subject, Var object) {
		throw new ShaclFeatureUnsupportedException();
	}

	@Override
	public String getQueryFragment(Var subject, Var object) {
		throw new ShaclFeatureUnsupportedException();
	}
}
