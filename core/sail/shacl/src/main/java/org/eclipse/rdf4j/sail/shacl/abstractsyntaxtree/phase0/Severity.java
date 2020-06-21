package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

public enum Severity {
	Info(SHACL.INFO),
	Warning(SHACL.WARNING),
	Violation(SHACL.VIOLATION);

	Value iri;

	Severity(Value iri) {
		this.iri = iri;
	}

	public Value getIri() {
		return iri;
	}
}
