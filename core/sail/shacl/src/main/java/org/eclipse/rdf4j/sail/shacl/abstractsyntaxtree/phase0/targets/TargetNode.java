package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.targets;

import java.util.Set;
import java.util.TreeSet;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

public class TargetNode extends Target {
	private final TreeSet<Value> targetNode;

	public TargetNode(TreeSet<Value> targetNode) {
		this.targetNode = targetNode;
		assert !this.targetNode.isEmpty();

	}

	@Override
	public IRI getPredicate() {
		return SHACL.TARGET_NODE;
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		targetNode.forEach(t -> {
			model.add(subject, getPredicate(), t);
		});
	}
}
