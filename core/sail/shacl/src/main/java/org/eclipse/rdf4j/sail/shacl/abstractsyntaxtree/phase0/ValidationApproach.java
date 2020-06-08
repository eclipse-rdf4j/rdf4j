package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0;

public enum ValidationApproach {

	Transactional,
	SPARQL;

	public static ValidationApproach reduce(ValidationApproach a, ValidationApproach b) {
		if (a == SPARQL)
			return a;
		if (b == SPARQL)
			return b;

		return a;
	}
}
