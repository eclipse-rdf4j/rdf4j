package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import java.util.Set;
import java.util.function.Function;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.DatatypeFilter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.FilterPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;

public class DatatypeConstraintComponent extends SimpleAbstractConstraintComponent {

	Resource datatype;

	public DatatypeConstraintComponent(Resource datatype) {
		this.datatype = datatype;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.DATATYPE, datatype);
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.DatatypeConstraintComponent;
	}

	@Override
	public ConstraintComponent deepClone() {
		return new DatatypeConstraintComponent(datatype);
	}

	@Override
	Function<PlanNode, FilterPlanNode> getFilterAttacher() {
		return (parent) -> new DatatypeFilter(parent, datatype);
	}

	@Override
	String getSparqlFilterExpression(String varName, boolean negated) {
		if (negated) {
			return "isLiteral(?" + varName + ") && datatype(?" + varName + ") = <" + datatype.stringValue() + ">";
		} else {
			return "!isLiteral(?" + varName + ") || datatype(?" + varName + ") != <" + datatype.stringValue() + ">";
		}
	}
}
