package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents;

import java.util.Set;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

public class NodeKindConstraintComponent implements ConstraintComponent {

	Resource nodeKind;

	public NodeKindConstraintComponent(Resource nodeKind) {
		this.nodeKind = nodeKind;
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.NODE_KIND_PROP, nodeKind);
	}
}
