package org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.sparqlbuilder.core.QueryElement;

public class NegatedPropertySet implements PropertyPath {
	private final PredicatePathOrInversePredicatePath[] properties;

	public NegatedPropertySet(PredicatePathOrInversePredicatePath... properties) {
		this.properties = properties;
	}

	@Override
	public String getQueryString() {
		if (properties.length == 1) {
			return "! " + properties[0].getQueryString();
		} else {
			return Arrays
					.stream(properties)
					.map(QueryElement::getQueryString)
					.collect(Collectors.joining("|", "! ( ", " )"));
		}
	}
}
