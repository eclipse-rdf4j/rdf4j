package org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath;

import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;

public class PredicatePath implements PredicatePathOrInversePredicatePath {
	private final Iri predicate;

	public PredicatePath(Iri predicate) {
		this.predicate = predicate;
	}

	@Override
	public String getQueryString() {
		return predicate.getQueryString();
	}
}
