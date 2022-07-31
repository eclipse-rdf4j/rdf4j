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

import java.util.Iterator;

import org.eclipse.rdf4j.model.Value;

/**
 * Abstract base class for {@link BindingSet} implementations, providing a.o. consistent implementations of
 * {@link BindingSet#equals(Object)} and {@link BindingSet#hashCode()}.
 *
 * @author Jeen Broekstra
 */
public abstract class AbstractBindingSet implements BindingSet {

	private static final long serialVersionUID = -2594123329154106048L;

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}

		if (!(other instanceof BindingSet)) {
			return false;
		}

		BindingSet that = (BindingSet) other;

		if (this.size() != that.size()) {
			return false;
		}

		if (this.size() == 1) {
			Binding binding = iterator().next();
			Binding thatBinding = that.iterator().next();

			return binding.getName().equals(thatBinding.getName()) && binding.getValue().equals(thatBinding.getValue());
		}

		// Compare other's bindings to own
		for (Binding binding : that) {
			Value ownValue = getValue(binding.getName());

			if (!binding.getValue().equals(ownValue)) {
				// Unequal bindings for this name
				return false;
			}
		}

		return true;
	}

	@Override
	public final int hashCode() {
		int hashCode = 0;

		for (Binding binding : this) {
			hashCode ^= binding.getName().hashCode() ^ binding.getValue().hashCode();
		}
		return hashCode;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(32 * size());

		sb.append('[');

		Iterator<Binding> iter = iterator();
		while (iter.hasNext()) {
			sb.append(iter.next().toString());
			if (iter.hasNext()) {
				sb.append(';');
			}
		}

		sb.append(']');

		return sb.toString();
	}

}
