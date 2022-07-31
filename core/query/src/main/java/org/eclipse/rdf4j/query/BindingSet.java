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
import java.util.Iterator;
import java.util.Set;

import org.eclipse.rdf4j.model.Value;

/**
 * A BindingSet is a set of named value bindings, which is used a.o. to represent a single query solution. Values are
 * indexed by name of the binding which typically corresponds to the names of the variables used in the projection of
 * the orginal query.
 */
public interface BindingSet extends Iterable<Binding>, Serializable {

	/**
	 * Creates an iterator over the bindings in this BindingSet. This only returns bindings with non-null values. An
	 * implementation is free to return the bindings in arbitrary order.
	 */
	@Override
	Iterator<Binding> iterator();

	/**
	 * Gets the names of the bindings in this BindingSet.
	 *
	 * @return A set of binding names.
	 */
	Set<String> getBindingNames();

	/**
	 * Gets the binding with the specified name from this BindingSet.
	 *
	 * @param bindingName The name of the binding.
	 * @return The binding with the specified name, or <var>null</var> if there is no such binding in this BindingSet.
	 */
	Binding getBinding(String bindingName);

	/**
	 * Checks whether this BindingSet has a binding with the specified name.
	 *
	 * @param bindingName The name of the binding.
	 * @return <var>true</var> if this BindingSet has a binding with the specified name, <var>false</var> otherwise.
	 */
	boolean hasBinding(String bindingName);

	/**
	 * Gets the value of the binding with the specified name from this BindingSet.
	 *
	 * @param bindingName The name of the binding.
	 * @return The value of the binding with the specified name, or <var>null</var> if there is no such binding in this
	 *         BindingSet.
	 */
	Value getValue(String bindingName);

	/**
	 * Returns the number of bindings in this BindingSet.
	 *
	 * @return The number of bindings in this BindingSet.
	 */
	int size();

	/**
	 * Compares a BindingSet object to another object.
	 *
	 * @param o The object to compare this binding to.
	 * @return <var>true</var> if the other object is an instance of {@link BindingSet} and it contains the same set of
	 *         bindings (disregarding order), <var>false</var> otherwise.
	 */
	@Override
	boolean equals(Object o);

	/**
	 * The hash code of a binding is defined as the bit-wise XOR of the hash codes of its bindings:
	 *
	 * <pre>
	 * int hashCode = 0;
	 *
	 * for (Binding binding : this) {
	 * 	hashCode &circ;= binding.getName().hashCode() &circ; binding.getValue().hashCode();
	 * }
	 * </pre>
	 *
	 * Note: the calculated hash code intentionally does not depend on the order in which the bindings are iterated
	 * over.
	 *
	 * @return A hash code for the BindingSet.
	 */
	@Override
	int hashCode();

	default boolean isEmpty() {
		return size() == 0;
	}
}
