package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.HelperTool;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.FilterPlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ValueInFilter;

public class InConstraintComponent extends SimpleAbstractConstraintComponent {

	private final Set<Value> in;

	public InConstraintComponent(RepositoryConnection connection, Resource in) {
		super(in);
		this.in = new HashSet<>(HelperTool.toList(connection, in, Value.class));
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.IN, getId());
		TreeSet<Value> values = new TreeSet<>(new ValueComparator());
		values.addAll(in);
		HelperTool.listToRdf(values, getId(), model);
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.InConstraintComponent;
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
					if (targetNode instanceof Resource) {
						return "<" + targetNode + ">";
					}
					if (targetNode instanceof Literal) {
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
