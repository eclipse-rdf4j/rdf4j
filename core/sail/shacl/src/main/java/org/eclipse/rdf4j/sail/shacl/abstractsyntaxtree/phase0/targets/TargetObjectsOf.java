package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.targets;

import org.eclipse.rdf4j.model.IRI;

import java.util.Set;

public class TargetObjectsOf extends Target {

	private final Set<IRI> targetObjectsOf;

	public TargetObjectsOf(Set<IRI> targetObjectsOf) {
		this.targetObjectsOf = targetObjectsOf;
		assert !this.targetObjectsOf.isEmpty();

	}

}
