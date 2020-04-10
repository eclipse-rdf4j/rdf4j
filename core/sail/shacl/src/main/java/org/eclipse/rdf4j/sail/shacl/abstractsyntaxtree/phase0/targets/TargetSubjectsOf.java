package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.targets;

import org.eclipse.rdf4j.model.IRI;

import java.util.Set;

public class TargetSubjectsOf extends Target {

	private final Set<IRI> targetSubjectsOf;

	public TargetSubjectsOf(Set<IRI> targetSubjectsOf) {
		this.targetSubjectsOf = targetSubjectsOf;
		assert !this.targetSubjectsOf.isEmpty();

	}

}
