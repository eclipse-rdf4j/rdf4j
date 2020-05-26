/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model;

/**
 * A Uniform Resource Identifier (URI).
 *
 * @deprecated since 2.0. Use {@link IRI} instead.
 * @author Jeen Broekstra
 * @author Arjohn Kampman
 * @see <a href="http://tools.ietf.org/html/rfc3986">RFC 3986</a>
 */
@Deprecated
public interface URI extends Resource {

	/**
	 * Returns the String-representation of this URI.
	 *
	 * @return The String-representation of this URI.
	 */
	@Override
	public String toString();

	/**
	 * Gets the namespace part of this URI. The namespace is defined as per the algorithm described in the class
	 * documentation.
	 *
	 * @return The URI's namespace.
	 */
	public String getNamespace();

	/**
	 * Gets the local name part of this URI. The local name is defined as per the algorithm described in the class
	 * documentation.
	 *
	 * @return The URI's local name.
	 */
	public String getLocalName();

	/**
	 * Compares a URI object to another object.
	 *
	 * @param o The object to compare this URI to.
	 * @return <tt>true</tt> if the other object is an instance of {@link URI} and their String-representations are
	 *         equal, <tt>false</tt> otherwise.
	 */
	@Override
	public boolean equals(Object o);

	/**
	 * The hash code of an URI is defined as the hash code of its String-representation: <tt>toString().hashCode</tt>.
	 *
	 * @return A hash code for the URI.
	 */
	@Override
	public int hashCode();

}
