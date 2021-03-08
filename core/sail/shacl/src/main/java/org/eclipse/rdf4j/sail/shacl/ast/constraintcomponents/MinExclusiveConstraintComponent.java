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

public class MinExclusiveConstraintComponent extends SimpleAbstractConstraintComponent {

	Literal minExclusive;

	public MinExclusiveConstraintComponent(Literal minExclusive) {
		this.minExclusive = minExclusive;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection,
			Set<Resource> rdfListDedupe) {
		model.add(subject, SHACL.MIN_EXCLUSIVE, minExclusive);
	}

	@Override
	String getSparqlFilterExpression(String varName, boolean negated) {
		if (negated) {
			return literalToString(minExclusive) + " < ?" + varName;
		} else {
			return literalToString(minExclusive) + " >= ?" + varName + "";
		}
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.MinExclusiveConstraintComponent;
	}

	@Override
	public ConstraintComponent deepClone() {
		return new MinExclusiveConstraintComponent(minExclusive);
	}

	@Override
	Function<PlanNode, FilterPlanNode> getFilterAttacher() {
		return (parent) -> new LiteralComparatorFilter(parent, minExclusive, Compare.CompareOp.LT);
	}
}
