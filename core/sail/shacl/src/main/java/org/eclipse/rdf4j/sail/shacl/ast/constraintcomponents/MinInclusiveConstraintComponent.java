package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import java.util.Set;
import java.util.function.Function;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.FilterPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.LiteralComparatorFilter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;

public class MinInclusiveConstraintComponent extends SimpleAbstractConstraintComponent {

	Literal minInclusive;

	public MinInclusiveConstraintComponent(Literal minInclusive) {
		this.minInclusive = minInclusive;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.MIN_INCLUSIVE, minInclusive);
	}

	@Override
	String getSparqlFilterExpression(String varName, boolean negated) {
		if (negated) {
			return literalToString(minInclusive) + " <= ?" + varName;
		} else {
			return literalToString(minInclusive) + " > ?" + varName + "";
		}
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.MinInclusiveConstraintComponent;
	}

	@Override
	public ConstraintComponent deepClone() {
		return new MinInclusiveConstraintComponent(minInclusive);
	}

	@Override
	Function<PlanNode, FilterPlanNode> getFilterAttacher() {
		return (parent) -> new LiteralComparatorFilter(parent, minInclusive, Compare.CompareOp.LE);
	}
}
