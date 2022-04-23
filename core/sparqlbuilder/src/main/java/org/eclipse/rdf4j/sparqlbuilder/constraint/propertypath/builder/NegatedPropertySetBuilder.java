/*
 * ******************************************************************************
 *  * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available under the terms of the Eclipse Distribution License v1.0
 *  * which accompanies this distribution, and is available at
 *  * http://www.eclipse.org/org/documents/edl-v10.php.
 *  ******************************************************************************
 */

package org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.builder;

import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.InversePredicatePath;
import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.NegatedPropertySet;
import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.PredicatePath;
import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.PredicatePathOrInversePredicatePath;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class NegatedPropertySetBuilder {
	private final List<PredicatePathOrInversePredicatePath> propertySet = new ArrayList<>();

	public NegatedPropertySetBuilder pred(IRI predicate) {
		return pred(iri(predicate));
	}

	public NegatedPropertySetBuilder pred(Iri predicate) {
		propertySet.add(new PredicatePath(predicate));
		return this;
	}

	public NegatedPropertySetBuilder invPred(IRI predicate) {
		return invPred(iri(predicate));
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
