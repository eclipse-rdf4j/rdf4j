package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.HelperTool;

public class LanguageInConstraintComponent extends AbstractConstraintComponent {

	private final Set<String> languageIn;

	public LanguageInConstraintComponent(RepositoryConnection connection,
			Resource languageIn) {
		super(languageIn);
		this.languageIn = HelperTool.toList(connection, languageIn, Value.class)
				.stream()
				.map(Value::stringValue)
				.collect(Collectors.toSet());
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.LANGUAGE_IN, getId());
		HelperTool.listToRdf(new TreeSet<>(languageIn).stream()
				.map(l -> SimpleValueFactory.getInstance().createLiteral(l))
				.collect(Collectors.toList()), getId(), model);
	}

}
