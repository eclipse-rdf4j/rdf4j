/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath;

import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;

/**
 * @author Florian Kleedorfer
 * @since 4.0.0
 */
public class PredicatePath implements PredicatePathOrInversePredicatePath {
	private final Iri predicate;

	public PredicatePath(Iri predicate) {
		this.predicate = predicate;
	}

	public PredicatePath(IRI predicate) {
		this(iri(predicate));
	}

	@Override
	public String getQueryString() {
		return predicate.getQueryString();
	}
}
