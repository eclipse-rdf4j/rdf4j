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
package org.eclipse.rdf4j.query;

import java.io.Serializable;

import org.eclipse.rdf4j.model.Value;

/**
 * A named value binding.
 */
public interface Binding extends Serializable {

	/**
	 * Gets the name of the binding (e.g. the variable name).
	 *
	 * @return The name of the binding.
	 */
	String getName();

	/**
	 * Gets the value of the binding. The returned value is never equal to <var>null</var>, such a "binding" is
	 * considered to be unbound.
	 *
	 * @return The value of the binding, never <var>null</var>.
	 */
	Value getValue();

	/**
	 * Compares a binding object to another object.
	 *
	 * @param o The object to compare this binding to.
	 * @return <var>true</var> if the other object is an instance of {@link Binding} and both their names and values are
	 *         equal, <var>false</var> otherwise.
	 */
	@Override
	boolean equals(Object o);

	/**
	 * The hash code of a binding is defined as the bit-wise XOR of the hash codes of its name and value:
	 *
	 * <pre>
	 * name.hashCode() &circ; value.hashCode()
	 * </pre>
	 *
	 * .
	 *
	 * @return A hash code for the binding.
	 */
	@Override
	int hashCode();
}
