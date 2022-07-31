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

/**
 * An Internationalized Resource Identifier (IRI). IRIs may contain characters from the Universal Character Set
 * (Unicode/ISO 10646), including Chinese or Japanese kanji, Korean, Cyrillic characters, and so forth. It is defined by
 * RFC 3987.
 * <p>
 * An IRI can be split into a namespace part and a local name part, which are derived from an IRI string by splitting it
 * in two using the following algorithm:
 * <ul>
 * <li>Split after the first occurrence of the '#' character,
 * <li>If this fails, split after the last occurrence of the '/' character,
 * <li>If this fails, split after the last occurrence of the ':' character.
 * </ul>
 * The last step should never fail as every legal (full) IRI contains at least one ':' character to separate the scheme
 * from the rest of the IRI. The implementation should check this upon object creation.
 *
 * @author Jeen Broekstra
 * @see <a href="http://tools.ietf.org/html/rfc3987">RFC 3987</a>
 *
 * @implNote In order to ensure interoperability of concrete classes implementing this interface,
 *           {@link #equals(Object)} and {@link #hashCode()} methods must be implemented exactly as described in their
 *           specs.
 */
public interface IRI extends Resource {

	@Override
	default boolean isIRI() {
		return true;
	}

	/**
	 * Gets the namespace part of this IRI.
	 * <p>
	 * The namespace is defined as per the algorithm described in the class documentation.
	 *
	 * @return the namespace of this IRI
	 */
	String getNamespace();

	/**
	 * Gets the local name part of this IRI.
	 * <p>
	 * The local name is defined as per the algorithm described in the class documentation.
	 *
	 * @return the local name of this IRI
	 */
	String getLocalName();

	/**
	 * Compares this IRI to another object.
	 *
	 * @param o the object to compare this IRI to
	 *
	 * @return {@code true}, if the other object is an instance of {@code IRI} and their {@linkplain #stringValue()
	 *         string values} are equal; {@code false}, otherwise
	 */
	@Override
	boolean equals(Object o);

	/**
	 * Computes the hash code of this IRI.
	 *
	 * @return a hash code for this IRI computed as {@link #stringValue()}{@code .hashCode()}
	 */
	@Override
	int hashCode();

}
