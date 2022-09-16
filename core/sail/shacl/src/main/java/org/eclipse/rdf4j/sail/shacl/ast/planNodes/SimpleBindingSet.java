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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.impl.SimpleBinding;

/**
 * A simple binding set tuned for the use case that the ShaclSail has.
 */
public class SimpleBindingSet implements BindingSet {

	private static final long serialVersionUID = 1001660194269450975L;

	private final Set<String> bindingNamesSet;
	private final Binding[] bindings;

	private int cachedHashCode = 0;

	public SimpleBindingSet(Set<String> bindingNamesSet, List<String> varNamesList, List<Value> values) {
		assert varNamesList.size() == values.size();
		this.bindingNamesSet = bindingNamesSet;
		this.bindings = new Binding[varNamesList.size()];

		for (int i = 0; i < varNamesList.size(); i++) {
			bindings[i] = new SimpleBinding(varNamesList.get(i), values.get(i));
		}

	}

	public SimpleBindingSet(Set<String> bindingNamesSet, Binding[] bindings) {
		this.bindingNamesSet = bindingNamesSet;
		this.bindings = bindings;
	}

	@Override
	public Iterator<Binding> iterator() {
		return Arrays.asList(bindings).iterator();
	}

	@Override
	public Set<String> getBindingNames() {
		return bindingNamesSet;
	}

	@Override
	public Binding getBinding(String bindingName) {
		for (Binding binding : bindings) {
			if (binding.getName().equals(bindingName)) {
				return binding;
			}
		}
		return null;
	}

	@Override
	public boolean hasBinding(String bindingName) {
		return bindingNamesSet.contains(bindingName);
	}

	@Override
	public Value getValue(String bindingName) {
		Binding binding = getBinding(bindingName);
		if (binding != null) {
			return binding.getValue();
		}
		return null;
	}

	@Override
	public int size() {
		return bindings.length;
	}

	@Override
	public int hashCode() {
		if (cachedHashCode == 0) {
			int hashCode = 0;

			for (Binding binding : bindings) {
				hashCode ^= binding.getName().hashCode() ^ binding.getValue().hashCode();
			}
			cachedHashCode = hashCode;
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
