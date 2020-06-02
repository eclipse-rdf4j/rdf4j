package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.paths;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.Exportable;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.Identifiable;

public abstract class Path implements Identifiable, Exportable {

	Resource id;

	public Path(Resource id) {
		this.id = id;
	}

	@Override
	public Resource getId() {
		return id;
	}

	static public Path buildPath(RepositoryConnection connection, Resource id) {
		if (id == null)
			return null;

		if (id instanceof BNode) {
			List<Statement> collect = connection.getStatements(id, null, null, true)
					.stream()
					.collect(Collectors.toList());

			for (Statement statement : collect) {
				IRI pathType = statement.getPredicate();

				switch (pathType.toString()) {
				case "http://www.w3.org/ns/shacl#inversePath":
					return new InversePath(id, (IRI) statement.getObject());
				default:
					break;
				}

			}

			throw new UnsupportedOperationException();

		} else {
			return new SimplePath((IRI) id);
		}

	}

}
