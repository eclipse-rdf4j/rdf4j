package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryConnection;

public class LanguageInConstraintComponent extends AbstractConstraintComponent {

	private final Set<String> languageIn;

	public LanguageInConstraintComponent(RepositoryConnection connection,
			Resource languageIn) {
		super(languageIn);
		this.languageIn = toList(connection, languageIn).stream().map(Value::stringValue).collect(Collectors.toSet());
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.LANGUAGE_IN, getId());
		RDFCollections.asRDF(new TreeSet<>(languageIn).stream()
				.map(l -> SimpleValueFactory.getInstance().createLiteral(l))
				.collect(Collectors.toList()), getId(), model);
	}

}
