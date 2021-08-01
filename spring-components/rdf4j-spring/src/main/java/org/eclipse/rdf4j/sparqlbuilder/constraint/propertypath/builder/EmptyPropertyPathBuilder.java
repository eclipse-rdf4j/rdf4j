package org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.builder;

import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.PropertyPath;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;

public class EmptyPropertyPathBuilder {
	private NegatedPropertySetBuilder negatedPropertySetBuilder = null;
	private PropertyPathBuilder propertyPathBuilder = null;

	public EmptyPropertyPathBuilder() {
	}

	public NegatedPropertySetBuilder negProp() {
		if (this.propertyPathBuilder != null || this.negatedPropertySetBuilder != null) {
			throw new IllegalStateException(
					"Only one call to either negProp() and pred() is allowed");
		}
		this.negatedPropertySetBuilder = new NegatedPropertySetBuilder();
		return negatedPropertySetBuilder;
	}

	public PropertyPathBuilder pred(Iri predicate) {
		if (this.propertyPathBuilder != null || this.negatedPropertySetBuilder != null) {
			throw new IllegalStateException(
					"Only one call to either negProp() and pred() is allowed");
		}
		this.propertyPathBuilder = new PropertyPathBuilder(predicate);
		return this.propertyPathBuilder;
	}

	public PropertyPath build() {
		if (this.propertyPathBuilder != null) {
			return this.propertyPathBuilder.build();
		} else if (this.negatedPropertySetBuilder != null) {
			return negatedPropertySetBuilder.build();
		}
		throw new IllegalStateException("Nothing built yet");
	}
}
