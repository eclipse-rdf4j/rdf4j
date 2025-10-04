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
 * The supertype of all RDF model objects (URIs, blank nodes and literals).
 */
public interface Value extends Serializable {

	enum Type {
		IRI,
		BNode,
		Literal,
		Triple
	}

	/**
	 * Check if the object is an instance of the given type. Typically 2x than using instanceof.
	 * <p>
	 * For implementers: This default implementation is overridden in the repsective sub-interface.
	 *
	 * @return true if instance of {@link BNode}
	 */
	default boolean isBNode() {
		return false;
	}

	/**
	 * Check if the object is an instance of the given type. Typically 2x than using instanceof.
	 * <p>
	 * For implementers: This default implementation is overridden in the repsective sub-interface.
	 *
	 * @return true if instance of {@link IRI}
	 */
	default boolean isIRI() {
		return false;
	}

	/**
	 * Check if the object is an instance of the given type. Typically 2x than using instanceof.
	 * <p>
	 * For implementers: This default implementation is overridden in the repsective sub-interface.
	 *
	 * @return true if instance of {@link Resource}
	 */
	default boolean isResource() {
		return false;
	}

	/**
	 * Check if the object is an instance of the given type. Typically 2x than using instanceof.
	 * <p>
	 * For implementers: This default implementation is overridden in the repsective sub-interface.
	 *
	 * @return true if instance of {@link Literal}
	 */
	default boolean isLiteral() {
		return false;
	}

	/**
	 * Check if the object is an instance of the given type. Typically 2x than using instanceof.
	 * <p>
	 * For implementers: This default implementation is overridden in the repsective sub-interface.
	 *
	 * @return true if instance of {@link Triple}
	 */
	default boolean isTriple() {
		return false;
	}

	default Type getType() {

		if (isIRI()) {
			return Type.IRI;
		} else if (isBNode()) {
			return Type.BNode;
		} else if (isLiteral()) {
			return Type.Literal;
		} else if (isTriple()) {
			return Type.Triple;
		} else {
			throw new IllegalStateException("Unknown value type");
		}
	}

	/**
	 * Returns the String-value of a <var>Value</var> object. This returns either a {@link Literal}'s label, a
	 * {@link IRI}'s URI or a {@link BNode}'s ID.
	 */
	String stringValue();

}
