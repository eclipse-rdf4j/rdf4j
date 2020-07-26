package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents;

import java.util.Set;
import java.util.function.Function;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.ShaclFeatureUnsupportedException;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.FilterPlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.NodeKindFilter;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;

public class NodeKindConstraintComponent extends SimpleAbstractConstraintComponent {

	NodeKind nodeKind;

	public NodeKindConstraintComponent(Resource nodeKind) {
		this.nodeKind = NodeKind.from(nodeKind);
	}

	@Override
	String getFilter(String varName, boolean negated) {
		throw new ShaclFeatureUnsupportedException();
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.NODE_KIND_PROP, nodeKind.iri);
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.NodeKindConstraintComponent;
	}

	@Override
	Function<PlanNode, FilterPlanNode> getFilterAttacher() {
		return (parent) -> new NodeKindFilter(parent, nodeKind);
	}

	public enum NodeKind {

		BlankNode(SHACL.BLANK_NODE),
		IRI(SHACL.IRI),
		Literal(SHACL.LITERAL),
		BlankNodeOrIRI(SHACL.BLANK_NODE_OR_IRI),
		BlankNodeOrLiteral(SHACL.BLANK_NODE_OR_LITERAL),
		IRIOrLiteral(SHACL.IRI_OR_LITERAL),
		;

		IRI iri;

		NodeKind(IRI iri) {
			this.iri = iri;
		}

		public static NodeKind from(Resource resource) {
			for (NodeKind value : NodeKind.values()) {
				if (value.iri.equals(resource)) {
					return value;
				}
			}

			throw new IllegalStateException("Unknown nodeKind: " + resource);
		}
	}
}
