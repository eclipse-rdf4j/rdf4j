/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.model;

import java.io.Serializable;

/**
 * A namespace, consisting of a namespace name and a prefix that has been assigned to it.
 *
 * @implNote In order to ensure interoperability of concrete classes implementing this interface,
 *           {@link #equals(Object)} and {@link #hashCode()} methods must be implemented exactly as described in their
 *           specs.
 */
public interface Namespace extends Serializable, Comparable<Namespace> {

	/**
	 * Gets the prefix of the current namespace. The default namespace is represented by an empty prefix string.
	 *
	 * @return prefix of namespace, or an empty string in case of the default namespace.
	 */
	String getPrefix();

	/**
	 * Gets the name of the current namespace (i.e. its IRI).
	 *
	 * @return name of namespace
	 */
	String getName();

	/**
	 * Compares this namespace to another object.
	 *
	 * @param o The object to compare this namespace to
	 *
	 * @return {@code true} if the other object is an instance of {@code Namespace} and their {@linkplain #getPrefix()
	 *         prefixes} and {@linkplain #getName() names} are equal, {@code false} otherwise.
	 */
	@Override
	boolean equals(Object o);

	/**
	 * Computes the hash code of this namespace.
	 *
	 * @return a hash code for this namespace computed as {@link java.util.Objects#hash Objects.hash}(
	 *         {@link #getPrefix()}, {@link #getName()})
	 */
	@Override
	int hashCode();

}
