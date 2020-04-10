package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.targets;

import org.eclipse.rdf4j.model.Value;

import java.util.TreeSet;

public class TargetNode extends Target {
	private final TreeSet<Value> targetNode;

	public TargetNode(TreeSet<Value> targetNode) {
		this.targetNode = targetNode;
		assert !this.targetNode.isEmpty();

	}

}
