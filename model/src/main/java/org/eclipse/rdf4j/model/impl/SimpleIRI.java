/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.impl;

import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.URIUtil;

/**
 * The default implementation of the {@link IRI} interface.
 */
public class SimpleIRI implements IRI {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final long serialVersionUID = -7330406348751485330L;

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The IRI string.
	 */
	private String iriString;

	/**
	 * An index indicating the first character of the local name in the IRI string, -1 if not yet set.
	 */
	private int localNameIdx;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new, un-initialized IRI. This IRI's string value needs to be {@link #setIRIString(String) set} before
	 * the normal methods can be used.
	 */
	protected SimpleIRI() {
	}

	/**
	 * Creates a new IRI from the supplied string.
	 * <p>
	 * Note that creating SimpleIRI objects directly via this constructor is not the recommended approach. Instead, use
	 * a {@link org.eclipse.rdf4j.model.ValueFactory ValueFactory} (obtained from your repository or by using
	 * {@link SimpleValueFactory#getInstance()}) to create new IRI objects.
	 * 
	 * @param iriString A String representing a valid, absolute IRI. May not be <code>null</code>.
	 * @throws IllegalArgumentException If the supplied IRI is not a valid (absolute) IRI.
	 * @see {@link SimpleValueFactory#createIRI(String)}
	 */
	protected SimpleIRI(String iriString) {
		setIRIString(iriString);
	}

	/*---------*
	 * Methods *
	 *---------*/

	protected void setIRIString(String iriString) {
		Objects.requireNonNull(iriString, "iriString must not be null");

		if (iriString.indexOf(':') < 0) {
			throw new IllegalArgumentException("Not a valid (absolute) IRI: " + iriString);
		}

		this.iriString = iriString;
		this.localNameIdx = -1;
	}

	// Implements IRI.toString()
	@Override
	public String toString() {
		return iriString;
	}

	@Override
	public String stringValue() {
		return iriString;
	}

	@Override
	public String getNamespace() {
		if (localNameIdx < 0) {
			localNameIdx = URIUtil.getLocalNameIndex(iriString);
		}

		return iriString.substring(0, localNameIdx);
	}

	@Override
	public String getLocalName() {
		if (localNameIdx < 0) {
			localNameIdx = URIUtil.getLocalNameIndex(iriString);
		}

		return iriString.substring(localNameIdx);
	}

	// Implements IRI.equals(Object)
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o instanceof IRI) {
			return toString().equals(o.toString());
		}

		return false;
	}

	// Implements IRI.hashCode()
	@Override
	public int hashCode() {
		return iriString.hashCode();
	}
}
