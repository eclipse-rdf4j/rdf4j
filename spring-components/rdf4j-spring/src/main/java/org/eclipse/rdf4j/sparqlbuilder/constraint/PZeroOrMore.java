/*
 * ******************************************************************************
 *  * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available under the terms of the Eclipse Distribution License v1.0
 *  * which accompanies this distribution, and is available at
 *  * http://www.eclipse.org/org/documents/edl-v10.php.
 *  ******************************************************************************
 */

package org.eclipse.rdf4j.sparqlbuilder.constraint;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfPredicate;

public class PZeroOrMore implements RdfPredicate {
	private IRI[] property;

	public PZeroOrMore(IRI... property) {
		Objects.requireNonNull(property);
		this.property = property;
	}

	@Override
	public String getQueryString() {
		return "("
				+ Arrays
						.stream(property)
						.map(elem -> "<" + elem.toString() + ">")
						.collect(Collectors.joining("|"))
				+ ")*";
	}
}
