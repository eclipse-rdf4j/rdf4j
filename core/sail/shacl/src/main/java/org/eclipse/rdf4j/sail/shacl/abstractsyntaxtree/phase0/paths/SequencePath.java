package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.paths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;

public class SequencePath extends Path {

	private final List<Path> sequence;

	public SequencePath(Resource id, RepositoryConnection connection) {
		super(id);
		sequence = toList(connection, id).stream()
				.map(p -> Path.buildPath(connection, (Resource) p))
				.collect(Collectors.toList());

	}

	@Override
	public String toString() {
		return "SequencePath{ " + Arrays.toString(sequence.toArray()) + " }";
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		sequence.forEach(p -> p.toModel(p.getId(), model, exported));

		List<Resource> values = sequence.stream().map(Path::getId).collect(Collectors.toList());

		toRdf(id, model, values);
	}

	private void toRdf(Resource current, Model model, List<Resource> values) {
		ValueFactory vf = SimpleValueFactory.getInstance();
		Iterator<Resource> iter = values.iterator();
		while (iter.hasNext()) {
			Resource value = iter.next();
			model.add(current, RDF.FIRST, value);

			if (iter.hasNext()) {
				Resource next = vf.createBNode();
				model.add(current, RDF.REST, next);
				current = next;
			} else {
				model.add(current, RDF.REST, RDF.NIL);
			}
		}
	}

	static List<Value> toList(RepositoryConnection connection, Resource orList) {
		List<Value> ret = new ArrayList<>();
		while (!orList.equals(RDF.NIL)) {
			try (Stream<Statement> stream = connection.getStatements(orList, RDF.FIRST, null).stream()) {
				Value value = stream.map(Statement::getObject).findAny().get();
				ret.add(value);
			}

			try (Stream<Statement> stream = connection.getStatements(orList, RDF.REST, null).stream()) {
				orList = stream.map(Statement::getObject).map(v -> (Resource) v).findAny().get();
			}

		}

		return ret;

	}

}
