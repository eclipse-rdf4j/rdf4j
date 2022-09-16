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

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.Iterator;
import java.util.Set;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.impl.SimpleBinding;

/**
 * A simple binding set tuned for the use case that the ShaclSail has.
 */
public class SingletonBindingSet implements BindingSet {

	private static final long serialVersionUID = 6083219124988052038L;

	private final String name;
	private final Value value;

	private Binding cachedBinding;
	private int cachedHashCode = 0;

	public SingletonBindingSet(String bindingName, Value value) {
		this.name = bindingName;
		this.value = value;
	}

	@Override
	public Iterator<Binding> iterator() {
		if (cachedBinding == null) {
			cachedBinding = new SimpleBinding(name, value);
		}
		return new SingleIterator(cachedBinding);
	}

	private static class SingleIterator implements Iterator<Binding> {
		private Binding next;

		public SingleIterator(Binding next) {
			this.next = next;
		}

		@Override
		public boolean hasNext() {
			return next != null;
		}

		@Override
		public Binding next() {
			Binding temp = next;
			next = null;
			return temp;
		}
	}

	@Override
	public Set<String> getBindingNames() {
		return Set.of(name);
	}

	@Override
	public Binding getBinding(String bindingName) {
		if (this.name.equals(bindingName)) {
			if (cachedBinding == null) {
				cachedBinding = new SimpleBinding(name, value);
			}
			return cachedBinding;
		}
		return null;
	}

	@Override
	public boolean hasBinding(String bindingName) {
		return name.equals(bindingName);
	}

	@Override
	public Value getValue(String bindingName) {
		if (name.equals(bindingName)) {
			return value;
		}
		return null;
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public int hashCode() {
		if (cachedHashCode == 0) {
			cachedHashCode = (name.hashCode() ^ value.hashCode());
		}
		return cachedHashCode;
	}

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
