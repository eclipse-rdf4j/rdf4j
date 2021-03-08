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

public class MaxExclusiveConstraintComponent extends SimpleAbstractConstraintComponent {

	Literal maxExclusive;

	public MaxExclusiveConstraintComponent(Literal maxExclusive) {
		this.maxExclusive = maxExclusive;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection,
			Set<Resource> rdfListDedupe) {
		model.add(subject, SHACL.MAX_EXCLUSIVE, maxExclusive);
	}

	@Override
	String getSparqlFilterExpression(String varName, boolean negated) {
		if (negated) {
			return literalToString(maxExclusive) + " > ?" + varName;
		} else {
			return literalToString(maxExclusive) + " <= ?" + varName + "";
		}
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.MaxExclusiveConstraintComponent;
	}

	@Override
	public ConstraintComponent deepClone() {
		return new MaxExclusiveConstraintComponent(maxExclusive);
	}

	@Override
	Function<PlanNode, FilterPlanNode> getFilterAttacher() {
		return (parent) -> new LiteralComparatorFilter(parent, maxExclusive, Compare.CompareOp.GT);
	}

}
