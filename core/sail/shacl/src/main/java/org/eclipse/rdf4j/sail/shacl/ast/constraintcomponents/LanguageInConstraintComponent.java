package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclAstLists;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.FilterPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.LanguageInFilter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;

public class LanguageInConstraintComponent extends SimpleAbstractConstraintComponent {

	private final List<String> languageIn;
	private final ArrayList<String> languageRanges;
	private final Set<String> lowerCaseLanguageIn;

	public LanguageInConstraintComponent(RepositoryConnection connection,
			Resource languageIn) {
		super(languageIn);
		this.languageIn = ShaclAstLists.toList(connection, languageIn, Value.class)
				.stream()
				.map(Value::stringValue)
				.collect(Collectors.toList());

		this.languageRanges = new ArrayList<>(new HashSet<>(this.languageIn));

		this.lowerCaseLanguageIn = this.languageIn.stream()
				.filter(l -> !l.contains("*"))
				.map(String::toLowerCase)
				.collect(Collectors.toSet());
	}

	private LanguageInConstraintComponent(LanguageInConstraintComponent languageInConstraintComponent) {
		super(languageInConstraintComponent.getId());
		this.languageIn = languageInConstraintComponent.languageIn;
		this.languageRanges = languageInConstraintComponent.languageRanges;
		this.lowerCaseLanguageIn = languageInConstraintComponent.lowerCaseLanguageIn;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.LANGUAGE_IN, getId());

		if (!model.contains(getId(), null, null)) {
			ShaclAstLists.listToRdf(languageIn.stream()
					.map(Values::literal)
					.collect(Collectors.toList()), getId(), model);
		}
	}

	@Override
	String getSparqlFilterExpression(String varName, boolean negated) {
		if (negated) {
			return "lang(?" + varName + ") IN (" + getLangSetAsList() + ")";
		} else {
			return "lang(?" + varName + ") NOT IN (" + getLangSetAsList() + ")";
		}
	}

	private String getLangSetAsList() {
		return languageIn.stream().map(lang -> "\"" + lang + "\"").reduce((a, b) -> a + ", " + b).orElse("");
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.LanguageInConstraintComponent;
	}

	@Override
	public ConstraintComponent deepClone() {
		return new LanguageInConstraintComponent(this);
	}

	@Override
	Function<PlanNode, FilterPlanNode> getFilterAttacher() {
		return (parent) -> new LanguageInFilter(parent, lowerCaseLanguageIn, languageRanges);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		LanguageInConstraintComponent that = (LanguageInConstraintComponent) o;
		return lowerCaseLanguageIn.equals(that.lowerCaseLanguageIn);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), lowerCaseLanguageIn);
	}

	@Override
	public String toString() {
		return "LanguageInPropertyShape{" +
				"languageIn=" + Arrays.toString(languageIn.toArray()) +
				", lowerCaseLanguageIn=" + Arrays.toString(lowerCaseLanguageIn.toArray()) +
				", id=" + getId() +
				'}';
	}

}
