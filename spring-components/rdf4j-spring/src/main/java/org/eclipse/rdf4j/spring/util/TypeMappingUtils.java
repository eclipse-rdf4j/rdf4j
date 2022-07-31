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

package org.eclipse.rdf4j.spring.util;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class TypeMappingUtils {
	public static Iri toIri(IRI from) {
		return Rdf.iri(from.toString());
	}

	public static List<Iri> toIri(Collection<IRI> from) {
		return from.stream().map(e -> Rdf.iri(e.toString())).collect(Collectors.toList());
	}

	public static Iri[] toIriArray(Collection<IRI> from) {
		return toIri(from).toArray(new Iri[from.size()]);
	}

	public static IRI toIRI(String from) {
		return toIRIOptional(from)
				.orElseThrow(() -> new NullPointerException("iriString must not be null"));
	}

	public static IRI toIRIMaybe(String from) {
		return toIRIOptional(from).orElse(null);
	}

	public static Optional<IRI> toIRIOptional(String from) {
		return Optional.ofNullable(from).map(s -> SimpleValueFactory.getInstance().createIRI(s));
	}

	public static IRI toIRI(Value from) {
		return SimpleValueFactory.getInstance().createIRI(from.stringValue());
	}

	public static Boolean toBoolean(Value from) {
		return ((Literal) from).booleanValue();
	}

	public static Boolean toBooleanMaybe(Value from) {
		return (from == null) ? null : toBoolean(from);
	}

	public static Optional<Boolean> toBooleanOptional(Value from) {
		return Optional.ofNullable(from).map(TypeMappingUtils::toBoolean);
	}

	public static Boolean toBoolean(String from) {
		Objects.requireNonNull(from);
		return Boolean.valueOf(from);
	}

	public static Boolean toBooleanMaybe(String from) {
		return (from == null) ? null : toBoolean(from);
	}

	public static Boolean toBooleanMaybe(Boolean from) {
		return from;
	}

	public static Optional<Boolean> toBooleanOptional(String from) {
		return Optional.ofNullable(from).map(TypeMappingUtils::toBoolean);
	}

	public static Optional<Boolean> toBooleanOptional(Boolean from) {
		return Optional.ofNullable(from);
	}

	public static Double toDouble(Value from) {
		return ((Literal) from).doubleValue();
	}

	public static BigInteger toInteger(Value from) {
		return ((Literal) from).integerValue();
	}

	public static Integer toInt(Value from) {
		return ((Literal) from).intValue();
	}

	public static List<IRI> toIRI(Collection<String> from) {
		return from.stream().map(TypeMappingUtils::toIRI).collect(Collectors.toList());
	}

	public static final String toString(Value from) {
		return from.toString();
	}
}
