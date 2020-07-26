package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents;

import java.util.Set;
import java.util.function.Function;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.ShaclFeatureUnsupportedException;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.FilterPlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.LiteralComparatorFilter;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;

public class MinInclusiveConstraintComponent extends SimpleAbstractConstraintComponent {

	Literal minInclusive;

	public MinInclusiveConstraintComponent(Literal minInclusive) {
		this.minInclusive = minInclusive;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.MIN_INCLUSIVE, minInclusive);
	}

	@Override
	String getFilter(String varName, boolean negated) {
		throw new ShaclFeatureUnsupportedException();
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.MinInclusiveConstraintComponent;
	}

	@Override
	Function<PlanNode, FilterPlanNode> getFilterAttacher() {
		return (parent) -> new LiteralComparatorFilter(parent, minInclusive, value -> value <= 0);
	}
}
