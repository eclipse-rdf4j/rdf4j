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

package org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.sparqlbuilder.core.QueryElement;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
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
					.collect(Collectors.joining(" | ", "! ( ", " )"));
		}
	}
}
