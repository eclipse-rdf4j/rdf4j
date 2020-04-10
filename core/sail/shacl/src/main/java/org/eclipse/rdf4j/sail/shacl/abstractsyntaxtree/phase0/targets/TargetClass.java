package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.targets;

import org.eclipse.rdf4j.model.Resource;

import java.util.Set;

public class TargetClass extends Target {
	private final Set<Resource> targetClass;

	public TargetClass(Set<Resource> targetClass) {
		this.targetClass = targetClass;
		assert !this.targetClass.isEmpty();

	}

}
