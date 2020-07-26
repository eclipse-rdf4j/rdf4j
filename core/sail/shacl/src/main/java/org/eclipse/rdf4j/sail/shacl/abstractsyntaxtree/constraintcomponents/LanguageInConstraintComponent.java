package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents;

import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.HelperTool;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.ShaclFeatureUnsupportedException;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.FilterPlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.LanguageInFilter;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;

public class LanguageInConstraintComponent extends SimpleAbstractConstraintComponent {

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

	@Override
	String getFilter(String varName, boolean negated) {
		throw new ShaclFeatureUnsupportedException();
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.LanguageInConstraintComponent;
	}

	@Override
	Function<PlanNode, FilterPlanNode> getFilterAttacher() {
		return (parent) -> new LanguageInFilter(parent, languageIn);
	}

}
