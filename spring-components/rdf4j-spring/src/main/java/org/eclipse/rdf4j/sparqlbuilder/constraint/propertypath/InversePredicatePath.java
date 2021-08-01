package org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath;

import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;

public class InversePredicatePath implements PredicatePathOrInversePredicatePath {
	private final Iri predicate;

	public InversePredicatePath(Iri predicate) {
		this.predicate = predicate;
	}

	@Override
	public String getQueryString() {
		return "^ " + predicate.getQueryString();
	}
}
