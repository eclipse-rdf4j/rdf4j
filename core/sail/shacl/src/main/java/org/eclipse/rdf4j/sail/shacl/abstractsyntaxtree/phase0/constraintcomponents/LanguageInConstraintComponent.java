package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LanguageInConstraintComponent implements ConstraintComponent {

	private final Set<String> languageIn;
	private final Resource id;

	public LanguageInConstraintComponent(SailRepositoryConnection connection, Resource languageIn) {
		this.id = languageIn;
		this.languageIn = toList(connection, languageIn).stream().map(Value::stringValue).collect(Collectors.toSet());
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.LANGUAGE_IN, id);
		RDFCollections.asRDF(new TreeSet<>(languageIn).stream()
				.map(l -> SimpleValueFactory.getInstance().createLiteral(l))
				.collect(Collectors.toList()), id, model);
	}

	static List<Value> toList(SailRepositoryConnection connection, Resource orList) {
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
