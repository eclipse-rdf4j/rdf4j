
/*
 * ******************************************************************************
 *  * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  *
 * All rights reserved. This program and the accompanying materials
 *  * are made available under the terms of the Eclipse Distribution License v1.0
 *  * which accompanies this distribution, and is available at
 *  * http://www.eclipse.org/org/documents/edl-v10.php.
 *  ******************************************************************************
 */

package org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.builder;

import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.PropertyPath;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class EmptyPropertyPathBuilder {
	private NegatedPropertySetBuilder negatedPropertySetBuilder = null;
	private PropertyPathBuilder propertyPathBuilder = null;

	public EmptyPropertyPathBuilder() {
	}

	/**
	 * Start a path that will be enclosed with <code>`! (` and `)`</code>.
	 */
	public NegatedPropertySetBuilder negProp() {
		if (this.propertyPathBuilder != null || this.negatedPropertySetBuilder != null) {
			throw new IllegalStateException(
					"Only one call to either negProp() and pred() is allowed");
		}
		this.negatedPropertySetBuilder = new NegatedPropertySetBuilder();
		return negatedPropertySetBuilder;
	}

	/**
	 * Start the path with <code>predicate</code>.
	 */
	public PropertyPathBuilder pred(IRI predicate) {
		return pred(iri(predicate));
	}

	/**
	 * Start the path with <code>predicate</code>.
	 */
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
