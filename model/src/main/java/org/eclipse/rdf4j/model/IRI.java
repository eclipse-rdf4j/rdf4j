/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model;

/**
 * An Internationalized Resource Identifier (IRI). IRIs are an extension of the existing {@link URI}: while
 * URIs are limited to a subset of the ASCII character set, IRIs may contain characters from the Universal
 * Character Set (Unicode/ISO 10646), including Chinese or Japanese kanji, Korean, Cyrillic characters, and so
 * forth. It is defined by RFC 3987.
 * <p>
 * An IRI can be split into a namespace part and a local name part, which are derived from an IRI string by
 * splitting it in two using the following algorithm:
 * <ul>
 * <li>Split after the first occurrence of the '#' character,
 * <li>If this fails, split after the last occurrence of the '/' character,
 * <li>If this fails, split after the last occurrence of the ':' character.
 * </ul>
 * The last step should never fail as every legal (full) IRI contains at least one ':' character to seperate
 * the scheme from the rest of the IRI. The implementation should check this upon object creation.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc3987">RFC 3987</a>
 * @author Jeen Broekstra
 */
@SuppressWarnings("deprecation")
public interface IRI extends URI, Resource {

	/**
	 * Returns the String-representation of this IRI.
	 * 
	 * @return The String-representation of this IRI.
	 */
	@Override
	public String toString();

	/**
	 * Gets the namespace part of this IRI. The namespace is defined as per the algorithm described in the
	 * class documentation.
	 * 
	 * @return The IRI's namespace.
	 */
	@Override
	public String getNamespace();

	/**
	 * Gets the local name part of this IRI. The local name is defined as per the algorithm described in the
	 * class documentation.
	 * 
	 * @return The IRI's local name.
	 */
	@Override
	public String getLocalName();

	/**
	 * Compares a IRI object to another object.
	 * 
	 * @param o
	 *        The object to compare this IRI to.
	 * @return <tt>true</tt> if the other object is an instance of {@link IRI} and their
	 *         String-representations are equal, <tt>false</tt> otherwise.
	 */
	@Override
	public boolean equals(Object o);

	/**
	 * The hash code of an IRI is defined as the hash code of its String-representation:
	 * <tt>toString().hashCode</tt>.
	 * 
	 * @return A hash code for the IRI.
	 */
	@Override
	public int hashCode();
}
