package org.eclipse.rdf4j.sail.shacl.ast.paths;

import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclUnsupportedException;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeWrapper;

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
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.ONE_OR_MORE_PATH, oneOrMorePath.getId());
		oneOrMorePath.toModel(oneOrMorePath.getId(), null, model, cycleDetection);
	}

	@Override
	public PlanNode getAdded(ConnectionsGroup connectionsGroup, PlanNodeWrapper planNodeWrapper) {
		throw new ShaclUnsupportedException();
	}

	@Override
	public boolean isSupported() {
		return false;
	}

	@Override
	public Stream<StatementMatcher> getStatementMatcher(StatementMatcher.Variable subject,
			StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		throw new ShaclUnsupportedException();
	}

	@Override
	public String getTargetQueryFragment(StatementMatcher.Variable subject, StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		throw new ShaclUnsupportedException();
	}
}
