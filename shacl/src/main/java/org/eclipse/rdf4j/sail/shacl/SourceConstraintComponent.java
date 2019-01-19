package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

public enum SourceConstraintComponent {
	MaxCountConstraintComponent(SHACL.MAX_COUNT_CONSTRAINT_COMPONENT),
	DatatypeConstraintComponent(SHACL.DATATYPE_CONSTRAINT_COMPONENT),
	OrConstraintComponent(SHACL.OR_CONSTRAINT_COMPONENT),
	MinCountConstraintComponent(SHACL.MIN_COUNT_CONSTRAINT_COMPONENT);

	private final IRI iri;

	SourceConstraintComponent(IRI iri) {
		this.iri = iri;
	}

	public IRI getIri() {
		return iri;
	}

}
