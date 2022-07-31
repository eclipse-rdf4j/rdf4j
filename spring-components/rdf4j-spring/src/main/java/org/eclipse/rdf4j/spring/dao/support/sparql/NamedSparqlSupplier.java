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

package org.eclipse.rdf4j.spring.dao.support.sparql;

import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.rdf4j.common.annotation.Experimental;

/**
 * Associates a String key with a {@link Supplier<String>} that provides a SPARQL operation.
 *
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
@Experimental
public class NamedSparqlSupplier {
	private final String name;
	private final Supplier<String> sparqlSupplier;

	public NamedSparqlSupplier(String name, Supplier<String> sparqlSupplier) {
		this.name = name;
		this.sparqlSupplier = sparqlSupplier;
	}

	public String getName() {
		return name;
	}

	public Supplier<String> getSparqlSupplier() {
		return sparqlSupplier;
	}

	public static NamedSparqlSupplier of(String key, Supplier<String> generator) {
		return new NamedSparqlSupplier(key, generator);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		NamedSparqlSupplier that = (NamedSparqlSupplier) o;
		return Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}
}
