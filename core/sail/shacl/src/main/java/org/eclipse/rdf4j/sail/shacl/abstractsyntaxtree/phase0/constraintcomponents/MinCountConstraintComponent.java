package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents;

import org.eclipse.rdf4j.model.Resource;

public class MinCountConstraintComponent implements ConstraintComponent {

	long minCount;

	public MinCountConstraintComponent(long minCount) {
		this.minCount = minCount;
	}
}
