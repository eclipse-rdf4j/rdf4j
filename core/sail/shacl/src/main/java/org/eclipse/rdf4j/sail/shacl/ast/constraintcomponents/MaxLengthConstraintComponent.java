package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import java.util.Set;
import java.util.function.Function;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.FilterPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.MaxLengthFilter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;

public class MaxLengthConstraintComponent extends SimpleAbstractConstraintComponent {

	long maxLength;

	public MaxLengthConstraintComponent(long maxLength) {
		this.maxLength = maxLength;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.MAX_LENGTH,
				SimpleValueFactory.getInstance().createLiteral(maxLength + "", XMLSchema.INTEGER));
	}

	@Override
	String getSparqlFilterExpression(String varName, boolean negated) {
		if (negated) {
			return "STRLEN(STR(?" + varName + ")) <= " + maxLength;
		} else {
			return "STRLEN(STR(?" + varName + ")) > " + maxLength;
		}
	}

	@Override
	Function<PlanNode, FilterPlanNode> getFilterAttacher() {
		return (parent) -> new MaxLengthFilter(parent, maxLength);
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.MaxLengthConstraintComponent;
	}

	@Override
	public ConstraintComponent deepClone() {
		return new MaxLengthConstraintComponent(maxLength);
	}
}
