package org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.builder;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.InversePredicatePath;
import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.NegatedPropertySet;
import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.PredicatePath;
import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.PredicatePathOrInversePredicatePath;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;

public class NegatedPropertySetBuilder {
	private List<PredicatePathOrInversePredicatePath> propertySet = new ArrayList<>();

	public NegatedPropertySetBuilder pred(Iri predicate) {
		propertySet.add(new PredicatePath(predicate));
		return this;
	}

	public NegatedPropertySetBuilder invPred(Iri predicate) {
		propertySet.add(new InversePredicatePath(predicate));
		return this;
	}

	public NegatedPropertySet build() {
		return new NegatedPropertySet(
				propertySet.toArray(new PredicatePathOrInversePredicatePath[0]));
	}
}
