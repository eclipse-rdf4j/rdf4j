package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.HelperTool;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.FilterPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValueInFilter;

public class InConstraintComponent extends SimpleAbstractConstraintComponent {

	private final Set<Value> in;

	public InConstraintComponent(RepositoryConnection connection, Resource in) {
		super(in);
		this.in = Collections.unmodifiableSet(new LinkedHashSet<>(HelperTool.toList(connection, in, Value.class)));
	}

	public InConstraintComponent(InConstraintComponent inConstraintComponent) {
		super(inConstraintComponent.getId());
		in = inConstraintComponent.in;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection,
			Set<Resource> rdfListDedupe) {
		model.add(subject, SHACL.IN, getId());
		if (!rdfListDedupe.contains(getId())) {
			rdfListDedupe.add(getId());
			HelperTool.listToRdf(in, getId(), model);
		}

	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.InConstraintComponent;
	}

	@Override
	public ConstraintComponent deepClone() {
		return new InConstraintComponent(this);
	}

	@Override
	String getSparqlFilterExpression(String varName, boolean negated) {
		if (negated) {
			return "?" + varName + " IN (" + getInSetAsString() + ")";
		} else {
			return "?" + varName + " NOT IN (" + getInSetAsString() + ")";
		}
	}

	private String getInSetAsString() {
		return in.stream()
				.map(targetNode -> {
					if (targetNode.isResource()) {
						return "<" + targetNode + ">";
					}
					if (targetNode.isLiteral()) {
						IRI datatype = ((Literal) targetNode).getDatatype();
						if (datatype == null) {
							return "\"" + targetNode.stringValue() + "\"";
						}
						if (((Literal) targetNode).getLanguage().isPresent()) {
							return "\"" + targetNode.stringValue() + "\"@" + ((Literal) targetNode).getLanguage().get();
						}
						return "\"" + targetNode.stringValue() + "\"^^<" + datatype.stringValue() + ">";
					}

					throw new IllegalStateException(targetNode.getClass().getSimpleName());

				})
				.reduce((a, b) -> a + ", " + b)
				.orElse("");
	}

	@Override
	Function<PlanNode, FilterPlanNode> getFilterAttacher() {
		return (parent) -> new ValueInFilter(parent, in);
	}

}
