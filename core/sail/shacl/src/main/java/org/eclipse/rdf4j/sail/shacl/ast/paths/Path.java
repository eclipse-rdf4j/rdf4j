package org.eclipse.rdf4j.sail.shacl.ast.paths;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.ast.Exportable;
import org.eclipse.rdf4j.sail.shacl.ast.Identifiable;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclUnsupportedException;
import org.eclipse.rdf4j.sail.shacl.ast.Targetable;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeWrapper;

public abstract class Path implements Identifiable, Exportable, Targetable {

	Resource id;

	public Path(Resource id) {
		this.id = id;
	}

	@Override
	public Resource getId() {
		return id;
	}

	static public Path buildPath(RepositoryConnection connection, Resource id) {
		if (id == null) {
			return null;
		}

		if (id.isBNode()) {
			List<Statement> collect = connection.getStatements(id, null, null, true)
					.stream()
					.collect(Collectors.toList());

			for (Statement statement : collect) {
				IRI pathType = statement.getPredicate();

				switch (pathType.toString()) {
				case "http://www.w3.org/ns/shacl#inversePath":
					return new InversePath(id, (Resource) statement.getObject(), connection);
				case "http://www.w3.org/ns/shacl#alternativePath":
					return new AlternativePath(id, (Resource) statement.getObject(), connection);
				case "http://www.w3.org/ns/shacl#zeroOrMorePath":
					return new ZeroOrMorePath(id, (Resource) statement.getObject(), connection);
				case "http://www.w3.org/ns/shacl#oneOrMorePath":
					return new OneOrMorePath(id, (Resource) statement.getObject(), connection);
				case "http://www.w3.org/ns/shacl#zeroOrOnePath":
					return new ZeroOrOnePath(id, (Resource) statement.getObject(), connection);
				case "http://www.w3.org/1999/02/22-rdf-syntax-ns#first":
					return new SequencePath(id, connection);
				default:
					break;
				}

			}

			throw new ShaclUnsupportedException();

		} else {
			return new SimplePath((IRI) id);
		}

	}

	public abstract PlanNode getAdded(ConnectionsGroup connectionsGroup,
			PlanNodeWrapper planNodeWrapper);

	/**
	 *
	 * @return true if feature is currently supported by the ShaclSail
	 */
	public abstract boolean isSupported();
}
